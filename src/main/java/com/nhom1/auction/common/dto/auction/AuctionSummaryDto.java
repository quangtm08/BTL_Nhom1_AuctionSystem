package com.nhom1.auction.common.dto.auction;

public class AuctionSummaryDto {
    private String auctionId;
    private String itemId;
    private String itemName;
    private String itemCategory;
    private String status;
    private double currentHighestBid;
    private String highestBidderId;
    private String endTime;
    private String sellerId;

    public AuctionSummaryDto() {}

    public AuctionSummaryDto(
            String auctionId,
            String itemId,
            String itemName,
            String itemCategory,
            String status,
            double currentHighestBid,
            String highestBidderId,
            String endTime,
            String sellerId) {
        this.auctionId = auctionId;
        this.itemId = itemId;
        this.itemName = itemName;
        this.itemCategory = itemCategory;
        this.status = status;
        this.currentHighestBid = currentHighestBid;
        this.highestBidderId = highestBidderId;
        this.endTime = endTime;
        this.sellerId = sellerId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getItemId() {
        return itemId;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getCurrentHighestBid() {
        return currentHighestBid;
    }

    public void setCurrentHighestBid(double currentHighestBid) {
        this.currentHighestBid = currentHighestBid;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }
}
