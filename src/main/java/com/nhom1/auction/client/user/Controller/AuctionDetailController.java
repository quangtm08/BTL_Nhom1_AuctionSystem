package com.nhom1.auction.client.user.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AuctionDetailController {
	private static final double IMAGE_BOX_WIDTH = 420;
	private static final double IMAGE_BOX_HEIGHT = 320;

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
	private Label lblAutoBidStatus;

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
	private Button btnViewBidHistory;

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
	private ImageView itemImageView;

	@FXML
	private VBox contentBox;

	@FXML
	private VBox loadingBox;

	@FXML
	public void initialize() {
		if (btnBid != null) {
			btnBid.setOnAction(e -> onPlaceBid());
		}
		setLoadingState(true);

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
				.thenCompose(dto -> waitForPrimaryImageLoaded(dto).thenApply(ignored -> dto))
				.thenAccept(dto -> Platform.runLater(() -> {
					applyDetail(dto);
					setLoadingState(false);
				}))
				.exceptionally(ex -> {
					Platform.runLater(() -> {
						setLoadingState(false);
						String message = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
						System.err.println("Failed to load auction detail: " + message);
					});
					return null;
				});

		btnBack.setOnAction(e -> AppNavigator.navigateTo(AppView.AUCTION_BROWSE));
		if (btnAutoBid != null) {
			btnAutoBid.setOnAction(e -> onConfigureAutoBid());
		}
		if (btnCancelAutoBid != null) {
			btnCancelAutoBid.setOnAction(e -> onCancelAutoBid());
		}

		if (btnViewBidHistory != null) {
			btnViewBidHistory.setOnAction(e -> AppNavigator.navigateTo(AppView.BID_HISTORY_CHART));
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
				clearAutoBidStatus();
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
		if (lblCurrentBid != null) {
			BigDecimal currentBid = dto.getCurrentHighestBid();
			if (currentBid == null || currentBid.compareTo(BigDecimal.ZERO) <= 0) {
				currentBid = dto.getStartingPrice();
			}
			lblCurrentBid.setText(formatMoney(currentBid));
		}
		if (lblMinIncrement != null && dto.getMinBidIncrement() != null)
			lblMinIncrement.setText(formatMoney(dto.getMinBidIncrement()));
		boolean isOwnAuction = AppContext.getCurrentUser() != null
			&& AppContext.getCurrentUser().getUserID() != null
			&& dto.getSellerID() != null
			&& AppContext.getCurrentUser().getUserID().equals(dto.getSellerID());
		if (btnBid != null) {
			btnBid.setDisable(isOwnAuction);
		}
		if (txtBidInput != null) {
			txtBidInput.setDisable(isOwnAuction);
		}
		if (isOwnAuction) {
			showBidError("You cannot bid on your own auction.");
		} else {
			clearBidError();
		}
		if (bidHistoryList != null && dto.getBidHistory() != null)
			renderBidHistory(dto.getBidHistory());
		if (itemImageView != null) {
			String imageUrl = (dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()) ? dto.getImageUrls().get(0) : null;
			if (imageUrl != null && !imageUrl.isBlank()) {
				loadPrimaryImage(imageUrl);
			} else {
				itemImageView.setImage(null);
				itemImageView.setViewport(null);
			}
		}
	}

	private CompletableFuture<Void> waitForPrimaryImageLoaded(AuctionDetailDto dto) {
		if (dto == null || dto.getImageUrls() == null || dto.getImageUrls().isEmpty()) {
			return CompletableFuture.completedFuture(null);
		}
		String imageUrl = dto.getImageUrls().get(0);
		if (imageUrl == null || imageUrl.isBlank()) {
			return CompletableFuture.completedFuture(null);
		}

		CompletableFuture<Void> loaded = new CompletableFuture<>();
		Image image = new Image(imageUrl, IMAGE_BOX_WIDTH, IMAGE_BOX_HEIGHT, true, true, true);
		image.progressProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue.doubleValue() >= 1.0 && !loaded.isDone()) {
				loaded.complete(null);
			}
		});
		image.errorProperty().addListener((obs, wasError, isError) -> {
			if (Boolean.TRUE.equals(isError) && !loaded.isDone()) {
				loaded.complete(null);
			}
		});
		return loaded;
	}

	private void setLoadingState(boolean loading) {
		if (btnBid != null) btnBid.setDisable(loading);
		if (txtBidInput != null) txtBidInput.setDisable(loading);
		if (lblTitle != null && loading) lblTitle.setText("Loading...");
		if (contentBox != null) {
			contentBox.setVisible(!loading);
			contentBox.setManaged(!loading);
		}
		if (loadingBox != null) {
			loadingBox.setVisible(loading);
			loadingBox.setManaged(loading);
		}
	}

	private void loadPrimaryImage(String imageUrl) {
		Image image = new Image(imageUrl, IMAGE_BOX_WIDTH, IMAGE_BOX_HEIGHT, true, true, true);
		itemImageView.setFitWidth(IMAGE_BOX_WIDTH);
		itemImageView.setFitHeight(IMAGE_BOX_HEIGHT);
		itemImageView.setPreserveRatio(false);
		itemImageView.setImage(image);

		image.progressProperty().addListener((obs, oldValue, newValue) -> {
			if (newValue.doubleValue() >= 1.0) {
				Platform.runLater(() -> applyCoverViewport(image));
			}
		});

		image.errorProperty().addListener((obs, wasError, isError) -> {
			if (Boolean.TRUE.equals(isError)) {
				Platform.runLater(() -> {
					itemImageView.setImage(null);
					itemImageView.setViewport(null);
				});
			}
		});
	}

	private void applyCoverViewport(Image image) {
		double width = image.getWidth();
		double height = image.getHeight();
		if (width <= 0 || height <= 0) {
			itemImageView.setViewport(null);
			return;
		}

		double targetRatio = IMAGE_BOX_WIDTH / IMAGE_BOX_HEIGHT;
		double sourceRatio = width / height;
		double viewWidth = width;
		double viewHeight = height;

		if (sourceRatio > targetRatio) {
			viewWidth = height * targetRatio;
		} else {
			viewHeight = width / targetRatio;
		}

		double x = (width - viewWidth) / 2.0;
		double y = (height - viewHeight) / 2.0;
		itemImageView.setViewport(new Rectangle2D(x, y, viewWidth, viewHeight));
	}

	private void renderBidHistory(List<BidSummaryDto> history) {
		bidHistoryList.getChildren().clear();
		if (history == null || history.isEmpty()) {
			return;
		}
		// Render full history newest-first. ScrollPane handles overflow.
		int rank = 1;
		for (int i = history.size() - 1; i >= 0; i--) {
			BidSummaryDto bid = history.get(i);

			HBox row = new HBox(10);
			row.getStyleClass().add("bid-row");
			row.setAlignment(Pos.CENTER_LEFT);

			Label rankLabel = new Label("#" + rank++);
			rankLabel.getStyleClass().add("bid-rank");

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

			row.getChildren().addAll(rankLabel, bidderName, amount, type, time);
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

	private void showAutoBidStatus(String message) {
		if (lblAutoBidStatus != null) {
			lblAutoBidStatus.setText(message);
			lblAutoBidStatus.setVisible(true);
			lblAutoBidStatus.setManaged(true);
		}
	}

	private void clearAutoBidStatus() {
		if (lblAutoBidStatus != null) {
			lblAutoBidStatus.setVisible(false);
			lblAutoBidStatus.setManaged(false);
			lblAutoBidStatus.setText("");
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
		clearAutoBidStatus();

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
		clearBidError();
		showAutoBidStatus("Auto-bid stopped.");
	}

	private void showAutoBidDialog(String auctionId, BigDecimal increment, AutoBidConfigDetailResponse cfg) {
		Dialog<BigDecimal> dialog = new Dialog<>();
		dialog.setTitle("Configure Auto-bid");
		dialog.setHeaderText("Set your bid limit for this auction");

		dialog.getDialogPane().getStylesheets().addAll(
			getClass().getResource("/css/client/common_ui.css").toExternalForm(),
			getClass().getResource("/css/client/auction_detail.css").toExternalForm()
		);

		// Sử dụng ButtonType.CLOSE ẩn để Dialog hoạt động bình thường
		dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
		Button defaultCloseBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
		if (defaultCloseBtn != null) {
			defaultCloseBtn.setVisible(false);
			defaultCloseBtn.setManaged(false);
		}

		GridPane grid = new GridPane();
		grid.setHgap(15);
		grid.setVgap(15);
		grid.setPadding(new Insets(15, 15, 15, 15));

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

		// Custom Button Bar sử dụng HBox để căn giữa tuyệt đối 100%
		HBox customButtonBar = new HBox(15);
		customButtonBar.setAlignment(Pos.CENTER);
		customButtonBar.getStyleClass().add("custom-button-bar");

		Button saveButton = new Button("Save");
		saveButton.getStyleClass().add("btn-primary");
		saveButton.setPrefWidth(100);

		Button closeButton = new Button("Close");
		closeButton.getStyleClass().add("btn-danger");
		closeButton.setPrefWidth(100);

		customButtonBar.getChildren().addAll(saveButton, closeButton);

		VBox dialogContent = new VBox(10);
		dialogContent.getChildren().addAll(grid, customButtonBar);
		dialog.getDialogPane().setContent(dialogContent);

		saveButton.setOnAction(e -> {
			try {
				dialog.setResult(new BigDecimal(maxField.getText().trim()));
				dialog.close();
			} catch (Exception ex) {
				// Bỏ qua nếu giá trị nhập không hợp lệ
			}
		});

		closeButton.setOnAction(e -> {
			dialog.setResult(null);
			dialog.close();
		});

		Optional<BigDecimal> result = dialog.showAndWait();
		activeAutoBidCurrentBidLabel = null;
		result.ifPresent(maxAmount -> autoBidClientService.saveConfig(auctionId, maxAmount, increment)
			.thenAccept(resp -> Platform.runLater(() -> {
				if (resp != null) {
					clearBidError();
					showAutoBidStatus("Auto-bid is active up to " + formatMoney(maxAmount) + ".");
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
