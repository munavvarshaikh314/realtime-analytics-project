package com.analytics.app.repository;

import com.analytics.app.model.SocialMediaData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SocialMediaDataRepository extends JpaRepository<SocialMediaData, Long> {
    
    /**
     * Find social media data by platform
     */
    List<SocialMediaData> findByPlatform(String platform);
    
    /**
     * Find social media data by user ID
     */
    List<SocialMediaData> findByUserId(String userId);
    
    /**
     * Find social media data by username
     */
    List<SocialMediaData> findByUsername(String username);
    
    /**
     * Find social media data within a time range
     */
    List<SocialMediaData> findByPostTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find social media data collected within a time range
     */
    List<SocialMediaData> findByCollectedTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find trending posts
     */
    List<SocialMediaData> findByIsTrendingTrue();
    
    /**
     * Find posts by sentiment label
     */
    List<SocialMediaData> findBySentimentLabel(String sentimentLabel);
    
    /**
     * Find posts with sentiment score above threshold
     */
    List<SocialMediaData> findBySentimentScoreGreaterThan(Double threshold);
    
    /**
     * Find posts with sentiment score below threshold
     */
    List<SocialMediaData> findBySentimentScoreLessThan(Double threshold);
    
    /**
     * Count trending posts
     */
    long countByIsTrendingTrue();
    
    /**
     * Count posts collected after a specific timestamp
     */
    long countByCollectedTimestampAfter(LocalDateTime timestamp);
    
    /**
     * Find posts containing specific hashtags
     */
    @Query("SELECT s FROM SocialMediaData s JOIN s.hashtags h WHERE h = :hashtag")
    List<SocialMediaData> findByHashtag(@Param("hashtag") String hashtag);
    
    /**
     * Find posts mentioning specific users
     */
    @Query("SELECT s FROM SocialMediaData s JOIN s.mentions m WHERE m = :mention")
    List<SocialMediaData> findByMention(@Param("mention") String mention);
    
    /**
     * Find posts with high engagement rate
     */
    List<SocialMediaData> findByEngagementRateGreaterThan(Double threshold);
    
    /**
     * Find viral posts (high virality score)
     */
    List<SocialMediaData> findByViralityScoreGreaterThan(Double threshold);
    
    /**
     * Get top hashtags by frequency
     */
    @Query("SELECT h, COUNT(h) as frequency FROM SocialMediaData s JOIN s.hashtags h " +
           "WHERE s.collectedTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY h ORDER BY frequency DESC")
    List<Object[]> getTopHashtags(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);
    
    /**
     * Get sentiment distribution by platform
     */
    @Query("SELECT s.platform, s.sentimentLabel, COUNT(s) FROM SocialMediaData s " +
           "WHERE s.collectedTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY s.platform, s.sentimentLabel")
    List<Object[]> getSentimentDistributionByPlatform(@Param("startTime") LocalDateTime startTime,
                                                      @Param("endTime") LocalDateTime endTime);
    
    /**
     * Get average engagement metrics by platform
     */
    @Query("SELECT s.platform, AVG(s.likesCount), AVG(s.sharesCount), AVG(s.commentsCount), AVG(s.engagementRate) " +
           "FROM SocialMediaData s WHERE s.collectedTimestamp BETWEEN :startTime AND :endTime " +
           "GROUP BY s.platform")
    List<Object[]> getEngagementMetricsByPlatform(@Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find posts by language
     */
    List<SocialMediaData> findByLanguage(String language);
    
    /**
     * Find posts by location
     */
    List<SocialMediaData> findByLocation(String location);
    
    /**
     * Get posts with highest influence score
     */
    List<SocialMediaData> findTop10ByOrderByInfluenceScoreDesc();
    
    /**
     * Search posts by content keywords
     */
    @Query("SELECT s FROM SocialMediaData s WHERE LOWER(s.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<SocialMediaData> findByContentContaining(@Param("keyword") String keyword);
}

