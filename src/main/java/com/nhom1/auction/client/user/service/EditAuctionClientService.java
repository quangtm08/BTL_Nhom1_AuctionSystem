package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.auction.UpdateAuctionRequest;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

public class EditAuctionClientService extends BaseClientService {
    public CompletableFuture<String> updateAuction(String auctionId, String title, String description, String startingBid, ItemCategory category, ItemCondition condition, LocalDateTime endTime) {
        AuthResponse user = AppContext.getCurrentUser();
        if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
            return validationError("Please sign in again.");
        }
        if (auctionId == null || auctionId.isBlank()) return validationError("Auction ID is required.");
        if (title == null || title.isBlank()) return validationError("Title is required.");
        BigDecimal parsedStartingBid;
        try { parsedStartingBid = new BigDecimal(startingBid.trim()); } catch (Exception ex) { return validationError("Starting bid must be a valid number."); }
        if (category == null || condition == null) return validationError("Category and condition are required.");
        if (endTime == null || !endTime.isAfter(LocalDateTime.now())) return validationError("End time must be in the future.");

        UpdateAuctionRequest payload = new UpdateAuctionRequest();
        payload.setAuctionId(auctionId);
        payload.setSellerId(user.getUserID());
        payload.setName(title.trim());
        payload.setDescription(description);
        payload.setCategory(category);
        payload.setCondition(condition);
        payload.setStartingPrice(parsedStartingBid);
        payload.setEndTime(endTime);
        return send(new RequestMessage<>(MessageType.UPDATE_AUCTION, payload), String.class);
    }
}
