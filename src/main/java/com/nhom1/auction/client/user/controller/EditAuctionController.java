package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.client.user.service.MyListingsClientService;
import com.nhom1.auction.client.util.DisplayFormatters;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.utils.AppContext;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
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
import javafx.scene.control.DatePicker;
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
    @FXML private TextField startingBidField;
    @FXML private DatePicker startDatePicker;
    @FXML private TextField startTimeField;
    @FXML private Label statusLabel;
    @FXML private Button deleteButton;
    @FXML private Button saveButton;

    @FXML
    private void initialize() {
        categoryComboBox.getItems().setAll(ItemCategory.values());
        conditionComboBox.getItems().setAll(ItemCondition.values());

        customDurationField.textProperty().addListener((obs, oldValue, newValue) -> {
            if (newValue != null && !newValue.isBlank()) {
                clearActiveDurationButtons();
            }
        });

        auctionId = AppContext.getSelectedAuctionId();
        if (auctionId == null || auctionId.isBlank()) {
            showStatus("No listing selected.");
            setEditable(false);
            return;
        }

        setEditable(false);
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

    @FXML
    private void handleBackToListings() {
        AppNavigator.navigateTo(AppView.MY_LISTINGS);
    }

    @FXML
    private void handleSaveChanges() {
        LocalDateTime startTime = resolveStartTime();
        int durationDays = resolveDurationDays();
        showStatus("Saving changes...");
        setButtonsDisabled(true);
        listingsService.updateListing(
                auctionId,
                titleField.getText(),
                descriptionArea.getText(),
                startingBidField.getText(),
                categoryComboBox.getValue(),
                conditionComboBox.getValue(),
                durationDays,
                startTime
        ).thenAccept(response ->
                Platform.runLater(() -> AppNavigator.navigateTo(AppView.MY_LISTINGS))
        ).exceptionally(ex -> {
            Platform.runLater(() -> {
                showStatus(resolveErrorMessage(ex, "Unable to save listing."));
                setButtonsDisabled(false);
            });
            return null;
        });
    }

    @FXML
    private void handleDeleteListing() {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Delete this auction?");
        confirm.setContentText("This action cannot be undone.");
        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("No", ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);
        confirm.showAndWait().ifPresent(selected -> {
            if (selected != yes) {
                return;
            }
            showStatus("Deleting listing...");
            setButtonsDisabled(true);
            listingsService.deleteListing(auctionId)
                    .thenAccept(response ->
                            Platform.runLater(() -> AppNavigator.navigateTo(AppView.MY_LISTINGS))
                    )
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            showStatus(resolveErrorMessage(ex, "Unable to delete listing."));
                            setButtonsDisabled(false);
                        });
                        return null;
                    });
        });
    }

    private void applyDetail(AuctionDetailDto dto) {
        if (dto == null) {
            showStatus("Unable to load listing.");
            setEditable(false);
            return;
        }

        titleField.setText(dto.getItemName() != null ? dto.getItemName() : "");
        descriptionArea.setText(dto.getItemDescription() != null ? dto.getItemDescription() : "");
        categoryComboBox.setValue(dto.getItemCategory());
        conditionComboBox.setValue(dto.getItemCondition());
        startingBidField.setText(formatPlainNumber(dto.getStartingPrice()));
        if (dto.getStartTime() != null) {
            startDatePicker.setValue(dto.getStartTime().toLocalDate());
            startTimeField.setText(dto.getStartTime().toLocalTime().toString());
        }
        applyDuration(dto.getStartTime(), dto.getEndTime());

        boolean open = dto.getStatus() == AuctionStatus.OPEN;
        showStatus(
                open
                        ? "Editing open listing - " + DisplayFormatters.timeLeft(dto.getEndTime())
                        : "Only open listings can be edited."
        );
        setEditable(open);
    }

    private void setEditable(boolean editable) {
        titleField.setDisable(!editable);
        descriptionArea.setDisable(!editable);
        categoryComboBox.setDisable(!editable);
        conditionComboBox.setDisable(!editable);
        startingBidField.setDisable(!editable);
        startDatePicker.setDisable(!editable);
        startTimeField.setDisable(!editable);
        customDurationField.setDisable(!editable);
        Arrays.asList(duration1Btn, duration3Btn, duration7Btn, duration14Btn, duration30Btn)
                .forEach(button -> button.setDisable(!editable));
        setButtonsDisabled(!editable);
    }

    private void setButtonsDisabled(boolean disabled) {
        saveButton.setDisable(disabled);
        deleteButton.setDisable(disabled);
    }

    private void applyDuration(LocalDateTime startTime, LocalDateTime endTime) {
        clearActiveDurationButtons();
        if (startTime == null || endTime == null || !endTime.isAfter(startTime)) {
            duration7Btn.getStyleClass().add("duration-chip-active");
            return;
        }

        long days = Math.max(1, Duration.between(startTime, endTime).toDays());
        Button preset = switch ((int) days) {
            case 1 -> duration1Btn;
            case 3 -> duration3Btn;
            case 7 -> duration7Btn;
            case 14 -> duration14Btn;
            case 30 -> duration30Btn;
            default -> null;
        };
        if (preset != null) {
            preset.getStyleClass().add("duration-chip-active");
            customDurationField.clear();
        } else {
            customDurationField.setText(String.valueOf(days));
        }
    }

    private LocalDateTime resolveStartTime() {
        LocalDate date = startDatePicker.getValue();
        if (date == null) {
            return null;
        }
        String timeStr = startTimeField.getText();
        if (timeStr == null || timeStr.isBlank()) {
            return date.atStartOfDay();
        }
        try {
            return LocalDateTime.of(date, LocalTime.parse(timeStr.trim()));
        } catch (DateTimeParseException ex) {
            return null;
        }
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

    private void clearActiveDurationButtons() {
        Arrays.asList(duration1Btn, duration3Btn, duration7Btn, duration14Btn, duration30Btn)
                .forEach(button -> button.getStyleClass().remove("duration-chip-active"));
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
}
