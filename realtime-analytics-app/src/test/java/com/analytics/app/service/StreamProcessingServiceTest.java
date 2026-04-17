package com.analytics.app.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class StreamProcessingServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private StreamProcessingService streamProcessingService;

    @Test
    void testProcessSocialMediaStream_AcknowledgesJsonPayloadWithoutTrendingFlag() {
        String message = "{"
                + "\"platform\":\"instagram\","
                + "\"postId\":\"instagram_123\","
                + "\"username\":\"@user823\","
                + "\"content\":\"Air quality sensors showing excellent readings today #environment #health\","
                + "\"sentimentScore\":0.9,"
                + "\"sentimentLabel\":\"POSITIVE\""
                + "}";

        streamProcessingService.processSocialMediaStream(message, "social-media-feed", 0, acknowledgment);

        verify(webSocketService, times(1)).sendSocialMediaUpdate(any());
        verify(webSocketService, never()).sendAlert(any());
        verify(kafkaTemplate, times(1)).send(eq("processed-analytics-data"), anyString());
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void testProcessSocialMediaStream_SendsAlertWhenPostIsTrending() {
        String message = "{"
                + "\"platform\":\"instagram\","
                + "\"postId\":\"instagram_123\","
                + "\"username\":\"@user823\","
                + "\"content\":\"Air quality sensors showing excellent readings today #environment #health\","
                + "\"sentimentScore\":0.9,"
                + "\"sentimentLabel\":\"POSITIVE\","
                + "\"isTrending\":true,"
                + "\"influenceScore\":88.5"
                + "}";

        streamProcessingService.processSocialMediaStream(message, "social-media-feed", 2, acknowledgment);

        verify(webSocketService, times(1)).sendAlert(any());
        verify(webSocketService, times(1)).sendSocialMediaUpdate(any());
        verify(kafkaTemplate, times(1)).send(eq("processed-analytics-data"), anyString());
        verify(acknowledgment, times(1)).acknowledge();
    }
}
