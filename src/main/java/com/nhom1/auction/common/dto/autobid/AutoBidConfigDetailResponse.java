package com.nhom1.auction.common.dto.autobid;

public class AutoBidConfigDetailResponse {
  private String auctionId;
  private String bidderId;
  private String maxAmount;
  private String increment;
  private boolean configured;

  public AutoBidConfigDetailResponse() {}

  public AutoBidConfigDetailResponse(
      String auctionId, String bidderId, String maxAmount, String increment, boolean configured) {
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.maxAmount = maxAmount;
    this.increment = increment;
    this.configured = configured;
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

  public boolean isConfigured() {
    return configured;
  }

  public void setConfigured(boolean configured) {
    this.configured = configured;
  }
}
