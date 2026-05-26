package com.nhom1.auction.common.dto.auction;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class CreateAuctionRequest {

  private String name;
  private String description;
  private ItemCategory category;
  private ItemCondition condition;

  private BigDecimal startingPrice;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private Integer durationDays;

  private String sellerId;
  private List<String> imageUrls;

  public CreateAuctionRequest() {}

  public CreateAuctionRequest(
      String name,
      String description,
      ItemCategory category,
      ItemCondition condition,
      BigDecimal startingPrice,
      LocalDateTime startTime,
      LocalDateTime endTime,
      Integer durationDays,
      String sellerId) {
    this.name = name;
    this.description = description;
    this.category = category;
    this.condition = condition;
    this.startingPrice = startingPrice;
    this.startTime = startTime;
    this.endTime = endTime;
    this.durationDays = durationDays;
    this.sellerId = sellerId;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public ItemCategory getCategory() {
    return category;
  }

  public void setCategory(ItemCategory category) {
    this.category = category;
  }

  public ItemCondition getCondition() {
    return condition;
  }

  public void setCondition(ItemCondition condition) {
    this.condition = condition;
  }

  public BigDecimal getStartingPrice() {
    return startingPrice;
  }

  public void setStartingPrice(BigDecimal startingPrice) {
    this.startingPrice = startingPrice;
  }

  public LocalDateTime getStartTime() {
    return startTime;
  }

  public void setStartTime(LocalDateTime startTime) {
    this.startTime = startTime;
  }

  public LocalDateTime getEndTime() {
    return endTime;
  }

  public void setEndTime(LocalDateTime endTime) {
    this.endTime = endTime;
  }

  public Integer getDurationDays() {
    return durationDays;
  }

  public void setDurationDays(Integer durationDays) {
    this.durationDays = durationDays;
  }

  public String getSellerId() {
    return sellerId;
  }

  public void setSellerId(String sellerId) {
    this.sellerId = sellerId;
  }

  public List<String> getImageUrls() {
    return imageUrls;
  }

  public void setImageUrls(List<String> imageUrls) {
    this.imageUrls = imageUrls;
  }
}
