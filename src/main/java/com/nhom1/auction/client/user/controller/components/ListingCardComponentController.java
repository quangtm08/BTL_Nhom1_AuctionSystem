package com.nhom1.auction.client.user.controller.components;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.enums.AuctionStatus;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class ListingCardComponentController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label subLabel;
    @FXML
    private Label statusLabel;
    @FXML
    private Label priceLabel;
    @FXML
    private Label remainingLabel;
    @FXML
    private Button editButton;
    @FXML
    private Button deleteButton;

    public void bind(AuctionSummaryDto dto, String status, String price, String remaining, boolean ended, Runnable onEdit, Runnable onDelete) {
        titleLabel.setText(dto.getItemName() != null ? dto.getItemName() : "Untitled listing");
        subLabel.setText("Seller listing");
        statusLabel.setText(status);
        priceLabel.setText(price);
        remainingLabel.setText(remaining);

        statusLabel.getStyleClass().remove("status-badge-ended");
        if (ended && !statusLabel.getStyleClass().contains("status-badge-ended")) statusLabel.getStyleClass().add("status-badge-ended");

        boolean canModify = dto.getStatus() == AuctionStatus.OPEN;
        editButton.setDisable(!canModify);
        deleteButton.setDisable(!canModify);
        if (!canModify) {
            addDisabledClass(editButton);
            addDisabledClass(deleteButton);
            editButton.setOnAction(null);
            deleteButton.setOnAction(null);
        } else {
            editButton.getStyleClass().remove("btn-card-disabled");
            deleteButton.getStyleClass().remove("btn-card-disabled");
            editButton.setOnAction(e -> onEdit.run());
            deleteButton.setOnAction(e -> onDelete.run());
        }
    }

    public Label getPriceLabel() {
        return priceLabel;
    }

    private void addDisabledClass(Button button) {
        if (!button.getStyleClass().contains("btn-card-disabled")) {
            button.getStyleClass().add("btn-card-disabled");
        }
    }
}
