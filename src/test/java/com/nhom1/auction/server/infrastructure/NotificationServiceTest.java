package com.nhom1.auction.server.infrastructure;

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
}
