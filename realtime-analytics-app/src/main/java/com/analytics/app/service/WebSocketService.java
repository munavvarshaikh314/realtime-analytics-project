package com.analytics.app.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebSocketService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebSocketService.class);
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    /**
     * Send IoT data updates to connected clients
     */
    public void sendIoTUpdate(Map<String, Object> iotData) {
        try {
            messagingTemplate.convertAndSend("/topic/iot-updates", iotData);
            logger.debug("Sent IoT update to WebSocket clients");
        } catch (Exception e) {
            logger.error("Error sending IoT update via WebSocket: {}", e.getMessage());
        }
    }
    
    /**
     * Send social media data updates to connected clients
     */
    public void sendSocialMediaUpdate(Map<String, Object> socialData) {
        try {
            messagingTemplate.convertAndSend("/topic/social-updates", socialData);
            logger.debug("Sent social media update to WebSocket clients");
        } catch (Exception e) {
            logger.error("Error sending social media update via WebSocket: {}", e.getMessage());
        }
    }
    
    /**
     * Send ML prediction results to connected clients
     */
    public void sendMLPrediction(Map<String, Object> prediction) {
        try {
            messagingTemplate.convertAndSend("/topic/ml-predictions", prediction);
            logger.debug("Sent ML prediction to WebSocket clients");
        } catch (Exception e) {
            logger.error("Error sending ML prediction via WebSocket: {}", e.getMessage());
        }
    }
    
    /**
     * Send alerts to connected clients
     */
    public void sendAlert(Map<String, Object> alert) {
        try {
            messagingTemplate.convertAndSend("/topic/alerts", alert);
            logger.info("Sent alert to WebSocket clients: {}", alert.get("alertType"));
        } catch (Exception e) {
            logger.error("Error sending alert via WebSocket: {}", e.getMessage());
        }
    }
    
    /**
     * Send real-time metrics to connected clients
     */
    public void sendMetrics(Map<String, Object> metrics) {
        try {
            messagingTemplate.convertAndSend("/topic/metrics", metrics);
            logger.debug("Sent metrics to WebSocket clients");
        } catch (Exception e) {
            logger.error("Error sending metrics via WebSocket: {}", e.getMessage());
        }
    }

    /**
     * Send finance streaming updates to connected clients
     */
    public void sendFinanceUpdate(Map<String, Object> financeData) {
        try {
            messagingTemplate.convertAndSend("/topic/finance", financeData);
            logger.debug("Sent finance update to WebSocket clients");
        } catch (Exception e) {
            logger.error("Error sending finance update via WebSocket: {}", e.getMessage());
        }
    }
    
    /**
     * Send dashboard updates to connected clients
     */
    public void sendDashboardUpdate(Map<String, Object> dashboardData) {
        try {
            messagingTemplate.convertAndSend("/topic/dashboard", dashboardData);
            logger.debug("Sent dashboard update to WebSocket clients");
        } catch (Exception e) {
            logger.error("Error sending dashboard update via WebSocket: {}", e.getMessage());
        }
    }
    
    /**
     * Send system status updates to connected clients
     */
    public void sendSystemStatus(Map<String, Object> status) {
        try {
            messagingTemplate.convertAndSend("/topic/system-status", status);
            logger.debug("Sent system status to WebSocket clients");
        } catch (Exception e) {
            logger.error("Error sending system status via WebSocket: {}", e.getMessage());
        }
    }
    
    /**
     * Send personalized updates to specific user
     */
    public void sendUserUpdate(String userId, Map<String, Object> data) {
        try {
            messagingTemplate.convertAndSendToUser(userId, "/queue/updates", data);
            logger.debug("Sent personalized update to user: {}", userId);
        } catch (Exception e) {
            logger.error("Error sending user update via WebSocket: {}", e.getMessage());
        }
    }
}
