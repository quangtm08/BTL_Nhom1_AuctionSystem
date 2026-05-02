package com.nhom1.auction.common.dto.payment;

public class ProcessPaymentRequest {
    private String auctionId;
    private String bidderId;

    public ProcessPaymentRequest() {}

    public ProcessPaymentRequest(String auctionId, String bidderId) {
        this.auctionId = auctionId;
        this.bidderId = bidderId;
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
}
