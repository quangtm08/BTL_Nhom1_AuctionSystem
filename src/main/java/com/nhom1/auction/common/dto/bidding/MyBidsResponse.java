package com.nhom1.auction.common.dto.bidding;

import java.util.List;

public class MyBidsResponse {
  private List<BidWithAuctionDto> bids;

  public MyBidsResponse() {}

  public MyBidsResponse(List<BidWithAuctionDto> bids) {
    this.bids = bids;
  }

  public List<BidWithAuctionDto> getBids() {
    return bids;
  }

  public void setBids(List<BidWithAuctionDto> bids) {
    this.bids = bids;
  }
}
