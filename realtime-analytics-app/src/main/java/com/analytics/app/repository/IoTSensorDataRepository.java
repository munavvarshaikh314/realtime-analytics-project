package com.analytics.app.repository;

import com.analytics.app.model.IoTSensorData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface IoTSensorDataRepository extends JpaRepository<IoTSensorData, Long> {
    
    /**
     * Find sensor data by device ID
     */
    List<IoTSensorData> findByDeviceId(String deviceId);
    
    /**
     * Find sensor data by sensor type
     */
    List<IoTSensorData> findBySensorType(String sensorType);
    
    /**
     * Find sensor data by device ID and sensor type
     */
    List<IoTSensorData> findByDeviceIdAndSensorType(String deviceId, String sensorType);
    
    /**
     * Find sensor data within a time range
     */
    List<IoTSensorData> findByTimestampBetween(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Find anomalous sensor data
     */
    List<IoTSensorData> findByIsAnomalyTrue();
    
    /**
     * Find sensor data by device ID within a time range
     */
    List<IoTSensorData> findByDeviceIdAndTimestampBetween(String deviceId, LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * Count anomalous records
     */
    long countByIsAnomalyTrue();
    
    /**
     * Count records after a specific timestamp
     */
    long countByTimestampAfter(LocalDateTime timestamp);
    
    /**
     * Find latest sensor data for each device
     */
    @Query("SELECT s FROM IoTSensorData s WHERE s.timestamp = " +
           "(SELECT MAX(s2.timestamp) FROM IoTSensorData s2 WHERE s2.deviceId = s.deviceId)")
    List<IoTSensorData> findLatestDataForEachDevice();
    
    /**
     * Find sensor data with anomaly score above threshold
     */
    List<IoTSensorData> findByAnomalyScoreGreaterThan(Double threshold);
    
    /**
     * Find sensor data by location
     */
    List<IoTSensorData> findByLocation(String location);
    
    /**
     * Find sensor data within geographic bounds
     */
    @Query("SELECT s FROM IoTSensorData s WHERE s.latitude BETWEEN :minLat AND :maxLat " +
           "AND s.longitude BETWEEN :minLon AND :maxLon")
    List<IoTSensorData> findByGeographicBounds(@Param("minLat") Double minLatitude,
                                               @Param("maxLat") Double maxLatitude,
                                               @Param("minLon") Double minLongitude,
                                               @Param("maxLon") Double maxLongitude);
    
    /**
     * Get average sensor value by sensor type within time range
     */
    @Query("SELECT AVG(s.sensorValue) FROM IoTSensorData s WHERE s.sensorType = :sensorType " +
           "AND s.timestamp BETWEEN :startTime AND :endTime")
    Double getAverageSensorValue(@Param("sensorType") String sensorType,
                                @Param("startTime") LocalDateTime startTime,
                                @Param("endTime") LocalDateTime endTime);
    
    /**
     * Get sensor data statistics by device
     */
    @Query("SELECT s.deviceId, COUNT(s), AVG(s.sensorValue), MIN(s.sensorValue), MAX(s.sensorValue) " +
           "FROM IoTSensorData s WHERE s.sensorType = :sensorType " +
           "GROUP BY s.deviceId")
    List<Object[]> getSensorStatisticsByDevice(@Param("sensorType") String sensorType);
}

