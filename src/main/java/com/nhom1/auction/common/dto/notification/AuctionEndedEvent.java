package com.nhom1.auction.common.dto.notification;

public class AuctionEndedEvent {
    private String auctionId;
    private String winnerId;
    private double finalPrice;

    public AuctionEndedEvent() {}

    public AuctionEndedEvent(String auctionId, String winnerId, double finalPrice) {
        this.auctionId = auctionId;
        this.winnerId = winnerId;
        this.finalPrice = finalPrice;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getWinnerId() {
        return winnerId;
    }

    public void setWinnerId(String winnerId) {
        this.winnerId = winnerId;
    }

    public double getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(double finalPrice) {
        this.finalPrice = finalPrice;
    }
}
