package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.BidType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends BaseEntity {
  private static final BigDecimal MIN_INCREMENT_RATE = new BigDecimal("0.05");
  private static final int MIN_INCREMENT_SCALE = 2;
  private final UUID itemId;
  private final UUID sellerId;
  private final LocalDateTime startTime;
  private LocalDateTime endTime;
  private final BigDecimal startingPrice;
  private final List<BidTransaction> bidHistory;
  private final Integer durationDays;
  // Persistence-only concurrency token used by optimistic locking in the repository.
  // Business logic does not mutate this field directly; the database increments it.
  private final Integer version;

  // volatile để đảm bảo giá trị được cập nhật và đồng bộ hóa, thread-safe !
  private volatile UUID highestBidderId;
  private volatile BigDecimal currentHighestBid;
  private volatile AuctionStatus status;

  // thêm ReentrantLock đảm bảo thread-safety khi thay đổi trạng thái của phiên.
  private final ReentrantLock auctionLock =
      new ReentrantLock(true); // Fair lock để đảm bảo thứ tự truy cập công bằng
  // giữa các thread.
  private final Object bidHistoryMonitor =
      new Object(); // Monitor riêng để đồng bộ hóa truy cập vào bidHistory

  // TODO: Change exception to domain exception of the app.
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
    this.bidHistory = new ArrayList<>();
    this.durationDays = null;
    this.version = 0;
  }

  /** Use this constructor for loading an EXISTING auction from the database. */
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
    this.bidHistory = new ArrayList<>();
    this.version = version;
  }

  public BidTransaction placeBid(
      UUID bidderId, BigDecimal amount, BidType bidType, LocalDateTime bidTime) {

    auctionLock.lock(); // Lock to ensure thread safety when placing a bid
    try {
      AuctionBidValidator.validatePlaceBid(this, bidderId, amount, bidType, bidTime);

      BidTransaction bidTransaction = new BidTransaction(getId(), bidderId, amount, bidType);
      synchronized (
          bidHistoryMonitor) { // Synchronize to ensure thread safety when modifying the bid history
        bidHistory.add(bidTransaction);
      }
      highestBidderId = bidderId;
      currentHighestBid = amount;
      touchUpdatedAt();
      return bidTransaction;
    } finally {
      auctionLock.unlock();
    }
  }

  // Return a snapshot so callers cannot modify the auction's internal bid
  // history.
  public List<BidTransaction> getBidHistory() {
    synchronized (
        bidHistoryMonitor) { // Synchronize on the same monitor used when modifying the bid history
      return List.copyOf(bidHistory);
    }
  }

  // Used by AuctionScheduler for anti-sniping: extends the end time when a bid is
  // placed near the deadline.
  public void extendEndTime(LocalDateTime newEndTime) {
    auctionLock.lock();
    try {
      this.endTime = newEndTime;
      touchUpdatedAt();
    } finally {
      auctionLock.unlock();
    }
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
