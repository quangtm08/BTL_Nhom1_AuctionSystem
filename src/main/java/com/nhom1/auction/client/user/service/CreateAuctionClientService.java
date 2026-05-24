package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.auction.CreateAuctionRequest;
import com.nhom1.auction.common.dto.auction.CreateAuctionResponse;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class CreateAuctionClientService extends BaseClientService {
  private final ImageUploadService imageUploadService;

  public CreateAuctionClientService() {
    this.imageUploadService = new ImageUploadService();
  }

  // Business operations
  public CompletableFuture<CreateAuctionResponse> createAuction(
      String title,
      String description,
      String startingBid,
      ItemCategory category,
      ItemCondition condition,
      int durationDays,
      LocalDate openingDate,
      List<File> imageFiles) {
    return uploadImages(imageFiles)
        .thenCompose(
            imageUrls -> {
              CreateAuctionRequest payload =
                  buildCreateAuctionRequest(
                      title,
                      description,
                      startingBid,
                      category,
                      condition,
                      durationDays,
                      openingDate,
                      imageUrls);
              RequestMessage<CreateAuctionRequest> request =
                  new RequestMessage<>(MessageType.CREATE_AUCTION, payload);
              return send(request, CreateAuctionResponse.class);
            });
  }

  // Validation
  public String validateInput(
      String title,
      String startingBid,
      ItemCategory category,
      ItemCondition condition,
      int durationDays,
      LocalDate openingDate) {
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
    if (openingDate == null) {
      return "Opening date is required.";
    }
    if (!openingDate.isAfter(LocalDate.now())) {
      return "Opening date must be after today.";
    }
    return null;
  }

  // Helpers
  private CompletableFuture<List<String>> uploadImages(List<File> imageFiles) {
    if (imageFiles == null || imageFiles.isEmpty()) {
      return CompletableFuture.completedFuture(List.of());
    }
    List<CompletableFuture<String>> uploadTasks =
        imageFiles.stream().map(imageUploadService::upload).toList();
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
      LocalDate openingDate,
      List<String> imageUrls) {
    AuthResponse user = AppContext.getCurrentUser();
    BigDecimal parsedStartingBid = new BigDecimal(startingBid.trim());

    CreateAuctionRequest payload = new CreateAuctionRequest();
    payload.setSellerId(user.getUserID());
    payload.setName(title.trim());
    payload.setDescription(description);
    payload.setCategory(category);
    payload.setCondition(condition);
    payload.setStartingPrice(parsedStartingBid);
    payload.setStartTime(openingDate.atStartOfDay());
    payload.setDurationDays(durationDays);
    if (imageUrls != null && !imageUrls.isEmpty()) {
      payload.setImageUrls(imageUrls);
    }
    return payload;
  }
}
