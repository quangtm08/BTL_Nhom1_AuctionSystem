package com.nhom1.auction.common.dto.bidding;

public class PlaceBidResponse {
  private String bidId;
  private String newHighestBidderId;
  private String newHighestBidId;

  public PlaceBidResponse() {}

  public PlaceBidResponse(String bidId, String newHighestBidderId, String newHighestBidId) {
    this.bidId = bidId;
    this.newHighestBidderId = newHighestBidderId;
    this.newHighestBidId = newHighestBidId;
  }

  // Getters and Setters
  public String getBidId() {
    return bidId;
  }

  public void setBidId(String bidId) {
    this.bidId = bidId;
  }

  public String getNewHighestBidderId() {
    return newHighestBidderId;
  }

  public void setNewHighestBidderId(String newHighestBidderId) {
    this.newHighestBidderId = newHighestBidderId;
  }

  public String getNewHighestBidId() {
    return newHighestBidId;
  }

  public void setNewHighestBidId(String newHighestBidId) {
    this.newHighestBidId = newHighestBidId;
  }
}
