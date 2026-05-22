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
        DataSource dataSource
    ) {
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
        this.itemImageRepository = itemImageRepository;
        this.dataSource = dataSource;
    }

    public Auction createAuction(String sellerId, CreateAuctionRequest dto) {
        validateCreateAuctionRequest(sellerId, dto);

        UUID parsedSellerId = UUID.fromString(sellerId);
        Item item = createItem(dto);

        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                itemRepository.save(item, parsedSellerId, connection);
                itemImageRepository.saveImageUrls(
                    item.getId(),
                    dto.getImageUrls(),
                    connection
                );

                Auction auction = new Auction(
                    item.getId(),
                    parsedSellerId,
                    dto.getStartingPrice(),
                    dto.getStartTime(),
                    dto.getEndTime()
                );

                auctionRepository.save(auction, connection);
                // Keep the opening price in auction state for listing/display and first-bid validation.
                auctionRepository.updateHighestBid(
                    auction.getId(),
                    dto.getStartingPrice(),
                    null,
                    auction.getVersion(),
                    connection
                );

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

    public List<AuctionSummaryDto> getMyListings(String sellerId) {
        UUID parsedSellerId = parseSellerId(sellerId);

        return auctionRepository
            .findBySellerId(parsedSellerId)
            .stream()
            .map(auction -> {
                Item item = itemRepository
                    .findById(auction.getItemId())
                    .orElseThrow(() ->
                        new IllegalStateException(
                            "Item not found for auction id: " + auction.getId()
                        )
                    );

                BigDecimal startingPrice = auction.getStartingPrice();
                return new AuctionSummaryDto(
                    auction.getId().toString(),
                    item.getName(),
                    item.getCategory().name(),
                    startingPrice,
                    auction.getCurrentHighestBid(),
                    auction.getStartTime(),
                    auction.getEndTime(),
                    auction.getStatus(),
                    auction.getSellerId().toString()
                );
            })
            .toList();
    }

    public void updateAuction(UpdateAuctionRequest dto) {
        validateUpdateAuctionRequest(dto);
        UUID parsedSellerId = parseSellerId(dto.getSellerId());
        UUID parsedAuctionId = parseAuctionId(dto.getAuctionId());

        Auction auction = auctionRepository
            .findById(parsedAuctionId)
            .orElseThrow(() -> new NotFoundException("Auction not found"));

        if (!parsedSellerId.equals(auction.getSellerId())) {
            throw new UnauthorizedActionException(
                "You are not allowed to edit this auction"
            );
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new InvalidAuctionStateException(
                "Only open auctions can be edited"
            );
        }

        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int updatedItems = itemRepository.updateDetails(
                    auction.getItemId(),
                    dto.getName().trim(),
                    dto.getDescription(),
                    dto.getCategory(),
                    dto.getCondition(),
                    connection
                );
                int updatedAuctions = auctionRepository.updateOpenAuctionDetails(
                    parsedAuctionId,
                    dto.getStartingPrice(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    connection
                );
                if (updatedItems == 0) {
                    throw new NotFoundException("Item not found for auction");
                }
                if (updatedAuctions == 0) {
                    throw new InvalidAuctionStateException(
                        "Only open auctions can be edited"
                    );
                }
                connection.commit();
            } catch (AppException ex) {
                connection.rollback();
                throw ex;
            } catch (Exception ex) {
                connection.rollback();
                throw new RuntimeException("Update auction transaction failed", ex);
            } finally {
                connection.setAutoCommit(oldAutoCommit);
            }
        } catch (AppException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException("Update auction transaction failed", ex);
        }
    }

    public void deleteAuction(String sellerId, String auctionId) {
        UUID parsedSellerId = parseSellerId(sellerId);
        UUID parsedAuctionId = parseAuctionId(auctionId);

        Auction auction = auctionRepository
            .findById(parsedAuctionId)
            .orElseThrow(() -> new NotFoundException("Auction not found"));

        if (!parsedSellerId.equals(auction.getSellerId())) {
            throw new UnauthorizedActionException(
                "You are not allowed to delete this auction"
            );
        }
        if (auction.getStatus() != AuctionStatus.OPEN) {
            throw new InvalidAuctionStateException(
                "Only open auctions can be deleted"
            );
        }

        try (Connection connection = dataSource.getConnection()) {
            boolean oldAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                int deletedAuctions = auctionRepository.deleteById(
                    parsedAuctionId,
                    connection
                );
                int deletedItems = itemRepository.deleteById(
                    auction.getItemId(),
                    connection
                );
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

    private void validateUpdateAuctionRequest(UpdateAuctionRequest dto) {
        if (dto == null) {
            throw new ValidationException(
                "UpdateAuctionRequest must not be null"
            );
        }
        parseSellerId(dto.getSellerId());
        parseAuctionId(dto.getAuctionId());
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new ValidationException("name must not be blank");
        }
        if (dto.getCategory() == null) {
            throw new ValidationException("category must not be null");
        }
        if (dto.getCondition() == null) {
            throw new ValidationException("condition must not be null");
        }
        if (
            dto.getStartingPrice() == null ||
            dto.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new ValidationException(
                "startingPrice must be greater than 0"
            );
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new ValidationException(
                "startTime and endTime must not be null"
            );
        }
        if (dto.getStartTime().isBefore(java.time.LocalDateTime.now().minusMinutes(1))) {
            throw new ValidationException("startTime cannot be in the past");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new ValidationException("endTime must be after startTime");
        }
    }

    private void validateCreateAuctionRequest(
        String sellerId,
        CreateAuctionRequest dto
    ) {
        parseSellerId(sellerId);

        if (dto == null) {
            throw new ValidationException(
                "CreateAuctionRequest must not be null"
            );
        }
        if (
            dto.getStartingPrice() == null ||
            dto.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0
        ) {
            throw new ValidationException(
                "startingPrice must be greater than 0"
            );
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new ValidationException(
                "startTime and endTime must not be null"
            );
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new ValidationException("endTime must be after startTime");
        }

        if (
            dto.getSellerId() != null &&
            !dto.getSellerId().isBlank() &&
            !dto.getSellerId().equals(sellerId)
        ) {
            throw new ValidationException(
                "sellerId in request must match sellerId parameter"
            );
        }
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

    private UUID parseAuctionId(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            throw new ValidationException("auctionId must not be blank");
        }
        try {
            return UUID.fromString(auctionId);
        } catch (IllegalArgumentException ex) {
            throw new ValidationException("auctionId is not a valid UUID");
        }
    }

    private Item createItem(CreateAuctionRequest dto) {
        if (dto.getCategory() == null) {
            throw new ValidationException("category must not be null");
        }

        ItemCategory category = dto.getCategory();
        return switch (category) {
            case ART -> ItemFactory.createArt(
                dto.getName(),
                dto.getDescription(),
                dto.getCondition()
            );
            case ELECTRONICS -> ItemFactory.createElectronics(
                dto.getName(),
                dto.getDescription(),
                dto.getCondition()
            );
            case VEHICLE -> ItemFactory.createVehicle(
                dto.getName(),
                dto.getDescription(),
                dto.getCondition()
            );
        };
    }
}
