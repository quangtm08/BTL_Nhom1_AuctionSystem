package com.nhom1.auction.common.dto.auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.nhom1.auction.common.enums.AuctionStatus;

public class CreateAuctionResponse {

    private String id;
    private String itemId;
    private String sellerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String highestBidderId;
    private BigDecimal currentHighestBid;
    private AuctionStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public CreateAuctionResponse() {}

    public CreateAuctionResponse(String id, String itemId, String sellerId, LocalDateTime startTime, LocalDateTime endTime, String highestBidderId, BigDecimal currentHighestBid, AuctionStatus status, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.highestBidderId = highestBidderId;
        this.currentHighestBid = currentHighestBid;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and setters...
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(BigDecimal currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}