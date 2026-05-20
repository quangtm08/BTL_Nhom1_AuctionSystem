package com.nhom1.auction.common.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PendingPaymentDto {
    private String auctionId;
    private String itemName;
    private String itemCategory;
    private BigDecimal amount;
    private LocalDateTime endTime;

    public PendingPaymentDto() {
    }

    public PendingPaymentDto(String auctionId, String itemName, String itemCategory, BigDecimal amount, LocalDateTime endTime) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.itemCategory = itemCategory;
        this.amount = amount;
        this.endTime = endTime;
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

    public String getItemCategory() {
        return itemCategory;
    }

    public void setItemCategory(String itemCategory) {
        this.itemCategory = itemCategory;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
