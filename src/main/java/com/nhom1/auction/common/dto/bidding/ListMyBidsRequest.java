package com.nhom1.auction.common.dto.bidding;

public class ListMyBidsRequest {
  private String bidderId;

  public ListMyBidsRequest() {}

  public ListMyBidsRequest(String bidderId) {
    this.bidderId = bidderId;
  }

  public String getBidderId() {
    return bidderId;
  }

  public void setBidderId(String bidderId) {
    this.bidderId = bidderId;
  }
}
