package com.analytics.app.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "youtube_trending_videos")
public class YouTubeTrendingVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String videoId;
    private String title;
    private String channelTitle;

    @Lob
@Column(columnDefinition = "TEXT")
private String description;

    private Long viewCount;
    private Long likeCount;
    private Long commentCount;

    private String publishedAt;

    private LocalDateTime fetchedAt;

    public YouTubeTrendingVideo() {}

    // Getters and Setters
    public Long getId() { return id; }

    public String getVideoId() { return videoId; }
    public void setVideoId(String videoId) { this.videoId = videoId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getChannelTitle() { return channelTitle; }
    public void setChannelTitle(String channelTitle) { this.channelTitle = channelTitle; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }

    public Long getLikeCount() { return likeCount; }
    public void setLikeCount(Long likeCount) { this.likeCount = likeCount; }

    public Long getCommentCount() { return commentCount; }
    public void setCommentCount(Long commentCount) { this.commentCount = commentCount; }

    public String getPublishedAt() { return publishedAt; }
    public void setPublishedAt(String publishedAt) { this.publishedAt = publishedAt; }

    public LocalDateTime getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(LocalDateTime fetchedAt) { this.fetchedAt = fetchedAt; }
}