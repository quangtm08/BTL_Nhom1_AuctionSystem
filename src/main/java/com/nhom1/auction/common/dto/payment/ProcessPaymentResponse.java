package com.nhom1.auction.common.dto.payment;

import com.nhom1.auction.common.enums.AuctionStatus;

public class ProcessPaymentResponse {
    private String auctionId;
    private AuctionStatus newStatus;
    private String message;

    public ProcessPaymentResponse() {}

    public ProcessPaymentResponse(String auctionId, AuctionStatus newStatus, String message) {
        this.auctionId = auctionId;
        this.newStatus = newStatus;
        this.message = message;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public AuctionStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(AuctionStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
