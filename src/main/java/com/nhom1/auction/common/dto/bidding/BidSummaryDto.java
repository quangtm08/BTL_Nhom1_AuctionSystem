package com.nhom1.auction.common.dto.bidding;

import com.nhom1.auction.common.enums.BidType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidSummaryDto {
  private String bidId;
  private String bidderId;
  private BigDecimal amount;
  private BidType bidType;
  private LocalDateTime createdAt;
  private String bidderName;

  public BidSummaryDto() {}

  public BidSummaryDto(
      String bidId,
      String bidderId,
      BigDecimal amount,
      BidType bidType,
      LocalDateTime createdAt,
      String bidderName) {
    this.bidId = bidId;
    this.bidderId = bidderId;
    this.amount = amount;
    this.bidType = bidType;
    this.createdAt = createdAt;
    this.bidderName = bidderName;
  }

  public String getBidId() {
    return bidId;
  }

  public String getBidderId() {
    return bidderId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public BidType getBidType() {
    return bidType;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public String getBidderName() {
    return bidderName;
  }
}
