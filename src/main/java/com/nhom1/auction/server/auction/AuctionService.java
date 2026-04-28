package com.nhom1.auction.server.auction;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.auction.CreateAuctionRequest;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.factory.ItemFactory;

public class AuctionService {

    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;

    public AuctionService(AuctionRepository auctionRepository, ItemRepository itemRepository) {
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
    }

    public Auction createAuction(String sellerId, CreateAuctionRequest dto) {
        validateCreateAuctionRequest(sellerId, dto);

        UUID parsedSellerId = UUID.fromString(sellerId);
        Item item = createItem(dto);

        itemRepository.save(item, parsedSellerId);

        Auction auction = new Auction(
                item.getId(),
                parsedSellerId,
                dto.getStartTime(),
                dto.getEndTime()
        );
        auctionRepository.save(auction);
        // Keep the opening price in auction state for listing/display and first-bid validation.
        auctionRepository.updateHighestBid(auction.getId(), dto.getStartingPrice(), null);

        return auction;
    }

    public List<AuctionSummaryDto> getMyListings(String sellerId) {
        UUID parsedSellerId = parseSellerId(sellerId);

        return auctionRepository.findBySellerId(parsedSellerId).stream()
                .map(auction -> {
                    Item item = itemRepository.findById(auction.getItemId())
                            .orElseThrow(() -> new IllegalStateException(
                                    "Item not found for auction id: " + auction.getId()));

                    BigDecimal startingPrice = auction.getCurrentHighestBid();
                    return new AuctionSummaryDto(
                            auction.getId().toString(),
                            item.getName(),
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

    private void validateCreateAuctionRequest(String sellerId, CreateAuctionRequest dto) {
        parseSellerId(sellerId);

        if (dto == null) {
            throw new IllegalArgumentException("CreateAuctionRequest must not be null");
        }
        if (dto.getStartingPrice() == null || dto.getStartingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("startingPrice must be greater than 0");
        }
        if (dto.getStartTime() == null || dto.getEndTime() == null) {
            throw new IllegalArgumentException("startTime and endTime must not be null");
        }
        if (!dto.getEndTime().isAfter(dto.getStartTime())) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }

        if (dto.getSellerId() != null && !dto.getSellerId().isBlank() && !dto.getSellerId().equals(sellerId)) {
            throw new IllegalArgumentException("sellerId in request must match sellerId parameter");
        }
    }

    private UUID parseSellerId(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new IllegalArgumentException("sellerId must not be blank");
        }
        try {
            return UUID.fromString(sellerId);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("sellerId is not a valid UUID", ex);
        }
    }

    private Item createItem(CreateAuctionRequest dto) {
        if (dto.getCategory() == null) {
            throw new IllegalArgumentException("category must not be null");
        }

        ItemCategory category = dto.getCategory();
        return switch (category) {
            case ART -> ItemFactory.createArt(
                    dto.getName(),
                    dto.getDescription(),
                    dto.getCondition(),
                    dto.getArtist(),
                    dto.getEra()
            );
            case ELECTRONICS -> {
                Integer warrantyMonths = dto.getWarrantyMonths();
                if (warrantyMonths == null) {
                    throw new IllegalArgumentException("warrantyMonths must not be null for ELECTRONICS");
                }
                yield ItemFactory.createElectronics(
                        dto.getName(),
                        dto.getDescription(),
                        dto.getCondition(),
                        dto.getBrand(),
                        warrantyMonths
                );
            }
            case VEHICLE -> {
                Integer productionYear = dto.getProductionYear();
                if (productionYear == null) {
                    throw new IllegalArgumentException("productionYear must not be null for VEHICLE");
                }
                yield ItemFactory.createVehicle(
                        dto.getName(),
                        dto.getDescription(),
                        dto.getCondition(),
                        dto.getBrand(),
                        productionYear,
                        dto.getFuelType()
                );
            }
        };
    }
}
