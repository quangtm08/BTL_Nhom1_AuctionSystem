package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.AuctionStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Auction extends BaseEntity{
    private final UUID itemId;
    private final UUID sellerId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<BidTransaction> bidHistory;

    private UUID highestBidderId;
    private BigDecimal currentHighestBid;
    private AuctionStatus status;

    public Auction(UUID itemId,UUID sellerId, LocalDateTime startTime, LocalDateTime endTime){
        if (itemId == null) {
            throw new IllegalArgumentException("itemId must not be null");
        }
        if (sellerId == null) {
            throw new IllegalArgumentException("sellerId must not be null");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("startTime must not be null");
        }
        if (endTime == null) {
            throw new IllegalArgumentException("endTime must not be null");
        }
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }


        this.itemId = itemId;
        this.sellerId = sellerId;
        this.startTime = startTime;
        this.endTime = endTime;

        this.highestBidderId = null;
        this.currentHighestBid = null;
        this.status = AuctionStatus.OPEN;
        this.bidHistory = new ArrayList<>();
    }



    //getters
    public UUID getItemId() {
        return itemId;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    /*return a shallow copy to prevent manipulation. Shallow copy is enough since BidTransaction is
    immutable
     */
    public List<BidTransaction> getBidHistory() {
        List<BidTransaction> copiedBidHistory = new ArrayList<>();
        for (BidTransaction b : bidHistory){
            copiedBidHistory.add(b);
        }
        return copiedBidHistory;
    }

    public UUID getHighestBidderId() {
        return highestBidderId;
    }

    public BigDecimal getCurrentHighestBid() {
        return currentHighestBid;
    }

    public AuctionStatus getStatus() {
        return status;
    }
}
