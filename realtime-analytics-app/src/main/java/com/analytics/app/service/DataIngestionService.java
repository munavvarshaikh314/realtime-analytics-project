package com.analytics.app.service;

import com.analytics.app.model.IoTSensorData;
import com.analytics.app.model.SocialMediaData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.analytics.app.repository.IoTSensorDataRepository;
import com.analytics.app.repository.SocialMediaDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
public class DataIngestionService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataIngestionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    
    @Autowired
    private IoTSensorDataRepository iotRepository;
    
    @Autowired
    private SocialMediaDataRepository socialMediaRepository;
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private MLProcessingService mlProcessingService;
    
    /**
     * Process single IoT sensor data
     */
    public IoTSensorData processIoTData(IoTSensorData sensorData) {
        logger.info("Processing IoT data from device: {}", sensorData.getDeviceId());
        
        // Set timestamp if not provided
        if (sensorData.getTimestamp() == null) {
            sensorData.setTimestamp(LocalDateTime.now());
        }
        
        // Validate sensor data
        validateIoTData(sensorData);
        
        // Apply ML anomaly detection
        mlProcessingService.detectIoTAnomalies(sensorData);
        
        // Save to database
        IoTSensorData savedData = iotRepository.save(sensorData);
        
        // Send to Kafka for real-time processing
        sendToKafka("iot-sensor-data", writeMessage(savedData));
        
        logger.info("IoT data processed successfully with ID: {}", savedData.getId());
        return savedData;
    }
    
    /**
     * Process batch IoT sensor data
     */
    public List<IoTSensorData> processIoTDataBatch(List<IoTSensorData> sensorDataList) {
        logger.info("Processing batch IoT data with {} records", sensorDataList.size());
        
        return sensorDataList.stream()
                .map(this::processIoTData)
                .collect(Collectors.toList());
    }
    
    /**
     * Process single social media data
     */
    public SocialMediaData processSocialMediaData(SocialMediaData socialData) {
        logger.info("Processing social media data from platform: {}", socialData.getPlatform());
        
        // Set collection timestamp if not provided
        if (socialData.getCollectedTimestamp() == null) {
            socialData.setCollectedTimestamp(LocalDateTime.now());
        }
        
        // Validate social media data
        validateSocialMediaData(socialData);
        
        // Apply ML sentiment analysis and trend detection
        mlProcessingService.analyzeSentiment(socialData);
        mlProcessingService.detectTrends(socialData);
        
        // Calculate engagement metrics
        calculateEngagementMetrics(socialData);
        
        // Save to database
        SocialMediaData savedData = socialMediaRepository.save(socialData);
        
        // Send to Kafka for real-time processing
        sendToKafka("social-media-feed", writeMessage(savedData));
        
        logger.info("Social media data processed successfully with ID: {}", savedData.getId());
        return savedData;
    }
    
    /**
     * Process batch social media data
     */
    public List<SocialMediaData> processSocialMediaDataBatch(List<SocialMediaData> socialDataList) {
        logger.info("Processing batch social media data with {} records", socialDataList.size());
        
        return socialDataList.stream()
                .map(this::processSocialMediaData)
                .collect(Collectors.toList());
    }
    
    /**
     * Get ingestion statistics
     */
    public Map<String, Object> getIngestionStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        // IoT statistics
        long totalIoTRecords = iotRepository.count();
        long iotAnomalies = iotRepository.countByIsAnomalyTrue();
        long iotRecordsToday = iotRepository.countByTimestampAfter(LocalDateTime.now().minusDays(1));
        
        // Social media statistics
        long totalSocialRecords = socialMediaRepository.count();
        long socialRecordsToday = socialMediaRepository.countByCollectedTimestampAfter(LocalDateTime.now().minusDays(1));
        long trendingPosts = socialMediaRepository.countByIsTrendingTrue();
        
        stats.put("iot", Map.of(
            "totalRecords", totalIoTRecords,
            "anomalies", iotAnomalies,
            "recordsToday", iotRecordsToday,
            "anomalyRate", totalIoTRecords > 0 ? (double) iotAnomalies / totalIoTRecords : 0.0
        ));
        
        stats.put("socialMedia", Map.of(
            "totalRecords", totalSocialRecords,
            "recordsToday", socialRecordsToday,
            "trendingPosts", trendingPosts,
            "trendingRate", totalSocialRecords > 0 ? (double) trendingPosts / totalSocialRecords : 0.0
        ));
        
        stats.put("timestamp", LocalDateTime.now());
        
        return stats;
    }
    
    /**
     * Validate IoT sensor data
     */
    private void validateIoTData(IoTSensorData sensorData) {
        if (sensorData.getDeviceId() == null || sensorData.getDeviceId().trim().isEmpty()) {
            throw new IllegalArgumentException("Device ID cannot be null or empty");
        }
        
        if (sensorData.getSensorType() == null || sensorData.getSensorType().trim().isEmpty()) {
            throw new IllegalArgumentException("Sensor type cannot be null or empty");
        }
        
        if (sensorData.getSensorValue() == null) {
            throw new IllegalArgumentException("Sensor value cannot be null");
        }
        
        // Additional validation based on sensor type
        validateSensorValueRange(sensorData);
    }
    
    /**
     * Validate social media data
     */
    private void validateSocialMediaData(SocialMediaData socialData) {
        if (socialData.getPlatform() == null || socialData.getPlatform().trim().isEmpty()) {
            throw new IllegalArgumentException("Platform cannot be null or empty");
        }
        
        if (socialData.getPostId() == null || socialData.getPostId().trim().isEmpty()) {
            throw new IllegalArgumentException("Post ID cannot be null or empty");
        }
        
        if (socialData.getContent() == null || socialData.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("Content cannot be null or empty");
        }
    }
    
    /**
     * Validate sensor value ranges based on sensor type
     */
    private void validateSensorValueRange(IoTSensorData sensorData) {
        String sensorType = sensorData.getSensorType().toLowerCase();
        Double value = sensorData.getSensorValue();
        
        switch (sensorType) {
            case "temperature":
                if (value < -50 || value > 100) {
                    logger.warn("Temperature value {} is outside normal range for device {}", value, sensorData.getDeviceId());
                }
                break;
            case "humidity":
                if (value < 0 || value > 100) {
                    throw new IllegalArgumentException("Humidity value must be between 0 and 100");
                }
                break;
            case "pressure":
                if (value < 0) {
                    throw new IllegalArgumentException("Pressure value cannot be negative");
                }
                break;
            case "light":
                if (value < 0) {
                    throw new IllegalArgumentException("Light value cannot be negative");
                }
                break;
        }
    }
    
    /**
     * Calculate engagement metrics for social media data
     */
    private void calculateEngagementMetrics(SocialMediaData socialData) {
        if (socialData.getFollowersCount() != null && socialData.getFollowersCount() > 0) {
            int totalEngagement = (socialData.getLikesCount() != null ? socialData.getLikesCount() : 0) +
                                (socialData.getSharesCount() != null ? socialData.getSharesCount() : 0) +
                                (socialData.getCommentsCount() != null ? socialData.getCommentsCount() : 0);
            
            double engagementRate = (double) totalEngagement / socialData.getFollowersCount() * 100;
            socialData.setEngagementRate(engagementRate);
            
            // Calculate virality score based on shares and engagement
            double viralityScore = (socialData.getSharesCount() != null ? socialData.getSharesCount() : 0) * 2.0 + 
                                 engagementRate * 0.1;
            socialData.setViralityScore(viralityScore);
        }
    }
    
    /**
     * Send data to Kafka topic
     */
    private void sendToKafka(String topic, String message) {
        try {
            kafkaTemplate.send(topic, message);
            logger.debug("Message sent to Kafka topic: {}", topic);
        } catch (Exception e) {
            logger.error("Failed to send message to Kafka topic {}: {}", topic, e.getMessage());
        }
    }

    private String writeMessage(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            logger.warn("Falling back to plain string payload for Kafka message: {}", e.getMessage());
            return String.valueOf(payload);
        }
    }
}
