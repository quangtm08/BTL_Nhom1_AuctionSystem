package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.entity.BidTransaction;

import com.nhom1.auction.server.infrastructure.NotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AutoBidServiceTest {

    @Mock
    private AutoBidRepository autoBidRepository;

    @Mock
    private BidGateway bidGateway;

    @Mock
    private NotificationService notificationService;

    private AutoBidService autoBidService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        autoBidService = new AutoBidService(autoBidRepository, bidGateway, notificationService);
    }

    @Test
    public void testSaveConfig_ValidConfig_SavesSuccessfully() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount(200.0);
        dto.setIncrement(10.0);

        AutoBidConfigResponse result = autoBidService.saveConfig(dto);

        assertEquals("CONFIG_SAVED", result.getStatus());
        verify(autoBidRepository).save(any(AutoBidConfig.class));
    }

    @Test
    public void testSaveConfig_MaxAmountZero_Throws() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount(0.0);
        dto.setIncrement(10.0);

        assertThrows(IllegalArgumentException.class, () -> autoBidService.saveConfig(dto));
    }

    @Test
    public void testSaveConfig_IncrementZero_Throws() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount(200.0);
        dto.setIncrement(0.0);

        assertThrows(IllegalArgumentException.class, () -> autoBidService.saveConfig(dto));
    }

    @Test
    public void testSaveConfig_MaxAmountLessThanIncrement_Throws() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount(5.0);
        dto.setIncrement(10.0);

        assertThrows(IllegalArgumentException.class, () -> autoBidService.saveConfig(dto));
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

        autoBidService.triggerAutoBids(auctionId, newHighestBid, currentHighestBidderId);

        verify(bidGateway).placeAutoBid(botId, auctionId, new BigDecimal("110.00"));
    }

    @Test
    public void testTriggerAutoBids_CurrentLeaderExcluded() {
        UUID auctionId = UUID.randomUUID();
        BigDecimal newHighestBid = new BigDecimal("100.00");
        UUID currentHighestBidderId = UUID.randomUUID();
        AutoBidConfig config = new AutoBidConfig(auctionId, currentHighestBidderId, new BigDecimal("150.00"), new BigDecimal("10.00"));
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
        AutoBidConfig config1 = new AutoBidConfig(auctionId, bot1Id, new BigDecimal("10000.00"), new BigDecimal("10.00"));
        AutoBidConfig config2 = new AutoBidConfig(auctionId, bot2Id, new BigDecimal("10000.00"), new BigDecimal("10.00"));

        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config1, config2));
        
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

        verify(bidGateway, times(20)).placeAutoBid(any(), any(), any()); // MAX_TRIGGER_DEPTH
    }
}