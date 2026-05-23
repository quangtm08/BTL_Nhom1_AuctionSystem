package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.client.user.service.EditAuctionClientService;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.utils.AppContext;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

public class EditAuctionController {
  private final EditAuctionClientService editAuctionClientService = new EditAuctionClientService();
  private final BiddingClientService biddingClientService = new BiddingClientService();
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
  @FXML private Label statusSubLabel;
  @FXML private TextField startingBidField;
  @FXML private Button saveChangesButton;
  private LocalDateTime loadedStartTime;
  private LocalDateTime loadedEndTime;

  @FXML
  private void initialize() {
    categoryComboBox.getItems().setAll(ItemCategory.values());
    conditionComboBox.getItems().setAll(ItemCondition.values());
    categoryComboBox.setValue(null);
    conditionComboBox.setValue(null);
    customDurationField
        .textProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (newValue != null && !newValue.isBlank()) clearActiveDurationButtons();
            });
    loadAuctionForEdit();
  }

  @FXML
  private void handleDurationPreset(ActionEvent event) {
    if (!(event.getSource() instanceof Button selectedButton)) return;
    clearActiveDurationButtons();
    ObservableList<String> classes = selectedButton.getStyleClass();
    if (!classes.contains("duration-chip-active")) classes.add("duration-chip-active");
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
  private void handleSaveChanges() {
    String auctionId = AppContext.getSelectedAuctionId();
    if (auctionId == null || auctionId.isBlank()) {
      statusLabel.setText("Missing auction ID.");
      return;
    }
    int days = resolveDurationDays();
    if (days <= 0 && loadedEndTime == null) {
      statusLabel.setText("Duration must be greater than 0.");
      return;
    }
    LocalDateTime baseTime =
        loadedStartTime != null && loadedStartTime.isAfter(LocalDateTime.now())
            ? loadedStartTime
            : LocalDateTime.now();
    LocalDateTime targetEndTime = days > 0 ? baseTime.plusDays(days) : loadedEndTime;
    statusLabel.setText("Saving...");
    if (saveChangesButton != null) saveChangesButton.setDisable(true);
    editAuctionClientService
        .updateAuction(
            auctionId,
            titleField.getText(),
            descriptionArea.getText(),
            startingBidField.getText(),
            categoryComboBox.getValue(),
            conditionComboBox.getValue(),
            targetEndTime)
        .thenAccept(
            response ->
                Platform.runLater(
                    () -> {
                      if (saveChangesButton != null) saveChangesButton.setDisable(false);
                      statusLabel.setText("Updated successfully.");
                      AppNavigator.navigateTo(AppView.MY_LISTINGS);
                    }))
        .exceptionally(
            ex -> {
              Platform.runLater(
                  () -> {
                    if (saveChangesButton != null) saveChangesButton.setDisable(false);
                    statusLabel.setText(
                        ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage());
                  });
              return null;
            });
  }

  private void loadAuctionForEdit() {
    String auctionId = AppContext.getSelectedAuctionId();
    if (auctionId == null || auctionId.isBlank()) {
      statusLabel.setText("Missing auction ID.");
      return;
    }
    biddingClientService
        .getAuctionDetail(auctionId)
        .thenAccept(
            dto ->
                Platform.runLater(
                    () -> {
                      if (dto == null) {
                        statusLabel.setText("Cannot load auction detail.");
                        return;
                      }
                      titleField.setText(dto.getItemName());
                      descriptionArea.setText(dto.getItemDescription());
                      startingBidField.setText(
                          dto.getStartingPrice() != null
                              ? dto.getStartingPrice().stripTrailingZeros().toPlainString()
                              : "");
                      if (dto.getItemCategory() != null)
                        categoryComboBox.setValue(dto.getItemCategory());
                      if (dto.getItemCondition() != null)
                        conditionComboBox.setValue(dto.getItemCondition());
                      loadedStartTime = dto.getStartTime();
                      loadedEndTime = dto.getEndTime();
                      applyDurationFromEndTime(loadedStartTime, loadedEndTime);
                      bindMeta(dto.getStartTime(), dto.getEndTime());
                      statusLabel.setText("Ready");
                    }))
        .exceptionally(
            ex -> {
              Platform.runLater(
                  () ->
                      statusLabel.setText(
                          ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage()));
              return null;
            });
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
    return -1;
  }

  private void applyDurationFromEndTime(LocalDateTime startTime, LocalDateTime endTime) {
    clearActiveDurationButtons();
    if (endTime == null) return;
    LocalDateTime baseTime =
        startTime != null && startTime.isAfter(LocalDateTime.now())
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

  private void bindMeta(LocalDateTime startTime, LocalDateTime endTime) {
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    metaLabel.setText(startTime != null ? "Opens: " + startTime.format(fmt) : "Opens: N/A");
    if (endTime == null) {
      statusSubLabel.setText("No end time");
      return;
    }
    long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), endTime);
    statusSubLabel.setText(daysLeft > 0 ? daysLeft + " days left" : "Ending soon");
  }
}
