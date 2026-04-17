package com.analytics.app.service;

import com.analytics.app.model.SocialMediaData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@ConditionalOnProperty(value = "analytics.connectors.reddit.enabled", havingValue = "true")
public class RedditProducerService {

    private static final Logger logger = LoggerFactory.getLogger(RedditProducerService.class);
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#(\\w+)");

    private final RestTemplate restTemplate = new RestTemplate();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DataIngestionService dataIngestionService;

    @Value("${analytics.connectors.reddit.subreddits:investing,stocks,cryptocurrency}")
    private String subredditList;

    @Value("${analytics.connectors.reddit.fetch-limit:8}")
    private int fetchLimit;

    @Value("${analytics.connectors.reddit.user-agent:realtime-analytics-app/1.0}")
    private String userAgent;

    @Scheduled(fixedDelayString = "${analytics.connectors.reddit.schedule-ms:20000}")
    public void fetchRedditPosts() {
        String[] subreddits = subredditList.split(",");
        for (String subreddit : subreddits) {
            fetchSubreddit(subreddit.trim());
        }
    }

    private void fetchSubreddit(String subreddit) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.USER_AGENT, userAgent);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://www.reddit.com/r/" + subreddit + "/hot.json?limit=" + fetchLimit,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode children = root.path("data").path("children");
            if (!children.isArray()) {
                return;
            }

            for (JsonNode child : children) {
                JsonNode data = child.path("data");

                SocialMediaData socialData = new SocialMediaData();
                socialData.setPlatform("reddit");
                socialData.setPostId(data.path("id").asText());
                socialData.setUserId(data.path("author_fullname").asText(""));
                socialData.setUsername(data.path("author").asText(""));
                socialData.setContent(data.path("title").asText() + " " + data.path("selftext").asText(""));
                socialData.setLikesCount(data.path("ups").asInt(0));
                socialData.setSharesCount(0);
                socialData.setCommentsCount(data.path("num_comments").asInt(0));
                socialData.setFollowersCount(0);
                socialData.setPostTimestamp(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(data.path("created_utc").asLong()),
                        ZoneOffset.UTC
                ));
                socialData.setHashtags(extractHashtags(socialData.getContent()));

                dataIngestionService.processSocialMediaData(socialData);
            }

            logger.info("Reddit connector ingested posts from r/{}", subreddit);
        } catch (Exception e) {
            logger.warn("Reddit connector fetch failed for r/{}: {}", subreddit, e.getMessage());
        }
    }

    private List<String> extractHashtags(String content) {
        List<String> hashtags = new ArrayList<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        while (matcher.find()) {
            hashtags.add("#" + matcher.group(1).toLowerCase());
        }
        return hashtags;
    }
}
