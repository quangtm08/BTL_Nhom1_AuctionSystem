package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.exception.ValidationException;

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

        assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
    }

    @Test
    public void testSaveConfig_IncrementZero_Throws() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount(200.0);
        dto.setIncrement(0.0);

        assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
    }

    @Test
    public void testSaveConfig_MaxAmountLessThanIncrement_Throws() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount(5.0);
        dto.setIncrement(10.0);

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

    @Test
    public void testSaveConfig_InvalidAuctionId_Throws() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId("not-a-uuid");
        dto.setBidderId(UUID.randomUUID().toString());
        dto.setMaxAmount(100.0);
        dto.setIncrement(10.0);

        ValidationException ex = assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
        assertTrue(ex.getMessage().contains("auctionId is invalid UUID"));
    }

    @Test
    public void testSaveConfig_InvalidBidderId_Throws() {
        AutoBidConfigRequest dto = new AutoBidConfigRequest();
        dto.setAuctionId(UUID.randomUUID().toString());
        dto.setBidderId("not-a-uuid");
        dto.setMaxAmount(100.0);
        dto.setIncrement(10.0);

        ValidationException ex = assertThrows(ValidationException.class, () -> autoBidService.saveConfig(dto));
        assertTrue(ex.getMessage().contains("bidderId is invalid UUID"));
    }

    @Test
    public void testScheduleAutoBids_Valid_SubmitsToExecutor() throws Exception {
        UUID auctionId = UUID.randomUUID();
        BigDecimal currentHighestBid = new BigDecimal("100.00");
        UUID currentHighestBidderId = UUID.randomUUID();
        UUID botId = UUID.randomUUID();

        AutoBidConfig config = new AutoBidConfig(auctionId, botId, new BigDecimal("150.00"), new BigDecimal("10.00"));
        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));

        BidTransaction bidTx = mock(BidTransaction.class);
        when(bidTx.getAmount()).thenReturn(new BigDecimal("110.00"));
        when(bidTx.getBidderId()).thenReturn(botId);
        when(bidGateway.placeAutoBid(botId, auctionId, new BigDecimal("110.00"))).thenReturn(bidTx);

        autoBidService.scheduleAutoBids(auctionId, currentHighestBid, currentHighestBidderId);

        verify(bidGateway, timeout(1000)).placeAutoBid(botId, auctionId, new BigDecimal("110.00"));
        verify(notificationService, timeout(1000)).broadcastBidUpdate(auctionId, new BigDecimal("110.00"), botId);
    }

    @Test
    public void testRunAutoBids_NextAmtExceedsMaxAmount_BreaksLoop() {
        UUID auctionId = UUID.randomUUID();
        BigDecimal currentHighestBid = new BigDecimal("100.00");
        UUID currentHighestBidderId = UUID.randomUUID();
        UUID botId = UUID.randomUUID();

        // config's max amount is 105.00, increment is 10.00
        // next amount would be 100.00 + 10.00 = 110.00, which exceeds max amount (105.00)
        AutoBidConfig config = new AutoBidConfig(auctionId, botId, new BigDecimal("105.00"), new BigDecimal("10.00"));
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

        AutoBidConfig config = new AutoBidConfig(auctionId, botId, new BigDecimal("150.00"), new BigDecimal("10.00"));
        when(autoBidRepository.findByAuctionId(auctionId)).thenReturn(List.of(config));

        when(bidGateway.placeAutoBid(botId, auctionId, new BigDecimal("110.00")))
            .thenThrow(new RuntimeException("Database error during placeAutoBid"));

        assertDoesNotThrow(() -> autoBidService.triggerAutoBids(auctionId, currentHighestBid, currentHighestBidderId));

        verify(bidGateway, times(1)).placeAutoBid(botId, auctionId, new BigDecimal("110.00"));
        verify(notificationService, never()).broadcastBidUpdate(any(), any(), any());
    }
}
