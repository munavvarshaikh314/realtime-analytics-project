package com.analytics.app.model;

import com.analytics.app.config.FlexibleLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "social_media_data")
public class SocialMediaData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Platform is required")
    @Column(name = "platform")
    private String platform; // twitter, facebook, instagram, linkedin
    
    @NotBlank(message = "Post ID is required")
    @Column(name = "post_id")
    private String postId;
    
    @Column(name = "user_id")
    private String userId;
    
    @Column(name = "username")
    private String username;
    
   @NotBlank(message = "Content is required")
@Lob
@Column(name = "content", columnDefinition = "TEXT")
private String content;
    
    @Column(name = "hashtags")
    @ElementCollection
    private List<String> hashtags;
    
    @Column(name = "mentions")
    @ElementCollection
    private List<String> mentions;
    
    @Column(name = "likes_count")
    private Integer likesCount = 0;
    
    @Column(name = "shares_count")
    private Integer sharesCount = 0;
    
    @Column(name = "comments_count")
    private Integer commentsCount = 0;
    
    @Column(name = "followers_count")
    private Integer followersCount = 0;
    
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    @Column(name = "post_timestamp")
    private LocalDateTime postTimestamp;
    
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    @Column(name = "collected_timestamp")
    private LocalDateTime collectedTimestamp;
    
    @Column(name = "language")
    private String language;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "sentiment_score")
    private Double sentimentScore;
    
    @Column(name = "sentiment_label")
    private String sentimentLabel; // POSITIVE, NEGATIVE, NEUTRAL
    
    @Column(name = "emotion")
    private String emotion; // JOY, ANGER, FEAR, SADNESS, etc.
    
    @Column(name = "is_trending")
    private Boolean isTrending = false;
    
    @Column(name = "influence_score")
    private Double influenceScore;
    
    @Column(name = "engagement_rate")
    private Double engagementRate;
    
    @Column(name = "virality_score")
    private Double viralityScore;
    
    // Constructors
    public SocialMediaData() {
        this.collectedTimestamp = LocalDateTime.now();
    }
    
    public SocialMediaData(String platform, String postId, String content) {
        this();
        this.platform = platform;
        this.postId = postId;
        this.content = content;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getPlatform() {
        return platform;
    }
    
    public void setPlatform(String platform) {
        this.platform = platform;
    }
    
    public String getPostId() {
        return postId;
    }
    
    public void setPostId(String postId) {
        this.postId = postId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public List<String> getHashtags() {
        return hashtags;
    }
    
    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
    }
    
    public List<String> getMentions() {
        return mentions;
    }
    
    public void setMentions(List<String> mentions) {
        this.mentions = mentions;
    }
    
    public Integer getLikesCount() {
        return likesCount;
    }
    
    public void setLikesCount(Integer likesCount) {
        this.likesCount = likesCount;
    }
    
    public Integer getSharesCount() {
        return sharesCount;
    }
    
    public void setSharesCount(Integer sharesCount) {
        this.sharesCount = sharesCount;
    }
    
    public Integer getCommentsCount() {
        return commentsCount;
    }
    
    public void setCommentsCount(Integer commentsCount) {
        this.commentsCount = commentsCount;
    }
    
    public Integer getFollowersCount() {
        return followersCount;
    }
    
    public void setFollowersCount(Integer followersCount) {
        this.followersCount = followersCount;
    }
    
    public LocalDateTime getPostTimestamp() {
        return postTimestamp;
    }
    
    public void setPostTimestamp(LocalDateTime postTimestamp) {
        this.postTimestamp = postTimestamp;
    }
    
    public LocalDateTime getCollectedTimestamp() {
        return collectedTimestamp;
    }
    
    public void setCollectedTimestamp(LocalDateTime collectedTimestamp) {
        this.collectedTimestamp = collectedTimestamp;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public Double getSentimentScore() {
        return sentimentScore;
    }
    
    public void setSentimentScore(Double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }
    
    public String getSentimentLabel() {
        return sentimentLabel;
    }
    
    public void setSentimentLabel(String sentimentLabel) {
        this.sentimentLabel = sentimentLabel;
    }
    
    public String getEmotion() {
        return emotion;
    }
    
    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
    
    public Boolean getIsTrending() {
        return isTrending;
    }
    
    public void setIsTrending(Boolean isTrending) {
        this.isTrending = isTrending;
    }
    
    public Double getInfluenceScore() {
        return influenceScore;
    }
    
    public void setInfluenceScore(Double influenceScore) {
        this.influenceScore = influenceScore;
    }
    
    public Double getEngagementRate() {
        return engagementRate;
    }
    
    public void setEngagementRate(Double engagementRate) {
        this.engagementRate = engagementRate;
    }
    
    public Double getViralityScore() {
        return viralityScore;
    }
    
    public void setViralityScore(Double viralityScore) {
        this.viralityScore = viralityScore;
    }
    
    @Override
    public String toString() {
        return "SocialMediaData{" +
                "id=" + id +
                ", platform='" + platform + '\'' +
                ", postId='" + postId + '\'' +
                ", username='" + username + '\'' +
                ", content='" + content + '\'' +
                ", sentimentLabel='" + sentimentLabel + '\'' +
                ", postTimestamp=" + postTimestamp +
                '}';
    }
}
