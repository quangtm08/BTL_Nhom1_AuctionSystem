package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.auction.MyListingsResponse;
import com.nhom1.auction.common.dto.auction.UpdateAuctionRequest;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MyListingsClientService extends BaseClientService {

    public CompletableFuture<MyListingsResponse> listMyListings() {
        AuthResponse user = AppContext.getCurrentUser();
        if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
            return validationError("No user session. Please sign in again.");
        }

        RequestMessage<Map<String, String>> request = new RequestMessage<>(
                MessageType.LIST_MY_LISTINGS,
                Map.of("sellerId", user.getUserID())
        );
        return send(request, MyListingsResponse.class);
    }

    public CompletableFuture<String> deleteListing(String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return validationError("Auction ID is required.");
        }

        AuthResponse user = AppContext.getCurrentUser();
        if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
            return validationError("No user session. Please sign in again.");
        }

        RequestMessage<Map<String, String>> request = new RequestMessage<>(
                MessageType.DELETE_AUCTION,
                Map.of("sellerId", user.getUserID(), "auctionId", auctionId)
        );
        return send(request, String.class);
    }

    public CompletableFuture<String> updateListing(
        String auctionId,
        String title,
        String description,
        String startingBid,
        ItemCategory category,
        ItemCondition condition,
        LocalDateTime endTime
    ) {
        String error = validateUpdateInput(
            auctionId,
            title,
            startingBid,
            category,
            condition,
            endTime
        );
        if (error != null) {
            return validationError(error);
        }

        AuthResponse user = AppContext.getCurrentUser();
        BigDecimal parsedStartingBid = new BigDecimal(startingBid.trim());

        UpdateAuctionRequest payload = new UpdateAuctionRequest();
        payload.setAuctionId(auctionId);
        payload.setSellerId(user.getUserID());
        payload.setName(title.trim());
        payload.setDescription(description);
        payload.setCategory(category);
        payload.setCondition(condition);
        payload.setStartingPrice(parsedStartingBid);
        payload.setEndTime(endTime);

        RequestMessage<UpdateAuctionRequest> request = new RequestMessage<>(
                MessageType.UPDATE_AUCTION,
                payload
        );
        return send(request, String.class);
    }

    private String validateUpdateInput(
        String auctionId,
        String title,
        String startingBid,
        ItemCategory category,
        ItemCondition condition,
        LocalDateTime endTime
    ) {
        AuthResponse user = AppContext.getCurrentUser();
        if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
            return "No user session. Please sign in again.";
        }
        if (auctionId == null || auctionId.isBlank()) {
            return "Auction ID is required.";
        }
        if (title == null || title.isBlank()) {
            return "Title is required.";
        }
        if (category == null || condition == null) {
            return "Category and condition are required.";
        }
        try {
            BigDecimal parsed = new BigDecimal(startingBid.trim());
            if (parsed.compareTo(BigDecimal.ZERO) <= 0) {
                return "Starting bid must be greater than 0.";
            }
        } catch (Exception ex) {
            return "Starting bid must be a valid number.";
        }
        if (endTime == null || !endTime.isAfter(LocalDateTime.now())) {
            return "End time must be in the future.";
        }
        return null;
    }
}
