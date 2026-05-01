package com.nhom1.auction.client.user.controller;


import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.auction.MyListingsResponse;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;


public class MyListingsController {

    @FXML
    private Label activeListingsLabel;
    @FXML
    private GridPane listingsGrid;

    @FXML
    private void initialize() {
        loadMyListings();
    }

    @FXML
    private void handleCreateListing() {
        AppNavigator.navigateTo(AppView.CREATE_LISTING);
    }

    @FXML
    private void handleEditListing() {
        AppNavigator.navigateTo(AppView.EDIT_LISTING);
    }

    private void loadMyListings() {
        AuthResponse user = AppContext.getCurrentUser();
        if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
            activeListingsLabel.setText("0");
            renderMessage("No user session. Please sign in again.");
            return;
        }
        System.out.println("[MyListings] sellerId=" + user.getUserID());

        RequestMessage<Map<String, String>> request = new RequestMessage<>(
                MessageType.LIST_MY_LISTINGS,
                Map.of("sellerId", user.getUserID())
        );

        ServerConnection.getInstance()
                .sendRequest(request, MyListingsResponse.class)
                .thenAccept(response -> Platform.runLater(() -> {
                    System.out.println("[MyListings] response success="
                            + (response != null && response.isSuccess()));
                    if (response != null && response.isSuccess()
                            && response.getPayload() != null
                            && response.getPayload().getListings() != null) {
                        List<AuctionSummaryDto> listings = response.getPayload().getListings();
                        System.out.println("[MyListings] listings size=" + listings.size());
                        activeListingsLabel.setText(String.valueOf(listings.size()));
                        if (listings.isEmpty()) {
                            renderMessage("No listings yet.");
                        } else {
                            renderListings(listings);
                        }
                    } else {
                        if (response != null && response.getError() != null) {
                            System.out.println("[MyListings] errorCode=" + response.getError().getCode()
                                    + ", errorMessage=" + response.getError().getMessage());
                        }
                        activeListingsLabel.setText("0");
                        String errorMessage = (response != null && response.getError() != null)
                                ? response.getError().getMessage() : "Unable to load listings.";
                        renderMessage(errorMessage);
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        System.out.println("[MyListings] exception=" + ex.getMessage());
                        activeListingsLabel.setText("0");
                        renderMessage("Connection error while loading listings.");
                    });
                    return null;
                });
    }

    private void renderListings(List<AuctionSummaryDto> listings) {
        listingsGrid.getChildren().clear();
        for (int i = 0; i < listings.size(); i++) {
            AuctionSummaryDto dto = listings.get(i);
            Node card = createListingCard(dto);
            int col = i % 2;
            int row = i / 2;
            listingsGrid.add(card, col, row);
        }
    }

    private Node createListingCard(AuctionSummaryDto dto) {
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);
        VBox titleBox = new VBox(2);
        Label title = new Label(dto.getItemName() != null ? dto.getItemName() : "Untitled listing");
        title.getStyleClass().add("card-title-text");
        Label category = new Label("Seller listing");
        category.getStyleClass().add("card-sub-text");
        titleBox.getChildren().addAll(title, category);

        Label status = new Label(formatStatus(dto.getStatus()));
        status.getStyleClass().add(isEnded(dto.getStatus()) ? "status-badge-ended" : "status-badge");
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        topRow.getChildren().addAll(titleBox, status);

        HBox priceRow = new HBox();
        priceRow.setAlignment(Pos.BOTTOM_LEFT);
        VBox left = new VBox(2);
        Label currentBidText = new Label("Current bid");
        currentBidText.getStyleClass().add("card-sub-text");
        Label price = new Label(formatMoney(dto.getCurrentHighestBid()));
        price.getStyleClass().add("card-price-text");
        Label bidsText = new Label("Listing");
        bidsText.getStyleClass().add("card-sub-text");
        left.getChildren().addAll(currentBidText, price, bidsText);

        VBox right = new VBox();
        right.setAlignment(Pos.BOTTOM_RIGHT);
        Label remaining = new Label(formatTimeLeft(dto.getEndTime()));
        remaining.getStyleClass().add("card-sub-text");
        right.getChildren().add(remaining);
        HBox.setHgrow(left, Priority.ALWAYS);
        priceRow.getChildren().addAll(left, right);

        Separator sep = new Separator();
        sep.getStyleClass().add("card-separator");

        HBox actions = new HBox(10);
        Button editBtn = new Button("Edit");
        editBtn.getStyleClass().add("btn-card-edit");
        editBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(editBtn, Priority.ALWAYS);
        editBtn.setDisable(isEnded(dto.getStatus()));
        if (isEnded(dto.getStatus())) {
            editBtn.getStyleClass().add("btn-card-disabled");
        } else {
            editBtn.setOnAction(e -> handleEditListing());
        }

        Button deleteBtn = new Button("Delete");
        deleteBtn.getStyleClass().add("btn-card-delete");
        deleteBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(deleteBtn, Priority.ALWAYS);
        deleteBtn.setOnAction(e -> handleDeleteListing(dto));
        actions.getChildren().addAll(editBtn, deleteBtn);

        card.getChildren().addAll(topRow, priceRow, sep, actions);
        return card;
    }

    private void renderMessage(String message) {
        listingsGrid.getChildren().clear();
        Label label = new Label(message);
        label.getStyleClass().add("card-sub-text");
        listingsGrid.add(label, 0, 0);
    }

    private void handleDeleteListing(AuctionSummaryDto dto) {
        Alert confirm = new Alert(AlertType.CONFIRMATION);
        confirm.setTitle("Confirm delete");
        confirm.setHeaderText("Delete this auction?");
        confirm.setContentText("This action cannot be undone.");
        confirm.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/client/my_listings.css").toExternalForm()
        );
        ButtonType yes = new ButtonType("Yes");
        ButtonType no = new ButtonType("No", ButtonData.CANCEL_CLOSE);
        confirm.getButtonTypes().setAll(yes, no);
        Button yesButton = (Button) confirm.getDialogPane().lookupButton(yes);
        Button noButton = (Button) confirm.getDialogPane().lookupButton(no);
        yesButton.getStyleClass().add("button-yes");
        noButton.getStyleClass().add("button-no");

        confirm.showAndWait().ifPresent(selected -> {
            if (selected != yes) {
                return;
            }

            AuthResponse user = AppContext.getCurrentUser();
            if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
                renderMessage("No user session. Please sign in again.");
                return;
            }

            RequestMessage<Map<String, String>> request = new RequestMessage<>(
                    MessageType.DELETE_AUCTION,
                    Map.of(
                            "sellerId", user.getUserID(),
                            "auctionId", dto.getId()
                    )
            );

            ServerConnection.getInstance()
                    .sendRequest(request, String.class)
                    .thenAccept(response -> Platform.runLater(() -> {
                        if (response != null && response.isSuccess()) {
                            loadMyListings();
                        } else {
                            String errorMessage = (response != null && response.getError() != null)
                                    ? response.getError().getMessage()
                                    : "Delete failed.";
                            renderMessage(errorMessage);
                        }
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> renderMessage("Connection error while deleting."));
                        return null;
                    });
        });
    }

    private boolean isEnded(AuctionStatus status) {
        return status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED || status == AuctionStatus.PAID;
    }

    private String formatStatus(AuctionStatus status) {
        if (status == null) return "Unknown";
        return isEnded(status) ? "Ended" : "Running";
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "$0";
        return "$" + amount.stripTrailingZeros().toPlainString();
    }

    private String formatTimeLeft(LocalDateTime endTime) {
        if (endTime == null) return "-";
        Duration duration = Duration.between(LocalDateTime.now(), endTime);
        if (duration.isNegative() || duration.isZero()) return "Ended";
        long days = duration.toDays();
        if (days > 0) return days + " days left";
        long hours = duration.toHours();
        if (hours > 0) return hours + " hours left";
        long minutes = Math.max(1, duration.toMinutes());
        return minutes + " min left";
    }
}
