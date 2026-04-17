package com.analytics.app.service;

import com.analytics.app.model.IoTSensorData;
import com.analytics.app.model.SocialMediaData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@ConditionalOnProperty(value = "analytics.simulation.enabled", havingValue = "true", matchIfMissing = true)
public class DataSimulationService {
    
    private static final Logger logger = LoggerFactory.getLogger(DataSimulationService.class);
    
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private DataIngestionService dataIngestionService;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Simulation data
    private final List<String> deviceIds = Arrays.asList(
        "TEMP_001", "TEMP_002", "TEMP_003", "HUM_001", "HUM_002", 
        "PRESS_001", "LIGHT_001", "MOTION_001", "AIR_001", "AIR_002"
    );
    
    private final List<String> locations = Arrays.asList(
        "Building A - Floor 1", "Building A - Floor 2", "Building B - Floor 1",
        "Warehouse", "Parking Lot", "Conference Room", "Server Room", "Lobby"
    );
    
    private final List<String> socialPlatforms = Arrays.asList("twitter", "facebook", "instagram", "linkedin");
    
    private final List<String> sampleTweets = Arrays.asList(
        "Just had an amazing experience with the new IoT sensors! #technology #innovation",
        "The weather monitoring system is working perfectly today #IoT #weather",
        "Loving the real-time analytics dashboard! So much data insight #analytics #data",
        "Smart building technology is the future! #smartbuilding #tech",
        "Air quality sensors showing excellent readings today #environment #health",
        "Temperature control system is maintaining perfect conditions #HVAC #automation",
        "Motion sensors detected unusual activity last night #security #monitoring",
        "The humidity levels are optimal for our equipment #monitoring #maintenance",
        "Pressure sensors indicate stable atmospheric conditions #weather #sensors",
        "Light sensors automatically adjusted brightness - love automation! #smart #lighting"
    );
    
    private final List<String> hashtags = Arrays.asList(
        "#IoT", "#technology", "#innovation", "#data", "#analytics", "#smart", 
        "#automation", "#sensors", "#monitoring", "#realtime", "#AI", "#ML"
    );
    
    private boolean simulationEnabled = true;
    
    /**
     * Generate simulated IoT sensor data every 5 seconds
     */
    @Scheduled(fixedRate = 5000)
    @Async
    public void generateIoTData() {
        if (!simulationEnabled) return;
        
        try {
            // Generate 1-3 sensor readings per cycle
            int count = ThreadLocalRandom.current().nextInt(1, 4);
            
            for (int i = 0; i < count; i++) {
                IoTSensorData sensorData = generateRandomIoTData();
                
                // Process through the ingestion service
                dataIngestionService.processIoTData(sensorData);
                
                logger.debug("Generated IoT data: {}", sensorData);
            }
            
        } catch (Exception e) {
            logger.error("Error generating IoT simulation data: {}", e.getMessage());
        }
    }
    
    /**
     * Generate simulated social media data every 10 seconds
     */
    @Scheduled(fixedRate = 10000)
    @Async
    public void generateSocialMediaData() {
        if (!simulationEnabled) return;
        
        try {
            // Generate 1-2 social media posts per cycle
            int count = ThreadLocalRandom.current().nextInt(1, 3);
            
            for (int i = 0; i < count; i++) {
                SocialMediaData socialData = generateRandomSocialMediaData();
                
                // Process through the ingestion service
                dataIngestionService.processSocialMediaData(socialData);
                
                logger.debug("Generated social media data: {}", socialData);
            }
            
        } catch (Exception e) {
            logger.error("Error generating social media simulation data: {}", e.getMessage());
        }
    }
    
    /**
     * Generate random IoT sensor data
     */
    private IoTSensorData generateRandomIoTData() {
        String deviceId = deviceIds.get(ThreadLocalRandom.current().nextInt(deviceIds.size()));
        String sensorType = extractSensorType(deviceId);
        String location = locations.get(ThreadLocalRandom.current().nextInt(locations.size()));
        
        IoTSensorData sensorData = new IoTSensorData();
        sensorData.setDeviceId(deviceId);
        sensorData.setSensorType(sensorType);
        sensorData.setLocation(location);
        sensorData.setTimestamp(LocalDateTime.now());
        
        // Generate realistic sensor values with occasional anomalies
        double value = generateSensorValue(sensorType);
        sensorData.setSensorValue(value);
        sensorData.setUnit(getSensorUnit(sensorType));
        
        // Add geographic coordinates (simulated)
        sensorData.setLatitude(40.7128 + ThreadLocalRandom.current().nextGaussian() * 0.01);
        sensorData.setLongitude(-74.0060 + ThreadLocalRandom.current().nextGaussian() * 0.01);
        
        // Add metadata
        Map<String, String> metadata = new HashMap<>();
        metadata.put("firmware_version", "v" + ThreadLocalRandom.current().nextInt(1, 5) + ".0");
        metadata.put("battery_level", String.valueOf(ThreadLocalRandom.current().nextInt(20, 101)));
        metadata.put("signal_strength", String.valueOf(ThreadLocalRandom.current().nextInt(-80, -30)));
        sensorData.setMetadata(metadata);
        
        return sensorData;
    }
    
    /**
     * Generate random social media data
     */
    private SocialMediaData generateRandomSocialMediaData() {
        String platform = socialPlatforms.get(ThreadLocalRandom.current().nextInt(socialPlatforms.size()));
        String content = sampleTweets.get(ThreadLocalRandom.current().nextInt(sampleTweets.size()));
        
        SocialMediaData socialData = new SocialMediaData();
        socialData.setPlatform(platform);
        socialData.setPostId(generatePostId(platform));
        socialData.setUserId("user_" + ThreadLocalRandom.current().nextInt(1000, 9999));
        socialData.setUsername("@user" + ThreadLocalRandom.current().nextInt(100, 999));
        socialData.setContent(content);
        socialData.setPostTimestamp(LocalDateTime.now().minusMinutes(ThreadLocalRandom.current().nextInt(0, 60)));
        socialData.setCollectedTimestamp(LocalDateTime.now());
        
        // Generate engagement metrics
        socialData.setLikesCount(ThreadLocalRandom.current().nextInt(0, 1000));
        socialData.setSharesCount(ThreadLocalRandom.current().nextInt(0, 100));
        socialData.setCommentsCount(ThreadLocalRandom.current().nextInt(0, 50));
        socialData.setFollowersCount(ThreadLocalRandom.current().nextInt(100, 10000));
        
        // Extract hashtags from content
        List<String> postHashtags = extractHashtags(content);
        if (postHashtags.isEmpty()) {
            // Add random hashtags if none found
            postHashtags.add(hashtags.get(ThreadLocalRandom.current().nextInt(hashtags.size())));
        }
        socialData.setHashtags(postHashtags);
        
        // Generate mentions
        if (ThreadLocalRandom.current().nextDouble() < 0.3) { // 30% chance of mentions
            socialData.setMentions(Arrays.asList("@techcompany", "@innovator"));
        }
        
        socialData.setLanguage("en");
        
        return socialData;
    }
    
    /**
     * Extract sensor type from device ID
     */
    private String extractSensorType(String deviceId) {
        if (deviceId.startsWith("TEMP")) return "temperature";
        if (deviceId.startsWith("HUM")) return "humidity";
        if (deviceId.startsWith("PRESS")) return "pressure";
        if (deviceId.startsWith("LIGHT")) return "light";
        if (deviceId.startsWith("MOTION")) return "motion";
        if (deviceId.startsWith("AIR")) return "air-quality";
        return "unknown";
    }
    
    /**
     * Generate realistic sensor values
     */
    private double generateSensorValue(String sensorType) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        
        switch (sensorType) {
            case "temperature":
                // Normal range: 18-26°C, with occasional anomalies
                if (random.nextDouble() < 0.05) { // 5% anomalies
                    return random.nextDouble() < 0.5 ? random.nextDouble(5, 15) : random.nextDouble(35, 45);
                }
                return 18 + random.nextGaussian() * 2 + 4; // Mean ~22°C
                
            case "humidity":
                // Normal range: 30-70%, with occasional anomalies
                if (random.nextDouble() < 0.05) {
                    return random.nextDouble() < 0.5 ? random.nextDouble(10, 25) : random.nextDouble(80, 95);
                }
                return Math.max(0, Math.min(100, 30 + random.nextGaussian() * 10 + 20));
                
            case "pressure":
                // Normal range: 1000-1030 hPa
                if (random.nextDouble() < 0.03) {
                    return random.nextDouble(950, 980);
                }
                return 1000 + random.nextGaussian() * 5 + 15;
                
            case "light":
                // Normal range: 100-1000 lux
                if (random.nextDouble() < 0.04) {
                    return random.nextDouble(0, 50);
                }
                return Math.max(0, 100 + random.nextGaussian() * 200 + 400);
                
            case "motion":
                // Binary sensor: 0 or 1
                return random.nextDouble() < 0.1 ? 1.0 : 0.0;
                
            case "air-quality":
                // AQI scale: 0-500
                if (random.nextDouble() < 0.08) {
                    return random.nextDouble(150, 300); // Unhealthy levels
                }
                return Math.max(0, Math.min(500, random.nextGaussian() * 20 + 50));
                
            default:
                return random.nextDouble(0, 100);
        }
    }
    
    /**
     * Get sensor unit
     */
    private String getSensorUnit(String sensorType) {
        switch (sensorType) {
            case "temperature": return "°C";
            case "humidity": return "%";
            case "pressure": return "hPa";
            case "light": return "lux";
            case "motion": return "boolean";
            case "air-quality": return "AQI";
            default: return "unit";
        }
    }
    
    /**
     * Generate post ID
     */
    private String generatePostId(String platform) {
        return platform + "_" + System.currentTimeMillis() + "_" + ThreadLocalRandom.current().nextInt(1000, 9999);
    }
    
    /**
     * Extract hashtags from content
     */
    private List<String> extractHashtags(String content) {
        List<String> hashtags = new ArrayList<>();
        String[] words = content.split("\\s+");
        
        for (String word : words) {
            if (word.startsWith("#") && word.length() > 1) {
                hashtags.add(word);
            }
        }
        
        return hashtags;
    }
    
    /**
     * Control simulation
     */
    public void enableSimulation() {
        simulationEnabled = true;
        logger.info("Data simulation enabled");
    }
    
    public void disableSimulation() {
        simulationEnabled = false;
        logger.info("Data simulation disabled");
    }
    
    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }
    
    /**
     * Generate batch data for testing
     */
    @Async
    public void generateBatchData(int iotCount, int socialCount) {
        logger.info("Generating batch data: {} IoT records, {} social media records", iotCount, socialCount);
        
        // Generate IoT data
        for (int i = 0; i < iotCount; i++) {
            try {
                IoTSensorData sensorData = generateRandomIoTData();
                dataIngestionService.processIoTData(sensorData);
                
                if (i % 100 == 0) {
                    Thread.sleep(100); // Small delay to prevent overwhelming
                }
            } catch (Exception e) {
                logger.error("Error generating batch IoT data: {}", e.getMessage());
            }
        }
        
        // Generate social media data
        for (int i = 0; i < socialCount; i++) {
            try {
                SocialMediaData socialData = generateRandomSocialMediaData();
                dataIngestionService.processSocialMediaData(socialData);
                
                if (i % 50 == 0) {
                    Thread.sleep(100); // Small delay to prevent overwhelming
                }
            } catch (Exception e) {
                logger.error("Error generating batch social media data: {}", e.getMessage());
            }
        }
        
        logger.info("Batch data generation completed");
    }
}
