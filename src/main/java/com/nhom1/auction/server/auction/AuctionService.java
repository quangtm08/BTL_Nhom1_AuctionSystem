package com.nhom1.auction.server.auction;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.auction.CreateAuctionRequest;
import com.nhom1.auction.common.dto.auction.UpdateAuctionRequest;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.exception.AppException;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.common.factory.ItemFactory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

public class AuctionService {

  private final AuctionRepository auctionRepository;
  private final ItemRepository itemRepository;
  private final ItemImageRepository itemImageRepository;
  private final DataSource dataSource;

  public AuctionService(
      AuctionRepository auctionRepository,
      ItemRepository itemRepository,
      ItemImageRepository itemImageRepository,
      DataSource dataSource) {
    this.auctionRepository = auctionRepository;
    this.itemRepository = itemRepository;
    this.itemImageRepository = itemImageRepository;
    this.dataSource = dataSource;
  }

  // Business operations
  public Auction createAuction(String sellerId, CreateAuctionRequest dto) {
    validateCreateAuctionRequest(sellerId, dto);

    UUID parsedSellerId = UUID.fromString(sellerId);
    Item item = createItem(dto);
    LocalDateTime startTime = dto.getStartTime();
    LocalDateTime endTime = resolveCreateEndTime(dto, startTime);

    try (Connection connection = dataSource.getConnection()) {
      boolean oldAutoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);
      try {
        itemRepository.save(item, parsedSellerId, connection);
        itemImageRepository.saveImageUrls(item.getId(), dto.getImageUrls(), connection);

        Auction auction =
            new Auction(item.getId(), parsedSellerId, dto.getStartingPrice(), startTime, endTime);
        auctionRepository.save(auction, connection);
        auctionRepository.updateStatus(auction.getId(), AuctionStatus.PENDING, connection);
        connection.commit();
        return auction;
      } catch (AppException ex) {
        connection.rollback();
        throw ex;
      } catch (Exception ex) {
        connection.rollback();
        throw ex;
      } finally {
        connection.setAutoCommit(oldAutoCommit);
      }
    } catch (AppException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException("Create auction transaction failed", ex);
    }
  }

  public void deleteAuction(String sellerId, String auctionId) {
    UUID parsedSellerId = parseSellerId(sellerId);
    if (auctionId == null || auctionId.isBlank()) {
      throw new ValidationException("auctionId must not be blank");
    }

    UUID parsedAuctionId;
    try {
      parsedAuctionId = UUID.fromString(auctionId);
    } catch (IllegalArgumentException ex) {
      throw new ValidationException("auctionId is not a valid UUID");
    }

    Auction auction =
        auctionRepository
            .findById(parsedAuctionId)
            .orElseThrow(() -> new NotFoundException("Auction not found"));
    if (!parsedSellerId.equals(auction.getSellerId())) {
      throw new UnauthorizedActionException("You are not allowed to delete this auction");
    }

    if (!isEditableAuctionStatus(auction.getStatus())) {
      throw new InvalidAuctionStateException("Only pending or open auctions can be deleted");
    }

    try (Connection connection = dataSource.getConnection()) {
      boolean oldAutoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);
      try {
        int deletedAuctions = auctionRepository.deleteById(parsedAuctionId, connection);
        int deletedItems = itemRepository.deleteById(auction.getItemId(), connection);
        if (deletedAuctions == 0) {
          throw new IllegalStateException("Auction was not deleted.");
        }
        if (deletedItems == 0) {
          throw new IllegalStateException("Item was not deleted.");
        }
        connection.commit();
      } catch (AppException ex) {
        connection.rollback();
        throw ex;
      } catch (Exception ex) {
        connection.rollback();
        throw ex;
      } finally {
        connection.setAutoCommit(oldAutoCommit);
      }
    } catch (AppException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException("Delete transaction failed", ex);
    }
  }

  public void updateAuction(UpdateAuctionRequest dto) {
    if (dto == null) throw new ValidationException("UpdateAuctionRequest must not be null");
    UUID sellerUuid = parseSellerId(dto.getSellerId());
    if (dto.getAuctionId() == null || dto.getAuctionId().isBlank())
      throw new ValidationException("auctionId must not be blank");
    UUID auctionUuid;
    try {
      auctionUuid = UUID.fromString(dto.getAuctionId());
    } catch (IllegalArgumentException ex) {
      throw new ValidationException("auctionId is not a valid UUID");
    }
    Auction auction =
        auctionRepository
            .findById(auctionUuid)
            .orElseThrow(() -> new NotFoundException("Auction not found"));
    if (!sellerUuid.equals(auction.getSellerId()))
      throw new UnauthorizedActionException("You are not allowed to edit this auction");

    if (auction.getStatus() == AuctionStatus.RUNNING
        && auction.getStartTime() != null
        && auction.getStartTime().isAfter(LocalDateTime.now())) {
      auctionRepository.updateStatus(auctionUuid, AuctionStatus.OPEN);
      auction =
          auctionRepository
              .findById(auctionUuid)
              .orElseThrow(() -> new NotFoundException("Auction not found"));
    }

    if (!isEditableAuctionStatus(auction.getStatus()))
      throw new InvalidAuctionStateException("Only pending or open auctions can be edited");

    if (auction.getHighestBidderId() != null)
      throw new ValidationException("Auction already has bids and cannot be edited");
    if (dto.getEndTime() == null) throw new ValidationException("endTime is required");
    if (auction.getStartTime() != null && !dto.getEndTime().isAfter(auction.getStartTime()))
      throw new ValidationException("endTime must be after startTime");
    if (!dto.getEndTime().isAfter(LocalDateTime.now()))
      throw new ValidationException("endTime must be in the future");
    if (dto.getStartingPrice() == null || dto.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0)
      throw new ValidationException("startingPrice must be greater than 0");
    if (dto.getName() == null || dto.getName().isBlank())
      throw new ValidationException("name must not be blank");
    if (dto.getCategory() == null || dto.getCondition() == null)
      throw new ValidationException("category and condition are required");

    try (Connection connection = dataSource.getConnection()) {
      boolean oldAutoCommit = connection.getAutoCommit();
      connection.setAutoCommit(false);
      try {
        int updatedItem =
            itemRepository.updateBasicInfo(
                auction.getItemId(),
                dto.getName().trim(),
                dto.getDescription(),
                dto.getCategory(),
                dto.getCondition(),
                connection);
        int updatedAuction =
            auctionRepository.updateOpenAuctionForEdit(
                auctionUuid, dto.getStartingPrice(), dto.getEndTime(), connection);
        if (updatedAuction == 0) throw new ValidationException("Auction is no longer editable");
        if (updatedItem == 0) throw new NotFoundException("Auction item not found");
        connection.commit();
      } catch (Exception ex) {
        connection.rollback();
        throw ex;
      } finally {
        connection.setAutoCommit(oldAutoCommit);
      }
    } catch (AppException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new RuntimeException("Update auction transaction failed", ex);
    }
  }

  // Query operations
  public List<AuctionSummaryDto> getMyListings(String sellerId) {
    UUID parsedSellerId = parseSellerId(sellerId);
    List<AuctionSummaryDto> listings = new ArrayList<>();
    for (Auction auction : auctionRepository.findBySellerId(parsedSellerId)) {
      Item item = itemRepository.findById(auction.getItemId()).orElse(null);
      if (item == null) {
        // Data may be inconsistent in production; skip corrupted row instead of failing all
        // listings.
        continue;
      }
      BigDecimal startingPrice = auction.getStartingPrice();
      String itemCategory = item.getCategory() != null ? item.getCategory().name() : "UNKNOWN";
      listings.add(
          new AuctionSummaryDto(
              auction.getId().toString(),
              item.getName(),
              itemCategory,
              startingPrice,
              auction.getCurrentHighestBid(),
              auction.getStartTime(),
              auction.getEndTime(),
              auction.getStatus(),
              auction.getSellerId().toString()));
    }
    return listings;
  }

  // Validation
  private void validateCreateAuctionRequest(String sellerId, CreateAuctionRequest dto) {
    parseSellerId(sellerId);

    if (dto == null) {
      throw new ValidationException("CreateAuctionRequest must not be null");
    }
    if (dto.getStartingPrice() == null || dto.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new ValidationException("startingPrice must be greater than 0");
    }
    if (dto.getDurationDays() != null && dto.getDurationDays() <= 0) {
      throw new ValidationException("durationDays must be greater than 0");
    }
    if (dto.getStartTime() == null) {
      throw new ValidationException("opening date must be provided");
    }
    if (!dto.getStartTime().isAfter(LocalDateTime.now())) {
      throw new ValidationException("opening date must be in the future");
    }
    if (dto.getStartTime() != null
        && dto.getEndTime() != null
        && !dto.getEndTime().isAfter(dto.getStartTime())) {
      throw new ValidationException("endTime must be after startTime");
    }

    if (dto.getSellerId() != null
        && !dto.getSellerId().isBlank()
        && !dto.getSellerId().equals(sellerId)) {
      throw new ValidationException("sellerId in request must match sellerId parameter");
    }
  }

  private boolean isEditableAuctionStatus(AuctionStatus status) {
    return status == AuctionStatus.PENDING || status == AuctionStatus.OPEN;
  }

  private UUID parseSellerId(String sellerId) {
    if (sellerId == null || sellerId.isBlank()) {
      throw new ValidationException("sellerId must not be blank");
    }
    try {
      return UUID.fromString(sellerId);
    } catch (IllegalArgumentException ex) {
      throw new ValidationException("sellerId is not a valid UUID");
    }
  }

  // DTO mapping
  private Item createItem(CreateAuctionRequest dto) {
    if (dto.getCategory() == null) {
      throw new ValidationException("category must not be null");
    }

    ItemCategory category = dto.getCategory();
    return switch (category) {
      case ART -> ItemFactory.createArt(dto.getName(), dto.getDescription(), dto.getCondition());
      case ELECTRONICS ->
          ItemFactory.createElectronics(dto.getName(), dto.getDescription(), dto.getCondition());
      case VEHICLE ->
          ItemFactory.createVehicle(dto.getName(), dto.getDescription(), dto.getCondition());
    };
  }

  private LocalDateTime resolveCreateEndTime(CreateAuctionRequest dto, LocalDateTime startTime) {
    if (dto.getEndTime() != null) {
      return dto.getEndTime();
    }
    int durationDays =
        dto.getDurationDays() != null && dto.getDurationDays() > 0 ? dto.getDurationDays() : 7;
    return startTime.plusDays(durationDays);
  }
}
