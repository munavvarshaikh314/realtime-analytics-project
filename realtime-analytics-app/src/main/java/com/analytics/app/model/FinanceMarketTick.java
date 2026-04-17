package com.analytics.app.model;

import com.analytics.app.config.FlexibleLocalDateTimeDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

@Entity
@Table(name = "finance_market_ticks")
public class FinanceMarketTick {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Asset ID is required")
    @Column(name = "asset_id", nullable = false)
    private String assetId;

    @NotBlank(message = "Symbol is required")
    @Column(name = "symbol", nullable = false)
    private String symbol;

    @Column(name = "provider")
    private String provider;

    @Column(name = "market_segment")
    private String marketSegment = "CRYPTO";

    @NotNull(message = "Current price is required")
    @Column(name = "current_price", nullable = false)
    private Double currentPrice;

    @Column(name = "high_24h")
    private Double high24h;

    @Column(name = "low_24h")
    private Double low24h;

    @Column(name = "market_cap")
    private Double marketCap;

    @Column(name = "total_volume")
    private Double totalVolume;

    @Column(name = "price_change_percentage_1h")
    private Double priceChangePercentage1h;

    @Column(name = "price_change_percentage_24h")
    private Double priceChangePercentage24h;

    @JsonDeserialize(using = FlexibleLocalDateTimeDeserializer.class)
    @Column(name = "event_timestamp")
    private LocalDateTime eventTimestamp;

    @Column(name = "ingested_at")
    private LocalDateTime ingestedAt;

    public FinanceMarketTick() {
        this.eventTimestamp = LocalDateTime.now();
        this.ingestedAt = LocalDateTime.now();
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

    public String getMarketSegment() {
        return marketSegment;
    }

    public void setMarketSegment(String marketSegment) {
        this.marketSegment = marketSegment;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public Double getHigh24h() {
        return high24h;
    }

    public void setHigh24h(Double high24h) {
        this.high24h = high24h;
    }

    public Double getLow24h() {
        return low24h;
    }

    public void setLow24h(Double low24h) {
        this.low24h = low24h;
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

    public Double getPriceChangePercentage1h() {
        return priceChangePercentage1h;
    }

    public void setPriceChangePercentage1h(Double priceChangePercentage1h) {
        this.priceChangePercentage1h = priceChangePercentage1h;
    }

    public Double getPriceChangePercentage24h() {
        return priceChangePercentage24h;
    }

    public void setPriceChangePercentage24h(Double priceChangePercentage24h) {
        this.priceChangePercentage24h = priceChangePercentage24h;
    }

    public LocalDateTime getEventTimestamp() {
        return eventTimestamp;
    }

    public void setEventTimestamp(LocalDateTime eventTimestamp) {
        this.eventTimestamp = eventTimestamp;
    }

    public LocalDateTime getIngestedAt() {
        return ingestedAt;
    }

    public void setIngestedAt(LocalDateTime ingestedAt) {
        this.ingestedAt = ingestedAt;
    }
}
