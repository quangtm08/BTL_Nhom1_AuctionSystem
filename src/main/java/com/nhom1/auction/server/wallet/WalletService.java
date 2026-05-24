package com.nhom1.auction.server.wallet;

import com.nhom1.auction.common.dto.wallet.WalletResponse;
import com.nhom1.auction.common.dto.wallet.WalletTransactionDto;
import com.nhom1.auction.common.entity.Wallet;
import com.nhom1.auction.common.entity.WalletTransaction;
import com.nhom1.auction.common.exception.ValidationException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class WalletService {
  private final WalletRepository walletRepository;

  public WalletService(WalletRepository walletRepository) {
    this.walletRepository = walletRepository;
  }

  // Business operations
  public void deposit(UUID userId, BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ValidationException("Deposit amount must be greater than zero.");
    }

    Wallet wallet = getOrCreateWallet(userId);
    BigDecimal newBalance = wallet.getBalance().add(amount);
    wallet.setBalance(newBalance);
    wallet.touchUpdatedAt();
    walletRepository.save(wallet);

    WalletTransaction tx =
        new WalletTransaction(userId, amount, "DEPOSIT", null, "Deposited money via Mock Wallet");
    walletRepository.saveTransaction(tx);
  }

  public void withdraw(UUID userId, BigDecimal amount) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ValidationException("Withdrawal amount must be greater than zero.");
    }

    Wallet wallet = getOrCreateWallet(userId);
    if (wallet.getBalance().compareTo(amount) < 0) {
      throw new ValidationException("Insufficient wallet balance.");
    }

    BigDecimal newBalance = wallet.getBalance().subtract(amount);
    wallet.setBalance(newBalance);
    wallet.touchUpdatedAt();
    walletRepository.save(wallet);

    WalletTransaction tx =
        new WalletTransaction(userId, amount, "WITHDRAW", null, "Withdrew money from Mock Wallet");
    walletRepository.saveTransaction(tx);
  }

  public void transfer(
      UUID fromUserId,
      UUID toUserId,
      BigDecimal amount,
      String referenceId,
      String description,
      Connection conn) {
    if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ValidationException("Transfer amount must be greater than zero.");
    }

    Wallet fromWallet = getOrCreateWallet(fromUserId, conn);
    if (fromWallet.getBalance().compareTo(amount) < 0) {
      throw new ValidationException("Insufficient wallet balance to make this payment.");
    }

    Wallet toWallet = getOrCreateWallet(toUserId, conn);

    BigDecimal newFromBalance = fromWallet.getBalance().subtract(amount);
    fromWallet.setBalance(newFromBalance);
    fromWallet.touchUpdatedAt();
    walletRepository.save(fromWallet, conn);

    BigDecimal newToBalance = toWallet.getBalance().add(amount);
    toWallet.setBalance(newToBalance);
    toWallet.touchUpdatedAt();
    walletRepository.save(toWallet, conn);

    WalletTransaction fromTx =
        new WalletTransaction(fromUserId, amount, "PAYMENT", referenceId, description);
    walletRepository.saveTransaction(fromTx, conn);

    WalletTransaction toTx =
        new WalletTransaction(toUserId, amount, "RECEIPT", referenceId, description);
    walletRepository.saveTransaction(toTx, conn);
  }

  // Query operations
  public WalletResponse getWallet(UUID userId) {
    Wallet wallet = getOrCreateWallet(userId);
    List<WalletTransaction> txs = walletRepository.findTransactionsByUserId(userId);

    List<WalletTransactionDto> txDtos =
        txs.stream()
            .map(
                tx ->
                    new WalletTransactionDto(
                        tx.getId().toString(),
                        tx.getAmount(),
                        tx.getTransactionType(),
                        tx.getReferenceId(),
                        tx.getDescription(),
                        tx.getCreatedAt()))
            .collect(Collectors.toList());

    return new WalletResponse(userId.toString(), wallet.getBalance(), txDtos);
  }

  public Wallet getOrCreateWallet(UUID userId) {
    return walletRepository
        .findByUserId(userId)
        .orElseGet(
            () -> {
              Wallet wallet = new Wallet(userId, new BigDecimal("100000.00"));
              walletRepository.save(wallet);
              return wallet;
            });
  }

  public Wallet getOrCreateWallet(UUID userId, Connection conn) {
    return walletRepository
        .findByUserId(userId, conn)
        .orElseGet(
            () -> {
              Wallet wallet = new Wallet(userId, new BigDecimal("100000.00"));
              walletRepository.save(wallet, conn);
              return wallet;
            });
  }
}
