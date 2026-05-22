package com.nhom1.auction.common.dto.notification;

import java.math.BigDecimal;

public class WalletUpdateEvent {
    private String userId;
    private BigDecimal newBalance;

    public WalletUpdateEvent() {}

    public WalletUpdateEvent(String userId, BigDecimal newBalance) {
        this.userId = userId;
        this.newBalance = newBalance;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public void setNewBalance(BigDecimal newBalance) {
        this.newBalance = newBalance;
    }
}
