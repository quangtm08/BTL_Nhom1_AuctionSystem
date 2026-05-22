package com.nhom1.auction.server.bidding;

import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.enums.BidType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BidGatewayImplTest {

    @Mock
    private BidService bidService;

    @Mock
    private BidRepository bidRepository;

    private BidGatewayImpl bidGateway;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        bidGateway = new BidGatewayImpl(bidService, bidRepository);
    }

    @Test
    public void testPlaceAutoBid_Success() throws Exception {
        UUID bidderId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        BidTransaction mockTx = mock(BidTransaction.class);

        when(bidService.placeBid(bidderId, auctionId, amount, BidType.AUTO)).thenReturn(mockTx);

        BidTransaction result = bidGateway.placeAutoBid(bidderId, auctionId, amount);

        assertEquals(mockTx, result);
        verify(bidService).placeBid(bidderId, auctionId, amount, BidType.AUTO);
    }

    @Test
    public void testPlaceAutoBid_Failure_ThrowsRuntimeException() throws Exception {
        UUID bidderId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");

        when(bidService.placeBid(bidderId, auctionId, amount, BidType.AUTO))
                .thenThrow(new RuntimeException("Database down"));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                bidGateway.placeAutoBid(bidderId, auctionId, amount)
        );

        assertTrue(exception.getMessage().contains("Failed to place auto bid"));
        assertTrue(exception.getCause().getMessage().contains("Database down"));
    }

    @Test
    public void testFindLastBidTime() {
        UUID auctionId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        when(bidRepository.findLastBidTime(auctionId)).thenReturn(Optional.of(now));

        Optional<LocalDateTime> result = bidGateway.findLastBidTime(auctionId);

        assertTrue(result.isPresent());
        assertEquals(now, result.get());
        verify(bidRepository).findLastBidTime(auctionId);
    }
}
