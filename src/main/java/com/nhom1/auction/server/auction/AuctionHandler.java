package com.nhom1.auction.server.auction;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.auction.CreateAuctionRequest;
import com.nhom1.auction.common.dto.auction.CreateAuctionResponse;
import com.nhom1.auction.common.dto.auction.MyListingsResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;

public class AuctionHandler {
    private final AuctionService auctionService;
    private final NotificationService notificationService;

    public AuctionHandler(AuctionService auctionService, NotificationService notificationService) {
        this.auctionService = auctionService;
        this.notificationService = notificationService;
    }

    public void register(MessageRouter router) {
        router.register(MessageType.CREATE_AUCTION, (requestId, payloadJson) -> {
            try {
                CreateAuctionRequest dto = JsonUtil.fromJson(payloadJson, CreateAuctionRequest.class);
                return handleCreateAuction(requestId, dto);
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid CreateAuction JSON");
            }
        });

        router.register(MessageType.LIST_MY_LISTINGS, (requestId, payloadJson) -> {
            try {
                JsonNode payload = JsonUtil.fromJson(payloadJson, JsonNode.class);
                String sellerId = payload.has("sellerId") ? payload.get("sellerId").asText() : null;
                return handleListMyListings(requestId, sellerId);
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid ListMyListings JSON");
            }
        });

        router.register(MessageType.DELETE_AUCTION, (requestId, payloadJson) -> {
            try {
                JsonNode payload = JsonUtil.fromJson(payloadJson, JsonNode.class);
                String sellerId = payload.has("sellerId") ? payload.get("sellerId").asText() : null;
                String auctionId = payload.has("auctionId") ? payload.get("auctionId").asText() : null;
                return handleDeleteAuction(requestId, sellerId, auctionId);
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid DeleteAuction JSON");
            }
        });
    }

    private ResponseMessage<CreateAuctionResponse> handleCreateAuction(String requestId, CreateAuctionRequest dto) {
        try {
            Auction auction = auctionService.createAuction(dto.getSellerId(), dto);
            notificationService.broadcastNewAuction(
                    auction.getId().toString(),
                    dto.getName(),
                    auction.getStartingPrice()
            );

            CreateAuctionResponse response = new CreateAuctionResponse(
                    auction.getId().toString(),
                    auction.getItemId().toString(),
                    auction.getSellerId().toString(),
                    auction.getStartTime(),
                    auction.getEndTime(),
                    auction.getHighestBidderId() != null ? auction.getHighestBidderId().toString() : null,
                    auction.getCurrentHighestBid(),
                    auction.getStatus(),
                    auction.getCreatedAt(),
                    auction.getUpdatedAt()
            );

            return new ResponseMessage<>(requestId, response);
        } catch (Exception e) {
            String detail = e.getMessage();
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                detail = detail + " | cause: " + e.getCause().getMessage();
            }
            return new ResponseMessage<>(requestId, "CREATE_AUCTION_FAILED", detail);
        }
    }

    private ResponseMessage<MyListingsResponse> handleListMyListings(String requestId, String sellerId) {
        try {
            List<AuctionSummaryDto> listings = auctionService.getMyListings(sellerId);
            return new ResponseMessage<>(requestId, new MyListingsResponse(listings));
        } catch (Exception e) {
            return new ResponseMessage<>(requestId, "LIST_MY_LISTINGS_FAILED", e.getMessage());
        }
    }

    private ResponseMessage<String> handleDeleteAuction(String requestId, String sellerId, String auctionId) {
        try {
            auctionService.deleteAuction(sellerId, auctionId);
            notificationService.broadcastAuctionDeleted(auctionId);
            return new ResponseMessage<>(requestId, "Deleted");
        } catch (Exception e) {
            return new ResponseMessage<>(requestId, "DELETE_AUCTION_FAILED", e.getMessage());
        }
    }
}
