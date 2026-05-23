package com.nhom1.auction.common.dto.auction;

public class DeleteAuctionRequest {
  private String sellerId;
  private String auctionId;

  public DeleteAuctionRequest() {}

  public DeleteAuctionRequest(String sellerId, String auctionId) {
    this.sellerId = sellerId;
    this.auctionId = auctionId;
  }

  public String getSellerId() {
    return sellerId;
  }

  public void setSellerId(String sellerId) {
    this.sellerId = sellerId;
  }

  public String getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(String auctionId) {
    this.auctionId = auctionId;
  }
}
