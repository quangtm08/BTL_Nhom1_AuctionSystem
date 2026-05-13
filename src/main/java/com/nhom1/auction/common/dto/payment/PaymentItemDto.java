package com.nhom1.auction.common.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentItemDto {
    private String auctionId;
    private String itemName;
    private String itemCategory;
    private BigDecimal amount;
    private LocalDateTime eventTime;
    private String statusLabel;

    public PaymentItemDto() {}

    public PaymentItemDto(
            String auctionId,
            String itemName,
            String itemCategory,
            BigDecimal amount,
            LocalDateTime eventTime,
            String statusLabel) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.itemCategory = itemCategory;
        this.amount = amount;
        this.eventTime = eventTime;
        this.statusLabel = statusLabel;
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

    public LocalDateTime getEventTime() {
        return eventTime;
    }

    public void setEventTime(LocalDateTime eventTime) {
        this.eventTime = eventTime;
    }

    public String getStatusLabel() {
        return statusLabel;
    }

    public void setStatusLabel(String statusLabel) {
        this.statusLabel = statusLabel;
    }
}
