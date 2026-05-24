package com.nhom1.auction.server.admin;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import java.util.List;

public interface AdminAuctionGateway {
  List<AuctionSummaryDto> findAllAuctionSummaries();

  boolean cancelAuctionById(String auctionId);
}
