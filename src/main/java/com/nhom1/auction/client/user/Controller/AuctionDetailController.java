package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.PlaceBidResponse;
import com.nhom1.auction.common.utils.AppContext;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.math.BigDecimal;

public class AuctionDetailController {

	private final BiddingClientService biddingService = new BiddingClientService();

	@FXML
	private TextField txtBidInput; // bound in FXML

	@FXML
	private Button btnBid; // bound in FXML

	@FXML
	public void initialize() {
		// wire bid button if present
		if (btnBid != null) {
			btnBid.setOnAction(e -> onPlaceBid());
		}

		// load auction details if selected
		String sel = AppContext.getSelectedAuctionId();
		if (sel == null || sel.isBlank()) {
			AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
			return;
		}

		biddingService.getAuctionDetail(sel)
			.thenAccept(dto -> Platform.runLater(() -> applyDetail(dto)))
			.exceptionally(ex -> {
				Platform.runLater(() -> System.err.println("Failed to load auction detail: " + ex.getCause().getMessage()));
				return null;
			});
	}

	private void applyDetail(AuctionDetailDto dto) {
		if (dto == null) return;
		// UI updates: FXML lacks fx:id for many fields; keep minimal
	}

	private void onPlaceBid() {
		String auctionId = AppContext.getSelectedAuctionId();
		if (auctionId == null) return;

		String text = txtBidInput != null ? txtBidInput.getText() : null;
		if (text == null || text.isBlank()) {
			showError("Invalid bid", "Please enter a bid amount.");
			return;
		}

		BigDecimal amount;
		try {
			amount = new BigDecimal(text.trim());
		} catch (Exception ex) {
			showError("Invalid bid", "Enter a valid number.");
			return;
		}

		biddingService.placeBid(auctionId, amount)
			.thenAccept(resp -> Platform.runLater(() -> handlePlaceBidSuccess(resp)))
			.exceptionally(ex -> {
				Platform.runLater(() -> showError("Bid failed", ex.getCause().getMessage()));
				return null;
			});
	}

	private void handlePlaceBidSuccess(PlaceBidResponse resp) {
		if (resp == null) return;
		// simple UX: navigate back to browse or refresh detail
		AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
	}

	private void showError(String title, String message) {
		System.err.println(title + ": " + message);
	}
}
