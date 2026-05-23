package com.nhom1.auction.common.dto.notification;

import java.time.LocalDateTime;

public class AuctionTimeExtendedEvent {
  private String auctionId;
  private LocalDateTime newEndTime;
  private LocalDateTime triggeredAt;

  public AuctionTimeExtendedEvent() {}

  public AuctionTimeExtendedEvent(
      String auctionId, LocalDateTime newEndTime, LocalDateTime triggeredAt) {
    this.auctionId = auctionId;
    this.newEndTime = newEndTime;
    this.triggeredAt = triggeredAt;
  }

  public String getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(String auctionId) {
    this.auctionId = auctionId;
  }

  public LocalDateTime getNewEndTime() {
    return newEndTime;
  }

  public void setNewEndTime(LocalDateTime newEndTime) {
    this.newEndTime = newEndTime;
  }

  public LocalDateTime getTriggeredAt() {
    return triggeredAt;
  }

  public void setTriggeredAt(LocalDateTime triggeredAt) {
    this.triggeredAt = triggeredAt;
  }
}
