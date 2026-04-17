package com.analytics.app.service;

import com.analytics.app.model.FinanceMarketTick;
import com.analytics.app.model.FinancePrediction;
import com.analytics.app.repository.FinanceMarketTickRepository;
import com.analytics.app.repository.FinancePredictionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_PARTITION_ID;
import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC;

@Service
@Transactional
@ConditionalOnProperty(value = "analytics.finance.enabled", havingValue = "true")
public class FinanceStreamProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(FinanceStreamProcessingService.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FinanceMarketTickRepository financeMarketTickRepository;

    @Autowired
    private FinancePredictionRepository financePredictionRepository;

    @Autowired
    private FinanceMLService financeMLService;

    @Autowired
    private PredictionCacheService predictionCacheService;

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${analytics.finance.anomaly-threshold:2.0}")
    private double anomalyThreshold;

    @Value("${kafka.topics.finance-predictions:finance-predictions}")
    private String financePredictionsTopic;

    @KafkaListener(topics = "${kafka.topics.finance-data:finance-market-data}", groupId = "analytics-group")
    public void processFinanceStream(
            @Payload String message,
            @Header(RECEIVED_TOPIC) String topic,
            @Header(RECEIVED_PARTITION_ID) int partition,
            Acknowledgment acknowledgment
    ) {
        try {
            logger.debug("Processing finance tick from topic: {}, partition: {}", topic, partition);
            FinanceMarketTick tick = objectMapper.readValue(message, FinanceMarketTick.class);
            processTick(tick);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            logger.error("Error processing finance stream message: {}", e.getMessage());
        }
    }

    public FinancePrediction processTick(FinanceMarketTick tick) {
        if (tick.getEventTimestamp() == null) {
            tick.setEventTimestamp(LocalDateTime.now());
        }
        if (tick.getIngestedAt() == null) {
            tick.setIngestedAt(LocalDateTime.now());
        }

        FinanceMarketTick savedTick = financeMarketTickRepository.save(tick);
        FinancePrediction prediction = financeMLService.analyzeTick(savedTick);
        FinancePrediction savedPrediction = financePredictionRepository.save(prediction);

        predictionCacheService.cachePrediction(savedPrediction);
        publishPrediction(savedPrediction);

        Map<String, Object> financePayload = buildFinancePayload(savedTick, savedPrediction);
        webSocketService.sendFinanceUpdate(financePayload);

        if (savedPrediction.getAnomalyScore() != null && savedPrediction.getAnomalyScore() > anomalyThreshold) {
            sendFinanceAlert(savedPrediction, financePayload);
        }

        return savedPrediction;
    }

    public List<FinancePrediction> getLatestPredictions(int limit) {
        return financePredictionRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, limit)).getContent();
    }

    public List<FinancePrediction> getPredictionsForSymbol(String symbol, int limit) {
        return financePredictionRepository.findBySymbolOrderByCreatedAtDesc(symbol.toUpperCase(), PageRequest.of(0, limit)).getContent();
    }

    public List<FinanceMarketTick> getHistory(String symbol, int limit) {
        return financeMarketTickRepository.findBySymbolOrderByEventTimestampDesc(symbol.toUpperCase(), PageRequest.of(0, limit)).getContent();
    }

    public FinancePrediction getLatestPrediction(String symbol) {
        return predictionCacheService.getPrediction(symbol.toUpperCase())
                .orElseGet(() -> financePredictionRepository.findFirstBySymbolOrderByCreatedAtDesc(symbol.toUpperCase()));
    }

    public Map<String, Object> getFinanceOverview() {
        Map<String, Object> overview = new HashMap<>();
        List<FinancePrediction> recentPredictions = getLatestPredictions(10);
        Map<String, Long> providers = new HashMap<>();
        Map<String, Long> segments = new HashMap<>();

        for (FinancePrediction prediction : recentPredictions) {
            providers.merge(prediction.getProvider() != null ? prediction.getProvider() : "Unknown", 1L, Long::sum);

            FinanceMarketTick latestTick = financeMarketTickRepository.findFirstBySymbolOrderByEventTimestampDesc(prediction.getSymbol());
            if (latestTick != null) {
                segments.merge(latestTick.getMarketSegment() != null ? latestTick.getMarketSegment() : "UNKNOWN", 1L, Long::sum);
            }
        }

        overview.put("recentPredictions", recentPredictions);
        overview.put("predictionsToday", financePredictionRepository.countByCreatedAtAfter(LocalDateTime.now().minusDays(1)));
        overview.put("ticksToday", financeMarketTickRepository.countByEventTimestampAfter(LocalDateTime.now().minusDays(1)));
        overview.put("providers", providers);
        overview.put("segments", segments);
        overview.put("timestamp", LocalDateTime.now());
        return overview;
    }

    public int importTicksFromCsv(InputStream inputStream) {
        int importedCount = 0;

        try (CSVParser parser = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withTrim()
                .parse(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            for (CSVRecord record : parser) {
                FinanceMarketTick tick = new FinanceMarketTick();
                tick.setAssetId(record.get("assetId"));
                tick.setSymbol(record.get("symbol").toUpperCase());
                tick.setProvider(record.isMapped("provider") ? record.get("provider") : "csv-import");
                tick.setMarketSegment(record.isMapped("marketSegment") ? record.get("marketSegment") : "CRYPTO");
                tick.setCurrentPrice(Double.parseDouble(record.get("currentPrice")));
                tick.setHigh24h(readCsvDouble(record, "high24h"));
                tick.setLow24h(readCsvDouble(record, "low24h"));
                tick.setMarketCap(readCsvDouble(record, "marketCap"));
                tick.setTotalVolume(readCsvDouble(record, "totalVolume"));
                tick.setPriceChangePercentage1h(readCsvDouble(record, "priceChangePercentage1h"));
                tick.setPriceChangePercentage24h(readCsvDouble(record, "priceChangePercentage24h"));
                tick.setEventTimestamp(readCsvTimestamp(record));
                tick.setIngestedAt(LocalDateTime.now());

                processTick(tick);
                importedCount++;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unable to import finance CSV", e);
        }

        return importedCount;
    }

    private void publishPrediction(FinancePrediction prediction) {
        try {
            kafkaTemplate.send(financePredictionsTopic, objectMapper.writeValueAsString(prediction));
        } catch (Exception e) {
            logger.warn("Unable to publish finance prediction to Kafka: {}", e.getMessage());
        }
    }

    private Map<String, Object> buildFinancePayload(FinanceMarketTick tick, FinancePrediction prediction) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("assetId", tick.getAssetId());
        payload.put("symbol", tick.getSymbol());
        payload.put("provider", tick.getProvider());
        payload.put("marketSegment", tick.getMarketSegment());
        payload.put("currentPrice", tick.getCurrentPrice());
        payload.put("predictedPrice", prediction.getPredictedPrice());
        payload.put("predictedChangePercentage", prediction.getPredictedChangePercentage());
        payload.put("priceChangePercentage24h", tick.getPriceChangePercentage24h());
        payload.put("marketCap", tick.getMarketCap());
        payload.put("totalVolume", tick.getTotalVolume());
        payload.put("anomalyScore", prediction.getAnomalyScore());
        payload.put("predictionConfidence", prediction.getPredictionConfidence());
        payload.put("trendSignal", prediction.getTrendSignal());
        payload.put("insight", prediction.getInsight());
        payload.put("eventTimestamp", tick.getEventTimestamp());
        payload.put("createdAt", prediction.getCreatedAt());
        payload.put("modelVersion", prediction.getModelVersion());
        payload.put("modelTrainedAt", prediction.getModelTrainedAt());
        return payload;
    }

    private void sendFinanceAlert(FinancePrediction prediction, Map<String, Object> financePayload) {
        Map<String, Object> alert = new HashMap<>();
        alert.put("alertType", "Finance Volatility Alert");
        alert.put("severity", prediction.getAnomalyScore() > anomalyThreshold * 1.5 ? "HIGH" : "MEDIUM");
        alert.put("timestamp", LocalDateTime.now().toString());
        alert.put("data", financePayload);
        webSocketService.sendAlert(alert);
    }

    private Double readCsvDouble(CSVRecord record, String columnName) {
        if (!record.isMapped(columnName)) {
            return null;
        }

        String value = record.get(columnName);
        if (value == null || value.isBlank()) {
            return null;
        }

        return Double.parseDouble(value);
    }

    private LocalDateTime readCsvTimestamp(CSVRecord record) {
        if (!record.isMapped("eventTimestamp")) {
            return LocalDateTime.now();
        }

        String value = record.get("eventTimestamp");
        if (value == null || value.isBlank()) {
            return LocalDateTime.now();
        }

        if (value.endsWith("Z") || value.contains("+")) {
            return OffsetDateTime.parse(value).toLocalDateTime();
        }

        return LocalDateTime.parse(value);
    }
}
