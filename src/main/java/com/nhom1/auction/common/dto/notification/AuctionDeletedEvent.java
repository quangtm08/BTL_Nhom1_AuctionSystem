package com.nhom1.auction.common.dto.notification;

public class AuctionDeletedEvent {
    private String auctionId;

    public AuctionDeletedEvent() {}

    public AuctionDeletedEvent(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }
}
