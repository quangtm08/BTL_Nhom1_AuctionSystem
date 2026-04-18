package com.nhom1.auction.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.exception.AuctionClosedException;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.InvalidBidException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;


public class Auction extends BaseEntity{
    private final UUID itemId;
    private final UUID sellerId;
    private final LocalDateTime startTime;
    private final LocalDateTime endTime;
    private final List<BidTransaction> bidHistory;

//volatile để đảm bảo giá trị được cập nhật và đồng bộ hóa, thread-safe !
    private volatile UUID highestBidderId;
    private volatile BigDecimal currentHighestBid;
    private volatile AuctionStatus status;

// thêm ReentrantLock đảm bảo thread-safety khi thay đổi trạng thái của phiên.
    private final ReentrantLock auctionLock = new ReentrantLock(true); //Fair lock để đảm bảo thứ tự truy cập công bằng giữa các thread.
    private final Object bidHistoryMonitor = new Object(); // Monitor riêng để đồng bộ hóa truy cập vào bidHistory
    // TODO: Change exception to domain exception of the app.
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

    public void startAuction() throws InvalidAuctionStateException {
        auctionLock.lock(); // Lock to ensure thread safety when changing the auction status
        try {
            if (status == AuctionStatus.OPEN){
                status = AuctionStatus.RUNNING;
                touchUpdatedAt();
            } else {
                throw new InvalidAuctionStateException("Only OPEN auctions can be started");
            }
        } finally {
            auctionLock.unlock();
        }
    }

    public void endAuction() throws InvalidAuctionStateException {
        auctionLock.lock(); // Lock to ensure thread safety when changing the auction status
        try {
            if (status == AuctionStatus.RUNNING){
                status = AuctionStatus.FINISHED;
                touchUpdatedAt();
            } else {
                throw new InvalidAuctionStateException("Only RUNNING auctions can be ended");
            }
        } finally {
            auctionLock.unlock();
        }
    }

    // TODO: Add payment validation before allowing this transition.
    public void markAsPaid() throws InvalidAuctionStateException {
        auctionLock.lock(); // Lock to ensure thread safety when changing the auction status
        try {
            if (status == AuctionStatus.FINISHED){
                status = AuctionStatus.PAID;
                touchUpdatedAt();
            } else {
                throw new InvalidAuctionStateException("Only FINISHED auctions can be marked as paid");
            }
        } finally {
            auctionLock.unlock();
        }
    }

    public BidTransaction placeBid(UUID bidderId, BigDecimal amount, BidType bidType, LocalDateTime bidTime)
        throws InvalidBidException, AuctionClosedException, UnauthorizedActionException {

        auctionLock.lock(); // Lock to ensure thread safety when placing a bid
        try {
            AuctionBidValidator.validatePlaceBid(this, bidderId, amount, bidType, bidTime);

            BidTransaction bidTransaction = new BidTransaction(getId(), bidderId, amount, bidType);
            synchronized (bidHistoryMonitor) { // Synchronize to ensure thread safety when modifying the bid history
                bidHistory.add(bidTransaction);
            }
            highestBidderId = bidderId;
            currentHighestBid = amount;
            touchUpdatedAt();
            return bidTransaction;
        } finally {
            auctionLock.unlock();
        }
    }


    // Sellers may cancel only their own OPEN auctions. Admins may cancel OPEN or RUNNING auctions.
    public void cancelAuction(UUID callerId, UserRole userRole)
        throws InvalidAuctionStateException, UnauthorizedActionException {

        auctionLock.lock(); // Lock to ensure thread safety when canceling the auction
        try {
            if (userRole == UserRole.ADMIN
                && (status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING)){
                status = AuctionStatus.CANCELED;
                touchUpdatedAt();
            } else if (userRole == UserRole.SELLER && callerId != null && callerId.equals(sellerId)
                && status == AuctionStatus.OPEN) {
                status = AuctionStatus.CANCELED;
                touchUpdatedAt();
            } else if (userRole == UserRole.ADMIN || (userRole == UserRole.SELLER && callerId != null && callerId.equals(sellerId))) {
                throw new InvalidAuctionStateException("The auction cannot be canceled in its current state");
            } else {
                throw new UnauthorizedActionException("Only the owning seller or an admin can cancel this auction");
            }

        } finally {
            auctionLock.unlock();
        }
    }
    // Return a snapshot so callers cannot modify the auction's internal bid history.
    public List<BidTransaction> getBidHistory() {
        synchronized (bidHistoryMonitor) { // Synchronize on the same monitor used when modifying the bid history
            return List.copyOf(bidHistory);
        }
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

