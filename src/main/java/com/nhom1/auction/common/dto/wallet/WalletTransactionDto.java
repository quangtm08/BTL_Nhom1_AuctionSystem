package com.nhom1.auction.common.dto.wallet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class WalletTransactionDto {
    private String id;
    private BigDecimal amount;
    private String transactionType; // DEPOSIT, WITHDRAW, PAYMENT, RECEIPT, REFUND
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;

    public WalletTransactionDto() {}

    public WalletTransactionDto(String id, BigDecimal amount, String transactionType, String referenceId, String description, LocalDateTime createdAt) {
        this.id = id;
        this.amount = amount;
        this.transactionType = transactionType;
        this.referenceId = referenceId;
        this.description = description;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
