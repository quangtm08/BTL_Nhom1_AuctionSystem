package com.nhom1.auction.common.dto.autobid;

public class AutoBidConfigRequest {
  private String auctionId;
  private String bidderId;
  private String maxAmount;
  private String increment;

  public AutoBidConfigRequest() {}

  public AutoBidConfigRequest(
      String auctionId, String bidderId, String maxAmount, String increment) {
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.maxAmount = maxAmount;
    this.increment = increment;
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

  public String getMaxAmount() {
    return maxAmount;
  }

  public void setMaxAmount(String maxAmount) {
    this.maxAmount = maxAmount;
  }

  public String getIncrement() {
    return increment;
  }

  public void setIncrement(String increment) {
    this.increment = increment;
  }
}
