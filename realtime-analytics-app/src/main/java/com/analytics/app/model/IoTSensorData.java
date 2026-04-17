package com.analytics.app.model;

import com.analytics.app.config.FlexibleLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "iot_sensor_data")
public class IoTSensorData {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Device ID is required")
    @Column(name = "device_id")
    private String deviceId;
    
    @NotBlank(message = "Sensor type is required")
    @Column(name = "sensor_type")
    private String sensorType;
    
    @NotNull(message = "Sensor value is required")
    @Column(name = "sensor_value")
    private Double sensorValue;
    
    @Column(name = "unit")
    private String unit;
    
    @Column(name = "location")
    private String location;
    
    @Column(name = "latitude")
    private Double latitude;
    
    @Column(name = "longitude")
    private Double longitude;
    
    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    @Column(name = "timestamp")
    private LocalDateTime timestamp;
    
    @ElementCollection
    @CollectionTable(name = "sensor_metadata", joinColumns = @JoinColumn(name = "sensor_data_id"))
    @MapKeyColumn(name = "metadata_key")
    @Column(name = "metadata_value")
    private Map<String, String> metadata;
    
    @Column(name = "is_anomaly")
    private Boolean isAnomaly = false;
    
    @Column(name = "anomaly_score")
    private Double anomalyScore;
    
    // Constructors
    public IoTSensorData() {
        this.timestamp = LocalDateTime.now();
    }
    
    public IoTSensorData(String deviceId, String sensorType, Double sensorValue, String unit) {
        this();
        this.deviceId = deviceId;
        this.sensorType = sensorType;
        this.sensorValue = sensorValue;
        this.unit = unit;
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDeviceId() {
        return deviceId;
    }
    
    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }
    
    public String getSensorType() {
        return sensorType;
    }
    
    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }
    
    public Double getSensorValue() {
        return sensorValue;
    }
    
    public void setSensorValue(Double sensorValue) {
        this.sensorValue = sensorValue;
    }
    
    public String getUnit() {
        return unit;
    }
    
    public void setUnit(String unit) {
        this.unit = unit;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public Double getLatitude() {
        return latitude;
    }
    
    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }
    
    public Double getLongitude() {
        return longitude;
    }
    
    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }
    
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
    
    public Boolean getIsAnomaly() {
        return isAnomaly;
    }
    
    public void setIsAnomaly(Boolean isAnomaly) {
        this.isAnomaly = isAnomaly;
    }
    
    public Double getAnomalyScore() {
        return anomalyScore;
    }
    
    public void setAnomalyScore(Double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }
    
    @Override
    public String toString() {
        return "IoTSensorData{" +
                "id=" + id +
                ", deviceId='" + deviceId + '\'' +
                ", sensorType='" + sensorType + '\'' +
                ", sensorValue=" + sensorValue +
                ", unit='" + unit + '\'' +
                ", location='" + location + '\'' +
                ", timestamp=" + timestamp +
                ", isAnomaly=" + isAnomaly +
                '}';
    }
}
