package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.server.infrastructure.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.*;

public class AuctionSchedulerTest {

    @Mock
    private AuctionGateway auctionGateway;

    @Mock
    private BidGateway bidGateway;

    @Mock
    private NotificationService notificationService;

    private AuctionScheduler auctionScheduler;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        auctionScheduler = new AuctionScheduler(auctionGateway, bidGateway, notificationService);
    }

    @Test
    public void testTick_OpenAuctionPastStartTime_StatusUpdatedToRunning() {
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), now.minusMinutes(1), now.plusHours(1));
        when(auctionGateway.findAll()).thenReturn(List.of(auction));

        auctionScheduler.tick();

        verify(auctionGateway).updateStatus(auction.getId(), AuctionStatus.RUNNING);
    }

    @Test
    public void testTick_RunningAuctionPastEndTimeNoRecentBid_StatusUpdatedToFinishedAndNotificationSent() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), now.minusMinutes(2), now.minusMinutes(1));
        auction.startAuction();
        UUID bidderId = UUID.randomUUID();
        auction.placeBid(bidderId, new BigDecimal("150.00"), com.nhom1.auction.common.enums.BidType.MANUAL, now.minusMinutes(1).minusSeconds(10));

        when(auctionGateway.findAll()).thenReturn(List.of(auction));
        when(bidGateway.findLastBidTime(auction.getId())).thenReturn(Optional.empty());

        auctionScheduler.tick();

        verify(auctionGateway).updateStatus(auction.getId(), AuctionStatus.FINISHED);
        verify(notificationService).broadcastAuctionEnded(auction.getId(), bidderId, new BigDecimal("150.00"));
    }

    @Test
    public void testTick_RunningAuctionPastEndTimeBidInAntiSnipingWindow_EndTimeExtended() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = now.minusSeconds(10); // within 15s window
        LocalDateTime startTime = endTime.minusHours(1);
        Auction auction = new Auction(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), startTime, endTime);
        auction.startAuction();
        LocalDateTime lastBidTime = endTime.minusSeconds(5);

        when(auctionGateway.findAll()).thenReturn(List.of(auction));
        when(bidGateway.findLastBidTime(auction.getId())).thenReturn(Optional.of(lastBidTime));

        auctionScheduler.tick();

        verify(auctionGateway).updateEndTime(auction.getId(), endTime.plusSeconds(30));
        verify(auctionGateway, never()).updateStatus(any(), any());
        verify(notificationService).broadcastAuctionTimeExtended(auction.getId(), endTime.plusSeconds(30));
    }

    @Test
    public void testTick_OpenAuctionBeforeStartTime_NoStatusChange() {
        LocalDateTime now = LocalDateTime.now();
        Auction auction = new Auction(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), now.plusMinutes(1), now.plusHours(1));
        when(auctionGateway.findAll()).thenReturn(List.of(auction));

        auctionScheduler.tick();

        verify(auctionGateway, never()).updateStatus(any(), any());
    }
}