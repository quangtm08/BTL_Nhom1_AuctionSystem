package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.wallet.DepositRequest;
import com.nhom1.auction.common.dto.wallet.GetWalletRequest;
import com.nhom1.auction.common.dto.wallet.WalletResponse;
import com.nhom1.auction.common.dto.wallet.WithdrawRequest;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class WalletClientService extends BaseClientService {

    public CompletableFuture<WalletResponse> getWallet(String userId) {
        if (userId == null || userId.isBlank()) {
            return validationError("User ID is required.");
        }
        RequestMessage<GetWalletRequest> request = new RequestMessage<>(
                MessageType.GET_WALLET, new GetWalletRequest(userId)
        );
        return send(request, WalletResponse.class);
    }

    public CompletableFuture<WalletResponse> deposit(String userId, BigDecimal amount) {
        if (userId == null || userId.isBlank()) {
            return validationError("User ID is required.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return validationError("Deposit amount must be greater than zero.");
        }
        RequestMessage<DepositRequest> request = new RequestMessage<>(
                MessageType.DEPOSIT_MONEY, new DepositRequest(userId, amount)
        );
        return send(request, WalletResponse.class);
    }

    public CompletableFuture<WalletResponse> withdraw(String userId, BigDecimal amount) {
        if (userId == null || userId.isBlank()) {
            return validationError("User ID is required.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return validationError("Withdrawal amount must be greater than zero.");
        }
        RequestMessage<WithdrawRequest> request = new RequestMessage<>(
                MessageType.WITHDRAW_MONEY, new WithdrawRequest(userId, amount)
        );
        return send(request, WalletResponse.class);
    }
}
