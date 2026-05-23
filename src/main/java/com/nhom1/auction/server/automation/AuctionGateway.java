package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-team contract: - Implemented by auction module (member owning AuctionRepository). - Keeps
 * scheduler independent from concrete repository classes.
 */
public interface AuctionGateway {
  List<Auction> findAll();

  Optional<Auction> findById(UUID auctionId);

  void updateStatus(UUID auctionId, AuctionStatus status);

  void updateEndTime(UUID auctionId, LocalDateTime newEndTime);
}
