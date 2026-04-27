package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.entity.BidTransaction;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class AutoBidService {
    private static final int MAX_TRIGGER_DEPTH = 20;

    private final AutoBidRepository autoBidRepository;
    private final BidGateway bidGateway;

    public AutoBidService(AutoBidRepository autoBidRepository, BidGateway bidGateway) {
        this.autoBidRepository = autoBidRepository;
        this.bidGateway = bidGateway;
    }

    public AutoBidConfigResponse saveConfig(AutoBidConfigRequest dto) {
        UUID auctionId = parseUuid(dto.getAuctionId(), "auctionId");
        UUID bidderId = parseUuid(dto.getBidderId(), "bidderId");
        BigDecimal maxAmount = BigDecimal.valueOf(dto.getMaxAmount());
        BigDecimal increment = BigDecimal.valueOf(dto.getIncrement());

        if (maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("maxAmount must be > 0");
        }
        if (increment.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("increment must be > 0");
        }
        if (maxAmount.compareTo(increment) < 0) {
            throw new IllegalArgumentException("maxAmount must be >= increment");
        }

        autoBidRepository.save(new AutoBidConfig(auctionId, bidderId, maxAmount, increment));
        return new AutoBidConfigResponse("CONFIG_SAVED");
    }

    public void triggerAutoBids(UUID auctionId, BigDecimal newHighestBid, UUID currentHighestBidderId) {
        // Entry point is called with latest accepted highest bid state
        // (usually right after a successful manual bid in BidHandler).
        triggerAutoBidsInternal(auctionId, newHighestBid, currentHighestBidderId, 0);
    }

    private void triggerAutoBidsInternal(
            UUID auctionId,
            BigDecimal currentHighestBid,
            UUID currentHighestBidderId,
            int depth
    ) {
        // Prevent accidental endless ping-pong when multiple bots keep overbidding.
        if (depth >= MAX_TRIGGER_DEPTH) {
            return;
        }

        // Candidate selection for this round:
        // 1) Exclude the current leader to avoid self-overbidding loops.
        // 2) Keep only bidders whose maxAmount is still above currentHighestBid.
        //    (They still have budget to place at least one higher auto bid.)
        List<AutoBidConfig> eligibleConfigs = autoBidRepository.findByAuctionId(auctionId).stream()
            .filter(cfg -> !cfg.getBidderId().equals(currentHighestBidderId))
            .filter(cfg -> cfg.getMaxAmount().compareTo(currentHighestBid) > 0)
            .toList();

        if (eligibleConfigs.isEmpty()) {
            return;
        }

        // Selection rule:
        // choose the config that can place the highest immediate next bid in this round.
        // This resolves "multiple eligible bots at once" deterministically.
        AutoBidConfig selected = eligibleConfigs.stream()
            .max(Comparator.comparing(cfg -> nextAmount(cfg, currentHighestBid)))
            .orElse(null);
        if (selected == null) {
            return;
        }

        // Candidate bid amount for selected bot in this round.
        BigDecimal nextAmount = nextAmount(selected, currentHighestBid);
        if (nextAmount.compareTo(selected.getMaxAmount()) > 0) {
            // Safety check for rounding/edge cases: do not place bids above configured ceiling.
            return;
        }

        try {
            BidTransaction bid = bidGateway.placeAutoBid(selected.getBidderId(), auctionId, nextAmount);
            // Cross-team dependency:
            // Bid module should call this service from BidHandler after every accepted bid.
            triggerAutoBidsInternal(auctionId, bid.getAmount(), bid.getBidderId(), depth + 1);
        } catch (Exception ignored) {
            // Cross-team dependency:
            // If BidService rejects due to stale state/race, automation should stop silently.
            // Bid module remains source of truth for bid validity.
        }
    }

    private BigDecimal nextAmount(AutoBidConfig config, BigDecimal currentHighestBid) {
        return currentHighestBid.add(config.getIncrement());
    }

    private UUID parseUuid(String value, String fieldName) {
        try {
            return UUID.fromString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException(fieldName + " is invalid UUID");
        }
    }
}
