// package com.analytics.app.service;

// import com.analytics.app.model.SocialMediaData;
// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;
// import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
// import org.springframework.scheduling.annotation.Scheduled;
// import org.springframework.stereotype.Service;
// import org.springframework.web.client.RestTemplate;
// import org.springframework.web.util.UriComponentsBuilder;

// import java.time.OffsetDateTime;
// import java.util.ArrayList;
// import java.util.List;
// import java.util.regex.Matcher;
// import java.util.regex.Pattern;

// @Service
// @ConditionalOnProperty(value = "analytics.connectors.youtube.enabled", havingValue = "true")
// public class YouTubeProducerService {

//     private static final Logger logger = LoggerFactory.getLogger(YouTubeProducerService.class);
//     private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

//     private final RestTemplate restTemplate = new RestTemplate();

//     @Autowired
//     private ObjectMapper objectMapper;

//     @Autowired
//     private DataIngestionService dataIngestionService;

//     @Value("${connectors.youtube.api.key:}")
// private String apiKey;

//     @Value("${analytics.connectors.youtube.region:IN}")
//     private String region;

//     @Value("${analytics.connectors.youtube.fetch-max:10}")
//     private int maxResults;

//     @Scheduled(fixedDelayString = "${analytics.connectors.youtube.schedule-ms:15000}")
//     public void fetchTrendingVideos() {
//         if (apiKey == null || apiKey.isBlank()) {
//             logger.debug("Skipping YouTube connector because no API key is configured");
//             return;
//         }

//         try {
//             String url = UriComponentsBuilder
//                     .fromHttpUrl("https://www.googleapis.com/youtube/v3/videos")
//                     .queryParam("part", "snippet,statistics")
//                     .queryParam("chart", "mostPopular")
//                     .queryParam("regionCode", region)
//                     .queryParam("maxResults", maxResults)
//                     .queryParam("key", apiKey)
//                     .toUriString();

//             String response = restTemplate.getForObject(url, String.class);
//             JsonNode root = objectMapper.readTree(response);
//             JsonNode items = root.get("items");

//             if (items == null || !items.isArray()) {
//                 return;
//             }

//             for (JsonNode item : items) {
//                 JsonNode snippet = item.get("snippet");
//                 JsonNode stats = item.get("statistics");

//                 SocialMediaData socialData = new SocialMediaData();
//                 socialData.setPlatform("youtube");
//                 socialData.setPostId(item.path("id").asText());
//                 socialData.setUserId(snippet.path("channelId").asText());
//                 socialData.setUsername(snippet.path("channelTitle").asText());
//                 socialData.setContent(snippet.path("title").asText() + " " + snippet.path("description").asText(""));
//                 socialData.setLikesCount(stats.has("likeCount") ? stats.get("likeCount").asInt() : 0);
//                 socialData.setSharesCount(0);
//                 socialData.setCommentsCount(stats.has("commentCount") ? stats.get("commentCount").asInt() : 0);
//                 socialData.setFollowersCount(0);
//                 socialData.setPostTimestamp(OffsetDateTime.parse(snippet.path("publishedAt").asText()).toLocalDateTime());
//                 socialData.setHashtags(extractHashtags(socialData.getContent()));

//                 dataIngestionService.processSocialMediaData(socialData);
//             }

//             logger.info("YouTube connector ingested {} trending videos", items.size());
//         } catch (Exception e) {
//             logger.warn("YouTube connector fetch failed: {}", e.getMessage());
//         }
//     }

//     private List<String> extractHashtags(String content) {
//         List<String> hashtags = new ArrayList<>();
//         Matcher matcher = HASHTAG_PATTERN.matcher(content);
//         while (matcher.find()) {
//             hashtags.add("#" + matcher.group(1).toLowerCase());
//         }
//         return hashtags;
//     }
// }


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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.*;

@Service
@ConditionalOnProperty(value = "analytics.connectors.youtube.enabled", havingValue = "true")
public class YouTubeProducerService {

    private static final Logger logger = LoggerFactory.getLogger(YouTubeProducerService.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    // IMPORTANT: this must match your YAML path
    @Value("${youtube.api.key:}")
private String apiKey;

    @Value("${analytics.connectors.youtube.region:IN}")
    private String region;

    @Value("${analytics.connectors.youtube.fetch-max:10}")
    private int maxResults;

    @Scheduled(fixedDelayString = "${analytics.connectors.youtube.schedule-ms:60000}")
    public void fetchTrendingVideos() {

        if (apiKey == null || apiKey.isBlank()) {
            logger.warn("Skipping YouTube connector: API key is missing");
            return;
        }

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://www.googleapis.com/youtube/v3/videos")
                    .queryParam("part", "snippet,statistics")
                    .queryParam("chart", "mostPopular")
                    .queryParam("regionCode", region)
                    .queryParam("maxResults", maxResults)
                    .queryParam("key", apiKey)
                    .toUriString();

            logger.info("Fetching YouTube trending videos from region={}", region);

            String response = restTemplate.getForObject(url, String.class);

            JsonNode root = objectMapper.readTree(response);
            JsonNode items = root.get("items");

            if (items == null || !items.isArray() || items.size() == 0) {
                logger.warn("No YouTube trending items received");
                return;
            }

            List<Map<String, Object>> videosList = new ArrayList<>();

            for (JsonNode item : items) {
                JsonNode snippet = item.get("snippet");
                JsonNode stats = item.get("statistics");

                if (snippet == null) continue;

                Map<String, Object> video = new HashMap<>();

                video.put("videoId", item.path("id").asText());
                video.put("title", snippet.path("title").asText(""));
                video.put("channelTitle", snippet.path("channelTitle").asText(""));
                video.put("description", snippet.path("description").asText(""));
                video.put("publishedAt", snippet.path("publishedAt").asText(""));

                video.put("viewCount", stats != null && stats.has("viewCount") ? stats.get("viewCount").asLong() : 0L);
                video.put("likeCount", stats != null && stats.has("likeCount") ? stats.get("likeCount").asLong() : 0L);
                video.put("commentCount", stats != null && stats.has("commentCount") ? stats.get("commentCount").asLong() : 0L);

                videosList.add(video);

                
            }

            String kafkaMessage = objectMapper.writeValueAsString(videosList);

            kafkaTemplate.send("youtube-trending", kafkaMessage);

            logger.info("🔥 Published {} YouTube trending videos to Kafka topic: youtube-trending", videosList.size());

        } catch (Exception e) {
            logger.error("❌ YouTube Producer Error: {}", e.getMessage());
        }
    }
}