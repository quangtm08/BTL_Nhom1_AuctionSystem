package com.nhom1.auction.client.user.controller;

import java.util.Arrays;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;

import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;

public class EditAuctionController {

    @FXML
    private ComboBox<ItemCategory> categoryComboBox;
    @FXML
    private ComboBox<ItemCondition> conditionComboBox;
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
    private void initialize() {
        categoryComboBox.getItems().setAll(ItemCategory.values());
        conditionComboBox.getItems().setAll(ItemCondition.values());
        categoryComboBox.setValue(ItemCategory.ART);
        conditionComboBox.setValue(ItemCondition.USED);

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
}
