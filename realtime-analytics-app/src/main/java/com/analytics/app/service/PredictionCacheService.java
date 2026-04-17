package com.analytics.app.service;

import com.analytics.app.model.FinancePrediction;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PredictionCacheService {

    private static final Logger logger = LoggerFactory.getLogger(PredictionCacheService.class);

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Map<String, CachedPrediction> localCache = new ConcurrentHashMap<>();

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Value("${analytics.finance.prediction-cache-ttl-seconds:300}")
    private long ttlSeconds;

    public void cachePrediction(FinancePrediction prediction) {
        String key = buildKey(prediction.getSymbol());

        try {
            String payload = objectMapper.writeValueAsString(prediction);
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, payload, Duration.ofSeconds(ttlSeconds));
                return;
            }

            localCache.put(key, new CachedPrediction(payload, Instant.now().plusSeconds(ttlSeconds)));
        } catch (Exception e) {
            logger.warn("Unable to cache finance prediction for {}: {}", prediction.getSymbol(), e.getMessage());
        }
    }

    public Optional<FinancePrediction> getPrediction(String symbol) {
        String key = buildKey(symbol);

        if (redisTemplate != null) {
            try {
                String payload = redisTemplate.opsForValue().get(key);
                if (payload != null) {
                    return Optional.of(objectMapper.readValue(payload, FinancePrediction.class));
                }
            } catch (Exception e) {
                logger.warn("Redis cache unavailable for {}: {}", symbol, e.getMessage());
            }
        }

        CachedPrediction cachedPrediction = localCache.get(key);
        if (cachedPrediction == null || cachedPrediction.expiresAt.isBefore(Instant.now())) {
            localCache.remove(key);
            return Optional.empty();
        }

        try {
            return Optional.of(objectMapper.readValue(cachedPrediction.payload, FinancePrediction.class));
        } catch (Exception e) {
            logger.warn("Unable to deserialize cached prediction for {}: {}", symbol, e.getMessage());
            localCache.remove(key);
            return Optional.empty();
        }
    }

    private String buildKey(String symbol) {
        return "finance:prediction:" + symbol.toUpperCase();
    }

    private static class CachedPrediction {
        private final String payload;
        private final Instant expiresAt;

        private CachedPrediction(String payload, Instant expiresAt) {
            this.payload = payload;
            this.expiresAt = expiresAt;
        }
    }
}
