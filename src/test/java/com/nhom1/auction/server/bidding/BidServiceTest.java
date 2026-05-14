package com.nhom1.auction.server.bidding;

import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BidServiceTest {

    @Mock
    private BidRepository bidRepository;

    @Mock
    private AuctionRepository auctionRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private Connection connection;

    private BidService bidService;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        bidService = new BidService(bidRepository, auctionRepository, itemRepository, userRepository, connection);
    }

    @Test
    public void testPlaceBid_ValidBid_SavesAndUpdatesHighestBid() throws Exception {
        UUID bidderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        Auction auction = new Auction(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.startAuction();
        UUID auctionId = auction.getId();
        when(connection.getAutoCommit()).thenReturn(true);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));

        BidTransaction result = bidService.placeBid(bidderId, auctionId, amount, BidType.MANUAL);

        assertNotNull(result);
        assertEquals(amount, result.getAmount());
        assertEquals(bidderId, result.getBidderId());
        verify(bidRepository).save(any(BidTransaction.class));
        verify(auctionRepository).updateHighestBid(auctionId, amount, bidderId);
        verify(connection).setAutoCommit(false);
        verify(connection).commit();
        verify(connection).setAutoCommit(true);
    }

    @Test
    public void testPlaceBid_AuctionNotFound_ThrowsValidationException() throws SQLException {
        UUID bidderId = UUID.randomUUID();
        UUID auctionId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        when(connection.getAutoCommit()).thenReturn(true);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> bidService.placeBid(bidderId, auctionId, amount, BidType.MANUAL));
        verify(connection).rollback();
        verify(connection).setAutoCommit(true);
        verify(connection, never()).commit();
    }

    @Test
    public void testPlaceBid_UpdateHighestBidFails_RollsBackAndRestoresAutoCommit() throws Exception {
        UUID bidderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("150.00");
        Auction auction = new Auction(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"),
                LocalDateTime.now().minusHours(1), LocalDateTime.now().plusHours(1));
        auction.startAuction();
        UUID auctionId = auction.getId();
        when(connection.getAutoCommit()).thenReturn(false);
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        doThrow(new RuntimeException("update failed")).when(auctionRepository)
                .updateHighestBid(eq(auctionId), eq(amount), eq(bidderId));

        RuntimeException thrown = assertThrows(RuntimeException.class,
                () -> bidService.placeBid(bidderId, auctionId, amount, BidType.MANUAL));

        assertTrue(thrown.getMessage().startsWith("Bid placement failed"));
        verify(bidRepository).save(any(BidTransaction.class));
        verify(connection).rollback();
        verify(connection, times(2)).setAutoCommit(false);
        verify(connection, never()).setAutoCommit(true);
        verify(connection, never()).commit();
    }

    @Test
    public void testGetAuctionDetail_ValidAuction_ReturnsDtoWithBidHistory() throws Exception {
        Item item = new Item("Test Item", "Test Description", ItemCategory.ART, ItemCondition.NEW) {
            @Override
            public void printInfo() {}
        };
        UUID itemId = item.getId();
        UUID sellerId = UUID.randomUUID();
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        LocalDateTime endTime = startTime.plusHours(2);
        Auction auction = new Auction(itemId, sellerId, new BigDecimal("100.00"), startTime, endTime);
        auction.startAuction();
        UUID auctionId = auction.getId();
        UUID highestBidderId = UUID.randomUUID();
        BigDecimal currentHighestBid = new BigDecimal("150.00");
        auction.placeBid(highestBidderId, currentHighestBid, BidType.MANUAL, LocalDateTime.now());

        when(auctionRepository.findById(auctionId)).thenReturn(Optional.of(auction));
        when(itemRepository.findById(itemId)).thenReturn(Optional.of(item));
        when(bidRepository.findByAuctionId(auctionId)).thenReturn(List.of());

        AuctionDetailDto result = bidService.getAuctionDetail(auctionId);

        assertNotNull(result);
        assertEquals(auctionId.toString(), result.getAuctionId());
        assertEquals(itemId.toString(), result.getItemID());
        assertEquals("Test Item", result.getItemName());
        assertEquals(currentHighestBid, result.getCurrentHighestBid());
        assertEquals(highestBidderId.toString(), result.getCurrentHighestBidderId());
        assertEquals(new BigDecimal("5.00"), result.getMinBidIncrement());
    }

    @Test
    public void testGetAuctionDetail_AuctionNotFound_ThrowsValidationException() {
        UUID auctionId = UUID.randomUUID();
        when(auctionRepository.findById(auctionId)).thenReturn(Optional.empty());

        assertThrows(ValidationException.class, () -> bidService.getAuctionDetail(auctionId));
    }
}
