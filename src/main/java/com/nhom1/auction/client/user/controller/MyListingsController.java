package com.nhom1.auction.client.user.controller;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.service.ClientPushService;
import com.nhom1.auction.client.user.controller.components.ListingCardComponentController;
import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.MyListingsClientService;
import com.nhom1.auction.client.util.DisplayFormatters;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.auction.MyListingsResponse;
import com.nhom1.auction.common.dto.notification.BidUpdateEvent;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.utils.AppContext;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class MyListingsController {
    private final ClientPushService pushService = ClientPushService.getInstance();
    private final MyListingsClientService listingsService = new MyListingsClientService();
    private final Map<String, Label> priceLabels = new HashMap<>();
    @FXML private Label activeListingsLabel;
    @FXML private GridPane listingsGrid;

    @FXML private void initialize() {
        loadMyListings();
        pushService.onBidUpdate(event -> Platform.runLater(() -> handleBidUpdatePush(event)));
        pushService.onNewAuction(event -> Platform.runLater(this::loadMyListings));
        pushService.onAuctionEnded(event -> Platform.runLater(this::loadMyListings));
        pushService.onAuctionDeleted(event -> Platform.runLater(this::loadMyListings));
    }
    @FXML private void handleCreateListing() { AppNavigator.navigateTo(AppView.CREATE_LISTING); }
    private void handleEditListing(AuctionSummaryDto dto) {
        if (dto == null || dto.getId() == null || dto.getId().isBlank()) return;
        AppContext.setSelectedAuctionId(dto.getId());
        AppNavigator.navigateTo(AppView.EDIT_LISTING);
    }

    private void renderListings(List<AuctionSummaryDto> listings) {
        priceLabels.clear(); listingsGrid.getChildren().clear();
        for (int i = 0; i < listings.size(); i++) listingsGrid.add(createListingCard(listings.get(i)), i % 2, i / 2);
    }

    private Parent createListingCard(AuctionSummaryDto dto) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/components/listing_card.fxml"));
            Parent card = loader.load();
            ListingCardComponentController c = loader.getController();
            c.bind(dto, DisplayFormatters.auctionStatusLabel(dto.getStatus()), DisplayFormatters.money(resolveDisplayCurrentBid(dto)), DisplayFormatters.timeLeft(dto.getEndTime()), !isEditableListing(dto), DisplayFormatters.isEnded(dto.getStatus()), () -> handleEditListing(dto), () -> handleDeleteListing(dto));
            if (dto.getId() != null) priceLabels.put(dto.getId(), c.getPriceLabel());
            return card;
        } catch (IOException e) { throw new RuntimeException("Failed to load listing card component", e); }
    }

    private void loadMyListings() {
        listingsService.listMyListings()
                .thenAccept(response -> Platform.runLater(() -> renderMyListings(response)))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        activeListingsLabel.setText("0");
                        renderMessage(resolveErrorMessage(ex, "Connection error while loading listings."));
                    });
                    return null;
                });
    }

    private void renderMyListings(MyListingsResponse response) {
        if (response != null && response.getListings() != null) {
            List<AuctionSummaryDto> listings = response.getListings();
            activeListingsLabel.setText(String.valueOf(listings.size()));
            if (listings.isEmpty()) renderMessage("No listings yet.");
            else renderListings(listings);
            return;
        }

        activeListingsLabel.setText("0");
        renderMessage("Unable to load listings.");
    }

    private void handleBidUpdatePush(BidUpdateEvent event)
    { if (event == null || event.getAuctionId() == null || event.getNewHighestBid() == null) return; Label label = priceLabels.get(event.getAuctionId()); if (label != null) label.setText(DisplayFormatters.money(event.getNewHighestBid())); }
    private void renderMessage(String message) { listingsGrid.getChildren().clear(); Label label = new Label(message); label.getStyleClass().add("card-sub-text"); listingsGrid.add(label, 0, 0); }
    private void handleDeleteListing(AuctionSummaryDto dto) { Alert confirm = new Alert(AlertType.CONFIRMATION); confirm.setTitle("Confirm delete"); confirm.setHeaderText("Delete this auction?"); confirm.setContentText("This action cannot be undone."); confirm.getDialogPane().getStylesheets().add(getClass().getResource("/css/client/my_listings.css").toExternalForm()); ButtonType yes = new ButtonType("Yes"); ButtonType no = new ButtonType("No", ButtonData.CANCEL_CLOSE); confirm.getButtonTypes().setAll(yes, no); ((Button) confirm.getDialogPane().lookupButton(yes)).getStyleClass().add("button-yes"); ((Button) confirm.getDialogPane().lookupButton(no)).getStyleClass().add("button-no"); confirm.showAndWait().ifPresent(selected -> { if (selected != yes) return; listingsService.deleteListing(dto.getId()).thenAccept(response -> Platform.runLater(this::loadMyListings)).exceptionally(ex -> { Platform.runLater(() -> renderMessage(resolveErrorMessage(ex, "Connection error while deleting."))); return null; }); }); }
    private String resolveErrorMessage(Throwable throwable, String fallback) { Throwable cause = BaseClientService.extractFailure(throwable); return cause.getMessage() != null && !cause.getMessage().isBlank() ? cause.getMessage() : fallback; }
    private BigDecimal resolveDisplayCurrentBid(AuctionSummaryDto dto) {
        if (dto == null) return BigDecimal.ZERO;
        BigDecimal current = dto.getCurrentHighestBid();
        if (current != null && current.compareTo(BigDecimal.ZERO) > 0) return current;
        return dto.getStartingPrice() != null ? dto.getStartingPrice() : BigDecimal.ZERO;
    }
    private boolean isEditableListing(AuctionSummaryDto dto) {
        return dto != null && (dto.getStatus() == AuctionStatus.PENDING || dto.getStatus() == AuctionStatus.OPEN);
    }
}
