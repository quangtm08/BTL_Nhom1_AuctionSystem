package com.nhom1.auction.server.automation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/** Value object for one auto-bid configuration per (auction, bidder). */
public class AutoBidConfig {
  private final UUID auctionId;
  private final UUID bidderId;
  private final BigDecimal maxAmount;
  private final BigDecimal increment;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  public AutoBidConfig(UUID auctionId, UUID bidderId, BigDecimal maxAmount, BigDecimal increment) {
    this(auctionId, bidderId, maxAmount, increment, LocalDateTime.now(), LocalDateTime.now());
  }

  public AutoBidConfig(
      UUID auctionId,
      UUID bidderId,
      BigDecimal maxAmount,
      BigDecimal increment,
      LocalDateTime createdAt) {
    this(auctionId, bidderId, maxAmount, increment, createdAt, createdAt);
  }

  public AutoBidConfig(
      UUID auctionId,
      UUID bidderId,
      BigDecimal maxAmount,
      BigDecimal increment,
      LocalDateTime createdAt,
      LocalDateTime updatedAt) {
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.maxAmount = maxAmount;
    this.increment = increment;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public UUID getAuctionId() {
    return auctionId;
  }

  public UUID getBidderId() {
    return bidderId;
  }

  public BigDecimal getMaxAmount() {
    return maxAmount;
  }

  public BigDecimal getIncrement() {
    return increment;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }
}
