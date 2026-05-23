package com.nhom1.auction.common.dto.notification;

import java.math.BigDecimal;

public class NewAuctionEvent {
  private String auctionId;
  private String itemName;
  private BigDecimal startingPrice;

  public NewAuctionEvent() {}

  public NewAuctionEvent(String auctionId, String itemName, BigDecimal startingPrice) {
    this.auctionId = auctionId;
    this.itemName = itemName;
    this.startingPrice = startingPrice;
  }

  public String getAuctionId() {
    return auctionId;
  }

  public void setAuctionId(String auctionId) {
    this.auctionId = auctionId;
  }

  public String getItemName() {
    return itemName;
  }

  public void setItemName(String itemName) {
    this.itemName = itemName;
  }

  public BigDecimal getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(BigDecimal startingPrice) {
    this.startingPrice = startingPrice;
  }
}
