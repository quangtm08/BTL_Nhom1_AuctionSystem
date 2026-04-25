package com.nhom1.auction.common.dto.bidding;
public class GetAuctionDetailRequest {
    private String auctionId;

    public GetAuctionDetailRequest() {}

    public GetAuctionDetailRequest(String auctionId) {
        this.auctionId = auctionId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }
}