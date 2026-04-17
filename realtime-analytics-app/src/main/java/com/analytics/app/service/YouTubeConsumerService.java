package com.analytics.app.service;

import com.analytics.app.model.YouTubeTrendingVideo;
import com.analytics.app.repository.YouTubeTrendingVideoRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(value = "analytics.connectors.youtube.enabled", havingValue = "true")
public class YouTubeConsumerService {

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private YouTubeTrendingVideoRepository repository;

    @Autowired
    private YouTubeWebSocketService webSocketService;
    
    @Autowired
    private MLProcessingService mlProcessingService;

    @KafkaListener(topics = "youtube-trending", groupId = "analytics-group")
    public void consumeTrendingVideos(String message) {
        try {
            List<Map<String, Object>> videos =
                    objectMapper.readValue(message, new TypeReference<List<Map<String, Object>>>() {});

            for (Map<String, Object> v : videos) {
                YouTubeTrendingVideo entity = new YouTubeTrendingVideo();

                entity.setVideoId((String) v.get("videoId"));
                entity.setTitle((String) v.get("title"));
                entity.setChannelTitle((String) v.get("channelTitle"));
                entity.setDescription((String) v.get("description"));
                entity.setPublishedAt((String) v.get("publishedAt"));

                entity.setViewCount(v.get("viewCount") == null ? 0L : ((Number) v.get("viewCount")).longValue());
                entity.setLikeCount(v.get("likeCount") == null ? 0L : ((Number) v.get("likeCount")).longValue());
                entity.setCommentCount(v.get("commentCount") == null ? 0L : ((Number) v.get("commentCount")).longValue());

                entity.setFetchedAt(LocalDateTime.now());

                if (!repository.existsByVideoId(entity.getVideoId())) {
                    repository.save(entity);
                    
                    // Perform ML analysis on the video
                    Map<String, Object> analysis = mlProcessingService.analyzeYouTubeVideo(entity);
                    
                    // Add analysis results to the video data for WebSocket
                    v.put("analysis", analysis);
                }
            
            }

            webSocketService.sendYouTubeUpdate(videos);

            System.out.println("🔥 YouTube Trending Videos saved + sent to WebSocket");

        } catch (Exception e) {
            System.out.println("❌ YouTube Consumer Error: " + e.getMessage());
        }
    }
}
