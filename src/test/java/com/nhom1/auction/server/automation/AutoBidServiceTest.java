package com.nhom1.auction.server.automation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.exception.ValidationException;
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

public class AutoBidServiceTest {

  @Mock private AutoBidRepository autoBidRepository;

  @Mock private AuctionGateway auctionGateway;

  @Mock private BidGateway bidGateway;

  @Mock private NotificationService notificationService;

  private AutoBidService autoBidService;

  @BeforeEach
  public void setUp() {
    MockitoAnnotations.openMocks(this);
    autoBidService =
        new AutoBidService(autoBidRepository, auctionGateway, bidGateway, notificationService);

    Auction defaultAuction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("200.00"),
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1),
            null,
            null,
            AuctionStatus.RUNNING,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null);
    when(auctionGateway.findById(any(UUID.class))).thenReturn(Optional.of(defaultAuction));
  }

  @Test
  public void testSaveConfig_ValidConfig_SavesSuccessfully() {
    AutoBidConfigRequest dto = new AutoBidConfigRequest();
    dto.setAuctionId(UUID.randomUUID().toString());
    dto.setBidderId(UUID.randomUUID().toString());
    dto.setMaxAmount("200.00");
    dto.setIncrement("10.00");

    AutoBidConfigResponse result = autoBidService.saveConfig(dto);

    assertEquals("CONFIG_SAVED", result.getStatus());
    verify(autoBidRepository).save(any(AutoBidConfig.class));
  }

  @Test
  public void testSaveConfig_MaxAmountZero_Throws() {
    AutoBidConfigRequest dto = new AutoBidConfigRequest();
    dto.setAuctionId(UUID.randomUUID().toString());
    dto.setBidderId(UUID.randomUUID().toString());
    dto.setMaxAmount("0.00");
    dto.setIncrement("10.00");

    assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
  }

  @Test
  public void testSaveConfig_IncrementZero_Throws() {
    AutoBidConfigRequest dto = new AutoBidConfigRequest();
    dto.setAuctionId(UUID.randomUUID().toString());
    dto.setBidderId(UUID.randomUUID().toString());
    dto.setMaxAmount("200.00");
    dto.setIncrement("0.00");

    assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
  }

  @Test
  public void testSaveConfig_MaxAmountLessThanIncrement_Throws() {
    AutoBidConfigRequest dto = new AutoBidConfigRequest();
    dto.setAuctionId(UUID.randomUUID().toString());
    dto.setBidderId(UUID.randomUUID().toString());
    dto.setMaxAmount("5.00");
    dto.setIncrement("10.00");

    assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
  }

  @Test
  public void testSaveConfig_CustomIncrementGreater_SavesSuccessfully() {
    AutoBidConfigRequest dto = new AutoBidConfigRequest();
    dto.setAuctionId(UUID.randomUUID().toString());
    dto.setBidderId(UUID.randomUUID().toString());
    dto.setMaxAmount("200.00");
    dto.setIncrement("15.00");

    AutoBidConfigResponse result = autoBidService.saveConfig(dto);

    assertEquals("CONFIG_SAVED", result.getStatus());
    verify(autoBidRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
                cfg -> cfg.getIncrement().compareTo(new BigDecimal("15.00")) == 0));
  }

  @Test
  public void testSaveConfig_IncrementLessThanMinimum_Throws() {
    AutoBidConfigRequest dto = new AutoBidConfigRequest();
    dto.setAuctionId(UUID.randomUUID().toString());
    dto.setBidderId(UUID.randomUUID().toString());
    dto.setMaxAmount("200.00");
    dto.setIncrement("5.00");

    assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
  }

  @Test
  public void testTriggerAutoBids_OneEligibleBot_PlacesAutoBid() throws Exception {
    UUID auctionId = UUID.randomUUID();
    BigDecimal newHighestBid = new BigDecimal("100.00");
    UUID currentHighestBidderId = UUID.randomUUID();
    UUID botId = UUID.randomUUID();
    AutoBidConfig config =
        new AutoBidConfig(auctionId, botId, new BigDecimal("150.00"), new BigDecimal("10.00"));
    BidTransaction bidTransaction = mock(BidTransaction.class);
    when(bidTransaction.getAmount()).thenReturn(new BigDecimal("110.00"));
    when(bidTransaction.getBidderId()).thenReturn(botId);
    when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));
    when(bidGateway.placeAutoBid(botId, auctionId, new BigDecimal("110.00")))
        .thenReturn(bidTransaction);

    autoBidService.triggerAutoBids(auctionId, newHighestBid, currentHighestBidderId);

    verify(bidGateway).placeAutoBid(botId, auctionId, new BigDecimal("110.00"));
  }

  @Test
  public void testTriggerAutoBids_CurrentLeaderExcluded() {
    UUID auctionId = UUID.randomUUID();
    BigDecimal newHighestBid = new BigDecimal("100.00");
    UUID currentHighestBidderId = UUID.randomUUID();
    AutoBidConfig config =
        new AutoBidConfig(
            auctionId, currentHighestBidderId, new BigDecimal("150.00"), new BigDecimal("10.00"));
    when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));

    autoBidService.triggerAutoBids(auctionId, newHighestBid, currentHighestBidderId);

    verify(bidGateway, never()).placeAutoBid(any(), any(), any());
  }

  @Test
  public void testTriggerAutoBids_NoEligibleBots_DoesNothing() {
    UUID auctionId = UUID.randomUUID();
    BigDecimal newHighestBid = new BigDecimal("100.00");
    UUID currentHighestBidderId = UUID.randomUUID();
    when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of());

    autoBidService.triggerAutoBids(auctionId, newHighestBid, currentHighestBidderId);

    verify(bidGateway, never()).placeAutoBid(any(), any(), any());
  }

  @Test
  public void testTriggerAutoBids_StopsAtMaxTriggerDepth() throws Exception {
    UUID auctionId = UUID.randomUUID();
    BigDecimal newHighestBid = new BigDecimal("100.00");
    UUID currentHighestBidderId = UUID.randomUUID();
    UUID bot1Id = UUID.randomUUID();
    UUID bot2Id = UUID.randomUUID();
    AutoBidConfig config1 =
        new AutoBidConfig(auctionId, bot1Id, new BigDecimal("10000.00"), new BigDecimal("10.00"));
    AutoBidConfig config2 =
        new AutoBidConfig(auctionId, bot2Id, new BigDecimal("10000.00"), new BigDecimal("10.00"));

    when(autoBidRepository.findByAuctionId(auctionId))
        .thenReturn(List.of(config1))
        .thenReturn(List.of(config2))
        .thenReturn(List.of(config1))
        .thenReturn(List.of(config2))
        .thenReturn(List.of(config1))
        .thenReturn(List.of(config2))
        .thenReturn(List.of(config1))
        .thenReturn(List.of(config2))
        .thenReturn(List.of(config1))
        .thenReturn(List.of(config2))
        .thenReturn(List.of(config1));

    java.util.concurrent.atomic.AtomicReference<BigDecimal> currentBidRef =
        new java.util.concurrent.atomic.AtomicReference<>(newHighestBid);
    when(bidGateway.placeAutoBid(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              UUID bidderId = invocation.getArgument(0);
              BigDecimal newBid = currentBidRef.get().add(new BigDecimal("10.00"));
              currentBidRef.set(newBid);
              BidTransaction bidTransaction = mock(BidTransaction.class);
              when(bidTransaction.getAmount()).thenReturn(newBid);
              when(bidTransaction.getBidderId()).thenReturn(bidderId);
              return bidTransaction;
            });

    // This should not cause infinite loop
    autoBidService.triggerAutoBids(auctionId, newHighestBid, currentHighestBidderId);

    verify(bidGateway, times(10)).placeAutoBid(any(), any(), any()); // MAX_TRIGGER_DEPTH
  }

  @Test
  public void testSaveConfig_InvalidAuctionId_Throws() {
    AutoBidConfigRequest dto = new AutoBidConfigRequest();
    dto.setAuctionId("not-a-uuid");
    dto.setBidderId(UUID.randomUUID().toString());
    dto.setMaxAmount("100.00");
    dto.setIncrement("10.00");

    ValidationException ex =
        assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
    assertTrue(ex.getMessage().contains("auctionId is invalid UUID"));
  }

  @Test
  public void testSaveConfig_InvalidBidderId_Throws() {
    AutoBidConfigRequest dto = new AutoBidConfigRequest();
    dto.setAuctionId(UUID.randomUUID().toString());
    dto.setBidderId("not-a-uuid");
    dto.setMaxAmount("100.00");
    dto.setIncrement("10.00");

    ValidationException ex =
        assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
    assertTrue(ex.getMessage().contains("bidderId is invalid UUID"));
  }

  @Test
  public void testScheduleAutoBids_Valid_SubmitsToExecutor() throws Exception {
    UUID auctionId = UUID.randomUUID();
    BigDecimal currentHighestBid = new BigDecimal("100.00");
    UUID currentHighestBidderId = UUID.randomUUID();
    UUID botId = UUID.randomUUID();

    AutoBidConfig config =
        new AutoBidConfig(auctionId, botId, new BigDecimal("150.00"), new BigDecimal("10.00"));
    when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));

    BidTransaction bidTx = mock(BidTransaction.class);
    when(bidTx.getAmount()).thenReturn(new BigDecimal("110.00"));
    when(bidTx.getBidderId()).thenReturn(botId);
    when(bidGateway.placeAutoBid(botId, auctionId, new BigDecimal("110.00"))).thenReturn(bidTx);

    autoBidService.scheduleAutoBids(auctionId, currentHighestBid, currentHighestBidderId);

    verify(bidGateway, timeout(1000)).placeAutoBid(botId, auctionId, new BigDecimal("110.00"));
    verify(notificationService, timeout(1000))
        .broadcastBidUpdate(auctionId, new BigDecimal("110.00"), botId);
  }

  @Test
  public void testRunAutoBids_NextAmtExceedsMaxAmount_BreaksLoop() {
    UUID auctionId = UUID.randomUUID();
    BigDecimal currentHighestBid = new BigDecimal("100.00");
    UUID currentHighestBidderId = UUID.randomUUID();
    UUID botId = UUID.randomUUID();

    // config's max amount is 105.00, increment is 10.00
    // next amount would be 100.00 + 10.00 = 110.00, which exceeds max amount (105.00)
    AutoBidConfig config =
        new AutoBidConfig(auctionId, botId, new BigDecimal("105.00"), new BigDecimal("10.00"));
    when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));

    autoBidService.triggerAutoBids(auctionId, currentHighestBid, currentHighestBidderId);

    verify(bidGateway, never()).placeAutoBid(any(), any(), any());
  }

  @Test
  public void testRunAutoBids_BidGatewayThrowsException_LogsErrorAndBreaksLoop() throws Exception {
    UUID auctionId = UUID.randomUUID();
    BigDecimal currentHighestBid = new BigDecimal("100.00");
    UUID currentHighestBidderId = UUID.randomUUID();
    UUID botId = UUID.randomUUID();

    AutoBidConfig config =
        new AutoBidConfig(auctionId, botId, new BigDecimal("150.00"), new BigDecimal("10.00"));
    when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));

    when(bidGateway.placeAutoBid(botId, auctionId, new BigDecimal("110.00")))
        .thenThrow(new RuntimeException("Database error during placeAutoBid"));

    assertDoesNotThrow(
        () -> autoBidService.triggerAutoBids(auctionId, currentHighestBid, currentHighestBidderId));

    verify(bidGateway, times(1)).placeAutoBid(botId, auctionId, new BigDecimal("110.00"));
    verify(notificationService, never()).broadcastBidUpdate(any(), any(), any());
  }

  @Test
  public void testGetConfig_Found() {
    UUID auctionId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    AutoBidConfig cfg =
        new AutoBidConfig(auctionId, bidderId, new BigDecimal("500.00"), new BigDecimal("10.00"));
    when(autoBidRepository.findByAuctionAndBidder(auctionId, bidderId))
        .thenReturn(Optional.of(cfg));

    var response = autoBidService.getConfig(auctionId.toString(), bidderId.toString());

    assertTrue(response.isConfigured());
    assertEquals("500.00", response.getMaxAmount());
    assertEquals("10.00", response.getIncrement());
  }

  @Test
  public void testGetConfig_NotFound() {
    UUID auctionId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    when(autoBidRepository.findByAuctionAndBidder(auctionId, bidderId))
        .thenReturn(Optional.empty());

    var response = autoBidService.getConfig(auctionId.toString(), bidderId.toString());

    assertFalse(response.isConfigured());
    assertNull(response.getMaxAmount());
  }

  @Test
  public void testDeleteConfig_Deleted() {
    UUID auctionId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    when(autoBidRepository.deleteByAuctionAndBidder(auctionId, bidderId)).thenReturn(1);

    var response = autoBidService.deleteConfig(auctionId.toString(), bidderId.toString());

    assertEquals("CONFIG_DELETED", response.getStatus());
  }

  @Test
  public void testDeleteConfig_NotFound() {
    UUID auctionId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    when(autoBidRepository.deleteByAuctionAndBidder(auctionId, bidderId)).thenReturn(0);

    var response = autoBidService.deleteConfig(auctionId.toString(), bidderId.toString());

    assertEquals("CONFIG_NOT_FOUND", response.getStatus());
  }

  @Test
  public void testRunAutoBids_AuctionNotFound_CleansUpConfigs() {
    UUID auctionId = UUID.randomUUID();
    when(auctionGateway.findById(auctionId)).thenReturn(Optional.empty());

    autoBidService.triggerAutoBids(auctionId, BigDecimal.TEN, UUID.randomUUID());

    verify(autoBidRepository).deleteByAuctionId(auctionId);
  }

  @Test
  public void testRunAutoBids_AuctionNotRunning_CleansUpConfigs() {
    UUID auctionId = UUID.randomUUID();
    Auction auction =
        new Auction(
            auctionId,
            UUID.randomUUID(),
            BigDecimal.TEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1));
    when(auctionGateway.findById(auctionId)).thenReturn(Optional.of(auction));

    autoBidService.triggerAutoBids(auctionId, BigDecimal.TEN, UUID.randomUUID());

    verify(autoBidRepository).deleteByAuctionId(auctionId);
  }

  @Test
  public void testTriggerAutoBids_ChallengerWithLowerMaxAmount_EscalatesBid() throws Exception {
    UUID auctionId = UUID.randomUUID();
    UUID higherBidderId = UUID.randomUUID();
    UUID lowerBidderId = UUID.randomUUID();

    // Higher bidder config: max 100.00, increment 10.00
    AutoBidConfig higherConfig =
        new AutoBidConfig(
            auctionId, higherBidderId, new BigDecimal("100.00"), new BigDecimal("10.00"));
    // Lower bidder config: max 50.00, increment 5.00
    AutoBidConfig lowerConfig =
        new AutoBidConfig(
            auctionId, lowerBidderId, new BigDecimal("50.00"), new BigDecimal("5.00"));

    when(autoBidRepository.findByAuctionId(auctionId))
        .thenReturn(List.of(higherConfig, lowerConfig));

    // Current bid is 10.00, held by the higher bidder (higherBidderId)
    BigDecimal currentHighestBid = new BigDecimal("10.00");
    UUID currentHighestBidderId = higherBidderId;

    // First, the challenger (lowerConfig) is processed and bids its max amount 50.00
    BidTransaction tx1 = mock(BidTransaction.class);
    when(tx1.getAmount()).thenReturn(new BigDecimal("50.00"));
    when(tx1.getBidderId()).thenReturn(lowerBidderId);
    when(bidGateway.placeAutoBid(lowerBidderId, auctionId, new BigDecimal("50.00")))
        .thenReturn(tx1);

    // Second, the higher config outbids the challenger at 50.00 + 10.00 = 60.00
    BidTransaction tx2 = mock(BidTransaction.class);
    when(tx2.getAmount()).thenReturn(new BigDecimal("60.00"));
    when(tx2.getBidderId()).thenReturn(higherBidderId);
    when(bidGateway.placeAutoBid(higherBidderId, auctionId, new BigDecimal("60.00")))
        .thenReturn(tx2);

    autoBidService.triggerAutoBids(auctionId, currentHighestBid, currentHighestBidderId);

    // Verify both bids were placed
    verify(bidGateway).placeAutoBid(lowerBidderId, auctionId, new BigDecimal("50.00"));
    verify(bidGateway).placeAutoBid(higherBidderId, auctionId, new BigDecimal("60.00"));
    verify(notificationService)
        .broadcastBidUpdate(auctionId, new BigDecimal("60.00"), higherBidderId);
  }

  @Test
  public void testTriggerAutoBids_UserScenario_Max100kAndMax12k_EscalatesTo12100()
      throws Exception {
    UUID auctionId = UUID.randomUUID();
    UUID bidderA = UUID.randomUUID();
    UUID bidderB = UUID.randomUUID();

    // Bidder A: max 100,000.00, increment 100.00
    AutoBidConfig configA =
        new AutoBidConfig(
            auctionId, bidderA, new BigDecimal("100000.00"), new BigDecimal("100.00"));
    // Bidder B: max 12,000.00, increment 50.00
    AutoBidConfig configB =
        new AutoBidConfig(auctionId, bidderB, new BigDecimal("12000.00"), new BigDecimal("50.00"));

    when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(configA, configB));

    // Initially A is the leader at starting price (e.g. 10,000.00)
    BigDecimal currentHighestBid = new BigDecimal("10000.00");
    UUID currentHighestBidderId = bidderA;

    // B bids its max 12,000.00
    BidTransaction tx1 = mock(BidTransaction.class);
    when(tx1.getAmount()).thenReturn(new BigDecimal("12000.00"));
    when(tx1.getBidderId()).thenReturn(bidderB);
    when(bidGateway.placeAutoBid(bidderB, auctionId, new BigDecimal("12000.00"))).thenReturn(tx1);

    // A outbids B at 12,000.00 + 100.00 = 12,100.00
    BidTransaction tx2 = mock(BidTransaction.class);
    when(tx2.getAmount()).thenReturn(new BigDecimal("12100.00"));
    when(tx2.getBidderId()).thenReturn(bidderA);
    when(bidGateway.placeAutoBid(bidderA, auctionId, new BigDecimal("12100.00"))).thenReturn(tx2);

    autoBidService.triggerAutoBids(auctionId, currentHighestBid, currentHighestBidderId);

    verify(bidGateway).placeAutoBid(bidderB, auctionId, new BigDecimal("12000.00"));
    verify(bidGateway).placeAutoBid(bidderA, auctionId, new BigDecimal("12100.00"));
    verify(notificationService).broadcastBidUpdate(auctionId, new BigDecimal("12100.00"), bidderA);
  }

  @Test
  public void testTriggerAutoBids_PriorityQueueOrderByCreatedAt() throws Exception {
    UUID auctionId = UUID.randomUUID();
    BigDecimal startingPrice = new BigDecimal("100.00");
    UUID bot1Id = UUID.randomUUID();
    UUID bot2Id = UUID.randomUUID();

    AutoBidConfig config1 =
        new AutoBidConfig(
            auctionId,
            bot1Id,
            new BigDecimal("130.00"),
            new BigDecimal("10.00"),
            LocalDateTime.now().minusMinutes(10));

    AutoBidConfig config2 =
        new AutoBidConfig(
            auctionId,
            bot2Id,
            new BigDecimal("150.00"),
            new BigDecimal("10.00"),
            LocalDateTime.now().minusMinutes(5));

    when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config1, config2));

    java.util.List<String> bidSequence = new java.util.ArrayList<>();
    java.util.List<BigDecimal> amountSequence = new java.util.ArrayList<>();

    when(bidGateway.placeAutoBid(any(), any(), any()))
        .thenAnswer(
            invocation -> {
              UUID bidderId = invocation.getArgument(0);
              BigDecimal amount = invocation.getArgument(2);
              bidSequence.add(bidderId.toString());
              amountSequence.add(amount);

              BidTransaction bidTx = mock(BidTransaction.class);
              when(bidTx.getAmount()).thenReturn(amount);
              when(bidTx.getBidderId()).thenReturn(bidderId);
              return bidTx;
            });

    Auction auction =
        new Auction(
            auctionId,
            UUID.randomUUID(),
            UUID.randomUUID(),
            startingPrice,
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1),
            null,
            null,
            AuctionStatus.RUNNING,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null);
    when(auctionGateway.findById(auctionId)).thenReturn(Optional.of(auction));

    autoBidService.triggerAutoBids(auctionId, BigDecimal.ZERO, null);

    // With escalation logic, the bidding resolves more efficiently:
    // 1. bot1 bids starting price: 100.00
    // 2. bot2 outbids bot1's max: 130.00 + 10.00 = 140.00
    assertEquals(2, bidSequence.size());
    assertEquals(bot1Id.toString(), bidSequence.get(0));
    assertEquals(new BigDecimal("100.00"), amountSequence.get(0));

    assertEquals(bot2Id.toString(), bidSequence.get(1));
    assertEquals(new BigDecimal("140.00"), amountSequence.get(1));
  }

  @Test
  public void testTriggerAutoBids_SameMaxAmount_HigherPriorityWins() throws Exception {
    UUID auctionId = UUID.randomUUID();
    UUID priorityBidderId = UUID.randomUUID();
    UUID challengerBidderId = UUID.randomUUID();

    // Priority bidder config: max 400.00, increment 50.00, created 10 mins ago
    AutoBidConfig priorityConfig =
        new AutoBidConfig(
            auctionId,
            priorityBidderId,
            new BigDecimal("400.00"),
            new BigDecimal("50.00"),
            LocalDateTime.now().minusMinutes(10));
    // Challenger bidder config: max 400.00, increment 50.00, created 5 mins ago
    AutoBidConfig challengerConfig =
        new AutoBidConfig(
            auctionId,
            challengerBidderId,
            new BigDecimal("400.00"),
            new BigDecimal("50.00"),
            LocalDateTime.now().minusMinutes(5));

    when(autoBidRepository.findByAuctionId(auctionId))
        .thenReturn(List.of(priorityConfig, challengerConfig));

    // Initially, the priority bidder is the current leader at 100.00
    BigDecimal currentHighestBid = new BigDecimal("100.00");
    UUID currentHighestBidderId = priorityBidderId;

    // 1. Challenger (lower priority) bids capped at leader.max - leader.increment = 400.00 - 50.00
    // = 350.00
    BidTransaction tx1 = mock(BidTransaction.class);
    when(tx1.getAmount()).thenReturn(new BigDecimal("350.00"));
    when(tx1.getBidderId()).thenReturn(challengerBidderId);
    when(bidGateway.placeAutoBid(challengerBidderId, auctionId, new BigDecimal("350.00")))
        .thenReturn(tx1);

    // 2. Priority bidder outbids at max amount 400.00
    BidTransaction tx2 = mock(BidTransaction.class);
    when(tx2.getAmount()).thenReturn(new BigDecimal("400.00"));
    when(tx2.getBidderId()).thenReturn(priorityBidderId);
    when(bidGateway.placeAutoBid(priorityBidderId, auctionId, new BigDecimal("400.00")))
        .thenReturn(tx2);

    autoBidService.triggerAutoBids(auctionId, currentHighestBid, currentHighestBidderId);

    // Verify both bids were placed in sequence and priority bidder wins at 400.00
    verify(bidGateway).placeAutoBid(challengerBidderId, auctionId, new BigDecimal("350.00"));
    verify(bidGateway).placeAutoBid(priorityBidderId, auctionId, new BigDecimal("400.00"));
    verify(notificationService)
        .broadcastBidUpdate(auctionId, new BigDecimal("400.00"), priorityBidderId);
  }
}
