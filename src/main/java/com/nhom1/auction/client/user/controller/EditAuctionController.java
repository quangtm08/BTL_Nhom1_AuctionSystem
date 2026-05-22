package com.nhom1.auction.client.user.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.client.user.service.MyListingsClientService;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.utils.AppContext;

import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class EditAuctionController {
    private final BiddingClientService biddingService = new BiddingClientService();
    private final MyListingsClientService listingsService = new MyListingsClientService();

    private String auctionId;

    @FXML private ComboBox<ItemCategory> categoryComboBox;
    @FXML private ComboBox<ItemCondition> conditionComboBox;
    @FXML private Button duration1Btn;
    @FXML private Button duration3Btn;
    @FXML private Button duration7Btn;
    @FXML private Button duration14Btn;
    @FXML private Button duration30Btn;
    @FXML private TextField customDurationField;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private Label statusLabel;
    @FXML private Label metaLabel;
    @FXML private TextField startingBidField;
    @FXML private Button saveChangesButton;
    @FXML private Button deleteButton;
    private LocalDateTime loadedStartTime;
    private LocalDateTime loadedEndTime;

    @FXML private void initialize() {
        categoryComboBox.getItems().setAll(ItemCategory.values());
        conditionComboBox.getItems().setAll(ItemCondition.values());
        customDurationField.textProperty().addListener((obs, oldValue, newValue) -> { if (newValue != null && !newValue.isBlank()) clearActiveDurationButtons(); });

        auctionId = AppContext.getSelectedAuctionId();
        if (auctionId == null || auctionId.isBlank()) {
            showStatus("No listing selected.");
            setEditable(false);
            return;
        }

        setEditable(false);
        loadAuctionForEdit();
    }
    @FXML private void handleDurationPreset(ActionEvent event) {
        if (!(event.getSource() instanceof Button selectedButton)) return;
        clearActiveDurationButtons();
        ObservableList<String> classes = selectedButton.getStyleClass();
        if (!classes.contains("duration-chip-active")) classes.add("duration-chip-active");
        customDurationField.clear();
    }
    private void clearActiveDurationButtons() { Arrays.asList(duration1Btn, duration3Btn, duration7Btn, duration14Btn, duration30Btn).forEach(button -> button.getStyleClass().remove("duration-chip-active")); }
    @FXML private void handleBackToListings() { AppNavigator.navigateTo(AppView.MY_LISTINGS); }
    @FXML private void handleSaveChanges() {
        int days = resolveDurationDays();
        if (days <= 0 && loadedEndTime == null) {
            showStatus("Duration must be greater than 0.");
            return;
        }
        LocalDateTime baseTime = loadedStartTime != null && loadedStartTime.isAfter(LocalDateTime.now())
                ? loadedStartTime
                : LocalDateTime.now();
        LocalDateTime targetEndTime = days > 0 ? baseTime.plusDays(days) : loadedEndTime;
        showStatus("Saving changes...");
        setButtonsDisabled(true);
        listingsService.updateListing(
                auctionId,
                titleField.getText(),
                descriptionArea.getText(),
                startingBidField.getText(),
                categoryComboBox.getValue(),
                conditionComboBox.getValue(),
                targetEndTime
        ).thenAccept(response -> Platform.runLater(() -> AppNavigator.navigateTo(AppView.MY_LISTINGS)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showStatus(resolveErrorMessage(ex, "Unable to save listing."));
                        setButtonsDisabled(false);
                    });
                    return null;
                });
    }

    @FXML private void handleDeleteListing() {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Delete this auction?");
        confirm.setContentText("This action cannot be undone.");
        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("No", ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);
        confirm.showAndWait().ifPresent(selected -> {
            if (selected != yes) return;
            showStatus("Deleting listing...");
            setButtonsDisabled(true);
            listingsService.deleteListing(auctionId)
                    .thenAccept(response -> Platform.runLater(() -> AppNavigator.navigateTo(AppView.MY_LISTINGS)))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            showStatus(resolveErrorMessage(ex, "Unable to delete listing."));
                            setButtonsDisabled(false);
                        });
                        return null;
                    });
        });
    }

    private void loadAuctionForEdit() {
        biddingService.getAuctionDetail(auctionId)
            .thenAccept(dto -> Platform.runLater(() -> applyDetail(dto)))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    showStatus(resolveErrorMessage(ex, "Unable to load listing."));
                    setEditable(false);
                });
                return null;
            });
    }
    private int resolveDurationDays() {
        if (customDurationField.getText() != null && !customDurationField.getText().isBlank()) { try { return Integer.parseInt(customDurationField.getText().trim()); } catch (NumberFormatException ex) { return -1; } }
        if (duration1Btn.getStyleClass().contains("duration-chip-active")) return 1;
        if (duration3Btn.getStyleClass().contains("duration-chip-active")) return 3;
        if (duration7Btn.getStyleClass().contains("duration-chip-active")) return 7;
        if (duration14Btn.getStyleClass().contains("duration-chip-active")) return 14;
        if (duration30Btn.getStyleClass().contains("duration-chip-active")) return 30;
        return 7;
    }

    private void applyDurationFromEndTime(LocalDateTime startTime, LocalDateTime endTime) {
        clearActiveDurationButtons();
        if (endTime == null) {
            duration7Btn.getStyleClass().add("duration-chip-active");
            return;
        }
        LocalDateTime baseTime = startTime != null && startTime.isAfter(LocalDateTime.now())
                ? startTime
                : LocalDateTime.now();
        long daysLeft = Math.max(1, ChronoUnit.DAYS.between(baseTime, endTime));
        if (daysLeft == 1) duration1Btn.getStyleClass().add("duration-chip-active");
        else if (daysLeft == 3) duration3Btn.getStyleClass().add("duration-chip-active");
        else if (daysLeft == 7) duration7Btn.getStyleClass().add("duration-chip-active");
        else if (daysLeft == 14) duration14Btn.getStyleClass().add("duration-chip-active");
        else if (daysLeft == 30) duration30Btn.getStyleClass().add("duration-chip-active");
        else customDurationField.setText(String.valueOf(daysLeft));
    }

    private void applyDetail(AuctionDetailDto dto) {
        if (dto == null) {
            showStatus("Unable to load listing.");
            setEditable(false);
            return;
        }

        titleField.setText(dto.getItemName() != null ? dto.getItemName() : "");
        descriptionArea.setText(dto.getItemDescription() != null ? dto.getItemDescription() : "");
        startingBidField.setText(formatPlainNumber(dto.getStartingPrice()));
        categoryComboBox.setValue(dto.getItemCategory());
        conditionComboBox.setValue(dto.getItemCondition());
        loadedStartTime = dto.getStartTime();
        loadedEndTime = dto.getEndTime();
        applyDurationFromEndTime(loadedStartTime, loadedEndTime);
        bindMeta(dto.getStartTime(), dto.getEndTime());

        boolean editable = isEditableStatus(dto.getStatus());
        showStatus(editable ? "Editing " + statusName(dto.getStatus()) + " listing" : "Only pending or open listings can be edited.");
        setEditable(editable);
    }

    private void setEditable(boolean editable) {
        titleField.setDisable(!editable);
        descriptionArea.setDisable(!editable);
        categoryComboBox.setDisable(!editable);
        conditionComboBox.setDisable(!editable);
        startingBidField.setDisable(!editable);
        customDurationField.setDisable(!editable);
        Arrays.asList(duration1Btn, duration3Btn, duration7Btn, duration14Btn, duration30Btn)
                .forEach(button -> button.setDisable(!editable));
        setButtonsDisabled(!editable);
    }

    private void setButtonsDisabled(boolean disabled) {
        if (saveChangesButton != null) saveChangesButton.setDisable(disabled);
        if (deleteButton != null) deleteButton.setDisable(disabled);
    }

    private void showStatus(String message) {
        statusLabel.setText(message);
    }

    private String resolveErrorMessage(Throwable throwable, String fallback) {
        Throwable cause = BaseClientService.extractFailure(throwable);
        String message = cause.getMessage();
        return message == null || message.isBlank() ? fallback : message;
    }

    private String formatPlainNumber(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    private void bindMeta(LocalDateTime startTime, LocalDateTime endTime) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        String opens = startTime != null ? startTime.format(fmt) : "N/A";
        String ends = endTime != null ? endTime.format(fmt) : "N/A";
        metaLabel.setText("Opens: " + opens + " | Ends: " + ends);
    }

    private boolean isEditableStatus(AuctionStatus status) {
        return status == AuctionStatus.PENDING || status == AuctionStatus.OPEN;
    }

    private String statusName(AuctionStatus status) {
        return status == null ? "selected" : status.name().toLowerCase();
    }
}
