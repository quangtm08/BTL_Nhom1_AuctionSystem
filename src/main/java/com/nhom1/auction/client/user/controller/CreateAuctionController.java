package com.nhom1.auction.client.user.controller;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.CreateAuctionClientService;
import com.nhom1.auction.common.dto.auction.CreateAuctionResponse;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.exception.ValidationException;

import com.nhom1.auction.common.exception.AppException;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Window;

public class CreateAuctionController {
    private final List<File> selectedImageFiles = new ArrayList<>();
    private final CreateAuctionClientService createAuctionService = new CreateAuctionClientService();

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
    private DatePicker openingDatePicker;

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
        String validationError = createAuctionService.validateInput(
                titleField.getText(),
                startingBidField.getText(),
                categoryComboBox.getValue(),
                conditionComboBox.getValue(),
                resolveDurationDays(),
                openingDatePicker.getValue()
        );
        if (validationError != null) {
            uploadCountLabel.setText(validationError);
            return;
        }

        int durationDays = resolveDurationDays();
        if (durationDays <= 0) {
            uploadCountLabel.setText("Duration must be greater than 0.");
            return;
        }

        uploadCountLabel.setText("Uploading images and publishing...");
        createAuctionService
                .createAuction(
                        titleField.getText(),
                        descriptionArea.getText(),
                        startingBidField.getText(),
                        categoryComboBox.getValue(),
                        conditionComboBox.getValue(),
                        durationDays,
                        openingDatePicker.getValue(),
                        selectedImageFiles
                )
                .thenAccept(response -> Platform.runLater(() -> {
                    if (response != null) {
                        uploadCountLabel.setText("Published successfully.");
                        AppNavigator.navigateTo(AppView.MY_LISTINGS);
                    } else {
                        uploadCountLabel.setText("Failed to publish listing.");
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> uploadCountLabel.setText(resolveErrorMessage(ex)));
                    return null;
                });
    }

    private String resolveErrorMessage(Throwable ex) {
        Throwable cause = BaseClientService.extractFailure(ex);
        if (cause instanceof AppException || cause instanceof ValidationException) {
            String message = cause.getMessage();
            return (message == null || message.isBlank()) ? "Connection error." : message;
        }
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
