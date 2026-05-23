package com.nhom1.auction.server.infrastructure;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class NotificationServiceTest {

  private ClientRegistry mockRegistry;
  private NotificationService service;

  @BeforeEach
  public void setUp() {
    mockRegistry = mock(ClientRegistry.class);
    service = new NotificationService(mockRegistry);
  }

  @Test
  public void testBroadcastBidUpdate() {
    UUID auctionId = UUID.randomUUID();
    service.broadcastBidUpdate(auctionId, BigDecimal.TEN, UUID.randomUUID());
    verify(mockRegistry).broadcast(anyString());
  }

  @Test
  public void testBroadcastBidUpdate_NullBidder() {
    UUID auctionId = UUID.randomUUID();
    service.broadcastBidUpdate(auctionId, BigDecimal.TEN, null);
    verify(mockRegistry).broadcast(anyString());
  }

  @Test
  public void testBroadcastAuctionEnded() {
    UUID auctionId = UUID.randomUUID();
    service.broadcastAuctionEnded(auctionId, UUID.randomUUID(), BigDecimal.TEN);
    verify(mockRegistry).broadcast(anyString());
  }

  @Test
  public void testBroadcastAuctionEnded_NullWinner() {
    UUID auctionId = UUID.randomUUID();
    service.broadcastAuctionEnded(auctionId, null, BigDecimal.TEN);
    verify(mockRegistry).broadcast(anyString());
  }

  @Test
  public void testBroadcastNewAuction() {
    service.broadcastNewAuction("auc-1", "wood", BigDecimal.TEN);
    verify(mockRegistry).broadcast(anyString());
  }

  @Test
  public void testBroadcastAuctionDeleted() {
    service.broadcastAuctionDeleted("auc-1");
    verify(mockRegistry).broadcast(anyString());
  }

  @Test
  public void testBroadcastUserDeleted() {
    service.broadcastUserDeleted("user-1");
    verify(mockRegistry).broadcast(anyString());
  }

  @Test
  public void testBroadcastUserCreated() {
    service.broadcastUserCreated("user-1", "alice", "alice@mail.com");
    verify(mockRegistry).broadcast(anyString());
  }

  @Test
  public void testSendWalletUpdate() {
    java.util.UUID userId = java.util.UUID.randomUUID();
    service.sendWalletUpdate(userId, java.math.BigDecimal.TEN);
    verify(mockRegistry).sendToUser(eq(userId), anyString());
  }

  @Test
  public void testBroadcastAuctionTimeExtended() {
    java.util.UUID auctionId = java.util.UUID.randomUUID();
    service.broadcastAuctionTimeExtended(auctionId, java.time.LocalDateTime.now().plusDays(1));
    verify(mockRegistry).broadcast(anyString());
  }

  @Test
  public void testBroadcastBidUpdate_BroadcastThrows_DoesNotPropagate() {
    java.util.UUID auctionId = java.util.UUID.randomUUID();
    doThrow(new RuntimeException("Serialization failure"))
        .when(mockRegistry)
        .broadcast(anyString());
    // Should not throw - error is caught and logged
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> service.broadcastBidUpdate(auctionId, java.math.BigDecimal.TEN, null));
  }

  @Test
  public void testSendWalletUpdate_SendToUserThrows_DoesNotPropagate() {
    java.util.UUID userId = java.util.UUID.randomUUID();
    doThrow(new RuntimeException("Network error"))
        .when(mockRegistry)
        .sendToUser(any(), anyString());
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () -> service.sendWalletUpdate(userId, java.math.BigDecimal.valueOf(500)));
  }
}
