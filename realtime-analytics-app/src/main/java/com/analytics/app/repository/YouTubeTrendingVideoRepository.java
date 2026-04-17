// package com.analytics.app.repository;

// import com.analytics.app.model.YouTubeTrendingVideo;
// import org.springframework.data.jpa.repository.JpaRepository;

// public interface YouTubeTrendingVideoRepository extends JpaRepository<YouTubeTrendingVideo, Long> {
// }

package com.analytics.app.repository;

import com.analytics.app.model.YouTubeTrendingVideo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface YouTubeTrendingVideoRepository extends JpaRepository<YouTubeTrendingVideo, Long> {

    boolean existsByVideoId(String videoId);

}