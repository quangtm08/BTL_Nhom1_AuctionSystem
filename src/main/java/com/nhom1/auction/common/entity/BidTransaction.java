package com.nhom1.auction.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import com.nhom1.auction.common.enums.BidType;

public class BidTransaction {
    private UUID id;
    private UUID auctionId;
    private UUID bidderId;
    private BigDecimal amount;
    private LocalDateTime timestamp;
    private BidType bidType;

    public BidTransaction(UUID auctionId, UUID bidderId, BigDecimal amount, BidType bidType) {
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId must not be null");
        }
        if (bidderId == null) {
            throw new IllegalArgumentException("bidderId must not be null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount must not be null");
        }
        if (bidType == null) {
            throw new IllegalArgumentException("bidType must not be null");
        }

        this.id = UUID.randomUUID();
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.timestamp = LocalDateTime.now();
        this.bidType = bidType;
    }
}


