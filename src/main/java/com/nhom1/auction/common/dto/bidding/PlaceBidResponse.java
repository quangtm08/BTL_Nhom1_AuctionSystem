package com.nhom1.auction.common.dto.bidding;

import java.util.UUID;

public class PlaceBidResponse {
    private UUID bidId;
    private UUID newHighestBidderId;
    private UUID newHighestBidId;

    public PlaceBidResponse() {}

    public PlaceBidResponse(UUID bidId, UUID newHighestBidderId, UUID newHighestBidId) {
        this.bidId = bidId;
        this.newHighestBidderId = newHighestBidderId;
        this.newHighestBidId = newHighestBidId;
    }

    // Getters and Setters
    public UUID getBidId() {
        return bidId;
    }

    public void setBidId(UUID bidId) {
        this.bidId = bidId;
    }

    public UUID getNewHighestBidderId() {
        return newHighestBidderId;
    }

    public void setNewHighestBidderId(UUID newHighestBidderId) {
        this.newHighestBidderId = newHighestBidderId;
    }

    public UUID getNewHighestBidId() {
        return newHighestBidId;
    }

    public void setNewHighestBidId(UUID newHighestBidId) {
        this.newHighestBidId = newHighestBidId;
    }
}
