package com.nhom1.auction.common.entity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

import com.nhom1.auction.common.enums.BidType;

public class BidTransactionTest {

    @Test
    public void testConstructor_ValidArgs_CreatesSuccessfully() {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        BigDecimal amount = new BigDecimal("100.00");
        BidType bidType = BidType.MANUAL;

        BidTransaction bid = new BidTransaction(auctionId, bidderId, amount, bidType);

        assertNotNull(bid);
        assertEquals(auctionId, bid.getAuctionId());
        assertEquals(bidderId, bid.getBidderId());
        assertEquals(amount, bid.getAmount());
        assertEquals(bidType, bid.getBidType());
    }

    @Test
    public void testConstructor_NullAuctionId_Throws() {
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(
            null,
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            BidType.MANUAL
        ));
    }

    @Test
    public void testConstructor_NullBidderId_Throws() {
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(
            UUID.randomUUID(),
            null,
            new BigDecimal("100.00"),
            BidType.MANUAL
        ));
    }

    @Test
    public void testConstructor_NullAmount_Throws() {
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            null,
            BidType.MANUAL
        ));
    }

    @Test
    public void testConstructor_NullBidType_Throws() {
        assertThrows(IllegalArgumentException.class, () -> new BidTransaction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            null
        ));
    }
}