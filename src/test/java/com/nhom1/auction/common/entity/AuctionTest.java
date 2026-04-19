package com.nhom1.auction.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.exception.AuctionClosedException;
import com.nhom1.auction.common.exception.InvalidBidException;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class AuctionTest {

    @Test
    public void testPlaceBid_Success() throws Exception {
        Auction auction = createRunningAuction();
        UUID bidderId = UUID.randomUUID();
        LocalDateTime bidTime = auction.getStartTime().plusMinutes(10);

        BidTransaction bidTransaction = auction.placeBid(
            bidderId,
            new BigDecimal("100.00"),
            BidType.MANUAL,
            bidTime
        );

        assertNotNull(bidTransaction, "The created bid should not be null");
        assertEquals(bidderId, auction.getHighestBidderId(), "The highest bidder should be updated");
        assertEquals(new BigDecimal("100.00"), auction.getCurrentHighestBid(), "The highest bid should be updated");
        assertEquals(1, auction.getBidHistory().size(), "The bid history should contain the new bid");
        assertSame(bidTransaction, auction.getBidHistory().getFirst(), "The bid history should store the created bid");
    }

    @Test
    public void testPlaceBid_RejectWhenAuctionIsNotRunning() {
        LocalDateTime startTime = LocalDateTime.now().plusHours(1);
        Auction auction = new Auction(UUID.randomUUID(), UUID.randomUUID(), startTime, startTime.plusHours(2));

        assertThrows(
            AuctionClosedException.class,
            () -> auction.placeBid(
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                BidType.MANUAL,
                startTime.plusMinutes(5)
            ),
            "Bids should be rejected when the auction is not RUNNING"
        );
    }

    @Test
    public void testPlaceBid_RejectWhenAmountDoesNotExceedCurrentHighestBid() throws Exception {
        Auction auction = createRunningAuction();
        LocalDateTime bidTime = auction.getStartTime().plusMinutes(10);

        auction.placeBid(UUID.randomUUID(), new BigDecimal("100.00"), BidType.MANUAL, bidTime);

        assertThrows(
            InvalidBidException.class,
            () -> auction.placeBid(
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                BidType.MANUAL,
                bidTime.plusMinutes(1)
            ),
            "A new bid must be strictly greater than the current highest bid"
        );
    }

    @Test
    public void testPlaceBid_RejectWhenSellerBidsOnOwnAuction() throws Exception {
        Auction auction = createRunningAuction();

        assertThrows(
            UnauthorizedActionException.class,
            () -> auction.placeBid(
                auction.getSellerId(),
                new BigDecimal("100.00"),
                BidType.MANUAL,
                auction.getStartTime().plusMinutes(5)
            ),
            "The seller should not be allowed to bid on their own auction"
        );
    }

    private Auction createRunningAuction() throws InvalidAuctionStateException {
        LocalDateTime startTime = LocalDateTime.now().minusHours(1);
        Auction auction = new Auction(UUID.randomUUID(), UUID.randomUUID(), startTime, startTime.plusHours(2));
        auction.startAuction();
        return auction;
    }
}
