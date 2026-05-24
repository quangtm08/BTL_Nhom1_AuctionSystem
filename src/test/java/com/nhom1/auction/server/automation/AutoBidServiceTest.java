package com.nhom1.auction.server.automation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.BidTransaction;
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
            new BigDecimal("200.00"),
            LocalDateTime.now().minusHours(1),
            LocalDateTime.now().plusHours(1));
    defaultAuction.startAuction();
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
}
