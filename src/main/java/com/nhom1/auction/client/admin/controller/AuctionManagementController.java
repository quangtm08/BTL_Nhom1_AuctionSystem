package com.nhom1.auction.client.admin.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.nhom1.auction.client.admin.service.AdminClientService;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.enums.AuctionStatus;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class AuctionManagementController {

    private final AdminClientService adminClientService = new AdminClientService();

    @FXML private Label lblAuctionSummary;
    @FXML private GridPane auctionGrid;

    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        reloadAuctions();
    }

    private void reloadAuctions() {
        lblAuctionSummary.setText("Loading auctions...");

        adminClientService.listAllAuctions()
                .thenAccept(res -> Platform.runLater(() -> renderAuctions(res.getAuctions())))
                .exceptionally(ex -> {
                    Throwable cause = AdminClientService.extractFailure(ex);
                    Platform.runLater(() ->
                            lblAuctionSummary.setText("Load auctions failed: " + cause.getMessage()));
                    return null;
                });
    }

    private void renderAuctions(List<AuctionSummaryDto> auctions) {
        clearRowsFrom(1);

        List<AuctionSummaryDto> safeAuctions = auctions != null ? auctions : List.of();

        for (int i = 0; i < safeAuctions.size(); i++) {
            addRow(i + 1, safeAuctions.get(i));
        }

        long running = safeAuctions.stream()
                .filter(a -> a.getStatus() == AuctionStatus.RUNNING)
                .count();

        lblAuctionSummary.setText(safeAuctions.size() + " total auctions - " + running + " running");
    }

    private void addRow(int row, AuctionSummaryDto auction) {
        HBox bg = new HBox();
        bg.getStyleClass().add("table-row-bg");
        auctionGrid.add(bg, 0, row, 8, 1);

        // Item name
        addLabel(0, row, nvl(auction.getItemName()), "table-text-main");

        // Category (DTO không có → để "-")
        addLabel(1, row, nvl(auction.getItemCategory()), "table-text-sub");

        // Seller
        addLabel(2, row, shortId(auction.getSellerId()), "table-text-sub");

        // Starting price
        addLabel(3, row, formatPrice(auction.getStartingPrice()), "table-text-sub");

        // Current highest bid
        addLabel(4, row, formatPrice(auction.getCurrentHighestBid()), "price-highlight");

        // End time
        addLabel(5, row, formatDateTime(auction.getEndTime()), "table-text-sub");

        // Status (enum)
        Label status = new Label(
                auction.getStatus() != null ? auction.getStatus().name() : "-"
        );

        status.getStyleClass().add(
                auction.getStatus() == AuctionStatus.RUNNING
                        ? "status-running"
                        : "table-text-sub"
        );

        auctionGrid.add(status, 6, row);

        // Cancel button
        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-cancel");

        boolean canCancel = auction.getStatus() == AuctionStatus.OPEN
                || auction.getStatus() == AuctionStatus.RUNNING;

        cancelBtn.setDisable(!canCancel);

        cancelBtn.setOnAction(e ->
                adminClientService.cancelAuction(auction.getId())
                        .thenAccept(ok -> Platform.runLater(this::reloadAuctions))
                        .exceptionally(ex -> {
                            Throwable cause = AdminClientService.extractFailure(ex);
                            Platform.runLater(() ->
                                    lblAuctionSummary.setText("Cancel failed: " + cause.getMessage()));
                            return null;
                        })
        );

        HBox actions = new HBox(cancelBtn);
        actions.setAlignment(Pos.CENTER);
        auctionGrid.add(actions, 7, row);
    }

    private void addLabel(int col, int row, String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        auctionGrid.add(label, col, row);
    }

    private void clearRowsFrom(int startRow) {
        auctionGrid.getChildren().removeIf(node -> {
            Integer row = GridPane.getRowIndex(node);
            return row != null && row >= startRow;
        });
    }

    // ================= HELPER =================

    private String nvl(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String shortId(String id) {
        return (id == null || id.length() < 8)
                ? nvl(id)
                : id.substring(0, 8) + "...";
    }

    private String formatPrice(BigDecimal price) {
        return price == null ? "-" : price.toPlainString();
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "-" : time.format(formatter);
    }
}
