package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigDetailResponse;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.infrastructure.NotificationService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AutoBidService {

  private static final int MAX_TRIGGER_DEPTH = 10;

  private final AutoBidRepository autoBidRepository;
  private final AuctionGateway auctionGateway;
  private final BidGateway bidGateway;
  private final NotificationService notificationService;

  private final ExecutorService executor =
      Executors.newSingleThreadExecutor(
          r -> {
            Thread t = new Thread(r, "auto-bid-worker");
            t.setDaemon(true);
            return t;
          });

  public AutoBidService(
      AutoBidRepository autoBidRepository,
      AuctionGateway auctionGateway,
      BidGateway bidGateway,
      NotificationService notificationService) {
    this.autoBidRepository = autoBidRepository;
    this.auctionGateway = auctionGateway;
    this.bidGateway = bidGateway;
    this.notificationService = notificationService;
  }

  // Business operations
  public AutoBidConfigResponse saveConfig(AutoBidConfigRequest dto) {
    UUID auctionId = parseUuid(dto.getAuctionId(), "auctionId");
    UUID bidderId = parseUuid(dto.getBidderId(), "bidderId");
    BigDecimal maxAmount = parseMoney(dto.getMaxAmount(), "maxAmount");
    BigDecimal incrementFromClient = parseMoney(dto.getIncrement(), "increment");
    Auction auction = requireRunningAuction(auctionId);
    BigDecimal increment = auction.getMinBidIncrement();

    if (maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ValidationException("maxAmount must be > 0");
    }
    if (incrementFromClient.compareTo(increment) < 0) {
      throw new ValidationException(
          "increment must be >= minimum increment (" + increment.toPlainString() + ")");
    }
    if (maxAmount.compareTo(incrementFromClient) < 0) {
      throw new ValidationException("maxAmount must be >= increment");
    }

    autoBidRepository.save(new AutoBidConfig(auctionId, bidderId, maxAmount, incrementFromClient));

    scheduleAutoBids(
        auctionId,
        auction.getCurrentHighestBid() == null ? BigDecimal.ZERO : auction.getCurrentHighestBid(),
        auction.getHighestBidderId());

    return new AutoBidConfigResponse("CONFIG_SAVED");
  }

  public AutoBidConfigResponse deleteConfig(String auctionIdRaw, String bidderIdRaw) {
    UUID auctionId = parseUuid(auctionIdRaw, "auctionId");
    UUID bidderId = parseUuid(bidderIdRaw, "bidderId");
    int deleted = autoBidRepository.deleteByAuctionAndBidder(auctionId, bidderId);
    return new AutoBidConfigResponse(deleted > 0 ? "CONFIG_DELETED" : "CONFIG_NOT_FOUND");
  }

  public void scheduleAutoBids(UUID auctionId, BigDecimal highestBid, UUID highestBidderId) {
    executor.submit(() -> runAutoBids(auctionId, highestBid, highestBidderId));
  }

  public void triggerAutoBids(
      UUID auctionId, BigDecimal newHighestBid, UUID currentHighestBidderId) {
    runAutoBids(auctionId, newHighestBid, currentHighestBidderId);
  }

  // Query operations
  public AutoBidConfigDetailResponse getConfig(String auctionIdRaw, String bidderIdRaw) {
    UUID auctionId = parseUuid(auctionIdRaw, "auctionId");
    UUID bidderId = parseUuid(bidderIdRaw, "bidderId");
    return autoBidRepository
        .findByAuctionAndBidder(auctionId, bidderId)
        .map(
            cfg ->
                new AutoBidConfigDetailResponse(
                    cfg.getAuctionId().toString(),
                    cfg.getBidderId().toString(),
                    cfg.getMaxAmount().toPlainString(),
                    cfg.getIncrement().toPlainString(),
                    true))
        .orElse(
            new AutoBidConfigDetailResponse(
                auctionId.toString(), bidderId.toString(), null, null, false));
  }

  // Helpers
  private void runAutoBids(
      UUID auctionId, BigDecimal currentHighestBid, UUID currentHighestBidderId) {
    Auction auction = auctionGateway.findById(auctionId).orElse(null);
    if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) {
      autoBidRepository.deleteByAuctionId(auctionId);
      return;
    }

    BigDecimal finalBid = currentHighestBid;
    UUID finalBidderId = currentHighestBidderId;
    boolean anyBidPlaced = false;
    boolean hasBids =
        (currentHighestBidderId != null
            && currentHighestBid != null
            && currentHighestBid.compareTo(BigDecimal.ZERO) > 0);

    for (int depth = 0; depth < MAX_TRIGGER_DEPTH; depth++) {
      final BigDecimal snapshotBid = currentHighestBid;
      final UUID snapshotBidderId = currentHighestBidderId;

      List<AutoBidConfig> allConfigs = autoBidRepository.findByAuctionId(auctionId);

      // Find the current leader's config to check for escalation
      AutoBidConfig leaderConfig =
          allConfigs.stream()
              .filter(cfg -> cfg.getBidderId().equals(snapshotBidderId))
              .findFirst()
              .orElse(null);

      PriorityQueue<AutoBidConfig> queue =
          new PriorityQueue<>(
              Comparator.comparing(AutoBidConfig::getCreatedAt)
                  .thenComparing(AutoBidConfig::getBidderId));

      for (AutoBidConfig cfg : allConfigs) {
        if (cfg.getBidderId().equals(snapshotBidderId)) {
          continue;
        }

        BigDecimal minRequired =
            !hasBids ? auction.getStartingPrice() : snapshotBid.add(cfg.getIncrement());

        if (cfg.getMaxAmount().compareTo(minRequired) >= 0) {
          queue.add(cfg);
        }
      }

      AutoBidConfig selected = queue.poll();
      if (selected == null) break;

      BigDecimal nextAmt;
      UUID bidderToBid = selected.getBidderId();
      if (leaderConfig != null) {
        // Escalation: if we're competing against another auto-bidder, jump to outbid them
        nextAmt =
            leaderConfig.getMaxAmount().add(selected.getIncrement()).min(selected.getMaxAmount());

        // If we are the "lower" one, we bid our max and let the leader outbid us in the next step
        if (selected.getMaxAmount().compareTo(leaderConfig.getMaxAmount()) <= 0) {
          nextAmt = selected.getMaxAmount();

          // Tie-breaker: If both configs have the same maxAmount, and selected is the newer one,
          // we let the leader (older config) bid their max amount to retain leadership.
          if (selected.getMaxAmount().compareTo(leaderConfig.getMaxAmount()) == 0
              && selected.getCreatedAt().isAfter(leaderConfig.getCreatedAt())) {
            bidderToBid = leaderConfig.getBidderId();
          }
        }

        // Ensure we at least bid the minimum required amount
        BigDecimal minRequired =
            !hasBids ? auction.getStartingPrice() : snapshotBid.add(selected.getIncrement());
        if (nextAmt.compareTo(minRequired) < 0) {
          nextAmt = minRequired;
        }
      } else {
        // Standard minimum increment bid
        nextAmt = !hasBids ? auction.getStartingPrice() : snapshotBid.add(selected.getIncrement());
      }

      try {
        BidTransaction bid = bidGateway.placeAutoBid(bidderToBid, auctionId, nextAmt);
        currentHighestBid = bid.getAmount();
        currentHighestBidderId = bid.getBidderId();
        finalBid = currentHighestBid;
        finalBidderId = currentHighestBidderId;
        anyBidPlaced = true;
        hasBids = true;
      } catch (Exception ignored) {
        System.err.println(
            "Auto-bid failed for auction " + auctionId + ": " + ignored.getMessage());
        break;
      }
    }

    // Broadcast once after the entire chain settles
    if (anyBidPlaced) {
      notificationService.broadcastBidUpdate(auctionId, finalBid, finalBidderId);
    }
  }

  private UUID parseUuid(String value, String fieldName) {
    try {
      return UUID.fromString(value);
    } catch (Exception e) {
      throw new ValidationException(fieldName + " is invalid UUID");
    }
  }

  private BigDecimal parseMoney(String value, String fieldName) {
    try {
      return new BigDecimal(value);
    } catch (Exception e) {
      throw new ValidationException(fieldName + " is invalid decimal");
    }
  }

  private Auction requireRunningAuction(UUID auctionId) {
    Auction auction =
        auctionGateway
            .findById(auctionId)
            .orElseThrow(() -> new NotFoundException("Auction not found"));
    if (auction.getStatus() != AuctionStatus.RUNNING) {
      throw new ValidationException("Auction is not running");
    }
    return auction;
  }
}
