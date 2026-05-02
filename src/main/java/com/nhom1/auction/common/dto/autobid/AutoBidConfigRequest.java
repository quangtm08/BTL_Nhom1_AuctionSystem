package com.nhom1.auction.common.dto.autobid;

public class AutoBidConfigRequest {
    private String auctionId;
    private String bidderId;
    private double maxAmount;
    private double increment;

    public AutoBidConfigRequest() {}

    public AutoBidConfigRequest(String auctionId, String bidderId, double maxAmount, double increment) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.maxAmount = maxAmount;
        this.increment = increment;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(double maxAmount) {
        this.maxAmount = maxAmount;
    }

    public double getIncrement() {
        return increment;
    }

    public void setIncrement(double increment) {
        this.increment = increment;
    }
}
