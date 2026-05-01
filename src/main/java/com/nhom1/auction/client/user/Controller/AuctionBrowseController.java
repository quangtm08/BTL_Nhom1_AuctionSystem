package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.bidding.ListAuctionsResponse;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class AuctionBrowseController {

	private final BiddingClientService biddingService = new BiddingClientService();

	@FXML
	private HBox mainContainer; // present in FXML as fx:id

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
		// For now set selected auction to first item to simplify navigation flows
		String firstAuctionId = resp.getAuctions().get(0).getId();
		com.nhom1.auction.common.utils.AppContext.setSelectedAuctionId(firstAuctionId);
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
}
