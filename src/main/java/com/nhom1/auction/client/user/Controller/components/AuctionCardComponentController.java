package com.nhom1.auction.client.user.controller.components;

import java.util.function.Consumer;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class AuctionCardComponentController {
    @FXML
    private Label titleLabel;
    @FXML
    private Label categoryLabel;
    @FXML
    private Label statusBadgeLabel;
    @FXML
    private Label priceValueLabel;
    @FXML
    private Label timeLeftLabel;
    @FXML
    private Button actionButton;

    public void bind(AuctionSummaryDto dto, String status, String price, String timeLeft, Consumer<String> onRaiseBid) {
        titleLabel.setText(dto.getItemName() != null ? dto.getItemName() : "Untitled");
        categoryLabel.setText(dto.getItemCategory() != null ? dto.getItemCategory() : "Uncategorized");
        statusBadgeLabel.setText(status);
        priceValueLabel.setText(price);
        timeLeftLabel.setText(timeLeft);
        actionButton.setOnAction(e -> onRaiseBid.accept(dto.getId()));
    }

    public Label getPriceValueLabel() {
        return priceValueLabel;
    }
}
