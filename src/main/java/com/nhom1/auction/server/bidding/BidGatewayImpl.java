package com.nhom1.auction.server.bidding;

import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.server.automation.BidGateway;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class BidGatewayImpl implements BidGateway {
    private final BidService bidService;
    private final BidRepository bidRepository;

    public BidGatewayImpl(BidService bidService, BidRepository bidRepository) {
        this.bidService = bidService;
        this.bidRepository = bidRepository;
    }

    @Override
    public BidTransaction placeAutoBid(UUID bidderId, UUID auctionId, BigDecimal amount) {
        try {
            return bidService.placeBid(bidderId, auctionId, amount, BidType.AUTO);
        } catch (Exception e) {
            throw new RuntimeException("Failed to place auto bid", e);
        }
    }

    @Override
    public Optional<LocalDateTime> findLastBidTime(UUID auctionId) {
        return bidRepository.findLastBidTime(auctionId);
    }
}
