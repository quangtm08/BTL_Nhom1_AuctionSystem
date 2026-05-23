package com.nhom1.auction.common.dto.admin;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import java.util.List;

public class AdminAuctionListResponse {
  private List<AuctionSummaryDto> auctions;

  public AdminAuctionListResponse() {}

  public AdminAuctionListResponse(List<AuctionSummaryDto> auctions) {
    this.auctions = auctions;
  }

  public List<AuctionSummaryDto> getAuctions() {
    return auctions;
  }

  public void setAuctions(List<AuctionSummaryDto> auctions) {
    this.auctions = auctions;
  }
}
