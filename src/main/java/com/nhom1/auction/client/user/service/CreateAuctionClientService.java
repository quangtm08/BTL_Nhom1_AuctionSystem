package com.nhom1.auction.client.user.service;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.nhom1.auction.common.dto.auction.CreateAuctionRequest;
import com.nhom1.auction.common.dto.auction.CreateAuctionResponse;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;

public class CreateAuctionClientService extends BaseClientService {
    private final ImageUploadService imageUploadService;

    public CreateAuctionClientService() {
        this.imageUploadService = new ImageUploadService();
    }

    public String validateInput(
            String title,
            String startingBid,
            ItemCategory category,
            ItemCondition condition,
            int durationDays
    ) {
        AuthResponse user = AppContext.getCurrentUser();
        if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
            return "Please sign in again.";
        }
        if (title == null || title.isBlank()) {
            return "Title is required.";
        }
        if (category == null || condition == null) {
            return "Category and condition are required.";
        }
        try {
            new BigDecimal(startingBid.trim());
        } catch (Exception ex) {
            return "Starting bid must be a valid number.";
        }
        if (durationDays <= 0) {
            return "Duration must be greater than 0.";
        }
        return null;
    }

    public CompletableFuture<CreateAuctionResponse> createAuction(
            String title,
            String description,
            String startingBid,
            ItemCategory category,
            ItemCondition condition,
            int durationDays,
            List<File> imageFiles
    ) {
        return uploadImages(imageFiles)
                .thenCompose(imageUrls -> {
                    CreateAuctionRequest payload = buildCreateAuctionRequest(
                            title, description, startingBid, category, condition, durationDays, imageUrls
                    );
                    RequestMessage<CreateAuctionRequest> request = new RequestMessage<>(MessageType.CREATE_AUCTION, payload);
                    return send(request, CreateAuctionResponse.class);
                });
    }

    private CompletableFuture<List<String>> uploadImages(List<File> imageFiles) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<CompletableFuture<String>> uploadTasks = imageFiles.stream()
                .map(imageUploadService::upload)
                .toList();
        return CompletableFuture.allOf(uploadTasks.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> uploadTasks.stream().map(CompletableFuture::join).toList());
    }

    private CreateAuctionRequest buildCreateAuctionRequest(
            String title,
            String description,
            String startingBid,
            ItemCategory category,
            ItemCondition condition,
            int durationDays,
            List<String> imageUrls
    ) {
        AuthResponse user = AppContext.getCurrentUser();
        BigDecimal parsedStartingBid = new BigDecimal(startingBid.trim());

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;
        CreateAuctionRequest dto = new CreateAuctionRequest();
        dto.setSellerId(user.getUserID());
        dto.setName(title.trim());
        dto.setDescription(description);
        dto.setCategory(category);
        dto.setCondition(condition);
        dto.setStartingPrice(parsedStartingBid);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);
        dto.setDurationDays(durationDays);
        dto.setImageUrls(imageUrls);

        return dto;
    }
}
