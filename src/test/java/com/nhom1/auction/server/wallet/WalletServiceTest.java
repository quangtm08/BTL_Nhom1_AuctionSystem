package com.nhom1.auction.server.wallet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.wallet.WalletResponse;
import com.nhom1.auction.common.entity.Wallet;
import com.nhom1.auction.common.entity.WalletTransaction;
import com.nhom1.auction.common.exception.ValidationException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WalletServiceTest {

  private WalletRepository mockWalletRepository;
  private WalletService walletService;
  private Connection mockConnection;

  @BeforeEach
  public void setUp() {
    mockWalletRepository = mock(WalletRepository.class);
    mockConnection = mock(Connection.class);
    walletService = new WalletService(mockWalletRepository);
  }

  @Test
  public void testGetWallet_Success() {
    UUID userId = UUID.randomUUID();
    BigDecimal balance = new BigDecimal("50000.00");
    Wallet wallet = new Wallet(userId, balance);

    WalletTransaction tx =
        new WalletTransaction(
            UUID.randomUUID(),
            userId,
            new BigDecimal("100.00"),
            "DEPOSIT",
            "ref-1",
            "deposit description",
            LocalDateTime.now());

    when(mockWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));
    when(mockWalletRepository.findTransactionsByUserId(userId)).thenReturn(List.of(tx));

    WalletResponse response = walletService.getWallet(userId);

    assertEquals(userId.toString(), response.getUserId());
    assertEquals(balance, response.getBalance());
    assertEquals(1, response.getTransactions().size());
    assertEquals("100.00", response.getTransactions().get(0).getAmount().toString());
    assertEquals("DEPOSIT", response.getTransactions().get(0).getTransactionType());
    assertEquals("ref-1", response.getTransactions().get(0).getReferenceId());
    assertEquals("deposit description", response.getTransactions().get(0).getDescription());
  }

  @Test
  public void testGetOrCreateWallet_Existing() {
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(userId, new BigDecimal("150.0"));
    when(mockWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

    Wallet result = walletService.getOrCreateWallet(userId);
    assertSame(wallet, result);
    verify(mockWalletRepository, never()).save(any());
  }

  @Test
  public void testGetOrCreateWallet_NotExisting_CreatesNewWithDefaultBalance() {
    UUID userId = UUID.randomUUID();
    when(mockWalletRepository.findByUserId(userId)).thenReturn(Optional.empty());

    Wallet result = walletService.getOrCreateWallet(userId);
    assertNotNull(result);
    assertEquals(userId, result.getUserId());
    assertEquals(new BigDecimal("100000.00"), result.getBalance());
    verify(mockWalletRepository).save(result);
  }

  @Test
  public void testGetOrCreateWalletWithConnection_Existing() {
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(userId, new BigDecimal("250.0"));
    when(mockWalletRepository.findByUserId(userId, mockConnection)).thenReturn(Optional.of(wallet));

    Wallet result = walletService.getOrCreateWallet(userId, mockConnection);
    assertSame(wallet, result);
    verify(mockWalletRepository, never()).save(any(), any());
  }

  @Test
  public void testGetOrCreateWalletWithConnection_NotExisting_CreatesNew() {
    UUID userId = UUID.randomUUID();
    when(mockWalletRepository.findByUserId(userId, mockConnection)).thenReturn(Optional.empty());

    Wallet result = walletService.getOrCreateWallet(userId, mockConnection);
    assertNotNull(result);
    assertEquals(userId, result.getUserId());
    assertEquals(new BigDecimal("100000.00"), result.getBalance());
    verify(mockWalletRepository).save(result, mockConnection);
  }

  @Test
  public void testDeposit_Success() {
    UUID userId = UUID.randomUUID();
    BigDecimal initialBalance = new BigDecimal("1000.00");
    Wallet wallet = new Wallet(userId, initialBalance);

    when(mockWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

    BigDecimal depositAmount = new BigDecimal("500.00");
    walletService.deposit(userId, depositAmount);

    assertEquals(new BigDecimal("1500.00"), wallet.getBalance());
    verify(mockWalletRepository).save(wallet);
    verify(mockWalletRepository).saveTransaction(any(WalletTransaction.class));
  }

  @Test
  public void testDeposit_InvalidAmount_ThrowsValidationException() {
    UUID userId = UUID.randomUUID();

    // Null amount
    assertThrows(ValidationException.class, () -> walletService.deposit(userId, null));
    // Zero amount
    assertThrows(ValidationException.class, () -> walletService.deposit(userId, BigDecimal.ZERO));
    // Negative amount
    assertThrows(
        ValidationException.class, () -> walletService.deposit(userId, new BigDecimal("-10.0")));

    verify(mockWalletRepository, never()).save(any());
  }

  @Test
  public void testWithdraw_Success() {
    UUID userId = UUID.randomUUID();
    BigDecimal initialBalance = new BigDecimal("1000.00");
    Wallet wallet = new Wallet(userId, initialBalance);

    when(mockWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

    BigDecimal withdrawAmount = new BigDecimal("400.00");
    walletService.withdraw(userId, withdrawAmount);

    assertEquals(new BigDecimal("600.00"), wallet.getBalance());
    verify(mockWalletRepository).save(wallet);
    verify(mockWalletRepository).saveTransaction(any(WalletTransaction.class));
  }

  @Test
  public void testWithdraw_InvalidAmount_ThrowsValidationException() {
    UUID userId = UUID.randomUUID();

    // Null amount
    assertThrows(ValidationException.class, () -> walletService.withdraw(userId, null));
    // Zero amount
    assertThrows(ValidationException.class, () -> walletService.withdraw(userId, BigDecimal.ZERO));
    // Negative amount
    assertThrows(
        ValidationException.class, () -> walletService.withdraw(userId, new BigDecimal("-5.0")));

    verify(mockWalletRepository, never()).save(any());
  }

  @Test
  public void testWithdraw_InsufficientBalance_ThrowsValidationException() {
    UUID userId = UUID.randomUUID();
    BigDecimal initialBalance = new BigDecimal("100.00");
    Wallet wallet = new Wallet(userId, initialBalance);

    when(mockWalletRepository.findByUserId(userId)).thenReturn(Optional.of(wallet));

    assertThrows(
        ValidationException.class, () -> walletService.withdraw(userId, new BigDecimal("100.01")));
    verify(mockWalletRepository, never()).save(any());
  }

  @Test
  public void testTransfer_Success() {
    UUID fromUserId = UUID.randomUUID();
    UUID toUserId = UUID.randomUUID();
    Wallet fromWallet = new Wallet(fromUserId, new BigDecimal("500.00"));
    Wallet toWallet = new Wallet(toUserId, new BigDecimal("200.00"));

    when(mockWalletRepository.findByUserId(fromUserId, mockConnection))
        .thenReturn(Optional.of(fromWallet));
    when(mockWalletRepository.findByUserId(toUserId, mockConnection))
        .thenReturn(Optional.of(toWallet));

    BigDecimal transferAmount = new BigDecimal("150.00");
    walletService.transfer(
        fromUserId, toUserId, transferAmount, "ref-pay", "item payment", mockConnection);

    assertEquals(new BigDecimal("350.00"), fromWallet.getBalance());
    assertEquals(new BigDecimal("350.00"), toWallet.getBalance());

    verify(mockWalletRepository).save(fromWallet, mockConnection);
    verify(mockWalletRepository).save(toWallet, mockConnection);
    verify(mockWalletRepository, times(2))
        .saveTransaction(any(WalletTransaction.class), eq(mockConnection));
  }

  @Test
  public void testTransfer_InvalidAmount_ThrowsValidationException() {
    UUID fromUserId = UUID.randomUUID();
    UUID toUserId = UUID.randomUUID();

    assertThrows(
        ValidationException.class,
        () -> walletService.transfer(fromUserId, toUserId, null, "ref", "desc", mockConnection));
    assertThrows(
        ValidationException.class,
        () ->
            walletService.transfer(
                fromUserId, toUserId, BigDecimal.ZERO, "ref", "desc", mockConnection));
    assertThrows(
        ValidationException.class,
        () ->
            walletService.transfer(
                fromUserId, toUserId, new BigDecimal("-1.0"), "ref", "desc", mockConnection));
  }

  @Test
  public void testTransfer_InsufficientBalance_ThrowsValidationException() {
    UUID fromUserId = UUID.randomUUID();
    UUID toUserId = UUID.randomUUID();
    Wallet fromWallet = new Wallet(fromUserId, new BigDecimal("50.00"));

    when(mockWalletRepository.findByUserId(fromUserId, mockConnection))
        .thenReturn(Optional.of(fromWallet));

    assertThrows(
        ValidationException.class,
        () ->
            walletService.transfer(
                fromUserId, toUserId, new BigDecimal("50.01"), "ref", "desc", mockConnection));
  }

}
