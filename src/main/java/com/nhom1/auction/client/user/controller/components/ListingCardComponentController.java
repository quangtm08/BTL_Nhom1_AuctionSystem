package com.nhom1.auction.client.user.controller.components;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;

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

        editButton.setDisable(ended);
        if (ended) {
            if (!editButton.getStyleClass().contains("btn-card-disabled")) editButton.getStyleClass().add("btn-card-disabled");
            editButton.setOnAction(null);
        } else {
            editButton.getStyleClass().remove("btn-card-disabled");
            editButton.setOnAction(e -> onEdit.run());
        }
        deleteButton.setOnAction(e -> onDelete.run());
    }

    public Label getPriceLabel() {
        return priceLabel;
    }
}
