package com.analytics.app.controller;

import com.analytics.app.model.IoTSensorData;
import com.analytics.app.model.SocialMediaData;
import com.analytics.app.service.DataIngestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DataIngestionController.class)
class DataIngestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DataIngestionService dataIngestionService;

    @Autowired
    private ObjectMapper objectMapper;

    private IoTSensorData sampleIoTData;
    private SocialMediaData sampleSocialData;

    @BeforeEach
    void setUp() {
        sampleIoTData = new IoTSensorData();
        sampleIoTData.setId(1L);
        sampleIoTData.setDeviceId("TEMP_001");
        sampleIoTData.setSensorType("temperature");
        sampleIoTData.setSensorValue(22.5);
        sampleIoTData.setUnit("C");
        sampleIoTData.setLocation("Building A - Floor 1");
        sampleIoTData.setTimestamp(LocalDateTime.now());

        sampleSocialData = new SocialMediaData();
        sampleSocialData.setId(1L);
        sampleSocialData.setPlatform("twitter");
        sampleSocialData.setPostId("tweet_123");
        sampleSocialData.setContent("Great IoT technology! #innovation #tech");
        sampleSocialData.setUserId("user_456");
        sampleSocialData.setUsername("@techuser");
        sampleSocialData.setLikesCount(10);
        sampleSocialData.setSharesCount(5);
        sampleSocialData.setCommentsCount(2);
        sampleSocialData.setFollowersCount(1000);
        sampleSocialData.setSentimentScore(0.8);
    }

    @Test
    void testIngestIoTData_Success() throws Exception {
        when(dataIngestionService.processIoTData(any(IoTSensorData.class))).thenReturn(sampleIoTData);

        mockMvc.perform(post("/api/data/iot/sensor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleIoTData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("IoT data ingested successfully"))
                .andExpect(jsonPath("$.dataId").value(1));
    }

    @Test
    void testIngestIoTData_AcceptsIsoTimestamp() throws Exception {
        when(dataIngestionService.processIoTData(any(IoTSensorData.class))).thenReturn(sampleIoTData);

        String payload = "{"
                + "\"deviceId\":\"TEMP_001\","
                + "\"sensorType\":\"temperature\","
                + "\"sensorValue\":22.5,"
                + "\"unit\":\"C\","
                + "\"location\":\"Building A - Floor 1\","
                + "\"timestamp\":\"2024-12-07T10:30:00Z\""
                + "}";

        mockMvc.perform(post("/api/data/iot/sensor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.dataId").value(1));
    }

    @Test
    void testIngestIoTData_InvalidData() throws Exception {
        IoTSensorData invalidData = new IoTSensorData();

        when(dataIngestionService.processIoTData(any(IoTSensorData.class)))
                .thenThrow(new IllegalArgumentException("Device ID cannot be null or empty"));

        mockMvc.perform(post("/api/data/iot/sensor")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void testIngestIoTDataBatch_Success() throws Exception {
        when(dataIngestionService.processIoTDataBatch(any()))
                .thenReturn(Arrays.asList(sampleIoTData));

        mockMvc.perform(post("/api/data/iot/sensor/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Arrays.asList(sampleIoTData))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.processedCount").value(1));
    }

    @Test
    void testIngestSocialMediaData_Success() throws Exception {
        when(dataIngestionService.processSocialMediaData(any(SocialMediaData.class)))
                .thenReturn(sampleSocialData);

        mockMvc.perform(post("/api/data/social-media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleSocialData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value("Social media data ingested successfully"))
                .andExpect(jsonPath("$.dataId").value(1))
                .andExpect(jsonPath("$.platform").value("twitter"))
                .andExpect(jsonPath("$.sentimentScore").value(0.8));
    }

    @Test
    void testIngestSocialMediaData_AcceptsIsoTimestamp() throws Exception {
        when(dataIngestionService.processSocialMediaData(any(SocialMediaData.class)))
                .thenReturn(sampleSocialData);

        String payload = "{"
                + "\"platform\":\"twitter\","
                + "\"postId\":\"tweet_123\","
                + "\"content\":\"Great IoT technology! #innovation #tech\","
                + "\"userId\":\"user_456\","
                + "\"username\":\"@techuser\","
                + "\"likesCount\":10,"
                + "\"sharesCount\":5,"
                + "\"commentsCount\":2,"
                + "\"postTimestamp\":\"2024-12-07T10:30:00Z\""
                + "}";

        mockMvc.perform(post("/api/data/social-media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.dataId").value(1));
    }

    @Test
    void testIngestSocialMediaData_InvalidData() throws Exception {
        SocialMediaData invalidData = new SocialMediaData();

        when(dataIngestionService.processSocialMediaData(any(SocialMediaData.class)))
                .thenThrow(new IllegalArgumentException("Platform cannot be null or empty"));

        mockMvc.perform(post("/api/data/social-media")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidData)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/api/data/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("healthy"))
                .andExpect(jsonPath("$.service").value("Data Ingestion Service"));
    }

    @Test
    void testGetIngestionStats() throws Exception {
        Map<String, Object> stats = new HashMap<>();
        stats.put("iot", Map.of("totalRecords", 100L, "anomalies", 5L));
        stats.put("socialMedia", Map.of("totalRecords", 50L, "trendingPosts", 3L));

        when(dataIngestionService.getIngestionStatistics()).thenReturn(stats);

        mockMvc.perform(get("/api/data/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iot.totalRecords").value(100))
                .andExpect(jsonPath("$.iot.anomalies").value(5))
                .andExpect(jsonPath("$.socialMedia.totalRecords").value(50))
                .andExpect(jsonPath("$.socialMedia.trendingPosts").value(3));
    }
}
