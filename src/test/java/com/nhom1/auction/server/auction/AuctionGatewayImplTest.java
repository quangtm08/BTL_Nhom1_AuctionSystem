package com.nhom1.auction.server.auction;

import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuctionGatewayImplTest {

    @Mock
    private AuctionRepository auctionRepository;

    private AuctionGatewayImpl auctionGateway;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        auctionGateway = new AuctionGatewayImpl(auctionRepository);
    }

    @Test
    public void testFindAll() {
        Auction mockAuction = mock(Auction.class);
        List<Auction> expected = Collections.singletonList(mockAuction);
        when(auctionRepository.findAll()).thenReturn(expected);

        List<Auction> result = auctionGateway.findAll();

        assertEquals(expected, result);
        verify(auctionRepository).findAll();
    }

    @Test
    public void testUpdateStatus() {
        UUID auctionId = UUID.randomUUID();
        AuctionStatus status = AuctionStatus.FINISHED;

        auctionGateway.updateStatus(auctionId, status);

        verify(auctionRepository).updateStatus(auctionId, status);
    }

    @Test
    public void testUpdateEndTime() {
        UUID auctionId = UUID.randomUUID();
        LocalDateTime newEndTime = LocalDateTime.now().plusDays(2);

        auctionGateway.updateEndTime(auctionId, newEndTime);

        verify(auctionRepository).updateEndTime(auctionId, newEndTime);
    }
}
