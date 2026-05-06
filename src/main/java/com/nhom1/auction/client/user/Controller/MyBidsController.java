package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.bidding.BidWithAuctionDto;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;
import com.nhom1.auction.common.utils.AppContext;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class MyBidsController {

    @FXML
    private GridPane cardsGridPane;

    private final BiddingClientService biddingService = new BiddingClientService();

    @FXML
    public void initialize() {
        biddingService.getMyBids()
            .thenAccept(resp -> Platform.runLater(() -> renderMyBids(resp)))
            .exceptionally(ex -> {
                Platform.runLater(() -> System.err.println("Failed to load my bids: " + ex.getCause().getMessage()));
                return null;
            });
    }

    private void renderMyBids(MyBidsResponse resp) {
        cardsGridPane.getChildren().clear();
        if (resp == null || resp.getBids() == null || resp.getBids().isEmpty()) return;

        List<BidWithAuctionDto> bids = resp.getBids();
        for (int i = 0; i < bids.size(); i++) {
            cardsGridPane.add(createBidCard(bids.get(i)), i % 2, i / 2);
        }
    }

    private Node createBidCard(BidWithAuctionDto bid) {
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card");

        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label(bid.getItemName() != null ? bid.getItemName() : "Unknown item");
        title.getStyleClass().add("card-title-text");
        Label sub = new Label("Your bid: " + formatMoney(bid.getYourBid()));
        sub.getStyleClass().add("card-sub-text");
        titleBox.getChildren().addAll(title, sub);

        Label statusBadge = new Label(bid.isWinning() ? "Winning" : "Outbid");
        statusBadge.getStyleClass().add("status-badge");
        if (!bid.isWinning()) {
            statusBadge.setStyle("-fx-text-fill: #ff4d4d; -fx-background-color: #331a1a;");
        }

        HBox.setHgrow(titleBox, Priority.ALWAYS);
        topRow.getChildren().addAll(titleBox, statusBadge);

        VBox spacer = new VBox();
        spacer.setPrefHeight(50.0);

        HBox bottomRow = new HBox(10);
        bottomRow.setAlignment(Pos.BOTTOM_LEFT);

        VBox priceSection = new VBox(2);
        Label priceLabel = new Label("Current bid");
        priceLabel.getStyleClass().add("card-sub-text");
        Label priceValue = new Label(formatMoney(bid.getCurrentHighestBid()));
        priceValue.getStyleClass().add("card-price-text");
        priceSection.getChildren().addAll(priceLabel, priceValue);

        VBox timeSection = new VBox(2);
        timeSection.setAlignment(Pos.BOTTOM_RIGHT);
        Label timeLeft = new Label(formatTimeLeft(bid.getEndTime()));
        timeLeft.getStyleClass().add("card-sub-text");
        Button raiseBtn = new Button("Raise bid");
        raiseBtn.getStyleClass().add("btn-action-green");
        raiseBtn.setOnAction(e -> navigateToDetail(bid.getAuctionId()));
        timeSection.getChildren().addAll(timeLeft, raiseBtn);

        HBox.setHgrow(priceSection, Priority.ALWAYS);
        bottomRow.getChildren().addAll(priceSection, timeSection);

        card.getChildren().addAll(topRow, spacer, bottomRow);
        return card;
    }

    private void navigateToDetail(String auctionId) {
        if (auctionId != null) {
            AppContext.setSelectedAuctionId(auctionId);
        }
        if (AppNavigator.getCurrentView() == AppView.AUCTION_DETAIL) return;
        AppNavigator.navigateTo(AppView.AUCTION_DETAIL);
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "$0";
        return "$" + NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    private String formatTimeLeft(LocalDateTime endTime) {
        if (endTime == null) return "N/A";
        long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), endTime);
        long hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), endTime);
        long minutesLeft = ChronoUnit.MINUTES.between(LocalDateTime.now(), endTime);

        if (daysLeft > 0) return daysLeft + " day" + (daysLeft > 1 ? "s" : "");
        if (hoursLeft > 0) return hoursLeft + " hour" + (hoursLeft > 1 ? "s" : "");
        if (minutesLeft > 0) return minutesLeft + " min" + (minutesLeft > 1 ? "s" : "");
        return "Ended";
    }
}
