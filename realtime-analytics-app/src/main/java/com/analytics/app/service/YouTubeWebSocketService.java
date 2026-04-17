package com.analytics.app.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class YouTubeWebSocketService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public void sendYouTubeUpdate(Object data) {
        messagingTemplate.convertAndSend("/topic/youtube", data);
    }
}