package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.auction.CreateAuctionResponse;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
        int durationDays,
        LocalDateTime startTime
    ) {
        AuthResponse user = AppContext.getCurrentUser();
        if (
            user == null ||
            user.getUserID() == null ||
            user.getUserID().isBlank()
        ) {
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
        if (
            startTime == null ||
            startTime.isBefore(LocalDateTime.now().minusMinutes(1))
        ) {
            return "Start time cannot be in the past.";
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
        LocalDateTime startTime,
        List<File> imageFiles
    ) {
        return uploadImages(imageFiles).thenCompose(imageUrls -> {
            Map<String, Object> payload = buildCreateAuctionPayload(
                title,
                description,
                startingBid,
                category,
                condition,
                durationDays,
                startTime,
                imageUrls
            );
            RequestMessage<Map<String, Object>> request = new RequestMessage<>(
                MessageType.CREATE_AUCTION,
                payload
            );
            return send(request, CreateAuctionResponse.class);
        });
    }

    private CompletableFuture<List<String>> uploadImages(
        List<File> imageFiles
    ) {
        if (imageFiles == null || imageFiles.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<CompletableFuture<String>> uploadTasks = imageFiles
            .stream()
            .map(imageUploadService::upload)
            .toList();
        return CompletableFuture.allOf(
            uploadTasks.toArray(new CompletableFuture[0])
        ).thenApply(ignored ->
            uploadTasks.stream().map(CompletableFuture::join).toList()
        );
    }

    private Map<String, Object> buildCreateAuctionPayload(
        String title,
        String description,
        String startingBid,
        ItemCategory category,
        ItemCondition condition,
        int durationDays,
        LocalDateTime startTime,
        List<String> imageUrls
    ) {
        AuthResponse user = AppContext.getCurrentUser();
        BigDecimal parsedStartingBid = new BigDecimal(startingBid.trim());

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sellerId", user.getUserID());
        payload.put("name", title.trim());
        payload.put("description", description);
        payload.put("category", category);
        payload.put("condition", condition);
        payload.put("startingPrice", parsedStartingBid);
        payload.put("startTime", startTime);
        payload.put("durationDays", durationDays);
        // Backward compatibility with older Railway server schema:
        // only send imageUrls when it actually has data.
        if (imageUrls != null && !imageUrls.isEmpty()) {
            payload.put("imageUrls", imageUrls);
        }
        return payload;
    }
}
