package com.nhom1.auction.server.bidding;

import java.math.BigDecimal;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.BidSummaryDto;
import com.nhom1.auction.common.dto.bidding.BidWithAuctionDto;
import com.nhom1.auction.common.dto.bidding.ListAuctionsResponse;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.exception.AppException;
import com.nhom1.auction.common.exception.AuctionClosedException;
import com.nhom1.auction.common.exception.InvalidBidException;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.server.auction.AuctionRepository;
import com.nhom1.auction.server.auction.ItemRepository;
import com.nhom1.auction.server.auth.UserRepository;

public class BidService {

    private final BidRepository bidRepository;
    private final AuctionRepository auctionRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final Connection connection;

    public BidService(BidRepository bidRepository, AuctionRepository auctionRepository,
                      ItemRepository itemRepository, UserRepository userRepository,
                      Connection connection) {
        this.bidRepository = bidRepository;
        this.auctionRepository = auctionRepository;
        this.itemRepository = itemRepository;
        this.userRepository = userRepository;
        this.connection = connection;
    }

    public BidTransaction placeBid(UUID bidderId, UUID auctionId, BigDecimal amount, BidType bidType) {
        synchronized (connection) {
            try {
                boolean oldAutoCommit = connection.getAutoCommit();
                connection.setAutoCommit(false);
                try {
                    Auction auction = auctionRepository.findById(auctionId)
                            .orElseThrow(() -> new NotFoundException("Auction not found"));

                    BidTransaction bidTransaction = auction.placeBid(bidderId, amount, bidType, LocalDateTime.now());

                    bidRepository.save(bidTransaction);
                    auctionRepository.updateHighestBid(auctionId, bidTransaction.getAmount(), bidTransaction.getBidderId());

                    connection.commit();
                    return bidTransaction;

                } catch (AppException e) {
                    connection.rollback();
                    throw e;
                } catch (Exception e) {
                    connection.rollback();
                    throw new RuntimeException("Bid placement failed: " + e.getMessage(), e);
                } finally {
                    connection.setAutoCommit(oldAutoCommit);
                }
            } catch (AppException | IllegalStateException e) {
                throw e;
            } catch (Exception e) {
                throw new RuntimeException("Bid placement failed: " + e.getMessage(), e);
            }
        }
    }

    public AuctionDetailDto getAuctionDetail(UUID auctionId) {
    	Auction auction = auctionRepository.findById(auctionId)
    		.orElseThrow(() -> new NotFoundException("Auction not found"));

    	Item item = itemRepository.findById(auction.getItemId())
    		.orElseThrow(() -> new IllegalStateException("Item not found for auction"));

    	String sellerName = userRepository.findById(auction.getSellerId())
    		.map(u -> u.getUsername())
    		.orElse("Unknown");

    	List<BidSummaryDto> bidHistory = bidRepository.findByAuctionId(auctionId).stream()
    		.map(this::toBidSummaryDto)
    		.toList();

    	AuctionDetailDto dto = new AuctionDetailDto(
    		auction.getId().toString(),
    		item.getId().toString(),
    		item.getName(),
    		item.getDescription(),
    		item.getCategory(),
    		item.getCondition(),
    		auction.getSellerId().toString(),
    		auction.getCurrentHighestBid() == null ? BigDecimal.ZERO : auction.getCurrentHighestBid(),
    		auction.getHighestBidderId() == null ? null : auction.getHighestBidderId().toString(),
    		auction.getMinBidIncrement(),
    		auction.getStatus(),
    		auction.getStartTime(),
    		auction.getEndTime(),
    		bidHistory
    	);
    	dto.setSellerName(sellerName);
    	return dto;
    }

    public ListAuctionsResponse listAllAuctions() {
    	List<AuctionSummaryDto> auctions = auctionRepository.findAll().stream()
    		.map(this::toAuctionSummaryDto)
    		.toList(); // Convert Auction to AuctionSummaryDto for client consumption

    	ListAuctionsResponse response = new ListAuctionsResponse();
    	response.setAuctions(auctions);
    	return response;
    }

    public MyBidsResponse getMyBids(UUID bidderId) {
    	List<BidWithAuctionDto> bids = bidRepository.findByBidderId(bidderId);
    	return new MyBidsResponse(bids);
    }

    private BidSummaryDto toBidSummaryDto(BidTransaction bidTransaction) {
    	String bidderName = userRepository.findById(bidTransaction.getBidderId())
    		.map(u -> u.getUsername())
    		.orElse("Unknown");
    	return new BidSummaryDto(
    		bidTransaction.getId().toString(),
    		bidTransaction.getBidderId().toString(),
    		bidTransaction.getAmount(),
    		bidTransaction.getBidType(),
    		bidTransaction.getCreatedAt(),
    		bidderName
    	);
    }

    private AuctionSummaryDto toAuctionSummaryDto(Auction auction) {
    	Item item = itemRepository.findById(auction.getItemId()).orElse(null);
    	String itemName = item == null ? "Unknown item" : item.getName();
    	String itemCategory = item == null ? "UNKNOWN" : item.getCategory().name();

    	return new AuctionSummaryDto(
    		auction.getId().toString(),
    		itemName,
    		itemCategory,
    		auction.getStartingPrice(),
    		auction.getCurrentHighestBid() == null ? BigDecimal.ZERO : auction.getCurrentHighestBid(),
    		auction.getStartTime(),
    		auction.getEndTime(),
    		auction.getStatus(),
    		auction.getSellerId().toString()
    	);
    }
}
