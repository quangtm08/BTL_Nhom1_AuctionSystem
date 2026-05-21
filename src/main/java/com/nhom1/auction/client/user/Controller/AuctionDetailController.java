package com.nhom1.auction.client.user.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.client.user.service.AutoBidClientService;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigDetailResponse;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
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
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AuctionDetailController {

	private final BiddingClientService biddingService = new BiddingClientService();
	private final AutoBidClientService autoBidClientService = new AutoBidClientService();
	private final ObjectMapper mapper = new ObjectMapper();
	private static final DateTimeFormatter BID_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm dd/MM");
	private Label activeAutoBidCurrentBidLabel;

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
	private Button btnAutoBid;
	@FXML
	private Button btnCancelAutoBid;

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
		if (btnAutoBid != null) {
			btnAutoBid.setOnAction(e -> onConfigureAutoBid());
		}
		if (btnCancelAutoBid != null) {
			btnCancelAutoBid.setOnAction(e -> onCancelAutoBid());
		}

		ServerConnection.getInstance().registerPushHandler(
			MessageType.PUSH_BID_UPDATE,
			json -> handleBidUpdatePush(json)
		);
	}

	private void handleBidUpdatePush(String json) {
		try {
			JsonNode root = mapper.readTree(json);
			JsonNode node = root.has("payload") && !root.get("payload").isNull()
				? root.get("payload") : root;
			String auctionId = node.has("auctionId") ? node.get("auctionId").asText() : null;

			String currentAuctionId = AppContext.getSelectedAuctionId();
			if (auctionId == null || !auctionId.equals(currentAuctionId)) {
				return;
			}

			BigDecimal newBid = null;
			if (node.has("newHighestBid")) {
				newBid = new BigDecimal(node.get("newHighestBid").asText());
			}

			final BigDecimal bid = newBid;
			Platform.runLater(() -> {
				if (bid != null && lblCurrentBid != null) {
					lblCurrentBid.setText(formatMoney(bid));
				}
				if (bid != null && activeAutoBidCurrentBidLabel != null) {
					activeAutoBidCurrentBidLabel.setText(formatMoney(bid));
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
					// Hiển thị lỗi từ server (vd: bid thấp hơn minimum, auction đã kết thúc) lên UI.
					String msg = (ex != null && ex.getCause() != null) ? ex.getCause().getMessage()
						: (ex != null ? ex.getMessage() : "Bid failed");
					Platform.runLater(() -> showBidError(msg));
					return null;
				});
	}

	private void onConfigureAutoBid() {
		String auctionId = AppContext.getSelectedAuctionId();
		if (auctionId == null || auctionId.isBlank()) {
			return;
		}
		BigDecimal increment = parseDisplayedMoney(lblMinIncrement != null ? lblMinIncrement.getText() : null);
		if (increment == null || increment.compareTo(BigDecimal.ZERO) <= 0) {
			showBidError("Could not read increment value from auction.");
			return;
		}

		autoBidClientService.getConfig(auctionId)
			.thenAccept(cfg -> Platform.runLater(() -> showAutoBidDialog(auctionId, increment, cfg)))
			.exceptionally(ex -> {
				String msg = (ex != null && ex.getCause() != null) ? ex.getCause().getMessage()
					: (ex != null ? ex.getMessage() : "Failed to load auto-bid config");
				Platform.runLater(() -> showBidError(msg));
				return null;
			});
	}

	private void onCancelAutoBid() {
		String auctionId = AppContext.getSelectedAuctionId();
		if (auctionId == null || auctionId.isBlank()) return;
		autoBidClientService.deleteConfig(auctionId)
			.thenAccept(resp -> Platform.runLater(() -> handleDeleteAutoBid(resp)))
			.exceptionally(ex -> {
				String msg = (ex != null && ex.getCause() != null) ? ex.getCause().getMessage()
					: (ex != null ? ex.getMessage() : "Failed to cancel auto-bid");
				Platform.runLater(() -> showBidError(msg));
				return null;
			});
	}

	private void handleDeleteAutoBid(AutoBidConfigResponse resp) {
		if (resp == null) return;
		showBidError("Auto-bid status: " + resp.getStatus());
	}

	private void showAutoBidDialog(String auctionId, BigDecimal increment, AutoBidConfigDetailResponse cfg) {
		Dialog<BigDecimal> dialog = new Dialog<>();
		dialog.setTitle("Configure Auto-bid");
		dialog.setHeaderText("Set your bid limit for this auction");

		ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
		ButtonType closeButtonType = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
		dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, closeButtonType);

		GridPane grid = new GridPane();
		grid.setHgap(10);
		grid.setVgap(10);

		Label currentBidLabel = new Label(lblCurrentBid != null ? lblCurrentBid.getText() : "$0");
		activeAutoBidCurrentBidLabel = currentBidLabel;
		Label incrementLabel = new Label("$" + increment.toPlainString());
		TextField maxField = new TextField();
		maxField.setPromptText("ENTER YOUR BID LIMIT");
		maxField.setTextFormatter(new TextFormatter<String>(change ->
			change.getControlNewText().matches("\\d*(\\.\\d{0,2})?") ? change : null
		));

		if (cfg != null && cfg.isConfigured() && cfg.getMaxAmount() != null) {
			maxField.setText(cfg.getMaxAmount());
		}

		grid.addRow(0, new Label("Current highest bid:"), currentBidLabel);
		grid.addRow(1, new Label("Increment:"), incrementLabel);
		grid.addRow(2, new Label("Max amount:"), maxField);
		dialog.getDialogPane().setContent(grid);

		dialog.setResultConverter(button -> {
			if (button == saveButtonType) {
				try {
					return new BigDecimal(maxField.getText().trim());
				} catch (Exception e) {
					return null;
				}
			}
			return null;
		});

		Optional<BigDecimal> result = dialog.showAndWait();
		activeAutoBidCurrentBidLabel = null;
		result.ifPresent(maxAmount -> autoBidClientService.saveConfig(auctionId, maxAmount, increment)
			.thenAccept(resp -> Platform.runLater(() -> {
				if (resp != null) {
					showBidError("Auto-bid status: " + resp.getStatus());
				}
			}))
			.exceptionally(ex -> {
				String msg = (ex != null && ex.getCause() != null) ? ex.getCause().getMessage()
					: (ex != null ? ex.getMessage() : "Failed to save auto-bid config");
				Platform.runLater(() -> showBidError(msg));
				return null;
			}));
	}

	private void handlePlaceBidSuccess(PlaceBidResponse resp) {
		if (resp == null)
			return;
		if (txtBidInput != null)
			txtBidInput.setText("");
	}

	private String formatMoney(BigDecimal amount) {
		if (amount == null) return "$0";
		return "$" + NumberFormat.getNumberInstance(Locale.US).format(amount);
	}

	private BigDecimal parseDisplayedMoney(String display) {
		if (display == null || display.isBlank()) return null;
		String normalized = display.replace("$", "").replace(",", "").trim();
		try {
			return new BigDecimal(normalized);
		} catch (Exception e) {
			return null;
		}
	}
}
