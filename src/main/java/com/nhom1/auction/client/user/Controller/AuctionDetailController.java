package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.PlaceBidResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.utils.AppContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.math.BigDecimal;

public class AuctionDetailController {

	private final BiddingClientService biddingService = new BiddingClientService();
	private final ObjectMapper mapper = new ObjectMapper();

	@FXML
	private TextField txtBidInput; // bound in FXML

	@FXML
	private Button btnBid; // bound in FXML

	@FXML
	private Label lblCurrentBid; // newly added for real-time updates

	@FXML
	private Label lblMinIncrement; // newly added for real-time updates

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
					Platform.runLater(
							() -> System.err.println("Failed to load auction detail: " + ex.getCause().getMessage()));
					return null;
				});

		// Register real-time push handler for bid updates
		ServerConnection.getInstance().registerPushHandler(
			MessageType.PUSH_BID_UPDATE,
			json -> handleBidUpdatePush(json)
		);
	}

	/**
	 * Handle incoming real-time bid update push notification
	 * Payload format: { "auctionId": "...", "currentHighestBid": 10000, "highestBidderId": "..." }
	 */
	private void handleBidUpdatePush(String json) {
		try {
			JsonNode node = mapper.readTree(json);
			String auctionId = node.has("auctionId") ? node.get("auctionId").asText() : null;
			
			// Only update if this push is for the current auction
			String currentAuctionId = AppContext.getSelectedAuctionId();
			if (auctionId == null || !auctionId.equals(currentAuctionId)) {
				return;
			}

			// Extract bid information
			BigDecimal newBid = null;
			if (node.has("currentHighestBid")) {
				newBid = new BigDecimal(node.get("currentHighestBid").asText());
			}

			// Update UI on FX thread
			final BigDecimal bid = newBid;
			Platform.runLater(() -> {
				if (bid != null && lblCurrentBid != null) {
					lblCurrentBid.setText(formatMoney(bid));
					System.out.println("[AuctionDetail] Updated bid to: " + bid);
				}
			});

		} catch (Exception e) {
			System.err.println("Error parsing bid update push: " + e.getMessage());
		}
	}

	private void applyDetail(AuctionDetailDto dto) {
		if (dto == null)
			return;
		// UI updates: populate labels with auction details
		if (lblCurrentBid != null && dto.getCurrentHighestBid() != null) {
			lblCurrentBid.setText(formatMoney(dto.getCurrentHighestBid()));
		}
		if (lblMinIncrement != null && dto.getMinBidIncrement() != null) {
			lblMinIncrement.setText(formatMoney(dto.getMinBidIncrement()));
		}
	}

	private void onPlaceBid() {
		String auctionId = AppContext.getSelectedAuctionId();
		if (auctionId == null)
			return;

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
		if (resp == null)
			return;
		// simple UX: navigate back to browse or refresh detail
		AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
	}

	private void showError(String title, String message) {
		System.err.println(title + ": " + message);
	}

	private String formatMoney(BigDecimal amount) {
		if (amount == null) return "$0";
		return "$" + amount.toPlainString();
	}
}
