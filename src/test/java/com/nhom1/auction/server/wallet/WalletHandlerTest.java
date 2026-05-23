package com.nhom1.auction.server.wallet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.wallet.WalletResponse;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WalletHandlerTest {

  private WalletService mockWalletService;
  private WalletHandler walletHandler;
  private MessageRouter messageRouter;

  @BeforeEach
  public void setUp() {
    mockWalletService = mock(WalletService.class);
    walletHandler = new WalletHandler(mockWalletService);
    messageRouter = new MessageRouter();
    walletHandler.register(messageRouter);
  }

  @Test
  public void testGetWallet_Handler_Success() throws Exception {
    UUID userId = UUID.randomUUID();
    WalletResponse walletResponse =
        new WalletResponse(userId.toString(), new BigDecimal("1000.0"), Collections.emptyList());

    when(mockWalletService.getWallet(userId)).thenReturn(walletResponse);

    String requestJson =
        String.format(
            "{\"type\":\"GET_WALLET\",\"requestId\":\"req-1\",\"payload\":{\"userId\":\"%s\"}}",
            userId);

    String responseJson = messageRouter.handleRequest(requestJson);

    assertNotNull(responseJson);
    assertTrue(responseJson.contains("\"success\":true"));
    assertTrue(responseJson.contains(userId.toString()));
    assertTrue(responseJson.contains("1000.0"));
    verify(mockWalletService).getWallet(userId);
  }

  @Test
  public void testGetWallet_Handler_Exception() throws Exception {
    UUID userId = UUID.randomUUID();

    when(mockWalletService.getWallet(userId))
        .thenThrow(new IllegalArgumentException("Invalid user ID"));

    String requestJson =
        String.format(
            "{\"type\":\"GET_WALLET\",\"requestId\":\"req-2\",\"payload\":{\"userId\":\"%s\"}}",
            userId);

    String responseJson = messageRouter.handleRequest(requestJson);

    assertNotNull(responseJson);
    assertTrue(responseJson.contains("\"success\":false"));
    assertTrue(responseJson.contains("Invalid user ID"));
  }

  @Test
  public void testDepositMoney_Handler_Success() throws Exception {
    UUID userId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("500.0");
    WalletResponse walletResponse =
        new WalletResponse(userId.toString(), new BigDecimal("1500.0"), Collections.emptyList());

    when(mockWalletService.getWallet(userId)).thenReturn(walletResponse);

    String requestJson =
        String.format(
            "{\"type\":\"DEPOSIT_MONEY\",\"requestId\":\"req-3\",\"payload\":{\"userId\":\"%s\",\"amount\":500.0}}",
            userId);

    String responseJson = messageRouter.handleRequest(requestJson);

    assertNotNull(responseJson);
    assertTrue(responseJson.contains("\"success\":true"));
    assertTrue(responseJson.contains("1500.0"));
    verify(mockWalletService).deposit(userId, amount);
  }

  @Test
  public void testDepositMoney_Handler_Exception() throws Exception {
    UUID userId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("500.0");

    doThrow(new IllegalArgumentException("Deposit error"))
        .when(mockWalletService)
        .deposit(userId, amount);

    String requestJson =
        String.format(
            "{\"type\":\"DEPOSIT_MONEY\",\"requestId\":\"req-4\",\"payload\":{\"userId\":\"%s\",\"amount\":500.0}}",
            userId);

    String responseJson = messageRouter.handleRequest(requestJson);

    assertNotNull(responseJson);
    assertTrue(responseJson.contains("\"success\":false"));
    assertTrue(responseJson.contains("Deposit error"));
  }

  @Test
  public void testWithdrawMoney_Handler_Success() throws Exception {
    UUID userId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("200.0");
    WalletResponse walletResponse =
        new WalletResponse(userId.toString(), new BigDecimal("800.0"), Collections.emptyList());

    when(mockWalletService.getWallet(userId)).thenReturn(walletResponse);

    String requestJson =
        String.format(
            "{\"type\":\"WITHDRAW_MONEY\",\"requestId\":\"req-5\",\"payload\":{\"userId\":\"%s\",\"amount\":200.0}}",
            userId);

    String responseJson = messageRouter.handleRequest(requestJson);

    assertNotNull(responseJson);
    assertTrue(responseJson.contains("\"success\":true"));
    assertTrue(responseJson.contains("800.0"));
    verify(mockWalletService).withdraw(userId, amount);
  }

  @Test
  public void testWithdrawMoney_Handler_Exception() throws Exception {
    UUID userId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("200.0");

    doThrow(new IllegalArgumentException("Withdraw error"))
        .when(mockWalletService)
        .withdraw(userId, amount);

    String requestJson =
        String.format(
            "{\"type\":\"WITHDRAW_MONEY\",\"requestId\":\"req-6\",\"payload\":{\"userId\":\"%s\",\"amount\":200.0}}",
            userId);

    String responseJson = messageRouter.handleRequest(requestJson);

    assertNotNull(responseJson);
    assertTrue(responseJson.contains("\"success\":false"));
    assertTrue(responseJson.contains("Withdraw error"));
  }
}
