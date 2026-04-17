package com.analytics.app.service;

import com.analytics.app.model.IoTSensorData;
import com.analytics.app.model.SocialMediaData;
import com.analytics.app.repository.IoTSensorDataRepository;
import com.analytics.app.repository.SocialMediaDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataIngestionServiceTest {

    @Mock
    private IoTSensorDataRepository iotRepository;

    @Mock
    private SocialMediaDataRepository socialMediaRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private MLProcessingService mlProcessingService;

    @InjectMocks
    private DataIngestionService dataIngestionService;

    private IoTSensorData sampleIoTData;
    private SocialMediaData sampleSocialData;

    @BeforeEach
    void setUp() {
        sampleIoTData = new IoTSensorData();
        sampleIoTData.setDeviceId("TEMP_001");
        sampleIoTData.setSensorType("temperature");
        sampleIoTData.setSensorValue(22.5);
        sampleIoTData.setUnit("°C");
        sampleIoTData.setLocation("Building A - Floor 1");
        sampleIoTData.setTimestamp(LocalDateTime.now());

        sampleSocialData = new SocialMediaData();
        sampleSocialData.setPlatform("twitter");
        sampleSocialData.setPostId("tweet_123");
        sampleSocialData.setContent("Great IoT technology! #innovation #tech");
        sampleSocialData.setUserId("user_456");
        sampleSocialData.setUsername("@techuser");
        sampleSocialData.setLikesCount(10);
        sampleSocialData.setSharesCount(5);
        sampleSocialData.setCommentsCount(2);
        sampleSocialData.setFollowersCount(1000);
    }

    @Test
    void testProcessIoTData_Success() {
        // Arrange
        when(iotRepository.save(any(IoTSensorData.class))).thenReturn(sampleIoTData);
        doNothing().when(mlProcessingService).detectIoTAnomalies(any(IoTSensorData.class));

        // Act
        IoTSensorData result = dataIngestionService.processIoTData(sampleIoTData);

        // Assert
        assertNotNull(result);
        assertEquals("TEMP_001", result.getDeviceId());
        assertEquals("temperature", result.getSensorType());
        assertEquals(22.5, result.getSensorValue());
        
        verify(iotRepository, times(1)).save(any(IoTSensorData.class));
        verify(mlProcessingService, times(1)).detectIoTAnomalies(any(IoTSensorData.class));
        verify(kafkaTemplate, times(1)).send(eq("iot-sensor-data"), contains("\"deviceId\":\"TEMP_001\""));
    }

    @Test
    void testProcessIoTData_InvalidDeviceId() {
        // Arrange
        sampleIoTData.setDeviceId(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            dataIngestionService.processIoTData(sampleIoTData);
        });
        
        verify(iotRepository, never()).save(any(IoTSensorData.class));
    }

    @Test
    void testProcessIoTData_InvalidSensorValue() {
        // Arrange
        sampleIoTData.setSensorValue(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            dataIngestionService.processIoTData(sampleIoTData);
        });
        
        verify(iotRepository, never()).save(any(IoTSensorData.class));
    }

    @Test
    void testProcessSocialMediaData_Success() {
        // Arrange
        sampleSocialData.setIsTrending(true);
        when(socialMediaRepository.save(any(SocialMediaData.class))).thenReturn(sampleSocialData);
        doNothing().when(mlProcessingService).analyzeSentiment(any(SocialMediaData.class));
        doNothing().when(mlProcessingService).detectTrends(any(SocialMediaData.class));

        // Act
        SocialMediaData result = dataIngestionService.processSocialMediaData(sampleSocialData);

        // Assert
        assertNotNull(result);
        assertEquals("twitter", result.getPlatform());
        assertEquals("tweet_123", result.getPostId());
        assertNotNull(result.getEngagementRate());
        
        verify(socialMediaRepository, times(1)).save(any(SocialMediaData.class));
        verify(mlProcessingService, times(1)).analyzeSentiment(any(SocialMediaData.class));
        verify(mlProcessingService, times(1)).detectTrends(any(SocialMediaData.class));
        verify(kafkaTemplate, times(1)).send(eq("social-media-feed"), contains("\"isTrending\":true"));
    }

    @Test
    void testProcessSocialMediaData_InvalidPlatform() {
        // Arrange
        sampleSocialData.setPlatform(null);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> {
            dataIngestionService.processSocialMediaData(sampleSocialData);
        });
        
        verify(socialMediaRepository, never()).save(any(SocialMediaData.class));
    }

    @Test
    void testProcessIoTDataBatch_Success() {
        // Arrange
        List<IoTSensorData> batchData = Arrays.asList(sampleIoTData);
        when(iotRepository.save(any(IoTSensorData.class))).thenReturn(sampleIoTData);
        doNothing().when(mlProcessingService).detectIoTAnomalies(any(IoTSensorData.class));

        // Act
        List<IoTSensorData> result = dataIngestionService.processIoTDataBatch(batchData);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(iotRepository, times(1)).save(any(IoTSensorData.class));
    }

    @Test
    void testProcessSocialMediaDataBatch_Success() {
        // Arrange
        List<SocialMediaData> batchData = Arrays.asList(sampleSocialData);
        when(socialMediaRepository.save(any(SocialMediaData.class))).thenReturn(sampleSocialData);
        doNothing().when(mlProcessingService).analyzeSentiment(any(SocialMediaData.class));
        doNothing().when(mlProcessingService).detectTrends(any(SocialMediaData.class));

        // Act
        List<SocialMediaData> result = dataIngestionService.processSocialMediaDataBatch(batchData);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        verify(socialMediaRepository, times(1)).save(any(SocialMediaData.class));
    }

    @Test
    void testGetIngestionStatistics() {
        // Arrange
        when(iotRepository.count()).thenReturn(100L);
        when(iotRepository.countByIsAnomalyTrue()).thenReturn(5L);
        when(iotRepository.countByTimestampAfter(any(LocalDateTime.class))).thenReturn(20L);
        when(socialMediaRepository.count()).thenReturn(50L);
        when(socialMediaRepository.countByCollectedTimestampAfter(any(LocalDateTime.class))).thenReturn(10L);
        when(socialMediaRepository.countByIsTrendingTrue()).thenReturn(3L);

        // Act
        var stats = dataIngestionService.getIngestionStatistics();

        // Assert
        assertNotNull(stats);
        assertTrue(stats.containsKey("iot"));
        assertTrue(stats.containsKey("socialMedia"));
        
        @SuppressWarnings("unchecked")
        var iotStats = (java.util.Map<String, Object>) stats.get("iot");
        assertEquals(100L, iotStats.get("totalRecords"));
        assertEquals(5L, iotStats.get("anomalies"));
        
        @SuppressWarnings("unchecked")
        var socialStats = (java.util.Map<String, Object>) stats.get("socialMedia");
        assertEquals(50L, socialStats.get("totalRecords"));
        assertEquals(3L, socialStats.get("trendingPosts"));
    }
}
