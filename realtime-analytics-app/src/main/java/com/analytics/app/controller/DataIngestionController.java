package com.analytics.app.controller;

import com.analytics.app.model.IoTSensorData;
import com.analytics.app.model.SocialMediaData;
import com.analytics.app.service.DataIngestionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/data")
@CrossOrigin(origins = "*")
public class DataIngestionController {
    
    @Autowired
    private DataIngestionService dataIngestionService;
    
    /**
     * Endpoint for IoT devices to send sensor data
     */
    @PostMapping("/iot/sensor")
    public ResponseEntity<Map<String, Object>> ingestIoTData(@Valid @RequestBody IoTSensorData sensorData) {
        try {
            IoTSensorData savedData = dataIngestionService.processIoTData(sensorData);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "IoT data ingested successfully",
                "dataId", savedData.getId(),
                "timestamp", savedData.getTimestamp()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to ingest IoT data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint for batch IoT data ingestion
     */
    @PostMapping("/iot/sensor/batch")
    public ResponseEntity<Map<String, Object>> ingestIoTDataBatch(@Valid @RequestBody List<IoTSensorData> sensorDataList) {
        try {
            List<IoTSensorData> savedDataList = dataIngestionService.processIoTDataBatch(sensorDataList);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Batch IoT data ingested successfully",
                "processedCount", savedDataList.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to ingest batch IoT data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint for social media data ingestion
     */
    @PostMapping("/social-media")
    public ResponseEntity<Map<String, Object>> ingestSocialMediaData(@Valid @RequestBody SocialMediaData socialData) {
        try {
            SocialMediaData savedData = dataIngestionService.processSocialMediaData(socialData);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Social media data ingested successfully",
                "dataId", savedData.getId(),
                "platform", savedData.getPlatform(),
                "sentimentScore", savedData.getSentimentScore()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to ingest social media data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Endpoint for batch social media data ingestion
     */
    @PostMapping("/social-media/batch")
    public ResponseEntity<Map<String, Object>> ingestSocialMediaDataBatch(@Valid @RequestBody List<SocialMediaData> socialDataList) {
        try {
            List<SocialMediaData> savedDataList = dataIngestionService.processSocialMediaDataBatch(socialDataList);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Batch social media data ingested successfully",
                "processedCount", savedDataList.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to ingest batch social media data: " + e.getMessage()
            ));
        }
    }
    
    /**
     * Health check endpoint for data ingestion service
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "healthy",
            "service", "Data Ingestion Service",
            "timestamp", System.currentTimeMillis()
        ));
    }
    
    /**
     * Get ingestion statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getIngestionStats() {
        try {
            Map<String, Object> stats = dataIngestionService.getIngestionStatistics();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                "status", "error",
                "message", "Failed to retrieve statistics: " + e.getMessage()
            ));
        }
    }
}

