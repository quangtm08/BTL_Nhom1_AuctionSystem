package com.nhom1.auction.common.dto.wallet;

import java.math.BigDecimal;

public class WithdrawRequest {
  private String userId;
  private BigDecimal amount;

  public WithdrawRequest() {}

  public WithdrawRequest(String userId, BigDecimal amount) {
    this.userId = userId;
    this.amount = amount;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
