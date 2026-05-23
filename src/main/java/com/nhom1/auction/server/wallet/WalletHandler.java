package com.nhom1.auction.server.wallet;

import com.nhom1.auction.common.dto.wallet.DepositRequest;
import com.nhom1.auction.common.dto.wallet.GetWalletRequest;
import com.nhom1.auction.common.dto.wallet.WalletResponse;
import com.nhom1.auction.common.dto.wallet.WithdrawRequest;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.ResponseFactory;
import java.util.UUID;

public class WalletHandler {
  private final WalletService walletService;

  public WalletHandler(WalletService walletService) {
    this.walletService = walletService;
  }

  public void register(MessageRouter router) {
    router.register(
        MessageType.GET_WALLET,
        (requestId, payloadJson) -> {
          try {
            GetWalletRequest dto = JsonUtil.fromJson(payloadJson, GetWalletRequest.class);
            UUID userId = UUID.fromString(dto.getUserId());
            WalletResponse response = walletService.getWallet(userId);
            return ResponseFactory.success(requestId, response);
          } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
          }
        });

    router.register(
        MessageType.DEPOSIT_MONEY,
        (requestId, payloadJson) -> {
          try {
            DepositRequest dto = JsonUtil.fromJson(payloadJson, DepositRequest.class);
            UUID userId = UUID.fromString(dto.getUserId());
            walletService.deposit(userId, dto.getAmount());
            WalletResponse response = walletService.getWallet(userId);
            return ResponseFactory.success(requestId, response);
          } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
          }
        });

    router.register(
        MessageType.WITHDRAW_MONEY,
        (requestId, payloadJson) -> {
          try {
            WithdrawRequest dto = JsonUtil.fromJson(payloadJson, WithdrawRequest.class);
            UUID userId = UUID.fromString(dto.getUserId());
            walletService.withdraw(userId, dto.getAmount());
            WalletResponse response = walletService.getWallet(userId);
            return ResponseFactory.success(requestId, response);
          } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
          }
        });
  }
}
