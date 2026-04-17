package com.analytics.app.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_predictions")
public class FinancePrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asset_id", nullable = false)
    private String assetId;

    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "provider")
    private String provider;

    @Column(name = "current_price", nullable = false)
    private Double currentPrice;

    @Column(name = "predicted_price", nullable = false)
    private Double predictedPrice;

    @Column(name = "predicted_change_percentage")
    private Double predictedChangePercentage;

    @Column(name = "price_change_percentage_24h")
    private Double priceChangePercentage24h;

    @Column(name = "market_cap")
    private Double marketCap;

    @Column(name = "total_volume")
    private Double totalVolume;

    @Column(name = "anomaly_score")
    private Double anomalyScore;

    @Column(name = "prediction_confidence")
    private Double predictionConfidence;

    @Column(name = "trend_signal")
    private String trendSignal;

    @Column(name = "insight", length = 2000)
    private String insight;

    @Column(name = "model_version")
    private String modelVersion;

    @Column(name = "model_trained_at")
    private LocalDateTime modelTrainedAt;

    @Column(name = "event_timestamp")
    private LocalDateTime eventTimestamp;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public FinancePrediction() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Double getPredictedPrice() {
        return predictedPrice;
    }

    public void setPredictedPrice(Double predictedPrice) {
        this.predictedPrice = predictedPrice;
    }

    public Double getPredictedChangePercentage() {
        return predictedChangePercentage;
    }

    public void setPredictedChangePercentage(Double predictedChangePercentage) {
        this.predictedChangePercentage = predictedChangePercentage;
    }

    public Double getPriceChangePercentage24h() {
        return priceChangePercentage24h;
    }

    public void setPriceChangePercentage24h(Double priceChangePercentage24h) {
        this.priceChangePercentage24h = priceChangePercentage24h;
    }

    public Double getMarketCap() {
        return marketCap;
    }

    public void setMarketCap(Double marketCap) {
        this.marketCap = marketCap;
    }

    public Double getTotalVolume() {
        return totalVolume;
    }

    public void setTotalVolume(Double totalVolume) {
        this.totalVolume = totalVolume;
    }

    public Double getAnomalyScore() {
        return anomalyScore;
    }

    public void setAnomalyScore(Double anomalyScore) {
        this.anomalyScore = anomalyScore;
    }

    public Double getPredictionConfidence() {
        return predictionConfidence;
    }

    public void setPredictionConfidence(Double predictionConfidence) {
        this.predictionConfidence = predictionConfidence;
    }

    public String getTrendSignal() {
        return trendSignal;
    }

    public void setTrendSignal(String trendSignal) {
        this.trendSignal = trendSignal;
    }

    public String getInsight() {
        return insight;
    }

    public void setInsight(String insight) {
        this.insight = insight;
    }

    public String getModelVersion() {
        return modelVersion;
    }

    public void setModelVersion(String modelVersion) {
        this.modelVersion = modelVersion;
    }

    public LocalDateTime getModelTrainedAt() {
        return modelTrainedAt;
    }

    public void setModelTrainedAt(LocalDateTime modelTrainedAt) {
        this.modelTrainedAt = modelTrainedAt;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
