package com.nhom1.auction.common.entity;

import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.UserRole;
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

    //Constructor
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


    //Methods

    // Turn auction from open to running (i.e. when reaching the startTime)
    public void startAuction(){
        if (status == AuctionStatus.OPEN){
            status = AuctionStatus.RUNNING;
            touchUpdatedAt();
        } else {
            throw new IllegalArgumentException();
        }
    }

    // Turn auction from running to finished (i.e. when reaching the endTime)
    public void endAuction(){
        if (status == AuctionStatus.RUNNING){
            status = AuctionStatus.FINISHED;
            touchUpdatedAt();
        } else {
            throw new IllegalArgumentException();
        }
    }

    // Turn auction from Finished to Paid
    //Still missing payment validation logic
    public void markAsPaid(){
        if (status == AuctionStatus.FINISHED){
            status = AuctionStatus.PAID;
            touchUpdatedAt();
        } else {
            throw new IllegalArgumentException();
        }
    }


    /*Seller can cancel an auction when it is OPEN and NOT RUNNING.
    Admin can cancel at either OPEN or RUNNING
     */
    public void cancelAuction(UUID callerId, UserRole userRole){
        if (userRole == UserRole.ADMIN
            && (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING)){
            status = AuctionStatus.CANCELED;
            touchUpdatedAt();
        } else if (userRole == UserRole.SELLER && callerId.equals(sellerId)
            && status == AuctionStatus.OPEN){
            status = AuctionStatus.CANCELED;
            touchUpdatedAt();
        } else {
            throw new IllegalArgumentException();
        }
    }



    //getters

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
