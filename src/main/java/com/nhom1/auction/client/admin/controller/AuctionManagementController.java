package com.nhom1.auction.client.admin.controller;

import com.nhom1.auction.client.admin.service.AdminClientService;
import com.nhom1.auction.client.service.ClientPushService;
import com.nhom1.auction.client.util.DisplayFormatters;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.enums.AuctionStatus;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

public class AuctionManagementController {
  private final AdminClientService adminClientService = new AdminClientService();
  private final ClientPushService pushService = ClientPushService.getInstance();

  @FXML private Label lblAuctionSummary;
  @FXML private GridPane auctionGrid;

  private String cancelingAuctionId;

  // Initialization
  @FXML
  public void initialize() {
    reloadAuctions();

    pushService.onNewAuction(event -> Platform.runLater(this::reloadAuctions));
    pushService.onBidUpdate(event -> Platform.runLater(this::reloadAuctions));
    pushService.onAuctionDeleted(event -> Platform.runLater(this::reloadAuctions));
    pushService.onAuctionEnded(event -> Platform.runLater(this::reloadAuctions));
  }

  // Data loading
  private void reloadAuctions() {
    lblAuctionSummary.setText("Loading auctions...");

    adminClientService
        .listAllAuctions()
        .thenAccept(res -> Platform.runLater(() -> renderAuctions(res.getAuctions())))
        .exceptionally(
            ex -> {
              Throwable cause = AdminClientService.extractFailure(ex);
              Platform.runLater(
                  () -> lblAuctionSummary.setText("Load auctions failed: " + cause.getMessage()));
              return null;
            });
  }

  // Rendering
  private void renderAuctions(List<AuctionSummaryDto> auctions) {
    clearRowsFrom(1);
    List<AuctionSummaryDto> safeAuctions = auctions != null ? auctions : List.of();
    for (int i = 0; i < safeAuctions.size(); i++) {
      addRow(i + 1, safeAuctions.get(i));
    }

    long pending =
        safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.PENDING).count();
    long open = safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.OPEN).count();
    long running =
        safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.RUNNING).count();
    long finished =
        safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.FINISHED).count();
    long paid = safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.PAID).count();
    long canceled =
        safeAuctions.stream().filter(a -> a.getStatus() == AuctionStatus.CANCELED).count();

    lblAuctionSummary.setText(
        safeAuctions.size()
            + " total auctions - "
            + pending
            + " pending, "
            + open
            + " open, "
            + running
            + " running, "
            + finished
            + " finished, "
            + paid
            + " paid, "
            + canceled
            + " canceled");
  }

  private void addRow(int row, AuctionSummaryDto auction) {
    HBox bg = new HBox();
    bg.getStyleClass().add("table-row-bg");
    auctionGrid.add(bg, 0, row, 9, 1);

    addLabel(0, row, nvl(auction.getItemName()), "table-text-main");
    addLabel(1, row, nvl(auction.getItemCategory()), "table-text-sub");
    addLabel(2, row, shortId(auction.getSellerId()), "table-text-sub");
    addLabel(3, row, DisplayFormatters.moneyOrDash(auction.getStartingPrice()), "table-text-sub");
    addLabel(
        4, row, DisplayFormatters.moneyOrDash(auction.getCurrentHighestBid()), "price-highlight");
    addLabel(5, row, DisplayFormatters.dateTime(auction.getStartTime()), "table-text-sub");
    addLabel(6, row, DisplayFormatters.dateTime(auction.getEndTime()), "table-text-sub");

    Label status = new Label(DisplayFormatters.auctionStatusLabel(auction.getStatus()));
    status.getStyleClass().add(DisplayFormatters.adminAuctionStatusStyle(auction.getStatus()));
    auctionGrid.add(status, 7, row);

    Button cancelBtn = new Button("Cancel");
    cancelBtn.getStyleClass().add("btn-cancel");
    String auctionId = auction.getId();
    boolean missingAuctionId = auctionId == null || auctionId.isBlank();
    boolean canCancel =
        auction.getStatus() == AuctionStatus.PENDING
            || auction.getStatus() == AuctionStatus.OPEN
            || auction.getStatus() == AuctionStatus.RUNNING;
    cancelBtn.setDisable(missingAuctionId || !canCancel || auctionId.equals(cancelingAuctionId));
    cancelBtn.setOnAction(e -> cancelAuction(auctionId, cancelBtn));

    Button approveBtn = new Button("Approve");
    approveBtn.getStyleClass().add("btn-approve");
    approveBtn.setDisable(missingAuctionId || !(auction.getStatus() == AuctionStatus.PENDING));
    approveBtn.setOnAction(e -> approveAuction(auctionId, approveBtn));

    HBox actions = new HBox(6, approveBtn, cancelBtn);
    actions.setAlignment(Pos.CENTER_LEFT);
    auctionGrid.add(actions, 8, row);
  }

  // Event handlers
  private void cancelAuction(String auctionId, Button cancelBtn) {
    cancelingAuctionId = auctionId;
    cancelBtn.setDisable(true);
    lblAuctionSummary.setText("Canceling auction...");

    adminClientService
        .cancelAuction(auctionId)
        .thenAccept(
            ok ->
                Platform.runLater(
                    () -> {
                      cancelingAuctionId = null;
                      reloadAuctions();
                    }))
        .exceptionally(
            ex -> {
              Throwable cause = AdminClientService.extractFailure(ex);
              Platform.runLater(
                  () -> {
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
    adminClientService
        .approveAuction(auctionId)
        .thenAccept(ok -> Platform.runLater(() -> reloadAuctions()))
        .exceptionally(
            ex -> {
              Throwable cause = AdminClientService.extractFailure(ex);
              Platform.runLater(
                  () -> {
                    approveBtn.setDisable(false);
                    lblAuctionSummary.setText("Approve failed: " + cause.getMessage());
                  });
              return null;
            });
  }

  // Helpers
  private void addLabel(int col, int row, String text, String styleClass) {
    Label label = new Label(text);
    label.getStyleClass().add(styleClass);
    auctionGrid.add(label, col, row);
  }

  private void clearRowsFrom(int startRow) {
    auctionGrid
        .getChildren()
        .removeIf(
            node -> {
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
}
