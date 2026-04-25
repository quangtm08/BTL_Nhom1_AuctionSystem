package com.nhom1.auction.common.dto.bidding;

import java.math.BigDecimal;
import java.util.UUID;
public class PlaceBidRequest {
    private UUID auctionId;
    private UUID bidderId;
    private BigDecimal amount;

    public PlaceBidRequest() {}

    public PlaceBidRequest(UUID auctionId, UUID bidderId, BigDecimal amount) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
    }
// Getters and Setters:
    public UUID getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(UUID auctionId) {
        this.auctionId = auctionId;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public void setBidderId(UUID bidderId) {
        this.bidderId = bidderId;
    }

    public BigDecimal getBidAmount() {
        return amount;
    }

    public void setBidAmount(BigDecimal amount) {
        this.amount = amount;
    }
}