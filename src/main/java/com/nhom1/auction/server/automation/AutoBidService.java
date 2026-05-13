package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.server.infrastructure.NotificationService;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoBidService {
    private static final int MAX_TRIGGER_DEPTH = 20;

    private final AutoBidRepository autoBidRepository;
    private final BidGateway bidGateway;
    private final NotificationService notificationService;

    // Single daemon thread: auto-bid chains queue up and run sequentially
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "auto-bid-worker");
        t.setDaemon(true);
        return t;
    });

    public AutoBidService(AutoBidRepository autoBidRepository, BidGateway bidGateway,
                          NotificationService notificationService) {
        this.autoBidRepository = autoBidRepository;
        this.bidGateway = bidGateway;
        this.notificationService = notificationService;
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

    // Called from BidHandler — submits task and returns immediately
    public void scheduleAutoBids(UUID auctionId, BigDecimal highestBid, UUID highestBidderId) {
        executor.submit(() -> runAutoBids(auctionId, highestBid, highestBidderId));
    }

    // Kept for AuctionScheduler compatibility
    public void triggerAutoBids(UUID auctionId, BigDecimal newHighestBid, UUID currentHighestBidderId) {
        runAutoBids(auctionId, newHighestBid, currentHighestBidderId);
    }

    private void runAutoBids(UUID auctionId, BigDecimal currentHighestBid, UUID currentHighestBidderId) {
        BigDecimal finalBid = currentHighestBid;
        UUID finalBidderId = currentHighestBidderId;
        boolean anyBidPlaced = false;

        for (int depth = 0; depth < MAX_TRIGGER_DEPTH; depth++) {
            // Effectively-final snapshots required by lambda capture rules
            final BigDecimal snapshotBid = currentHighestBid;
            final UUID snapshotBidderId = currentHighestBidderId;

            List<AutoBidConfig> eligibleConfigs = autoBidRepository.findByAuctionId(auctionId).stream()
                .filter(cfg -> !cfg.getBidderId().equals(snapshotBidderId))
                .filter(cfg -> cfg.getMaxAmount().compareTo(snapshotBid) > 0)
                .toList();

            if (eligibleConfigs.isEmpty()) break;

            AutoBidConfig selected = eligibleConfigs.stream()
                .max(Comparator.comparing(cfg -> nextAmount(cfg, snapshotBid)))
                .orElse(null);
            if (selected == null) break;

            BigDecimal nextAmt = nextAmount(selected, currentHighestBid);
            if (nextAmt.compareTo(selected.getMaxAmount()) > 0) break;

            try {
                BidTransaction bid = bidGateway.placeAutoBid(selected.getBidderId(), auctionId, nextAmt);
                currentHighestBid = bid.getAmount();
                currentHighestBidderId = bid.getBidderId();
                finalBid = currentHighestBid;
                finalBidderId = currentHighestBidderId;
                anyBidPlaced = true;
            } catch (Exception ignored) {
                break;
            }
        }

        // Broadcast once after the entire chain settles
        if (anyBidPlaced) {
            notificationService.broadcastBidUpdate(auctionId, finalBid, finalBidderId);
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
