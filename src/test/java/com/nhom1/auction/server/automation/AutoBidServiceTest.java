package com.nhom1.auction.server.automation;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Mock;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigDetailResponse;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.infrastructure.NotificationService;

public class AutoBidServiceTest {

    @Mock
    private AutoBidRepository autoBidRepository;

    @Mock
    private BidGateway bidGateway;

    @Mock
    private AuctionGateway auctionGateway;

    @Mock
    private NotificationService notificationService;

    private AutoBidService autoBidService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        autoBidService = new AutoBidService(autoBidRepository, auctionGateway, bidGateway, notificationService);
    }

    @Test
    public void testSaveConfig_ValidConfig_SavesSuccessfully() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount("200.0");
        dto.setIncrement("10.00");
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auction.getMinBidIncrement()).thenReturn(new BigDecimal("10.00"));
        when(auctionGateway.findById(any())).thenReturn(java.util.Optional.of(auction));

        AutoBidConfigResponse result = autoBidService.saveConfig(dto);

        assertEquals("CONFIG_SAVED", result.getStatus());
        verify(autoBidRepository).save(any(AutoBidConfig.class));
    }

    @Test
    public void testSaveConfig_MaxAmountZero_Throws() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount("0.0");
        dto.setIncrement("10.00");
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auction.getMinBidIncrement()).thenReturn(new BigDecimal("10.00"));
        when(auctionGateway.findById(any())).thenReturn(java.util.Optional.of(auction));

        assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
    }

    @Test
    public void testSaveConfig_IncrementZero_Throws() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount("200.0");
        dto.setIncrement("0.00");
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auction.getMinBidIncrement()).thenReturn(new BigDecimal("10.00"));
        when(auctionGateway.findById(any())).thenReturn(java.util.Optional.of(auction));

        assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
    }

    @Test
    public void testSaveConfig_MaxAmountLessThanIncrement_Throws() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount("5.0");
        dto.setIncrement("10.00");
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auction.getMinBidIncrement()).thenReturn(new BigDecimal("10.00"));
        when(auctionGateway.findById(any())).thenReturn(java.util.Optional.of(auction));

        assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
    }

    @Test
    public void testTriggerAutoBids_OneEligibleBot_PlacesAutoBid() throws Exception {
        UUID auctionId = UUID.randomUUID();
        BigDecimal newHighestBid = new BigDecimal("100.00");
        UUID currentHighestBidderId = UUID.randomUUID();
        UUID botId = UUID.randomUUID();
        AutoBidConfig config = new AutoBidConfig(auctionId, botId, new BigDecimal("150.00"), new BigDecimal("10.00"));
        BidTransaction bidTransaction = mock(BidTransaction.class);
        when(bidTransaction.getAmount()).thenReturn(new BigDecimal("110.00"));
        when(bidTransaction.getBidderId()).thenReturn(botId);
        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));
        when(bidGateway.placeAutoBid(botId, auctionId, new BigDecimal("110.00"))).thenReturn(bidTransaction);
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auctionGateway.findById(auctionId)).thenReturn(java.util.Optional.of(auction));

        autoBidService.triggerAutoBids(auctionId, newHighestBid, currentHighestBidderId);

        verify(bidGateway).placeAutoBid(botId, auctionId, new BigDecimal("110.00"));
    }

    @Test
    public void testTriggerAutoBids_ProxyBidRaisesToSecondHighestMaxPlusIncrement() throws Exception {
        UUID auctionId = UUID.randomUUID();
        BigDecimal currentHighestBid = new BigDecimal("200.00");
        UUID currentHighestBidderId = UUID.randomUUID();
        UUID firstBidderId = UUID.randomUUID();
        UUID challengerId = UUID.randomUUID();

        AutoBidConfig firstConfig = new AutoBidConfig(auctionId, firstBidderId, new BigDecimal("200.00"), new BigDecimal("5.00"));
        AutoBidConfig challengerConfig = new AutoBidConfig(auctionId, challengerId, new BigDecimal("250.00"), new BigDecimal("5.00"));
        BidTransaction challengerBid = mock(BidTransaction.class);
        when(challengerBid.getAmount()).thenReturn(new BigDecimal("205.00"));
        when(challengerBid.getBidderId()).thenReturn(challengerId);

        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(firstConfig, challengerConfig));
        when(bidGateway.placeAutoBid(challengerId, auctionId, new BigDecimal("205.00"))).thenReturn(challengerBid);
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auctionGateway.findById(auctionId)).thenReturn(java.util.Optional.of(auction));

        autoBidService.triggerAutoBids(auctionId, currentHighestBid, currentHighestBidderId);

        verify(bidGateway).placeAutoBid(challengerId, auctionId, new BigDecimal("205.00"));
    }

    @Test
    public void testTriggerAutoBids_CurrentLeaderExcluded() {
        UUID auctionId = UUID.randomUUID();
        BigDecimal newHighestBid = new BigDecimal("100.00");
        UUID currentHighestBidderId = UUID.randomUUID();
        AutoBidConfig config = new AutoBidConfig(auctionId, currentHighestBidderId, new BigDecimal("150.00"), new BigDecimal("10.00"));
        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auctionGateway.findById(auctionId)).thenReturn(java.util.Optional.of(auction));

        autoBidService.triggerAutoBids(auctionId, newHighestBid, currentHighestBidderId);

        verify(bidGateway, never()).placeAutoBid(any(), any(), any());
    }

    @Test
    public void testTriggerAutoBids_NoEligibleBots_DoesNothing() {
        UUID auctionId = UUID.randomUUID();
        BigDecimal newHighestBid = new BigDecimal("100.00");
        UUID currentHighestBidderId = UUID.randomUUID();
        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of());
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auctionGateway.findById(auctionId)).thenReturn(java.util.Optional.of(auction));

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
        AutoBidConfig config1 = new AutoBidConfig(auctionId, bot1Id, new BigDecimal("10000.00"), new BigDecimal("10.00"));
        AutoBidConfig config2 = new AutoBidConfig(auctionId, bot2Id, new BigDecimal("10000.00"), new BigDecimal("10.00"));

        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config1, config2));
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auctionGateway.findById(auctionId)).thenReturn(java.util.Optional.of(auction));

        when(bidGateway.placeAutoBid(any(), any(), any())).thenAnswer(invocation -> {
            UUID bidderId = invocation.getArgument(0);
            BigDecimal amount = invocation.getArgument(2);
            BidTransaction bidTransaction = mock(BidTransaction.class);
            when(bidTransaction.getAmount()).thenReturn(amount);
            when(bidTransaction.getBidderId()).thenReturn(bidderId);
            return bidTransaction;
        });

        // This should not cause infinite loop
        autoBidService.triggerAutoBids(auctionId, newHighestBid, currentHighestBidderId);

        verify(bidGateway, atMost(5)).placeAutoBid(any(), any(), any()); // MAX_TRIGGER_DEPTH
    }

    @Test
    public void testGetConfig_ReturnsConfiguredResponse() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        AutoBidConfig config = new AutoBidConfig(auctionId, bidderId, new BigDecimal("500.00"), new BigDecimal("5.00"));
        when(autoBidRepository.findByAuctionAndBidder(auctionId, bidderId)).thenReturn(Optional.of(config));

        AutoBidConfigDetailResponse result = autoBidService.getConfig(auctionId.toString(), bidderId.toString());

        assertTrue(result.isConfigured());
        assertEquals("500.00", result.getMaxAmount());
        assertEquals("5.00", result.getIncrement());
    }

    @Test
    public void testDeleteConfig_ReturnsDeletedWhenRowExists() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        when(autoBidRepository.deleteByAuctionAndBidder(auctionId, bidderId)).thenReturn(1);

        AutoBidConfigResponse result = autoBidService.deleteConfig(auctionId.toString(), bidderId.toString());

        assertEquals("CONFIG_DELETED", result.getStatus());
    }

    @Test
    public void testTriggerAutoBids_CleansConfigsWhenAuctionNotRunning() {
        UUID auctionId = UUID.randomUUID();
        UUID currentHighestBidderId = UUID.randomUUID();
        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.FINISHED);
        when(auctionGateway.findById(auctionId)).thenReturn(Optional.of(auction));

        autoBidService.triggerAutoBids(auctionId, new BigDecimal("100.00"), currentHighestBidderId);

        verify(autoBidRepository).deleteByAuctionId(auctionId);
        verifyNoInteractions(bidGateway);
    }

    @Test
    public void testSaveConfig_TriggersAutoBidsImmediately() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(auctionId.toString());
        dto.setBidderId(bidderId.toString());
        dto.setMaxAmount("200.00");
        dto.setIncrement("10.00");

        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auction.getMinBidIncrement()).thenReturn(new BigDecimal("10.00"));
        when(auction.getStartingPrice()).thenReturn(new BigDecimal("100.00"));
        when(auction.getCurrentHighestBid()).thenReturn(null);
        when(auctionGateway.findById(auctionId)).thenReturn(Optional.of(auction));

        AutoBidConfig config = new AutoBidConfig(auctionId, bidderId, new BigDecimal("200.00"), new BigDecimal("10.00"));
        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));

        BidTransaction bidTx = mock(BidTransaction.class);
        when(bidTx.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(bidTx.getBidderId()).thenReturn(bidderId);
        when(bidGateway.placeAutoBid(bidderId, auctionId, new BigDecimal("100.00"))).thenReturn(bidTx);

        AutoBidConfigResponse result = autoBidService.saveConfig(dto);

        assertEquals("CONFIG_SAVED", result.getStatus());
        verify(autoBidRepository).save(any(AutoBidConfig.class));

        // Sleep briefly to allow asynchronous worker to process
        Thread.sleep(150);

        verify(bidGateway).placeAutoBid(bidderId, auctionId, new BigDecimal("100.00"));
    }

    @Test
    public void testTriggerAutoBids_FirstBidOnEmptyAuction_StartsAtStartingPrice() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        AutoBidConfig config = new AutoBidConfig(auctionId, bidderId, new BigDecimal("200.00"), new BigDecimal("10.00"));

        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auction.getStartingPrice()).thenReturn(new BigDecimal("100.00"));
        when(auction.getCurrentHighestBid()).thenReturn(null);
        when(auctionGateway.findById(auctionId)).thenReturn(Optional.of(auction));

        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));

        BidTransaction bidTx = mock(BidTransaction.class);
        when(bidTx.getAmount()).thenReturn(new BigDecimal("100.00"));
        when(bidTx.getBidderId()).thenReturn(bidderId);
        when(bidGateway.placeAutoBid(bidderId, auctionId, new BigDecimal("100.00"))).thenReturn(bidTx);

        autoBidService.triggerAutoBids(auctionId, BigDecimal.ZERO, null);

        verify(bidGateway).placeAutoBid(bidderId, auctionId, new BigDecimal("100.00"));
    }

    @Test
    public void testTriggerAutoBids_FirstBidOnEmptyAuction_WithCompetitor_ProxyBidsToCompetitorMaxPlusIncrement() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID botAId = UUID.randomUUID();
        UUID botBId = UUID.randomUUID();

        AutoBidConfig configA = new AutoBidConfig(auctionId, botAId, new BigDecimal("200.00"), new BigDecimal("10.00"));
        AutoBidConfig configB = new AutoBidConfig(auctionId, botBId, new BigDecimal("150.00"), new BigDecimal("10.00"));

        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auction.getStartingPrice()).thenReturn(new BigDecimal("100.00"));
        when(auction.getCurrentHighestBid()).thenReturn(null);
        when(auctionGateway.findById(auctionId)).thenReturn(Optional.of(auction));

        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(configA, configB));

        BidTransaction bidTx = mock(BidTransaction.class);
        when(bidTx.getAmount()).thenReturn(new BigDecimal("160.00"));
        when(bidTx.getBidderId()).thenReturn(botAId);
        when(bidGateway.placeAutoBid(botAId, auctionId, new BigDecimal("160.00"))).thenReturn(bidTx);

        autoBidService.triggerAutoBids(auctionId, BigDecimal.ZERO, null);

        verify(bidGateway).placeAutoBid(botAId, auctionId, new BigDecimal("160.00"));
        verify(bidGateway, atMost(1)).placeAutoBid(any(), any(), any());
    }

    @Test
    public void testTriggerAutoBids_BiddingWar_SettlesDirectlyWithoutLooping() throws Exception {
        UUID auctionId = UUID.randomUUID();
        UUID currentBidder = UUID.randomUUID();
        UUID botAId = UUID.randomUUID();
        UUID botBId = UUID.randomUUID();

        AutoBidConfig configA = new AutoBidConfig(auctionId, botAId, new BigDecimal("300.00"), new BigDecimal("10.00"));
        AutoBidConfig configB = new AutoBidConfig(auctionId, botBId, new BigDecimal("250.00"), new BigDecimal("10.00"));

        Auction auction = mock(Auction.class);
        when(auction.getStatus()).thenReturn(AuctionStatus.RUNNING);
        when(auctionGateway.findById(auctionId)).thenReturn(Optional.of(auction));

        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(configA, configB));

        BigDecimal currentBid = new BigDecimal("100.00");

        BidTransaction bidTx = mock(BidTransaction.class);
        when(bidTx.getAmount()).thenReturn(new BigDecimal("260.00"));
        when(bidTx.getBidderId()).thenReturn(botAId);
        when(bidGateway.placeAutoBid(botAId, auctionId, new BigDecimal("260.00"))).thenReturn(bidTx);

        autoBidService.triggerAutoBids(auctionId, currentBid, currentBidder);

        verify(bidGateway).placeAutoBid(botAId, auctionId, new BigDecimal("260.00"));
        verify(bidGateway, atMost(1)).placeAutoBid(any(), any(), any());
    }
}
