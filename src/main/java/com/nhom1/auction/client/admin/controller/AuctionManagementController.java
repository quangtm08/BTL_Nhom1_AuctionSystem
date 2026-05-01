package com.nhom1.auction.client.admin.controller;

import com.nhom1.auction.client.admin.service.AdminClientService;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class AuctionManagementController {
    private final AdminClientService adminClientService = new AdminClientService();

    @FXML private Label lblAuctionSummary;
    @FXML private GridPane auctionGrid;

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
                    Platform.runLater(() -> lblAuctionSummary.setText("Load auctions failed: " + cause.getMessage()));
                    return null;
                });
    }

    private void renderAuctions(List<AuctionSummaryDto> auctions) {
        clearRowsFrom(1);
        List<AuctionSummaryDto> safeAuctions = auctions != null ? auctions : List.of();
        for (int i = 0; i < safeAuctions.size(); i++) addRow(i + 1, safeAuctions.get(i));
        long running = safeAuctions.stream().filter(a -> "RUNNING".equals(a.getStatus())).count();
        lblAuctionSummary.setText(safeAuctions.size() + " total auctions - " + running + " running");
    }

    private void addRow(int row, AuctionSummaryDto auction) {
        HBox bg = new HBox();
        bg.getStyleClass().add("table-row-bg");
        auctionGrid.add(bg, 0, row, 8, 1);

        addLabel(0, row, nvl(auction.getItemName()), "table-text-main");
        addLabel(1, row, nvl(auction.getItemCategory()), "table-text-sub");
        addLabel(2, row, shortId(auction.getSellerId()), "table-text-sub");
        addLabel(3, row, "-", "table-text-sub");
        addLabel(4, row, String.valueOf(auction.getCurrentHighestBid()), "price-highlight");
        addLabel(5, row, nvl(auction.getEndTime()), "table-text-sub");

        Label status = new Label(nvl(auction.getStatus()));
        status.getStyleClass().add("RUNNING".equals(auction.getStatus()) ? "status-running" : "table-text-sub");
        auctionGrid.add(status, 6, row);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-cancel");
        boolean canCancel = "OPEN".equals(auction.getStatus()) || "RUNNING".equals(auction.getStatus());
        cancelBtn.setDisable(!canCancel);
        cancelBtn.setOnAction(e -> adminClientService.cancelAuction(auction.getAuctionId())
                .thenAccept(ok -> Platform.runLater(this::reloadAuctions))
                .exceptionally(ex -> {
                    Throwable cause = AdminClientService.extractFailure(ex);
                    Platform.runLater(() -> lblAuctionSummary.setText("Cancel failed: " + cause.getMessage()));
                    return null;
                }));

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

    private String nvl(String value) { return value == null || value.isBlank() ? "-" : value; }
    private String shortId(String id) { return (id == null || id.length() < 8) ? nvl(id) : id.substring(0, 8) + "..."; }
}
