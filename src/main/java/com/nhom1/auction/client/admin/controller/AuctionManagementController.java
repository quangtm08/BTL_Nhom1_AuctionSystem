package com.nhom1.auction.client.admin.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.nhom1.auction.client.admin.service.AdminClientService;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.protocol.MessageType;

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
    private String cancelingAuctionId;

    @FXML
    public void initialize() {
        reloadAuctions();

        // Register push handlers so admin view updates in realtime
        ServerConnection.getInstance().registerPushHandler(
            MessageType.PUSH_NEW_AUCTION,
            json -> Platform.runLater(this::reloadAuctions)
        );

        ServerConnection.getInstance().registerPushHandler(
            MessageType.PUSH_BID_UPDATE,
            json -> Platform.runLater(this::reloadAuctions)
        );

        ServerConnection.getInstance().registerPushHandler(
            MessageType.PUSH_AUCTION_DELETED,
            json -> Platform.runLater(this::reloadAuctions)
        );

        ServerConnection.getInstance().registerPushHandler(
            MessageType.PUSH_AUCTION_ENDED,
            json -> Platform.runLater(this::reloadAuctions)
        );
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
        for (int i = 0; i < safeAuctions.size(); i++) {
            addRow(i + 1, safeAuctions.get(i));
        }

        long open = safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.OPEN).count();
        long running = safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.RUNNING).count();
        long finished = safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.FINISHED).count();
        long paid = safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.PAID).count();
        long canceled = safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.CANCELED).count();

        lblAuctionSummary.setText(safeAuctions.size() + " total auctions - "
                + open + " open, " + running + " running, " + finished + " finished, "
                + paid + " paid, " + canceled + " canceled");
    }

    private void addRow(int row, AuctionSummaryDto auction) {
        HBox bg = new HBox();
        bg.getStyleClass().add("table-row-bg");
        auctionGrid.add(bg, 0, row, 9, 1);

        addLabel(0, row, nvl(auction.getItemName()), "table-text-main");
        addLabel(1, row, nvl(auction.getItemCategory()), "table-text-sub");
        addLabel(2, row, shortId(auction.getSellerId()), "table-text-sub");
        addLabel(3, row, formatPrice(auction.getStartingPrice()), "table-text-sub");
        addLabel(4, row, formatPrice(auction.getCurrentHighestBid()), "price-highlight");
        addLabel(5, row, formatDateTime(auction.getStartTime()), "table-text-sub");
        addLabel(6, row, formatDateTime(auction.getEndTime()), "table-text-sub");

        Label status = new Label(auction.getStatus() != null ? auction.getStatus().name() : "-");
        status.getStyleClass().add(statusStyle(auction.getStatus()));
        auctionGrid.add(status, 7, row);

        Button cancelBtn = new Button("Cancel");
        cancelBtn.getStyleClass().add("btn-cancel");
        boolean canCancel = auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING;
        cancelBtn.setDisable(!canCancel || auction.getId().equals(cancelingAuctionId));
        cancelBtn.setOnAction(e -> cancelAuction(auction.getId(), cancelBtn));
        // Approve button for OPEN auctions
        Button approveBtn = new Button("Approve");
        approveBtn.getStyleClass().add("btn-approve");
        approveBtn.setDisable(!(auction.getStatus() == AuctionStatus.OPEN));
        approveBtn.setOnAction(e -> approveAuction(auction.getId(), approveBtn));

        HBox actions = new HBox(6, approveBtn, cancelBtn);
        actions.setAlignment(Pos.CENTER_LEFT);
        auctionGrid.add(actions, 8, row);
    }

    private void cancelAuction(String auctionId, Button cancelBtn) {
        cancelingAuctionId = auctionId;
        cancelBtn.setDisable(true);
        lblAuctionSummary.setText("Canceling auction...");

        adminClientService.cancelAuction(auctionId)
                .thenAccept(ok -> Platform.runLater(() -> {
                    cancelingAuctionId = null;
                    reloadAuctions();
                }))
                .exceptionally(ex -> {
                    Throwable cause = AdminClientService.extractFailure(ex);
                    Platform.runLater(() -> {
                        cancelingAuctionId = null;
                        cancelBtn.setDisable(false);
                        lblAuctionSummary.setText("Cancel failed: " + cause.getMessage());
                    });
                    return null;
                });
    }

    private void approveAuction(String auctionId, Button approveBtn) {
        approveBtn.setDisable(true);
        lblAuctionSummary.setText("Approving auction...");
        adminClientService.approveAuction(auctionId)
                .thenAccept(ok -> Platform.runLater(() -> reloadAuctions()))
                .exceptionally(ex -> {
                    Throwable cause = AdminClientService.extractFailure(ex);
                    Platform.runLater(() -> {
                        approveBtn.setDisable(false);
                        lblAuctionSummary.setText("Approve failed: " + cause.getMessage());
                    });
                    return null;
                });
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

    private String nvl(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private String shortId(String id) {
        return (id == null || id.length() < 8) ? nvl(id) : id.substring(0, 8) + "...";
    }

    private String formatPrice(BigDecimal price) {
        return price == null ? "-" : price.toPlainString();
    }

    private String formatDateTime(LocalDateTime time) {
        return time == null ? "-" : time.format(formatter);
    }

    private String statusStyle(AuctionStatus status) {
        if (status == AuctionStatus.OPEN) {
            return "status-open";
        }
        if (status == AuctionStatus.RUNNING) {
            return "status-running";
        }
        if (status == AuctionStatus.PAID) {
            return "status-pill-active";
        }
        if (status == AuctionStatus.CANCELED) {
            return "status-pill-banned";
        }
        return "table-text-sub";
    }
}
