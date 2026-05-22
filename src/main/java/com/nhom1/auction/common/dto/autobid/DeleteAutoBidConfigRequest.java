package com.nhom1.auction.common.dto.autobid;

public class DeleteAutoBidConfigRequest {
    private String auctionId;
    private String bidderId;

    public DeleteAutoBidConfigRequest() {
    }

    public DeleteAutoBidConfigRequest(String auctionId, String bidderId) {
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
