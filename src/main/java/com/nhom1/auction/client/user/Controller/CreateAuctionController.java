package com.nhom1.auction.client.user.controller;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.client.user.service.ImageUploadService;
import com.nhom1.auction.common.dto.auction.CreateAuctionRequest;
import com.nhom1.auction.common.dto.auction.CreateAuctionResponse;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.AppContext;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class CreateAuctionController {
    private final List<File> selectedImageFiles = new ArrayList<>();
    private final ImageUploadService imageUploadService = new ImageUploadService();

    @FXML
    private ComboBox<ItemCategory> categoryComboBox;

    @FXML
    private ComboBox<ItemCondition> conditionComboBox;

    @FXML
    private Label uploadCountLabel;
    @FXML
    private Button duration1Btn;
    @FXML
    private Button duration3Btn;
    @FXML
    private Button duration7Btn;
    @FXML
    private Button duration14Btn;
    @FXML
    private Button duration30Btn;
    @FXML
    private TextField customDurationField;
    @FXML
    private TextField titleField;
    @FXML
    private TextArea descriptionArea;
    @FXML
    private TextField startingBidField;
    @FXML
    private TextField reservePriceField;

    @FXML
    private void initialize() {
        categoryComboBox.getItems().setAll(ItemCategory.values());
        conditionComboBox.getItems().setAll(ItemCondition.values());
        customDurationField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                clearActiveDurationButtons();
            }
        });
    }

    @FXML
    private void handleDurationPreset(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedButton)) {
            return;
        }
        clearActiveDurationButtons();
        ObservableList<String> classes = selectedButton.getStyleClass();
        if (!classes.contains("duration-chip-active")) {
            classes.add("duration-chip-active");
        }
        customDurationField.clear();
    }

    private void clearActiveDurationButtons() {
        Arrays.asList(duration1Btn, duration3Btn, duration7Btn, duration14Btn, duration30Btn)
                .forEach(button -> button.getStyleClass().remove("duration-chip-active"));
    }

    @FXML
    private void handleBackToListings() {
        AppNavigator.navigateTo(AppView.MY_LISTINGS);
    }

    @FXML
    private void handleChoosePhotos() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choose listing photos");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );

        Window window = uploadCountLabel.getScene() != null ? uploadCountLabel.getScene().getWindow() : null;
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(window);

        if (selectedFiles == null || selectedFiles.isEmpty()) {
            selectedImageFiles.clear();
            uploadCountLabel.setText("No photo selected");
            return;
        }
        selectedImageFiles.clear();
        selectedImageFiles.addAll(selectedFiles);

        String selectedNames = selectedFiles.stream()
                .limit(2)
                .map(File::getName)
                .collect(Collectors.joining(", "));
        if (selectedFiles.size() > 2) {
            selectedNames = selectedNames + " +" + (selectedFiles.size() - 2) + " more";
        }

        uploadCountLabel.setText(selectedFiles.size() + " photo(s): " + selectedNames);
    }

    @FXML
    private void handlePublishListing() {
        String validationError = validateInput();
        if (validationError != null) {
            uploadCountLabel.setText(validationError);
            return;
        }

        uploadCountLabel.setText("Uploading images...");
        uploadImagesToImgbb()
                .thenCompose(this::sendCreateAuctionRequest)
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response != null && response.isSuccess()) {
                        uploadCountLabel.setText("Published successfully.");
                        AppNavigator.navigateTo(AppView.MY_LISTINGS);
                    } else {
                        String err = (response != null && response.getError() != null)
                                ? response.getError().getMessage()
                                : "Failed to publish listing.";
                        uploadCountLabel.setText(err);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> uploadCountLabel.setText(resolveErrorMessage(ex)));
                    return null;
                });
    }

    private String validateInput() {
        AuthResponse user = AppContext.getCurrentUser();
        if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
            return "Please sign in again.";
        }
        if (titleField.getText() == null || titleField.getText().isBlank()) {
            return "Title is required.";
        }
        if (categoryComboBox.getValue() == null || conditionComboBox.getValue() == null) {
            return "Category and condition are required.";
        }
        try {
            new BigDecimal(startingBidField.getText().trim());
        } catch (Exception ex) {
            return "Starting bid must be a valid number.";
        }
        int durationDays = resolveDurationDays();
        if (durationDays <= 0) {
            return "Duration must be greater than 0.";
        }
        return null;
    }

    private CompletableFuture<List<String>> uploadImagesToImgbb() {
        if (selectedImageFiles.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }

        List<CompletableFuture<String>> uploadTasks = selectedImageFiles.stream()
                .map(imageUploadService::upload)
                .toList();

        return CompletableFuture.allOf(uploadTasks.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> uploadTasks.stream().map(CompletableFuture::join).toList());
    }

    private CompletableFuture<ResponseMessage<CreateAuctionResponse>> sendCreateAuctionRequest(List<String> imageUrls) {
        CreateAuctionRequest dto = buildCreateAuctionRequest();
        dto.setImageUrls(imageUrls);
        RequestMessage<CreateAuctionRequest> request = new RequestMessage<>(MessageType.CREATE_AUCTION, dto);
        return ServerConnection.getInstance().sendRequest(request, CreateAuctionResponse.class);
    }

    private CreateAuctionRequest buildCreateAuctionRequest() {
        AuthResponse user = AppContext.getCurrentUser();
        BigDecimal startingBid = new BigDecimal(startingBidField.getText().trim());
        int durationDays = resolveDurationDays();

        LocalDateTime startTime = LocalDateTime.now();
        LocalDateTime endTime = startTime.plusDays(durationDays);

        CreateAuctionRequest dto = new CreateAuctionRequest();
        dto.setSellerId(user.getUserID());
        dto.setName(titleField.getText().trim());
        dto.setDescription(descriptionArea.getText());
        dto.setCategory(categoryComboBox.getValue());
        dto.setCondition(conditionComboBox.getValue());
        dto.setStartingPrice(startingBid);
        dto.setStartTime(startTime);
        dto.setEndTime(endTime);

        switch (dto.getCategory()) {
            case ART -> {
                dto.setArtist("Unknown");
                dto.setEra("Unknown");
            }
            case ELECTRONICS -> {
                dto.setBrand("Unknown");
                dto.setWarrantyMonths(0);
            }
            case VEHICLE -> {
                dto.setBrand("Unknown");
                dto.setProductionYear(2000);
                dto.setFuelType(null);
            }
        }
        return dto;
    }

    private String resolveErrorMessage(Throwable ex) {
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String message = cause.getMessage();
        if (message == null || message.isBlank()) {
            return "Connection error.";
        }
        if (message.contains("IMGBB_API_KEY")) {
            return "Missing IMGBB_API_KEY. Please configure env var.";
        }
        return message;
    }

    private int resolveDurationDays() {
        if (customDurationField.getText() != null && !customDurationField.getText().isBlank()) {
            try {
                return Integer.parseInt(customDurationField.getText().trim());
            } catch (NumberFormatException ex) {
                return -1;
            }
        }
        if (duration1Btn.getStyleClass().contains("duration-chip-active")) return 1;
        if (duration3Btn.getStyleClass().contains("duration-chip-active")) return 3;
        if (duration7Btn.getStyleClass().contains("duration-chip-active")) return 7;
        if (duration14Btn.getStyleClass().contains("duration-chip-active")) return 14;
        if (duration30Btn.getStyleClass().contains("duration-chip-active")) return 30;
        return 7;
    }
}
