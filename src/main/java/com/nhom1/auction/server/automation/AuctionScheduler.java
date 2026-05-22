package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.server.infrastructure.NotificationService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Objects;

public class AuctionScheduler {
    private final AuctionGateway auctionGateway;
    private final BidGateway bidGateway;
    private final NotificationService notificationService;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    private final Duration antiSnipingWindow = Duration.ofSeconds(15);
    private final Duration antiSnipingExtension = Duration.ofSeconds(30);
    
    private String lastErrorMessage = null;

    public AuctionScheduler(
            AuctionGateway auctionGateway,
            BidGateway bidGateway,
            NotificationService notificationService
    ) {
        this.auctionGateway = auctionGateway;
        this.bidGateway = bidGateway;
        this.notificationService = notificationService;
    }

    public void start() {
        executor.scheduleAtFixedRate(this::safeTick, 1, 1, TimeUnit.SECONDS);
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void safeTick() {
        try {
            tick();
            // Reset error state on success
            if (lastErrorMessage != null) {
                lastErrorMessage = null;
            }
        } catch (Exception e) {
            String currentErrorMessage = e.getMessage();
            // Only log if the error message is different from the last one to avoid console spam
            if (!Objects.equals(currentErrorMessage, lastErrorMessage)) {
                System.err.println("AuctionScheduler tick failed: " + currentErrorMessage);
                lastErrorMessage = currentErrorMessage;
            }
        }
    }

    void tick() {
        LocalDateTime now = LocalDateTime.now();
        // Contract dependency:
        // AuctionGateway.findAll() is expected to return at least OPEN/RUNNING auctions
        // so scheduler can handle both start and finish transitions.
        List<Auction> auctions = auctionGateway.findAll();

        for (Auction auction : auctions) {
            if (auction.getStatus() == AuctionStatus.OPEN && !auction.getStartTime().isAfter(now)) {
                // startTime reached: OPEN auction moves to RUNNING.
                auctionGateway.updateStatus(auction.getId(), AuctionStatus.RUNNING);
                continue;
            }

            if (auction.getStatus() == AuctionStatus.RUNNING && !auction.getEndTime().isAfter(now)) {
                // endTime reached: either extend (anti-sniping) or finish.
                handleRunningAuctionTimeout(auction, now);
            }
        }
    }

    private void handleRunningAuctionTimeout(Auction auction, LocalDateTime now) {
        UUID auctionId = auction.getId();
        Optional<LocalDateTime> lastBidTime = bidGateway.findLastBidTime(auctionId);

        if (shouldExtend(auction.getEndTime(), lastBidTime, now)) {
            // Anti-sniping: if last bid lands in the configured final window, extend auction.
            LocalDateTime newEndTime = auction.getEndTime().plus(antiSnipingExtension);
            auctionGateway.updateEndTime(auctionId, newEndTime);
            notificationService.broadcastAuctionTimeExtended(auctionId, newEndTime);
            return;
        }

        // No extension condition met -> finalize auction lifecycle.
        auctionGateway.updateStatus(auctionId, AuctionStatus.FINISHED);
        // Cross-team dependency:
        // NotificationService implementation (member 4) decides push payload protocol.
        notificationService.broadcastAuctionEnded(
            auctionId, 
            auction.getHighestBidderId(), 
            auction.getCurrentHighestBid()
        );
    }

    private boolean shouldExtend(
            LocalDateTime endTime,
            Optional<LocalDateTime> lastBidTime,
            LocalDateTime now
    ) {
        if (lastBidTime.isEmpty()) {
            return false;
        }
        LocalDateTime windowStart = endTime.minus(antiSnipingWindow);
        LocalDateTime bidTime = lastBidTime.get();
        return (!bidTime.isBefore(windowStart)) && (!now.isBefore(endTime));
    }
}
