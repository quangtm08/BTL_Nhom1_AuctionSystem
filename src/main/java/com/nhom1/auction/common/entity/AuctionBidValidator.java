package com.nhom1.auction.common.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.exception.AuctionClosedException;
import com.nhom1.auction.common.exception.InvalidBidException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;

final class AuctionBidValidator {

    private AuctionBidValidator() {
    }

    static void validatePlaceBid(
        Auction auction,
        UUID bidderId,
        BigDecimal amount,
        BidType bidType,
        LocalDateTime bidTime
    ) throws InvalidBidException, AuctionClosedException, UnauthorizedActionException {
        if (bidderId == null) {
            throw new InvalidBidException("bidderId must not be null");
        }
        if (amount == null) {
            throw new InvalidBidException("amount must not be null");
        }
        if (bidType == null) {
            throw new InvalidBidException("bidType must not be null");
        }
        if (bidTime == null) {
            throw new InvalidBidException("bidTime must not be null");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBidException("amount must be greater than zero");
        }
        if (bidderId.equals(auction.getSellerId())) {
            throw new UnauthorizedActionException("seller cannot bid on their own auction");
        }
        if (auction.getStatus() != AuctionStatus.RUNNING) {
            throw new AuctionClosedException("auction is not accepting bids");
        }
        if (bidTime.isAfter(auction.getEndTime())) {
            throw new AuctionClosedException("auction has already ended");
        }
        if (auction.getCurrentHighestBid() == null) {
            if (amount.compareTo(auction.getStartingPrice()) < 0) {
                throw new InvalidBidException("The first bid must be at least the starting price of " + auction.getStartingPrice());
            }
        } else if (amount.compareTo(auction.getCurrentHighestBid()) <= 0) {
            throw new InvalidBidException("amount must be greater than currentHighestBid");
        } else {
            BigDecimal minIncrement = auction.getMinBidIncrement();
            BigDecimal requiredMinimum = auction.getCurrentHighestBid().add(minIncrement);
            if (amount.compareTo(requiredMinimum) < 0) {
                throw new InvalidBidException(
                    "amount must be at least " + requiredMinimum
                        + " (currentHighestBid + minIncrement)"
                );
            }
        }
    }
}
