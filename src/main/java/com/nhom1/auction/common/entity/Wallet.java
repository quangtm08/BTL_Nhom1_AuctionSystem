package com.nhom1.auction.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class Wallet extends BaseEntity {
  private BigDecimal balance;

  public Wallet() {
    super();
    this.balance = BigDecimal.ZERO;
  }

  public Wallet(UUID userId, BigDecimal balance) {
    super(userId, LocalDateTime.now(), LocalDateTime.now());
    this.balance = balance;
  }

  public Wallet(UUID userId, BigDecimal balance, LocalDateTime createdAt, LocalDateTime updatedAt) {
    super(userId, createdAt, updatedAt);
    this.balance = balance;
  }

  public UUID getUserId() {
    return getId();
  }

  public BigDecimal getBalance() {
    return balance;
  }

  public void setBalance(BigDecimal balance) {
    this.balance = balance;
  }
}
