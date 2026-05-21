package com.nhom1.auction.common.dto.auction;

import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import java.time.LocalDateTime;

public class UpdateAuctionRequest {
    private String auctionId;
    private String sellerId;
    private String name;
    private String description;
    private ItemCategory category;
    private ItemCondition condition;
    private LocalDateTime endTime;

    public String getAuctionId() { return auctionId; }
    public void setAuctionId(String auctionId) { this.auctionId = auctionId; }
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ItemCategory getCategory() { return category; }
    public void setCategory(ItemCategory category) { this.category = category; }
    public ItemCondition getCondition() { return condition; }
    public void setCondition(ItemCondition condition) { this.condition = condition; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}
