package com.analytics.app.service;

import com.analytics.app.model.FinanceMarketTick;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@ConditionalOnProperty(
        value = {"analytics.finance.enabled", "analytics.finance.providers.crypto.enabled"},
        havingValue = "true"
)
public class FinanceDataProducerService {

    private static final Logger logger = LoggerFactory.getLogger(FinanceDataProducerService.class);

    private final RestTemplate restTemplate;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${analytics.finance.providers.crypto.asset-ids:bitcoin,ethereum}")
    private String assetIds;

    @Value("${analytics.finance.providers.crypto.vs-currency:usd}")
    private String vsCurrency;

    @Value("${kafka.topics.finance-data:finance-market-data}")
    private String financeTopic;

    public FinanceDataProducerService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Scheduled(fixedDelayString = "${analytics.finance.providers.crypto.fetch-interval-ms:15000}")
    public void fetchCryptoMarketData() {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://api.coingecko.com/api/v3/coins/markets")
                    .queryParam("vs_currency", vsCurrency)
                    .queryParam("ids", assetIds)
                    .queryParam("price_change_percentage", "1h,24h")
                    .toUriString();

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            if (root == null || !root.isArray()) {
                return;
            }

            for (JsonNode node : root) {
                FinanceMarketTick tick = new FinanceMarketTick();
                tick.setAssetId(node.path("id").asText());
                tick.setSymbol(node.path("symbol").asText().toUpperCase(Locale.ROOT));
                tick.setProvider("CoinGecko");
                tick.setMarketSegment("CRYPTO");
                tick.setCurrentPrice(node.path("current_price").asDouble());
                tick.setHigh24h(readNullableDouble(node, "high_24h"));
                tick.setLow24h(readNullableDouble(node, "low_24h"));
                tick.setMarketCap(readNullableDouble(node, "market_cap"));
                tick.setTotalVolume(readNullableDouble(node, "total_volume"));
                tick.setPriceChangePercentage1h(readNullableDouble(node, "price_change_percentage_1h_in_currency"));
                tick.setPriceChangePercentage24h(readNullableDouble(node, "price_change_percentage_24h_in_currency"));
                tick.setEventTimestamp(LocalDateTime.now());
                tick.setIngestedAt(LocalDateTime.now());

                kafkaTemplate.send(financeTopic, objectMapper.writeValueAsString(tick));
            }

            logger.debug("Published CoinGecko finance ticks for assets: {}", assetIds);
        } catch (Exception e) {
            logger.warn("CoinGecko finance fetch failed: {}", e.getMessage());
        }
    }

    private Double readNullableDouble(JsonNode node, String fieldName) {
        return node.hasNonNull(fieldName) ? node.get(fieldName).asDouble() : null;
    }
}
