package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.entity.BidTransaction;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Cross-team contract:
 * - Implemented by bidding module (member owning BidService/BidRepository).
 * - Automation module depends on this interface only.
 */
public interface BidGateway {
    BidTransaction placeAutoBid(UUID bidderId, UUID auctionId, BigDecimal amount);
    Optional<LocalDateTime> findLastBidTime(UUID auctionId);
}
