package com.nhom1.auction.server.auction;

import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.server.automation.AuctionGateway;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class AuctionGatewayImpl implements AuctionGateway {
  private final AuctionRepository auctionRepository;

  public AuctionGatewayImpl(AuctionRepository auctionRepository) {
    this.auctionRepository = auctionRepository;
  }

  @Override
  public List<Auction> findAll() {
    return auctionRepository.findAll();
  }

  @Override
  public Optional<Auction> findById(UUID auctionId) {
    return auctionRepository.findById(auctionId);
  }

  @Override
  public void updateStatus(UUID auctionId, AuctionStatus status) {
    auctionRepository.updateStatus(auctionId, status);
  }

  @Override
  public void updateEndTime(UUID auctionId, LocalDateTime newEndTime) {
    auctionRepository.updateEndTime(auctionId, newEndTime);
  }
}
