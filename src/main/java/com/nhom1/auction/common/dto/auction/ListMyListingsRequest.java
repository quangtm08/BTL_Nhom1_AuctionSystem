package com.nhom1.auction.common.dto.auction;

public class ListMyListingsRequest {
  private String sellerId;

  public ListMyListingsRequest() {}

  public ListMyListingsRequest(String sellerId) {
    this.sellerId = sellerId;
  }

  public String getSellerId() {
    return sellerId;
  }

  public void setSellerId(String sellerId) {
    this.sellerId = sellerId;
  }
}
