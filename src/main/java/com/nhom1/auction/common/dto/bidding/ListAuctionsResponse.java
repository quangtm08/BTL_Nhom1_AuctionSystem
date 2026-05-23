package com.nhom1.auction.common.dto.bidding;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import java.util.List;

public class ListAuctionsResponse {
  private List<AuctionSummaryDto> auctions;

  public ListAuctionsResponse() {}

  public List<AuctionSummaryDto> getAuctions() {
    return auctions;
  }

  public void setAuctions(List<AuctionSummaryDto> auctions) {
    this.auctions = auctions;
  }
}
