package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.bidding.ListAuctionsResponse;

import javafx.animation.PauseTransition;
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
import javafx.util.Duration;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

public class AuctionBrowseController {

	private final BiddingClientService biddingService = new BiddingClientService();

	@FXML
	private HBox mainContainer; // present in FXML as fx:id

	@FXML
	private GridPane cardsGridPane; // newly added for dynamic card rendering

	@FXML
	public void initialize() {
		// Prefetch auctions for faster UX; UI binding left minimal to avoid heavy coupling here.
		biddingService.listAuctions()
			.thenAccept(resp -> Platform.runLater(() -> handleListResponse(resp)))
			.exceptionally(ex -> {
				Platform.runLater(() -> showError("Load auctions failed", ex.getCause().getMessage()));
				return null;
			});
	}

	private void handleListResponse(ListAuctionsResponse resp) {
		if (resp == null || resp.getAuctions() == null || resp.getAuctions().isEmpty()) return;
		
		// Render cards dynamically
		renderAuctionCards(resp.getAuctions());
		
		// For now set selected auction to first item to simplify navigation flows
		String firstAuctionId = resp.getAuctions().get(0).getId();
		com.nhom1.auction.common.utils.AppContext.setSelectedAuctionId(firstAuctionId);
	}

	/**
	 * Render auction cards dynamically in the GridPane
	 */
	private void renderAuctionCards(List<AuctionSummaryDto> auctions) {
		cardsGridPane.getChildren().clear();
		
		for (int i = 0; i < auctions.size(); i++) {
			AuctionSummaryDto auction = auctions.get(i);
			Node card = createAuctionCard(auction);
			int col = i % 2;
			int row = i / 2;
			cardsGridPane.add(card, col, row);
		}
	}

	/**
	 * Create a single auction card VBox with all components
	 */
	private Node createAuctionCard(AuctionSummaryDto dto) {
		VBox card = new VBox(12);
		card.getStyleClass().add("product-card");

		// Top row: Title + Status badge
		HBox topRow = new HBox();
		topRow.setAlignment(Pos.CENTER_LEFT);
		
		VBox titleBox = new VBox(2);
		Label title = new Label(dto.getItemName() != null ? dto.getItemName() : "Untitled");
		title.getStyleClass().add("card-title-text");
		Label category = new Label(dto.getItemCategory() != null ? dto.getItemCategory() : "Uncategorized");
		category.getStyleClass().add("card-sub-text");
		titleBox.getChildren().addAll(title, category);

		Label statusBadge = new Label(formatStatus(dto.getStatus()));
		statusBadge.getStyleClass().add("status-badge");
		
		HBox.setHgrow(titleBox, Priority.ALWAYS);
		topRow.getChildren().addAll(titleBox, statusBadge);

		// Spacer
		VBox spacer = new VBox();
		spacer.setPrefHeight(50.0);

		// Bottom row: Price + Time + Button
		HBox bottomRow = new HBox();
		bottomRow.setAlignment(Pos.BOTTOM_LEFT);
		bottomRow.setSpacing(10);

		VBox priceSection = new VBox(2);
		Label priceLabel = new Label("Current bid");
		priceLabel.getStyleClass().add("card-sub-text");
		Label priceValue = new Label(formatMoney(dto.getCurrentHighestBid()));
		priceValue.getStyleClass().add("card-price-text");
		priceValue.setMaxWidth(Double.MAX_VALUE);
		priceSection.getChildren().addAll(priceLabel, priceValue);

		VBox timeSection = new VBox(2);
		timeSection.setAlignment(Pos.BOTTOM_RIGHT);
		Label timeLeftLabel = new Label(formatTimeLeft(dto.getEndTime()));
		timeLeftLabel.getStyleClass().add("card-sub-text");
		
		Button bidButton = new Button("Raise bid");
		bidButton.getStyleClass().add("btn-action-green");
		timeSection.getChildren().addAll(timeLeftLabel, bidButton);

		HBox.setHgrow(priceSection, Priority.ALWAYS);
		bottomRow.getChildren().addAll(priceSection, timeSection);

		// Wire navigation button
		bidButton.setOnAction(e -> navigateToDetail(dto.getId()));

		// Assemble card
		card.getChildren().addAll(topRow, spacer, bottomRow);
		return card;
	}

	private void showError(String title, String message) {
		// lightweight: navigate to loading or show console; reuse AppNavigator to remain non-blocking
		System.err.println(title + ": " + message);
	}

	// Navigation helper used by UI buttons (can be wired later)
	public void navigateToDetail(String auctionId) {
		if (auctionId != null) com.nhom1.auction.common.utils.AppContext.setSelectedAuctionId(auctionId);
		if (AppNavigator.getCurrentView() == AppView.AUCTION_DETAIL) return;
		AppNavigator.navigateTo(AppView.LOADING);
		PauseTransition delay = new PauseTransition(Duration.seconds(0.4));
		delay.setOnFinished(e -> AppNavigator.navigateTo(AppView.AUCTION_DETAIL));
		delay.play();
	}

	// ================= HELPER METHODS =================
// Helper method để format status, money và thời gian còn lại cho hiển thị đẹp hơn
	private String formatStatus(Object status) {
		if (status == null) return "Unknown";
		return status.toString();
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
