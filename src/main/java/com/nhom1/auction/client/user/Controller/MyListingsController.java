package com.nhom1.auction.client.user.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.client.user.controller.components.ListingCardComponentController;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.auction.MyListingsResponse;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.layout.GridPane;

public class MyListingsController {
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Label> priceLabels = new HashMap<>();
    @FXML private Label activeListingsLabel;
    @FXML private GridPane listingsGrid;

    @FXML private void initialize() { loadMyListings(); ServerConnection.getInstance().registerPushHandler(MessageType.PUSH_BID_UPDATE, this::handleBidUpdatePush); }
    @FXML private void handleCreateListing() { AppNavigator.navigateTo(AppView.CREATE_LISTING); }
    @FXML private void handleEditListing() { AppNavigator.navigateTo(AppView.EDIT_LISTING); }

    private void renderListings(List<AuctionSummaryDto> listings) {
        priceLabels.clear(); listingsGrid.getChildren().clear();
        for (int i = 0; i < listings.size(); i++) listingsGrid.add(createListingCard(listings.get(i)), i % 2, i / 2);
    }

    private Parent createListingCard(AuctionSummaryDto dto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/components/listing_card.fxml"));
            Parent card = loader.load();
            ListingCardComponentController c = loader.getController();
            c.bind(dto, formatStatus(dto.getStatus()), formatMoney(dto.getCurrentHighestBid()), formatTimeLeft(dto.getEndTime()), isEnded(dto.getStatus()), this::handleEditListing, () -> handleDeleteListing(dto));
            if (dto.getId() != null) priceLabels.put(dto.getId(), c.getPriceLabel());
            return card;
        } catch (IOException e) { throw new RuntimeException("Failed to load listing card component", e); }
    }

    private void loadMyListings() { /* keep behavior */
        AuthResponse user = AppContext.getCurrentUser();
        if (user == null || user.getUserID() == null || user.getUserID().isBlank()) { activeListingsLabel.setText("0"); renderMessage("No user session. Please sign in again."); return; }
        RequestMessage<Map<String, String>> request = new RequestMessage<>(MessageType.LIST_MY_LISTINGS, Map.of("sellerId", user.getUserID()));
        ServerConnection.getInstance().sendRequest(request, MyListingsResponse.class).thenAccept(response -> Platform.runLater(() -> {
            if (response != null && response.isSuccess() && response.getPayload() != null && response.getPayload().getListings() != null) {
                List<AuctionSummaryDto> listings = response.getPayload().getListings(); activeListingsLabel.setText(String.valueOf(listings.size())); if (listings.isEmpty()) renderMessage("No listings yet."); else renderListings(listings);
            } else { activeListingsLabel.setText("0"); renderMessage((response != null && response.getError() != null) ? response.getError().getMessage() : "Unable to load listings."); }
        })).exceptionally(ex -> { Platform.runLater(() -> { activeListingsLabel.setText("0"); renderMessage("Connection error while loading listings."); }); return null; });
    }

    private void handleBidUpdatePush(String json) { try { JsonNode root = mapper.readTree(json); JsonNode node = root.has("payload") && !root.get("payload").isNull() ? root.get("payload") : root; String auctionId = node.has("auctionId") ? node.get("auctionId").asText() : null; if (auctionId == null) return; BigDecimal newBid = node.has("newHighestBid") ? new BigDecimal(node.get("newHighestBid").asText()) : null; if (newBid == null) return; Platform.runLater(() -> { Label label = priceLabels.get(auctionId); if (label != null) label.setText(formatMoney(newBid)); }); } catch (Exception e) { System.err.println("Error parsing bid update push: " + e.getMessage()); } }
    private void renderMessage(String message) { listingsGrid.getChildren().clear(); Label label = new Label(message); label.getStyleClass().add("card-sub-text"); listingsGrid.add(label, 0, 0); }
    private void handleDeleteListing(AuctionSummaryDto dto) { Alert confirm = new Alert(AlertType.CONFIRMATION); confirm.setTitle("Confirm delete"); confirm.setHeaderText("Delete this auction?"); confirm.setContentText("This action cannot be undone."); confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/client/my_listings.css").toExternalForm()); ButtonType yes = new ButtonType("Yes"); ButtonType no = new ButtonType("No", ButtonData.CANCEL_CLOSE); confirm.getButtonTypes().setAll(yes, no); ((Button) confirm.getDialogPane().lookupButton(yes)).getStyleClass().add("button-yes"); ((Button) confirm.getDialogPane().lookupButton(no)).getStyleClass().add("button-no"); confirm.showAndWait().ifPresent(selected -> { if (selected != yes) return; AuthResponse user = AppContext.getCurrentUser(); if (user == null || user.getUserID() == null || user.getUserID().isBlank()) { renderMessage("No user session. Please sign in again."); return; } RequestMessage<Map<String, String>> request = new RequestMessage<>(MessageType.DELETE_AUCTION, Map.of("sellerId", user.getUserID(), "auctionId", dto.getId())); ServerConnection.getInstance().sendRequest(request, String.class).thenAccept(response -> Platform.runLater(() -> { if (response != null && response.isSuccess()) loadMyListings(); else renderMessage((response != null && response.getError() != null) ? response.getError().getMessage() : "Delete failed."); })).exceptionally(ex -> { Platform.runLater(() -> renderMessage("Connection error while deleting.")); return null; }); }); }
    private boolean isEnded(AuctionStatus status) { return status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELED || status == AuctionStatus.PAID; }
    private String formatStatus(AuctionStatus status) { if (status == null) return "Unknown"; return isEnded(status) ? "Ended" : "Running"; }
    private String formatMoney(BigDecimal amount) { return amount == null ? "$0" : "$" + amount.stripTrailingZeros().toPlainString(); }
    private String formatTimeLeft(LocalDateTime endTime) { if (endTime == null) return "-"; Duration duration = Duration.between(LocalDateTime.now(), endTime); if (duration.isNegative() || duration.isZero()) return "Ended"; long days = duration.toDays(); if (days > 0) return days + " days left"; long hours = duration.toHours(); if (hours > 0) return hours + " hours left"; return Math.max(1, duration.toMinutes()) + " min left"; }
}
