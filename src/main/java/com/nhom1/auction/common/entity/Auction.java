package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.BidType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

public class Auction extends BaseEntity {

  private static final BigDecimal MIN_INCREMENT_RATE = new BigDecimal("0.05");
  private static final int MIN_INCREMENT_SCALE = 2;
  private final UUID itemId;
  private final UUID sellerId;
  private final LocalDateTime startTime;
  private LocalDateTime endTime;
  private final BigDecimal startingPrice;
  private final Integer durationDays;
  // Optimistic locking using version field in the database
  // The database increments the version.
  private final Integer version;

  private volatile UUID highestBidderId;
  private volatile BigDecimal currentHighestBid;
  private volatile AuctionStatus status;

  public Auction(
      UUID itemId,
      UUID sellerId,
      BigDecimal startingPrice,
      LocalDateTime startTime,
      LocalDateTime endTime) {
    if (itemId == null) {
      throw new IllegalArgumentException("itemId must not be null");
    }
    if (sellerId == null) {
      throw new IllegalArgumentException("sellerId must not be null");
    }
    if (startingPrice == null || startingPrice.compareTo(BigDecimal.ZERO) < 0) {
      throw new IllegalArgumentException("startingPrice must not be null or negative");
    }
    if (startTime == null || endTime == null) {
      throw new IllegalArgumentException("startTime and endTime must not be null");
    }
    if (!endTime.isAfter(startTime)) {
      throw new IllegalArgumentException("endTime must be after startTime");
    }

    this.itemId = itemId;
    this.sellerId = sellerId;
    this.startingPrice = startingPrice;
    this.startTime = startTime;
    this.endTime = endTime;

    this.highestBidderId = null;
    this.currentHighestBid = null;
    this.status = AuctionStatus.OPEN;
    this.durationDays = null;
    this.version = 0;
  }

  // Use this constructor for loading an EXISTING auction from the database.
  public Auction(
      UUID id,
      UUID itemId,
      UUID sellerId,
      BigDecimal startingPrice,
      LocalDateTime startTime,
      LocalDateTime endTime,
      UUID highestBidderId,
      BigDecimal currentHighestBid,
      AuctionStatus status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      Integer durationDays) {
    this(
        id,
        itemId,
        sellerId,
        startingPrice,
        startTime,
        endTime,
        highestBidderId,
        currentHighestBid,
        status,
        createdAt,
        updatedAt,
        durationDays,
        0);
  }

  public Auction(
      UUID id,
      UUID itemId,
      UUID sellerId,
      BigDecimal startingPrice,
      LocalDateTime startTime,
      LocalDateTime endTime,
      UUID highestBidderId,
      BigDecimal currentHighestBid,
      AuctionStatus status,
      LocalDateTime createdAt,
      LocalDateTime updatedAt,
      Integer durationDays,
      Integer version) {
    super(id, createdAt, updatedAt);
    this.itemId = itemId;
    this.sellerId = sellerId;
    this.startingPrice = startingPrice;
    this.startTime = startTime;
    this.endTime = endTime;
    this.highestBidderId = highestBidderId;
    this.currentHighestBid = currentHighestBid;
    this.status = status;
    this.durationDays = durationDays;
    this.version = version;
  }

  public BidTransaction placeBid(
      UUID bidderId, BigDecimal amount, BidType bidType, LocalDateTime bidTime) {
    AuctionBidValidator.validatePlaceBid(this, bidderId, amount, bidType, bidTime);

    BidTransaction bidTransaction = new BidTransaction(getId(), bidderId, amount, bidType);
    highestBidderId = bidderId;
    currentHighestBid = amount;
    touchUpdatedAt();
    return bidTransaction;
  }

  // Used by AuctionScheduler for anti-sniping
  public void extendEndTime(LocalDateTime newEndTime) {
    this.endTime = newEndTime;
    touchUpdatedAt();
  }

  public UUID getItemId() {
    return itemId;
  }

  public UUID getSellerId() {
    return sellerId;
  }

  public BigDecimal getStartingPrice() {
    return startingPrice;
  }

  public BigDecimal getMinBidIncrement() {
    return startingPrice
        .multiply(MIN_INCREMENT_RATE)
        .setScale(MIN_INCREMENT_SCALE, RoundingMode.HALF_UP);
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public Integer getDurationDays() {
    return durationDays;
  }

  public UUID getHighestBidderId() {
    return highestBidderId;
  }

  public BigDecimal getCurrentHighestBid() {
    return currentHighestBid;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public long getVersion() {
    return version;
  }
}
