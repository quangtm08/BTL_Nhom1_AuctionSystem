package com.nhom1.auction.common.dto.auction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.nhom1.auction.common.enums.AuctionStatus;


public class AuctionSummaryDto {

    
    private String id;
    private String itemName;
    private String itemCategory;
    private BigDecimal startingPrice;
    private BigDecimal currentHighestBid;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private AuctionStatus status;
    private String sellerId;

    public AuctionSummaryDto() {}

    public AuctionSummaryDto(String id, String itemName, String itemCategory, BigDecimal startingPrice,
            BigDecimal currentHighestBid, LocalDateTime startTime, LocalDateTime endTime,
            AuctionStatus status, String sellerId) {
        this.id = id;
        this.itemName = itemName;
        this.itemCategory = itemCategory;
        this.startingPrice = startingPrice;
        this.currentHighestBid = currentHighestBid;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
        this.sellerId = sellerId;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public String getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(BigDecimal currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
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

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
}
