package com.analytics.app.service;

import com.analytics.app.model.FinanceMarketTick;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
@ConditionalOnProperty(
        value = {"analytics.finance.enabled", "analytics.finance.providers.equities.enabled"},
        havingValue = "true"
)
public class EquitiesDataProducerService {

    private static final Logger logger = LoggerFactory.getLogger(EquitiesDataProducerService.class);

    private final RestTemplate restTemplate;
    private final AtomicBoolean missingApiKeyLogged = new AtomicBoolean(false);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${analytics.finance.providers.equities.api-key:}")
    private String apiKey;

    @Value("${analytics.finance.providers.equities.base-url:https://api.polygon.io}")
    private String baseUrl;

    @Value("${analytics.finance.providers.equities.tickers:AAPL,MSFT,NVDA,AMZN,GOOGL}")
    private String tickers;

    @Value("${kafka.topics.finance-data:finance-market-data}")
    private String financeTopic;

    public EquitiesDataProducerService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Scheduled(fixedDelayString = "${analytics.finance.providers.equities.fetch-interval-ms:15000}")
    public void fetchEquitiesMarketData() {
        if (apiKey == null || apiKey.isBlank()) {
            if (missingApiKeyLogged.compareAndSet(false, true)) {
                logger.info("Polygon equities feed is enabled but POLYGON_API_KEY is not configured. Skipping company market ingestion.");
            }
            return;
        }

        missingApiKeyLogged.set(false);

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(baseUrl)
                    .path("/v2/snapshot/locale/us/markets/stocks/tickers")
                    .queryParam("tickers", tickers)
                    .queryParam("apiKey", apiKey)
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root == null) {
                return;
            }

            if (root.hasNonNull("error")) {
                logger.warn("Polygon equities fetch failed: {}", root.get("error").asText());
                return;
            }

            JsonNode tickerNodes = root.path("tickers");
            if (!tickerNodes.isArray()) {
                return;
            }

            for (JsonNode tickerNode : tickerNodes) {
                FinanceMarketTick tick = mapSnapshot(tickerNode);
                if (tick == null) {
                    continue;
                }

                kafkaTemplate.send(financeTopic, objectMapper.writeValueAsString(tick));
            }

            logger.debug("Published Polygon equities finance ticks for tickers: {}", tickers);
        } catch (Exception e) {
            logger.warn("Polygon equities fetch failed: {}", e.getMessage());
        }
    }

    private FinanceMarketTick mapSnapshot(JsonNode snapshotNode) {
        String symbol = snapshotNode.path("ticker").asText("").trim().toUpperCase(Locale.ROOT);
        if (symbol.isBlank()) {
            return null;
        }

        JsonNode dayNode = snapshotNode.path("day");
        JsonNode previousDayNode = snapshotNode.path("prevDay");
        JsonNode minuteNode = snapshotNode.path("min");
        JsonNode lastTradeNode = snapshotNode.path("lastTrade");
        JsonNode tickerDetailsNode = snapshotNode.path("tickerDetails");

        Double currentPrice = firstPresent(
                readNullableDouble(lastTradeNode, "p"),
                readNullableDouble(minuteNode, "c"),
                readNullableDouble(dayNode, "c")
        );
        if (currentPrice == null) {
            return null;
        }

        FinanceMarketTick tick = new FinanceMarketTick();
        tick.setAssetId(symbol);
        tick.setSymbol(symbol);
        tick.setProvider("Polygon");
        tick.setMarketSegment("EQUITY");
        tick.setCurrentPrice(currentPrice);
        tick.setHigh24h(readNullableDouble(dayNode, "h"));
        tick.setLow24h(readNullableDouble(dayNode, "l"));
        tick.setMarketCap(firstPresent(
                readNullableDouble(snapshotNode, "marketCap"),
                readNullableDouble(tickerDetailsNode, "market_cap")
        ));
        tick.setTotalVolume(readNullableDouble(dayNode, "v"));
        tick.setPriceChangePercentage1h(null);
        tick.setPriceChangePercentage24h(firstPresent(
                readNullableDouble(snapshotNode, "todaysChangePerc"),
                calculateDayChangePercentage(dayNode, previousDayNode)
        ));
        tick.setEventTimestamp(parseTimestamp(firstPresent(
                readNullableLong(snapshotNode, "updated"),
                readNullableLong(lastTradeNode, "t"),
                readNullableLong(minuteNode, "t")
        )));
        tick.setIngestedAt(LocalDateTime.now());
        return tick;
    }

    private Double calculateDayChangePercentage(JsonNode dayNode, JsonNode previousDayNode) {
        Double currentClose = readNullableDouble(dayNode, "c");
        Double previousClose = readNullableDouble(previousDayNode, "c");

        if (currentClose == null || previousClose == null || previousClose == 0.0) {
            return null;
        }

        return ((currentClose - previousClose) / previousClose) * 100.0;
    }

    private LocalDateTime parseTimestamp(Long rawTimestamp) {
        if (rawTimestamp == null || rawTimestamp <= 0) {
            return LocalDateTime.now();
        }

        Instant instant;
        if (rawTimestamp >= 1_000_000_000_000_000_000L) {
            instant = Instant.ofEpochMilli(rawTimestamp / 1_000_000L);
        } else if (rawTimestamp >= 1_000_000_000_000_000L) {
            instant = Instant.ofEpochMilli(rawTimestamp / 1_000L);
        } else if (rawTimestamp >= 1_000_000_000_000L) {
            instant = Instant.ofEpochMilli(rawTimestamp);
        } else {
            instant = Instant.ofEpochSecond(rawTimestamp);
        }

        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
    }

    private Double readNullableDouble(JsonNode node, String fieldName) {
        return node != null && node.hasNonNull(fieldName) ? node.get(fieldName).asDouble() : null;
    }

    private Long readNullableLong(JsonNode node, String fieldName) {
        return node != null && node.hasNonNull(fieldName) ? node.get(fieldName).asLong() : null;
    }

    private Double firstPresent(Double... values) {
        for (Double value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Long firstPresent(Long... values) {
        for (Long value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
