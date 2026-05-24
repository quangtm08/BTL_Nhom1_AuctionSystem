package com.nhom1.auction.common.dto.bidding;

import com.nhom1.auction.common.enums.AuctionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidWithAuctionDto {
  private String auctionId;
  private String itemName;
  private BigDecimal yourBid;
  private BigDecimal currentHighestBid;
  private AuctionStatus status;
  private LocalDateTime endTime;
  private boolean winning;

  public BidWithAuctionDto() {}

  public BidWithAuctionDto(
      String auctionId,
      String itemName,
      BigDecimal yourBid,
      BigDecimal currentHighestBid,
      AuctionStatus status,
      LocalDateTime endTime,
      boolean winning) {
    this.auctionId = auctionId;
    this.itemName = itemName;
    this.yourBid = yourBid;
    this.currentHighestBid = currentHighestBid;
    this.status = status;
    this.endTime = endTime;
    this.winning = winning;
  }

  public String getAuctionId() {
    return auctionId;
  }

  public String getItemName() {
    return itemName;
  }

  public BigDecimal getYourBid() {
    return yourBid;
  }

  public BigDecimal getCurrentHighestBid() {
    return currentHighestBid;
  }

  public AuctionStatus getStatus() {
    return status;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public boolean isWinning() {
    return winning;
  }

  public void setWinning(boolean winning) {
    this.winning = winning;
  }
}
