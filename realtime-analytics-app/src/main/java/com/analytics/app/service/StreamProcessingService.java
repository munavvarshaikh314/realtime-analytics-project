package com.analytics.app.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;

import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class StreamProcessingService {
    
    private static final Logger logger = LoggerFactory.getLogger(StreamProcessingService.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private WebSocketService webSocketService;
    
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    
    // Real-time metrics
    private final AtomicLong iotMessageCount = new AtomicLong(0);
    private final AtomicLong socialMediaMessageCount = new AtomicLong(0);
    private final AtomicLong anomalyCount = new AtomicLong(0);
    private final AtomicLong trendingPostCount = new AtomicLong(0);
    
    // Sliding window data for aggregations
    private final Map<String, Queue<Double>> sensorWindows = new ConcurrentHashMap<>();
    private final Map<String, Queue<Double>> sentimentWindows = new ConcurrentHashMap<>();
    private final Map<String, Integer> hashtagCounts = new ConcurrentHashMap<>();
    
    // Alert thresholds
    private final Map<String, Double> alertThresholds = new HashMap<>();
    
    public StreamProcessingService() {
        initializeAlertThresholds();
    }
    
    /**
     * Process IoT sensor data stream
     */
    @KafkaListener(topics = "iot-sensor-data", groupId = "analytics-group")
    public void processIoTStream(@Payload String message,
                                @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                Acknowledgment acknowledgment) {
        try {
            logger.debug("Processing IoT message from topic: {}, partition: {}", topic, partition);
            
            // Parse IoT data (simplified - in real scenario would deserialize properly)
            Map<String, Object> processedData = processIoTMessage(message);
            
            // Update metrics
            iotMessageCount.incrementAndGet();
            
            // Check for anomalies and send alerts
            if (Boolean.TRUE.equals(processedData.get("isAnomaly"))) {
                anomalyCount.incrementAndGet();
                sendAlert("IoT Anomaly Detected", processedData);
            }
            
            // Send processed data to WebSocket for real-time updates
            webSocketService.sendIoTUpdate(processedData);
            
            // Send to processed data topic
            kafkaTemplate.send("processed-analytics-data", objectMapper.writeValueAsString(processedData));
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing IoT stream message: {}", e.getMessage());
        }
    }
    
    /**
     * Process social media data stream
     */
    @KafkaListener(topics = "social-media-feed", groupId = "analytics-group")
    public void processSocialMediaStream(@Payload String message,
                                        @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                        @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
                                        Acknowledgment acknowledgment) {
        try {
            logger.debug("Processing social media message from topic: {}, partition: {}", topic, partition);
            
            // Parse social media data
            Map<String, Object> processedData = processSocialMediaMessage(message);
            
            // Update metrics
            socialMediaMessageCount.incrementAndGet();
            
            // Check for trending posts
            if (Boolean.TRUE.equals(processedData.get("isTrending"))) {
                trendingPostCount.incrementAndGet();
                sendAlert("Trending Post Detected", processedData);
            }
            
            // Update hashtag counts
            updateHashtagCounts(processedData);
            
            // Send processed data to WebSocket for real-time updates
            webSocketService.sendSocialMediaUpdate(processedData);
            
            // Send to processed data topic
            kafkaTemplate.send("processed-analytics-data", objectMapper.writeValueAsString(processedData));
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing social media stream message: {}", e.getMessage());
        }
    }
    
    /**
     * Process ML prediction results
     */
    @KafkaListener(topics = "ml-prediction-results", groupId = "analytics-group")
    public void processMLPredictions(@Payload String message,
                                   @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
                                   Acknowledgment acknowledgment) {
        try {
            logger.debug("Processing ML prediction from topic: {}", topic);
            
            Map<String, Object> prediction = objectMapper.readValue(message, Map.class);
            
            // Send ML predictions to WebSocket
            webSocketService.sendMLPrediction(prediction);
            
            acknowledgment.acknowledge();
            
        } catch (Exception e) {
            logger.error("Error processing ML prediction message: {}", e.getMessage());
        }
    }
    
    /**
     * Process IoT message and extract key information
     */
    private Map<String, Object> processIoTMessage(String message) {
        Map<String, Object> processedData = tryReadJsonMessage(message);
        
        try {
            if (processedData == null) {
                processedData = new HashMap<>();
                processedData.put("deviceId", extractValue(message, "deviceId"));
                processedData.put("sensorType", extractValue(message, "sensorType"));
                processedData.put("sensorValue", extractDoubleValue(message, "sensorValue"));
                processedData.put("location", extractValue(message, "location"));
                processedData.put("isAnomaly", extractBooleanValue(message, "isAnomaly"));
                processedData.put("anomalyScore", extractDoubleValue(message, "anomalyScore"));
            }

            processedData.put("messageType", "iot");
            processedData.putIfAbsent("timestamp", LocalDateTime.now().toString());
            processedData.put("sensorValue", readDouble(processedData.get("sensorValue")));
            processedData.put("anomalyScore", readDouble(processedData.get("anomalyScore")));
            processedData.put("isAnomaly", readBoolean(processedData.get("isAnomaly")));
            
            // Calculate moving averages
            String sensorKey = processedData.get("deviceId") + "_" + processedData.get("sensorType");
            Double sensorValue = readDouble(processedData.get("sensorValue"));
            
            if (sensorValue != null) {
                Queue<Double> window = sensorWindows.computeIfAbsent(sensorKey, k -> new LinkedList<>());
                window.offer(sensorValue);
                
                // Keep only last 10 values
                if (window.size() > 10) {
                    window.poll();
                }
                
                // Calculate moving average
                double average = window.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                processedData.put("movingAverage", average);
            }
            
        } catch (Exception e) {
            logger.error("Error processing IoT message: {}", e.getMessage());
        }
        
        return processedData;
    }
    
    /**
     * Process social media message and extract key information
     */
    private Map<String, Object> processSocialMediaMessage(String message) {
        Map<String, Object> processedData = tryReadJsonMessage(message);
        
        try {
            if (processedData == null) {
                processedData = new HashMap<>();
                processedData.put("platform", extractValue(message, "platform"));
                processedData.put("postId", extractValue(message, "postId"));
                processedData.put("username", extractValue(message, "username"));
                processedData.put("content", extractValue(message, "content"));
                processedData.put("sentimentScore", extractDoubleValue(message, "sentimentScore"));
                processedData.put("sentimentLabel", extractValue(message, "sentimentLabel"));
                processedData.put("isTrending", extractBooleanValue(message, "isTrending"));
                processedData.put("engagementRate", extractDoubleValue(message, "engagementRate"));
                processedData.put("influenceScore", extractDoubleValue(message, "influenceScore"));
            }

            processedData.put("messageType", "social");
            processedData.putIfAbsent("timestamp", LocalDateTime.now().toString());
            processedData.put("sentimentScore", readDouble(processedData.get("sentimentScore")));
            processedData.put("engagementRate", readDouble(processedData.get("engagementRate")));
            processedData.put("influenceScore", readDouble(processedData.get("influenceScore")));
            processedData.put("isTrending", readBoolean(processedData.get("isTrending")));
            
            // Calculate sentiment moving average
            String platformKey = readString(processedData.get("platform"));
            Double sentimentScore = readDouble(processedData.get("sentimentScore"));
            
            if (sentimentScore != null && platformKey != null) {
                Queue<Double> window = sentimentWindows.computeIfAbsent(platformKey, k -> new LinkedList<>());
                window.offer(sentimentScore);
                
                // Keep only last 20 values
                if (window.size() > 20) {
                    window.poll();
                }
                
                // Calculate moving average sentiment
                double averageSentiment = window.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                processedData.put("averageSentiment", averageSentiment);
            }
            
        } catch (Exception e) {
            logger.error("Error processing social media message: {}", e.getMessage());
        }
        
        return processedData;
    }
    
    /**
     * Update hashtag counts for trending analysis
     */
    private void updateHashtagCounts(Map<String, Object> processedData) {
        String content = (String) processedData.get("content");
        if (content != null) {
            String[] words = content.split("\\s+");
            for (String word : words) {
                if (word.startsWith("#") && word.length() > 1) {
                    hashtagCounts.merge(word.toLowerCase(), 1, Integer::sum);
                }
            }
        }
    }
    
    /**
     * Send alert message
     */
    private void sendAlert(String alertType, Map<String, Object> data) {
        try {
            Map<String, Object> alert = new HashMap<>();
            alert.put("alertType", alertType);
            alert.put("timestamp", LocalDateTime.now().toString());
            alert.put("data", data);
            alert.put("severity", determineAlertSeverity(alertType, data));
            
            // Send to alerts topic
            kafkaTemplate.send("system-alerts", objectMapper.writeValueAsString(alert));
            
            // Send to WebSocket for immediate notification
            webSocketService.sendAlert(alert);
            
            logger.info("Alert sent: {}", alertType);
            
        } catch (Exception e) {
            logger.error("Error sending alert: {}", e.getMessage());
        }
    }
    
    /**
     * Determine alert severity
     */
    private String determineAlertSeverity(String alertType, Map<String, Object> data) {
        if (alertType.contains("Anomaly")) {
            Double anomalyScore = readDouble(data.get("anomalyScore"));
            if (anomalyScore != null) {
                if (anomalyScore > 2.0) return "HIGH";
                if (anomalyScore > 1.0) return "MEDIUM";
                return "LOW";
            }
        }
        
        if (alertType.contains("Trending")) {
            Double influenceScore = readDouble(data.get("influenceScore"));
            if (influenceScore != null) {
                if (influenceScore > 80.0) return "HIGH";
                if (influenceScore > 50.0) return "MEDIUM";
                return "LOW";
            }
        }
        
        return "MEDIUM";
    }
    
    /**
     * Get real-time metrics
     */
    public Map<String, Object> getRealTimeMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("iotMessageCount", iotMessageCount.get());
        metrics.put("socialMediaMessageCount", socialMediaMessageCount.get());
        metrics.put("anomalyCount", anomalyCount.get());
        metrics.put("trendingPostCount", trendingPostCount.get());
        metrics.put("timestamp", LocalDateTime.now().toString());
        
        // Top hashtags
        List<Map<String, Object>> topHashtags = hashtagCounts.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .limit(10)
        .map(entry -> {
            Map<String, Object> hashtagMap = new HashMap<>();
            hashtagMap.put("hashtag", entry.getKey());
            hashtagMap.put("count", entry.getValue());
            return hashtagMap;
        })
        .collect(Collectors.toList());
        
        metrics.put("topHashtags", topHashtags);
        
        return metrics;
    }
    
    /**
     * Initialize alert thresholds
     */
    private void initializeAlertThresholds() {
        alertThresholds.put("temperature_high", 35.0);
        alertThresholds.put("temperature_low", 10.0);
        alertThresholds.put("humidity_high", 80.0);
        alertThresholds.put("humidity_low", 20.0);
        alertThresholds.put("air_quality_poor", 150.0);
        alertThresholds.put("sentiment_very_negative", -0.8);
    }
    
    // Utility methods for extracting values from message strings
    private String extractValue(String message, String key) {
        try {
            int startIndex = message.indexOf(key + "=");
            if (startIndex == -1) return null;
            
            startIndex += key.length() + 1;
            int endIndex = message.indexOf(",", startIndex);
            if (endIndex == -1) endIndex = message.indexOf("}", startIndex);
            if (endIndex == -1) endIndex = message.length();
            
            String value = message.substring(startIndex, endIndex).trim();
            if (value.startsWith("'") && value.endsWith("'")) {
                value = value.substring(1, value.length() - 1);
            }
            
            return value;
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> tryReadJsonMessage(String message) {
        try {
            return objectMapper.readValue(message, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return null;
        }
    }

    private Double extractDoubleValue(String message, String key) {
        try {
            String value = extractValue(message, key);
            return value != null ? Double.parseDouble(value) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    private Boolean extractBooleanValue(String message, String key) {
        try {
            String value = extractValue(message, key);
            return value != null ? Boolean.parseBoolean(value) : null;
        } catch (Exception e) {
            return null;
        }
    }

    private String readString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private Double readDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String && !((String) value).isBlank()) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private Boolean readBoolean(Object value) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String && !((String) value).isBlank()) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
}
