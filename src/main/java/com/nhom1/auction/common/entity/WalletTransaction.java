package com.nhom1.auction.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class WalletTransaction {
  private UUID id;
  private UUID userId;
  private BigDecimal amount;
  private String transactionType; // DEPOSIT, WITHDRAW, PAYMENT, RECEIPT, REFUND
  private String referenceId;
  private String description;
  private LocalDateTime createdAt;

  public WalletTransaction() {}

  public WalletTransaction(
      UUID id,
      UUID userId,
      BigDecimal amount,
      String transactionType,
      String referenceId,
      String description,
      LocalDateTime createdAt) {
    this.id = id;
    this.userId = userId;
    this.amount = amount;
    this.transactionType = transactionType;
    this.referenceId = referenceId;
    this.description = description;
    this.createdAt = createdAt;
  }

  public WalletTransaction(
      UUID userId,
      BigDecimal amount,
      String transactionType,
      String referenceId,
      String description) {
    this.id = UUID.randomUUID();
    this.userId = userId;
    this.amount = amount;
    this.transactionType = transactionType;
    this.referenceId = referenceId;
    this.description = description;
    this.createdAt = LocalDateTime.now();
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getUserId() {
    return userId;
  }

  public void setUserId(UUID userId) {
    this.userId = userId;
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
