package com.nhom1.auction.common.dto.notification;

import java.math.BigDecimal;

public class AuctionEndedEvent {
    private String auctionId;
    private String winnerId;
    private BigDecimal finalPrice;

    public AuctionEndedEvent() {}

    public AuctionEndedEvent(String auctionId, String winnerId, BigDecimal finalPrice) {
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

    public BigDecimal getFinalPrice() {
        return finalPrice;
    }

    public void setFinalPrice(BigDecimal finalPrice) {
        this.finalPrice = finalPrice;
    }
}
