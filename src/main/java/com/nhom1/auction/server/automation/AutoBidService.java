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

      List<AutoBidConfig> eligibleConfigs =
          allConfigs.stream()
              .filter(cfg -> !cfg.getBidderId().equals(snapshotBidderId))
              .filter(cfg -> cfg.getMaxAmount().compareTo(snapshotBid) > 0)
              .toList();

      if (eligibleConfigs.isEmpty()) break;

      AutoBidConfig selected =
          eligibleConfigs.stream()
              .max(Comparator.comparing(AutoBidConfig::getMaxAmount))
              .orElse(null);
      if (selected == null) break;

      BigDecimal nextBestMax =
          allConfigs.stream()
              .filter(cfg -> !cfg.getBidderId().equals(selected.getBidderId()))
              .map(AutoBidConfig::getMaxAmount)
              .max(BigDecimal::compareTo)
              .orElse(BigDecimal.ZERO);

      BigDecimal requiredBid;
      if (!hasBids) {
        requiredBid = auction.getStartingPrice().max(nextBestMax.add(selected.getIncrement()));
      } else {
        requiredBid = currentHighestBid.max(nextBestMax).add(selected.getIncrement());
      }

      BigDecimal nextAmt = requiredBid.min(selected.getMaxAmount());
      BigDecimal minRequired =
          hasBids ? currentHighestBid.add(selected.getIncrement()) : auction.getStartingPrice();

      if (nextAmt.compareTo(minRequired) < 0) {
        break;
      }

      try {
        BidTransaction bid = bidGateway.placeAutoBid(selected.getBidderId(), auctionId, nextAmt);
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
