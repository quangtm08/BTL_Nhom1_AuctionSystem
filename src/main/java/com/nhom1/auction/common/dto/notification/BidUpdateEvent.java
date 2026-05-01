package com.nhom1.auction.common.dto.notification;

import java.util.UUID;

public class BidUpdateEvent {
    private String auctionId;
    private double newHighestBid;
    private String newHighestBidderId;
    private String timestamp;

    public BidUpdateEvent() {}

    public BidUpdateEvent(String auctionId, double newHighestBid, String newHighestBidderId, String timestamp) {
        this.auctionId = auctionId;
        this.newHighestBid = newHighestBid;
        this.newHighestBidderId = newHighestBidderId;
        this.timestamp = timestamp;
    }

    public BidUpdateEvent(String auctionId, double newHighestBid, UUID winnerId) {
        this.auctionId = auctionId;
        this.newHighestBid = newHighestBid;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public double getNewHighestBid() {
        return newHighestBid;
    }

    public void setNewHighestBid(double newHighestBid) {
        this.newHighestBid = newHighestBid;
    }

    public String getNewHighestBidderId() {
        return newHighestBidderId;
    }

    public void setNewHighestBidderId(String newHighestBidderId) {
        this.newHighestBidderId = newHighestBidderId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }
}
