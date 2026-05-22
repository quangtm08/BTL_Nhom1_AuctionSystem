package com.nhom1.auction.common.dto.admin;

public class AdminApproveAuctionRequest {
    private String auctionId;
    private String callerId;
    private String openingDate;

    public AdminApproveAuctionRequest() {}

    public AdminApproveAuctionRequest(String auctionId, String callerId) {
        this(auctionId, callerId, null);
    }

    public AdminApproveAuctionRequest(String auctionId, String callerId, String openingDate) {
        this.auctionId = auctionId;
        this.callerId = callerId;
        this.openingDate = openingDate;
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

    public String getOpeningDate() {
        return openingDate;
    }

    public void setOpeningDate(String openingDate) {
        this.openingDate = openingDate;
    }
}
