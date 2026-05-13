package com.nhom1.auction.common.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentHistoryEntryDto {
    private String auctionId;
    private String itemName;
    private BigDecimal amount;
    private String direction;
    private LocalDateTime paidAt;

    public PaymentHistoryEntryDto() {
    }

    public PaymentHistoryEntryDto(String auctionId, String itemName, BigDecimal amount, String direction, LocalDateTime paidAt) {
        this.auctionId = auctionId;
        this.itemName = itemName;
        this.amount = amount;
        this.direction = direction;
        this.paidAt = paidAt;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
