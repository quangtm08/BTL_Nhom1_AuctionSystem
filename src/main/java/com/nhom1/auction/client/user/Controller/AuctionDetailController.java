package com.nhom1.auction.client.user.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.BidSummaryDto;
import com.nhom1.auction.common.dto.bidding.PlaceBidResponse;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.utils.AppContext;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AuctionDetailController {

	private final BiddingClientService biddingService = new BiddingClientService();
	private final ObjectMapper mapper = new ObjectMapper();
	private static final DateTimeFormatter BID_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");

	@FXML
	private TextField txtBidInput;

	@FXML
	private Label lblBidError;

	@FXML
	private Button btnBid;

	@FXML
	private Label lblCurrentBid;

	@FXML
	private Label lblMinIncrement;

	@FXML
	private Button btnBack;

	@FXML
	private VBox bidHistoryList;

	@FXML // Reusing the same label for title and item name for simplicity
	private Label lblTitle; 

	@FXML
	private Label lblShortDesc;

	@FXML
	private Label lblSellerName;

	@FXML
	private Label lblDescription;

	@FXML
	public void initialize() {
		if (btnBid != null) {
			btnBid.setOnAction(e -> onPlaceBid());
		}

		// Khi user click vào TextField (focus gained) → xóa lỗi ngay lập tức.
		// Dùng focusedProperty listener thay vì setOnMouseClicked để bắt cả trường hợp
		// user Tab vào field chứ không chỉ click chuột.
		if (txtBidInput != null) {
			txtBidInput.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
				if (isFocused) clearBidError();
			});
		}

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

		btnBack.setOnAction(e -> AppNavigator.navigateTo(AppView.AUCTION_BROWSE));

		ServerConnection.getInstance().registerPushHandler(
			MessageType.PUSH_BID_UPDATE,
			json -> handleBidUpdatePush(json)
		);
	}

	private void handleBidUpdatePush(String json) {
		try {
			JsonNode node = mapper.readTree(json);
			String auctionId = node.has("auctionId") ? node.get("auctionId").asText() : null;

			String currentAuctionId = AppContext.getSelectedAuctionId();
			if (auctionId == null || !auctionId.equals(currentAuctionId)) {
				return;
			}

			BigDecimal newBid = null;
			if (node.has("currentHighestBid")) {
				newBid = new BigDecimal(node.get("currentHighestBid").asText());
			}

			final BigDecimal bid = newBid;
			Platform.runLater(() -> {
				if (bid != null && lblCurrentBid != null) {
					lblCurrentBid.setText(formatMoney(bid));
				}
			});

			// Re-fetch full detail to refresh bid history
			biddingService.getAuctionDetail(currentAuctionId)
				.thenAccept(dto -> Platform.runLater(() -> {
					if (dto != null && dto.getBidHistory() != null) {
						renderBidHistory(dto.getBidHistory());
					}
				}));

		} catch (Exception e) {
			System.err.println("Error parsing bid update push: " + e.getMessage());
		}
	}

	private void applyDetail(AuctionDetailDto dto) {
		if (dto == null)
			return;
		if (lblTitle != null)
			lblTitle.setText(dto.getItemName() != null ? dto.getItemName() : "");
		if (lblShortDesc != null)
			lblShortDesc.setText(dto.getItemDescription() != null ? dto.getItemDescription() : "");
		if (lblDescription != null)
			lblDescription.setText(dto.getItemDescription() != null ? dto.getItemDescription() : "");
		if (lblSellerName != null)
			lblSellerName.setText(dto.getSellerName() != null ? dto.getSellerName() : "Unknown");
		if (lblCurrentBid != null && dto.getCurrentHighestBid() != null)
			lblCurrentBid.setText(formatMoney(dto.getCurrentHighestBid()));
		if (lblMinIncrement != null && dto.getMinBidIncrement() != null)
			lblMinIncrement.setText(formatMoney(dto.getMinBidIncrement()));
		if (bidHistoryList != null && dto.getBidHistory() != null)
			renderBidHistory(dto.getBidHistory());
	}

	private void renderBidHistory(List<BidSummaryDto> history) {
		bidHistoryList.getChildren().clear();
		for (int i = 0; i < history.size(); i++) {
			BidSummaryDto bid = history.get(i);

			HBox row = new HBox(10);
			row.getStyleClass().add("bid-row");
			row.setAlignment(Pos.CENTER_LEFT);

			Label rank = new Label("#" + (i + 1));
			rank.getStyleClass().add("bid-rank");

			Label bidderName = new Label(bid.getBidderName() != null ? bid.getBidderName() : "—");
			bidderName.getStyleClass().add("bid-rank");
			HBox.setHgrow(bidderName, Priority.SOMETIMES);

			Label amount = new Label(formatMoney(bid.getAmount()));
			amount.getStyleClass().add("bid-amount");
			HBox.setHgrow(amount, Priority.ALWAYS);

			Label type = new Label(bid.getBidType() != null ? bid.getBidType().name() : "");
			type.getStyleClass().addAll("bid-type",
				bid.getBidType() == BidType.AUTO ? "bid-type-auto" : "bid-type-manual");

			Label time = new Label(bid.getCreatedAt() != null ? bid.getCreatedAt().format(BID_TIME_FMT) : "");
			time.getStyleClass().add("bid-time");

			row.getChildren().addAll(rank, bidderName, amount, type, time);
			bidHistoryList.getChildren().add(row);
		}
	}

	/**
	 * Hiển thị thông báo lỗi ngay dưới TextField và đổi viền thành đỏ.
	 * managed=true để label chiếm không gian trong layout khi hiện.
	 */
	private void showBidError(String message) {
		if (lblBidError != null) {
			lblBidError.setText(message);
			lblBidError.setVisible(true);
			lblBidError.setManaged(true);
		}
		if (txtBidInput != null) {
			txtBidInput.getStyleClass().remove("bid-input-error");
			txtBidInput.getStyleClass().add("bid-input-error");
		}
	}

	/**
	 * Ẩn thông báo lỗi và khôi phục viền TextField về bình thường.
	 * managed=false để label không chiếm không gian trong layout khi ẩn.
	 * Được gọi khi TextField được focus (user bắt đầu nhập lại).
	 */
	private void clearBidError() {
		if (lblBidError != null) {
			lblBidError.setVisible(false);
			lblBidError.setManaged(false);
		}
		if (txtBidInput != null) {
			txtBidInput.getStyleClass().remove("bid-input-error");
		}
	}

	private void onPlaceBid() {
		String auctionId = AppContext.getSelectedAuctionId();
		if (auctionId == null)
			return;

		String text = txtBidInput != null ? txtBidInput.getText() : null;
		if (text == null || text.isBlank()) {
			showBidError("Please enter a bid amount.");
			return;
		}

		BigDecimal amount;
		try {
			amount = new BigDecimal(text.trim());
		} catch (Exception ex) {
			showBidError("Invalid amount — please enter a number.");
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
		AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
	}

	private void showError(String title, String message) {
		System.err.println(title + ": " + message);
	}

	private String formatMoney(BigDecimal amount) {
		if (amount == null) return "$0";
		return "$" + NumberFormat.getNumberInstance(Locale.US).format(amount);
	}
}
