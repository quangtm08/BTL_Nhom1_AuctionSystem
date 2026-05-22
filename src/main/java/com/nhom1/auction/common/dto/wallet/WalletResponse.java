package com.nhom1.auction.common.dto.wallet;

import java.math.BigDecimal;
import java.util.List;

public class WalletResponse {
    private String userId;
    private BigDecimal balance;
    private List<WalletTransactionDto> transactions;

    public WalletResponse() {}

    public WalletResponse(String userId, BigDecimal balance, List<WalletTransactionDto> transactions) {
        this.userId = userId;
        this.balance = balance;
        this.transactions = transactions;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public List<WalletTransactionDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<WalletTransactionDto> transactions) {
        this.transactions = transactions;
    }
}
