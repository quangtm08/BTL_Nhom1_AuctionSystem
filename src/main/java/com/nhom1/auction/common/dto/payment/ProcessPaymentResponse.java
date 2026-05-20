package com.nhom1.auction.common.dto.payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ProcessPaymentResponse {
    private String auctionId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime paidAt;

    public ProcessPaymentResponse() {}

    public ProcessPaymentResponse(String auctionId, BigDecimal amount, String status, LocalDateTime paidAt) {
        this.auctionId = auctionId;
        this.amount = amount;
        this.status = status;
        this.paidAt = paidAt;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
}
