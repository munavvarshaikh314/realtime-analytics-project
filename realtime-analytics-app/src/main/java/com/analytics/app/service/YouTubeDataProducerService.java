package com.analytics.app.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(value = "analytics.connectors.youtube.enabled", havingValue = "true")
public class YouTubeDataProducerService {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeDataProducerService.class);

    @Value("${youtube.api.key}")
    private String apiKey;

    @Value("${analytics.connectors.youtube.fetch-max:10}")
    private int maxResults;

    @Value("${analytics.connectors.youtube.region:IN}")
    private String regionCode;

    private static final String YOUTUBE_TRENDING_URL =
        "https://www.googleapis.com/youtube/v3/videos?part=snippet,statistics&chart=mostPopular&regionCode=%s&maxResults=%d&key=%s";

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate;

    public YouTubeDataProducerService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    @Scheduled(fixedRateString = "${analytics.connectors.youtube.schedule-ms:15000}")
    public void fetchYouTubeTrendingVideos() {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.warn("YouTube API key not configured, skipping trending videos fetch");
            return;
        }

        try {
            String url = String.format(YOUTUBE_TRENDING_URL, regionCode, maxResults, apiKey);
            logger.info("Fetching YouTube trending videos from: {}", url.replace(apiKey, "***"));

            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response);

            List<Map<String, Object>> videos = new ArrayList<>();

            if (root.has("items")) {
                for (JsonNode item : root.get("items")) {
                    Map<String, Object> video = new HashMap<>();

                    JsonNode snippet = item.get("snippet");
                    JsonNode statistics = item.get("statistics");

                    video.put("videoId", item.get("id").asText());
                    video.put("title", snippet.get("title").asText());
                    video.put("channelTitle", snippet.get("channelTitle").asText());
                    video.put("description", snippet.get("description").asText());
                    video.put("publishedAt", snippet.get("publishedAt").asText());

                    // Handle statistics that might be missing or private
                    video.put("viewCount", statistics.has("viewCount") ?
                        statistics.get("viewCount").asLong() : 0L);
                    video.put("likeCount", statistics.has("likeCount") ?
                        statistics.get("likeCount").asLong() : 0L);
                    video.put("commentCount", statistics.has("commentCount") ?
                        statistics.get("commentCount").asLong() : 0L);

                    video.put("fetchedAt", LocalDateTime.now().toString());

                    videos.add(video);
                }
            }

            if (!videos.isEmpty()) {
                String message = objectMapper.writeValueAsString(videos);
                kafkaTemplate.send("youtube-trending", message);
                logger.info("Sent {} YouTube trending videos to Kafka", videos.size());
            }

        } catch (RestClientException e) {
            logger.error("Failed to fetch YouTube trending videos: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Error processing YouTube trending videos: {}", e.getMessage(), e);
        }
    }
}