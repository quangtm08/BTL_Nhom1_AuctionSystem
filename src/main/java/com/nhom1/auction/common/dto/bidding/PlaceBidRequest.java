package com.nhom1.auction.common.dto.bidding;

import java.math.BigDecimal;

public class PlaceBidRequest {
  private String auctionId;
  private String bidderId;
  private BigDecimal amount;

  public PlaceBidRequest() {}

  public PlaceBidRequest(String auctionId, String bidderId, BigDecimal amount) {
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.amount = amount;
  }

  // Getters and Setters:
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

  public BigDecimal getBidAmount() {
    return amount;
  }

  public void setBidAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
