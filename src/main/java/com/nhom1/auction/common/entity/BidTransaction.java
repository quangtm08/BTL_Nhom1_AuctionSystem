package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.BidType;
import java.math.BigDecimal;
import java.util.UUID;

public class BidTransaction extends BaseEntity {
    private final UUID auctionId;
    private final UUID bidderId;
    private final BigDecimal amount;
    private final BidType bidType;

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
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.amount = amount;
        this.bidType = bidType;
    }

    public UUID getAuctionId() {
        return auctionId;
    }

    public UUID getBidderId() {
        return bidderId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public BidType getBidType() {
        return bidType;
    }
}
