package com.nhom1.auction.common.dto.auction;

import java.util.List;

public class MyListingsResponse {

  private List<AuctionSummaryDto> listings;

  public MyListingsResponse() {}

  public MyListingsResponse(List<AuctionSummaryDto> listings) {
    this.listings = listings;
  }

  public List<AuctionSummaryDto> getListings() {
    return listings;
  }

  public void setListings(List<AuctionSummaryDto> listings) {
    this.listings = listings;
  }
}
