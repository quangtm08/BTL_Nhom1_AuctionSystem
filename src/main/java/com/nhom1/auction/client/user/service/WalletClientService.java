package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.wallet.DepositRequest;
import com.nhom1.auction.common.dto.wallet.GetWalletRequest;
import com.nhom1.auction.common.dto.wallet.WalletResponse;
import com.nhom1.auction.common.dto.wallet.WithdrawRequest;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class WalletClientService extends BaseClientService {

  public CompletableFuture<WalletResponse> getWallet() {
    String userId = currentUserId();
    if (userId == null) return validationError("No user session. Please sign in again.");
    RequestMessage<GetWalletRequest> request =
        new RequestMessage<>(MessageType.GET_WALLET, new GetWalletRequest(userId));
    return send(request, WalletResponse.class);
  }

  public CompletableFuture<WalletResponse> deposit(BigDecimal amount) {
    String userId = currentUserId();
    if (userId == null) return validationError("No user session. Please sign in again.");
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return validationError("Deposit amount must be greater than zero.");
    }
    RequestMessage<DepositRequest> request =
        new RequestMessage<>(MessageType.DEPOSIT_MONEY, new DepositRequest(userId, amount));
    return send(request, WalletResponse.class);
  }

  public CompletableFuture<WalletResponse> withdraw(BigDecimal amount) {
    String userId = currentUserId();
    if (userId == null) return validationError("No user session. Please sign in again.");
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      return validationError("Withdrawal amount must be greater than zero.");
    }
    RequestMessage<WithdrawRequest> request =
        new RequestMessage<>(MessageType.WITHDRAW_MONEY, new WithdrawRequest(userId, amount));
    return send(request, WalletResponse.class);
  }

  private String currentUserId() {
    return AppContext.getCurrentUser() == null ? null : AppContext.getCurrentUser().getUserID();
  }
}
