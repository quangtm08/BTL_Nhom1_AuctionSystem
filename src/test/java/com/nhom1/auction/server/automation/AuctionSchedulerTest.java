package com.nhom1.auction.server.automation;

import static org.mockito.Mockito.*;

import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.server.infrastructure.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class AuctionSchedulerTest {

  @Mock private AuctionGateway auctionGateway;

  @Mock private BidGateway bidGateway;

  @Mock private NotificationService notificationService;

  private AuctionScheduler auctionScheduler;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    auctionScheduler = new AuctionScheduler(auctionGateway, bidGateway, notificationService);
  }

  @Test
  public void testTick_OpenAuctionPastStartTime_StatusUpdatedToRunning() {
    LocalDateTime now = LocalDateTime.now();
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            now.minusMinutes(1),
            now.plusHours(1));
    when(auctionGateway.findAll()).thenReturn(List.of(auction));

    auctionScheduler.tick();

    verify(auctionGateway).updateStatus(auction.getId(), AuctionStatus.RUNNING);
  }

  @Test
  public void
      testTick_RunningAuctionPastEndTimeNoRecentBid_StatusUpdatedToFinishedAndNotificationSent()
          throws Exception {
    LocalDateTime now = LocalDateTime.now();
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            now.minusMinutes(2),
            now.minusMinutes(1));
    auction.startAuction();
    UUID bidderId = UUID.randomUUID();
    auction.placeBid(
        bidderId,
        new BigDecimal("150.00"),
        com.nhom1.auction.common.enums.BidType.MANUAL,
        now.minusMinutes(1).minusSeconds(10));

    when(auctionGateway.findAll()).thenReturn(List.of(auction));
    when(bidGateway.findLastBidTime(auction.getId())).thenReturn(Optional.empty());

    auctionScheduler.tick();

    verify(auctionGateway).updateStatus(auction.getId(), AuctionStatus.FINISHED);
    verify(notificationService)
        .broadcastAuctionEnded(auction.getId(), bidderId, new BigDecimal("150.00"));
  }

  @Test
  public void testTick_RunningAuctionPastEndTimeBidInAntiSnipingWindow_EndTimeExtended()
      throws Exception {
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime endTime = now.minusSeconds(10); // within 15s window
    LocalDateTime startTime = endTime.minusHours(1);
    Auction auction =
        new Auction(
            UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), startTime, endTime);
    auction.startAuction();
    LocalDateTime lastBidTime = endTime.minusSeconds(5);

    when(auctionGateway.findAll()).thenReturn(List.of(auction));
    when(bidGateway.findLastBidTime(auction.getId())).thenReturn(Optional.of(lastBidTime));

    auctionScheduler.tick();

    verify(auctionGateway).updateEndTime(auction.getId(), endTime.plusSeconds(30));
    verify(auctionGateway, never()).updateStatus(any(), any());
  }

  @Test
  public void testTick_OpenAuctionBeforeStartTime_NoStatusChange() {
    LocalDateTime now = LocalDateTime.now();
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            now.plusMinutes(1),
            now.plusHours(1));
    when(auctionGateway.findAll()).thenReturn(List.of(auction));

    auctionScheduler.tick();

    verify(auctionGateway, never()).updateStatus(any(), any());
  }

  @Test
  public void testStartStopAndSafeTick() throws Exception {
    // Test start/stop
    auctionScheduler.start();
    auctionScheduler.stop();

    // Test safeTick with exception logging
    java.lang.reflect.Method safeTickMethod = AuctionScheduler.class.getDeclaredMethod("safeTick");
    safeTickMethod.setAccessible(true);

    when(auctionGateway.findAll()).thenThrow(new RuntimeException("DB offline"));

    // Capture standard error stream to check deduplication
    java.io.ByteArrayOutputStream errContent = new java.io.ByteArrayOutputStream();
    java.io.PrintStream originalErr = System.err;
    System.setErr(new java.io.PrintStream(errContent));

    try {
      // First failure: should log to console
      safeTickMethod.invoke(auctionScheduler);
      String output1 = errContent.toString();
      org.junit.jupiter.api.Assertions.assertTrue(
          output1.contains("AuctionScheduler tick failed: DB offline"));

      // Clear bytes
      errContent.reset();

      // Second failure with same message: should NOT log to console (deduplicated)
      safeTickMethod.invoke(auctionScheduler);
      String output2 = errContent.toString();
      org.junit.jupiter.api.Assertions.assertFalse(
          output2.contains("AuctionScheduler tick failed"));

      // Third tick: make it succeed to reset lastErrorMessage
      reset(auctionGateway);
      when(auctionGateway.findAll()).thenReturn(java.util.Collections.emptyList());
      safeTickMethod.invoke(auctionScheduler);

      // Fourth tick: fail again with different message: should log to console
      errContent.reset();
      when(auctionGateway.findAll()).thenThrow(new RuntimeException("DB offline again"));
      safeTickMethod.invoke(auctionScheduler);
      String output4 = errContent.toString();
      org.junit.jupiter.api.Assertions.assertTrue(
          output4.contains("AuctionScheduler tick failed: DB offline again"));
    } finally {
      System.setErr(originalErr);
    }
  }
}
