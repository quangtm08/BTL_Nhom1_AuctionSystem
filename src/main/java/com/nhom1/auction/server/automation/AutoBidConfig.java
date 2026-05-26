package com.nhom1.auction.server.automation;

import java.math.BigDecimal;
import java.util.UUID;

/** Value object for one auto-bid configuration per (auction, bidder). */
public class AutoBidConfig {
  private final UUID auctionId;
  private final UUID bidderId;
  private final BigDecimal maxAmount;
  private final BigDecimal increment;
  private final java.time.LocalDateTime createdAt;

  public AutoBidConfig(UUID auctionId, UUID bidderId, BigDecimal maxAmount, BigDecimal increment) {
    this(auctionId, bidderId, maxAmount, increment, java.time.LocalDateTime.now());
  }

  public AutoBidConfig(
      UUID auctionId,
      UUID bidderId,
      BigDecimal maxAmount,
      BigDecimal increment,
      java.time.LocalDateTime createdAt) {
    this.auctionId = auctionId;
    this.bidderId = bidderId;
    this.maxAmount = maxAmount;
    this.increment = increment;
    this.createdAt = createdAt;
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

  public java.time.LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
