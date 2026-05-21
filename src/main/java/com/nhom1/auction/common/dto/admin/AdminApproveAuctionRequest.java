package com.nhom1.auction.common.dto.admin;

public class AdminApproveAuctionRequest {
    private String auctionId;
    private String callerId;

    public AdminApproveAuctionRequest() {}

    public AdminApproveAuctionRequest(String auctionId, String callerId) {
        this.auctionId = auctionId;
        this.callerId = callerId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getCallerId() {
        return callerId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }
}
