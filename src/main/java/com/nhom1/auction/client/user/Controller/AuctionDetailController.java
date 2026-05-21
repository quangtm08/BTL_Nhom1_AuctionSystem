package com.nhom1.auction.client.user.controller;

import java.math.BigDecimal;
import java.util.List;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.service.ClientPushService;
import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.client.util.DisplayFormatters;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.BidSummaryDto;
import com.nhom1.auction.common.dto.bidding.PlaceBidResponse;
import com.nhom1.auction.common.dto.notification.BidUpdateEvent;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.utils.AppContext;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class AuctionDetailController {
	private static final double IMAGE_BOX_WIDTH = 420;
	private static final double IMAGE_BOX_HEIGHT = 320;
	private static final int MAX_VISIBLE_BID_ROWS = 8;

	private final BiddingClientService biddingService = new BiddingClientService();
	private final ClientPushService pushService = ClientPushService.getInstance();

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

	@FXML
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
				.thenAccept(dto -> Platform.runLater(() -> {
					applyDetail(dto);
					setLoadingState(false);
				}))
				.exceptionally(ex -> {
					Platform.runLater(() -> {
						setLoadingState(false);
						Throwable cause = BaseClientService.extractFailure(ex);
						String message = cause.getMessage();
						System.err.println("Failed to load auction detail: " + message);
					});
					return null;
				});

		btnBack.setOnAction(e -> AppNavigator.navigateTo(AppView.AUCTION_BROWSE));

		pushService.onBidUpdate(this::handleBidUpdatePush);
	}

	private void handleBidUpdatePush(BidUpdateEvent event) {
		String currentAuctionId = AppContext.getSelectedAuctionId();
		if (event == null || event.getAuctionId() == null || !event.getAuctionId().equals(currentAuctionId)) {
			return;
		}

		BigDecimal bid = event.getNewHighestBid();
		Platform.runLater(() -> {
			if (bid != null && lblCurrentBid != null) {
				lblCurrentBid.setText(DisplayFormatters.money(bid));
			}
		});

		biddingService.getAuctionDetail(currentAuctionId)
			.thenAccept(dto -> Platform.runLater(() -> {
				if (dto != null && dto.getBidHistory() != null) {
					renderBidHistory(dto.getBidHistory());
				}
			}));
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
			lblCurrentBid.setText(DisplayFormatters.money(currentBid));
		}
		if (lblMinIncrement != null && dto.getMinBidIncrement() != null)
			lblMinIncrement.setText(DisplayFormatters.money(dto.getMinBidIncrement()));
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
				Platform.runLater(this::clearPrimaryImage);
			}
		});

		if (image.isError()) {
			clearPrimaryImage();
		} else if (image.getProgress() >= 1.0) {
			applyCoverViewport(image);
		}
	}

	private void clearPrimaryImage() {
		itemImageView.setImage(null);
		itemImageView.setViewport(null);
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
		int startIndex = Math.max(0, history.size() - MAX_VISIBLE_BID_ROWS);
		int rank = 1;
		for (int i = history.size() - 1; i >= startIndex; i--) {
			BidSummaryDto bid = history.get(i);

			HBox row = new HBox(10);
			row.getStyleClass().add("bid-row");
			row.setAlignment(Pos.CENTER_LEFT);

			Label rankLabel = new Label("#" + rank++);
			rankLabel.getStyleClass().add("bid-rank");

			Label bidderName = new Label(bid.getBidderName() != null ? bid.getBidderName() : "—");
			bidderName.getStyleClass().add("bid-rank");
			HBox.setHgrow(bidderName, Priority.SOMETIMES);

			Label amount = new Label(DisplayFormatters.money(bid.getAmount()));
			amount.getStyleClass().add("bid-amount");
			HBox.setHgrow(amount, Priority.ALWAYS);

			Label type = new Label(bid.getBidType() != null ? bid.getBidType().name() : "");
			type.getStyleClass().addAll("bid-type",
				bid.getBidType() == BidType.AUTO ? "bid-type-auto" : "bid-type-manual");

			Label time = new Label(DisplayFormatters.bidTime(bid.getCreatedAt()));
			time.getStyleClass().add("bid-time");

			row.getChildren().addAll(rankLabel, bidderName, amount, type, time);
			bidHistoryList.getChildren().add(row);
		}
	}

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
					Throwable cause = BaseClientService.extractFailure(ex);
					String msg = cause.getMessage() != null ? cause.getMessage() : "Bid failed";
					Platform.runLater(() -> showBidError(msg));
					return null;
				});
	}

	private void handlePlaceBidSuccess(PlaceBidResponse resp) {
		if (resp == null)
			return;
		if (txtBidInput != null)
			txtBidInput.setText("");
	}

}
