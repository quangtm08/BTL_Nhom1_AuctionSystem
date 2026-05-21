package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.auction.UpdateAuctionRequest;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public class EditAuctionClientService extends BaseClientService {
    public CompletableFuture<String> updateAuction(String auctionId, String title, String description, ItemCategory category, ItemCondition condition, LocalDateTime endTime) {
        AuthResponse user = AppContext.getCurrentUser();
        if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
            return validationError("Please sign in again.");
        }
        if (auctionId == null || auctionId.isBlank()) return validationError("Auction ID is required.");
        if (title == null || title.isBlank()) return validationError("Title is required.");
        if (category == null || condition == null) return validationError("Category and condition are required.");
        if (endTime == null || !endTime.isAfter(LocalDateTime.now())) return validationError("End time must be in the future.");

        UpdateAuctionRequest payload = new UpdateAuctionRequest();
        payload.setAuctionId(auctionId);
        payload.setSellerId(user.getUserID());
        payload.setName(title.trim());
        payload.setDescription(description);
        payload.setCategory(category);
        payload.setCondition(condition);
        payload.setEndTime(endTime);
        return send(new RequestMessage<>(MessageType.UPDATE_AUCTION, payload), String.class);
    }
}
