package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.service.ClientPushService;
import com.nhom1.auction.client.user.service.AutoBidClientService;
import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.client.util.CountdownAnimator;
import com.nhom1.auction.client.util.DisplayFormatters;
import com.nhom1.auction.client.util.SkeletonUtils;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigDetailResponse;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.BidSummaryDto;
import com.nhom1.auction.common.dto.bidding.PlaceBidResponse;
import com.nhom1.auction.common.dto.notification.AuctionTimeExtendedEvent;
import com.nhom1.auction.common.dto.notification.BidUpdateEvent;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.utils.AppContext;
import java.math.BigDecimal;
import java.util.List;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class AuctionDetailController {

  private static final double IMAGE_BOX_WIDTH = 420;
  private static final double IMAGE_BOX_HEIGHT = 320;

  private final BiddingClientService biddingService = new BiddingClientService();
  private final AutoBidClientService autoBidService = new AutoBidClientService();
  private final ClientPushService pushService = ClientPushService.getInstance();

  private Label activeAutoBidCurrentBidLabel;

  @FXML private TextField txtBidInput;

  @FXML private Label lblBidError;

  @FXML private Label lblAutoBidStatus;

  @FXML private Button btnBid;

  @FXML private Button btnAutoBid;

  @FXML private Button btnCancelAutoBid;

  @FXML private Button btnViewBidHistory;

  @FXML private Button btnSaveAutoBid;

  @FXML private Button btnCloseAutoBid;

  @FXML private VBox bidBox;

  @FXML private VBox autoBidConfigBox;

  @FXML private Label lblCurrentBid;

  @FXML private Label lblAutoBidCurrentBid;

  @FXML private Label lblAutoBidIncrement;

  @FXML private Label lblMinIncrement;

  @FXML private TextField txtAutoBidMax;

  @FXML private Button btnBack;

  @FXML private VBox bidHistoryList;

  @FXML private Label lblTitle;

  @FXML private Label lblShortDesc;

  @FXML private Label lblSellerName;

  @FXML private Label lblDescription;

  @FXML private ImageView itemImageView;

  @FXML private VBox contentBox;

  @FXML private VBox loadingBox;

  @FXML private Label lblCountdown;

  @FXML private Label lblCountdownLabel;

  @FXML private Label lblExtensionToast;

  private CountdownAnimator countdownAnimator;
  private PauseTransition extensionToastPause;
  private boolean isOwnAuction;

  @FXML
  public void initialize() {
    if (btnBid != null) {
      btnBid.setOnAction(e -> onPlaceBid());
    }
    if (btnAutoBid != null) {
      btnAutoBid.setOnAction(e -> onConfigureAutoBid());
    }
    if (btnCancelAutoBid != null) {
      btnCancelAutoBid.setOnAction(e -> onCancelAutoBid());
    }
    if (btnSaveAutoBid != null) {
      btnSaveAutoBid.setOnAction(e -> onSaveAutoBid());
    }
    if (btnCloseAutoBid != null) {
      btnCloseAutoBid.setOnAction(e -> hideAutoBidConfig());
    }
    if (btnViewBidHistory != null) {
      btnViewBidHistory.setOnAction(e -> AppNavigator.navigateTo(AppView.BID_HISTORY_CHART));
    }
    if (btnBack != null) {
      btnBack.setOnAction(e -> AppNavigator.navigateTo(AppView.AUCTION_BROWSE));
    }
    if (txtBidInput != null) {
      txtBidInput
          .focusedProperty()
          .addListener(
              (obs, wasFocused, isFocused) -> {
                if (isFocused) {
                  clearBidError();
                }
              });
    }
    if (txtAutoBidMax != null) {
      txtAutoBidMax.setTextFormatter(
          new TextFormatter<String>(
              change -> change.getControlNewText().matches("\\d*(\\.\\d{0,2})?") ? change : null));
      txtAutoBidMax
          .focusedProperty()
          .addListener(
              (obs, wasFocused, isFocused) -> {
                if (isFocused) {
                  clearBidError();
                }
              });
    }

    showLoading();
    loadAuctionDetail();

    pushService.onBidUpdate(this::handleBidUpdatePush);
    pushService.onAuctionTimeExtended(this::handleAuctionTimeExtendedPush);
  }

  private void loadAuctionDetail() {
    String selectedAuctionId = AppContext.getSelectedAuctionId();
    if (selectedAuctionId == null || selectedAuctionId.isBlank()) {
      AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
      return;
    }

    biddingService
        .getAuctionDetail(selectedAuctionId)
        .thenAccept(
            dto ->
                Platform.runLater(
                    () -> {
                      showContent();
                      applyDetail(dto);
                    }))
        .exceptionally(
            ex -> {
              Throwable cause = BaseClientService.extractFailure(ex);
              Platform.runLater(
                  () -> {
                    showContent();
                    System.err.println("Failed to load auction detail: " + cause.getMessage());
                  });
              return null;
            });
  }

  private void handleAuctionTimeExtendedPush(AuctionTimeExtendedEvent event) {
    String currentAuctionId = AppContext.getSelectedAuctionId();
    if (event == null
        || event.getAuctionId() == null
        || !event.getAuctionId().equals(currentAuctionId)) {
      return;
    }
    Platform.runLater(
        () -> {
          if (countdownAnimator != null && event.getNewEndTime() != null) {
            countdownAnimator.resetEndTime(event.getNewEndTime());
          }
          if (!isOwnAuction) {
            setBidSectionDisabled(false);
          }
          showExtensionToast();
        });
  }

  private void showExtensionToast() {
    if (lblExtensionToast == null) {
      return;
    }
    lblExtensionToast.setVisible(true);
    lblExtensionToast.setManaged(true);
    if (extensionToastPause != null) {
      extensionToastPause.stop();
    }
    extensionToastPause = new PauseTransition(Duration.seconds(3));
    extensionToastPause.setOnFinished(
        e -> {
          lblExtensionToast.setVisible(false);
          lblExtensionToast.setManaged(false);
        });
    extensionToastPause.play();
  }

  private void handleBidUpdatePush(BidUpdateEvent event) {
    String currentAuctionId = AppContext.getSelectedAuctionId();
    if (event == null
        || event.getAuctionId() == null
        || !event.getAuctionId().equals(currentAuctionId)) {
      return;
    }

    BigDecimal bid = event.getNewHighestBid();
    Platform.runLater(
        () -> {
          if (bid != null && lblCurrentBid != null) {
            lblCurrentBid.setText(DisplayFormatters.money(bid));
          }
          if (bid != null && activeAutoBidCurrentBidLabel != null) {
            activeAutoBidCurrentBidLabel.setText(DisplayFormatters.money(bid));
          }
          if (bid != null && lblAutoBidCurrentBid != null) {
            lblAutoBidCurrentBid.setText(DisplayFormatters.money(bid));
          }
          clearAutoBidStatus();
        });

    biddingService
        .getAuctionDetail(currentAuctionId)
        .thenAccept(
            dto ->
                Platform.runLater(
                    () -> {
                      if (dto != null && dto.getBidHistory() != null) {
                        renderBidHistory(dto.getBidHistory());
                      }
                    }))
        .exceptionally(
            ex -> {
              Throwable cause = BaseClientService.extractFailure(ex);
              System.err.println("Failed to refresh bid history: " + cause.getMessage());
              return null;
            });
  }

  private void applyDetail(AuctionDetailDto dto) {
    if (dto == null) {
      return;
    }
    if (lblTitle != null) {
      lblTitle.setText(dto.getItemName() != null ? dto.getItemName() : "");
    }
    if (lblShortDesc != null) {
      lblShortDesc.setText(dto.getItemDescription() != null ? dto.getItemDescription() : "");
    }
    if (lblDescription != null) {
      lblDescription.setText(dto.getItemDescription() != null ? dto.getItemDescription() : "");
    }
    if (lblSellerName != null) {
      lblSellerName.setText(dto.getSellerName() != null ? dto.getSellerName() : "Unknown");
    }
    if (lblCurrentBid != null) {
      BigDecimal currentBid = dto.getCurrentHighestBid();
      if (currentBid == null || currentBid.compareTo(BigDecimal.ZERO) <= 0) {
        currentBid = dto.getStartingPrice();
      }
      lblCurrentBid.setText(DisplayFormatters.money(currentBid));
    }
    if (lblMinIncrement != null && dto.getMinBidIncrement() != null) {
      lblMinIncrement.setText(DisplayFormatters.money(dto.getMinBidIncrement()));
    }

    isOwnAuction =
        AppContext.getCurrentUser() != null
            && AppContext.getCurrentUser().getUserID() != null
            && dto.getSellerID() != null
            && AppContext.getCurrentUser().getUserID().equals(dto.getSellerID());

    applyBidAvailability(dto);
    if (isOwnAuction) {
      showBidError("You cannot bid on your own auction.");
    } else if (dto.getStatus() == AuctionStatus.OPEN) {
      showBidError("Bidding opens when this auction starts.");
    } else if (DisplayFormatters.isEnded(dto.getStatus())) {
      showBidError("This auction has ended.");
    } else {
      clearBidError();
    }

    if (bidHistoryList != null && dto.getBidHistory() != null) {
      renderBidHistory(dto.getBidHistory());
    }

    applyCountdown(dto);
    if (itemImageView != null) {
      String imageUrl =
          dto.getImageUrls() != null && !dto.getImageUrls().isEmpty()
              ? dto.getImageUrls().get(0)
              : null;
      if (imageUrl != null && !imageUrl.isBlank()) {
        loadPrimaryImage(imageUrl);
      } else {
        clearPrimaryImage();
      }
    }
  }

  private void applyBidAvailability(AuctionDetailDto dto) {
    boolean notStarted = dto.getStatus() == AuctionStatus.OPEN;
    boolean ended = DisplayFormatters.isEnded(dto.getStatus());
    setBidSectionDisabled(isOwnAuction || notStarted || ended);
  }

  private void applyCountdown(AuctionDetailDto dto) {
    if (lblCountdown == null) {
      return;
    }
    if (countdownAnimator != null) {
      countdownAnimator.stop();
      countdownAnimator = null;
    }
    if (dto.getStatus() == AuctionStatus.OPEN && dto.getStartTime() != null) {
      if (lblCountdownLabel != null) {
        lblCountdownLabel.setText("Starts in");
      }
      countdownAnimator = new CountdownAnimator(lblCountdown, dto.getStartTime());
      countdownAnimator.setOnExpired(this::loadAuctionDetail);
      countdownAnimator.start();
      return;
    }
    if (dto.getEndTime() != null) {
      if (lblCountdownLabel != null) {
        lblCountdownLabel.setText("Time remaining");
      }
      countdownAnimator = new CountdownAnimator(lblCountdown, dto.getEndTime());
      countdownAnimator.setOnExpired(
          () -> {
            setBidSectionDisabled(true);
            loadAuctionDetail();
          });
      countdownAnimator.start();
    }
  }

  private void showLoading() {
    setBidControlsDisabled(true);
    if (lblTitle != null) {
      lblTitle.setText("Loading...");
    }
    SkeletonUtils.showLoading(loadingBox, contentBox);
  }

  private void showContent() {
    setBidControlsDisabled(false);
    SkeletonUtils.showContent(loadingBox, contentBox);
  }

  private void setBidControlsDisabled(boolean disabled) {
    if (btnBid != null) {
      btnBid.setDisable(disabled);
    }
    if (btnAutoBid != null) {
      btnAutoBid.setDisable(disabled);
    }
    if (btnCancelAutoBid != null) {
      btnCancelAutoBid.setDisable(disabled);
    }
    if (txtBidInput != null) {
      txtBidInput.setDisable(disabled);
    }
  }

  private void setBidSectionDisabled(boolean disabled) {
    if (bidBox != null) {
      bidBox.setDisable(disabled);
    } else {
      setBidControlsDisabled(disabled);
    }
  }

  private void loadPrimaryImage(String imageUrl) {
    Image image = new Image(imageUrl, IMAGE_BOX_WIDTH, IMAGE_BOX_HEIGHT, true, true, true);
    itemImageView.setFitWidth(IMAGE_BOX_WIDTH);
    itemImageView.setFitHeight(IMAGE_BOX_HEIGHT);
    itemImageView.setPreserveRatio(false);
    itemImageView.setImage(image);

    image
        .progressProperty()
        .addListener(
            (obs, oldValue, newValue) -> {
              if (newValue.doubleValue() >= 1.0) {
                Platform.runLater(() -> applyCoverViewport(image));
              }
            });

    image
        .errorProperty()
        .addListener(
            (obs, wasError, isError) -> {
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

    int rank = 1;
    for (int i = history.size() - 1; i >= 0; i--) {
      BidSummaryDto bid = history.get(i);

      HBox row = new HBox(10);
      row.getStyleClass().add("bid-row");
      row.setAlignment(Pos.CENTER_LEFT);

      Label rankLabel = new Label("#" + rank++);
      rankLabel.getStyleClass().add("bid-rank");

      Label bidderName = new Label(bid.getBidderName() != null ? bid.getBidderName() : "-");
      bidderName.getStyleClass().add("bid-rank");
      HBox.setHgrow(bidderName, Priority.SOMETIMES);

      Label amount = new Label(DisplayFormatters.money(bid.getAmount()));
      amount.getStyleClass().add("bid-amount");
      HBox.setHgrow(amount, Priority.ALWAYS);

      Label type = new Label(bid.getBidType() != null ? bid.getBidType().name() : "");
      type.getStyleClass()
          .addAll(
              "bid-type", bid.getBidType() == BidType.AUTO ? "bid-type-auto" : "bid-type-manual");

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
    if (auctionId == null) {
      return;
    }

    String text = txtBidInput != null ? txtBidInput.getText() : null;
    if (text == null || text.isBlank()) {
      showBidError("Please enter a bid amount.");
      return;
    }

    BigDecimal amount;
    try {
      amount = new BigDecimal(text.trim());
    } catch (Exception ex) {
      showBidError("Invalid amount - please enter a number.");
      return;
    }

    biddingService
        .placeBid(auctionId, amount)
        .thenAccept(resp -> Platform.runLater(() -> handlePlaceBidSuccess(resp)))
        .exceptionally(
            ex -> {
              Throwable cause = BaseClientService.extractFailure(ex);
              String message = cause.getMessage() != null ? cause.getMessage() : "Bid failed";
              Platform.runLater(() -> showBidError(message));
              return null;
            });
  }

  private void onConfigureAutoBid() {
    String auctionId = AppContext.getSelectedAuctionId();
    if (auctionId == null || auctionId.isBlank()) {
      return;
    }

    BigDecimal increment =
        parseDisplayedMoney(lblMinIncrement != null ? lblMinIncrement.getText() : null);
    if (increment == null || increment.compareTo(BigDecimal.ZERO) <= 0) {
      showBidError("Could not read increment value from auction.");
      return;
    }
    clearAutoBidStatus();

    autoBidService
        .getConfig(auctionId)
        .thenAccept(config -> Platform.runLater(() -> showAutoBidConfig(increment, config)))
        .exceptionally(
            ex -> {
              Throwable cause = BaseClientService.extractFailure(ex);
              String message =
                  cause.getMessage() != null
                      ? cause.getMessage()
                      : "Failed to load auto-bid config";
              Platform.runLater(() -> showBidError(message));
              return null;
            });
  }

  private void onCancelAutoBid() {
    String auctionId = AppContext.getSelectedAuctionId();
    if (auctionId == null || auctionId.isBlank()) {
      return;
    }

    autoBidService
        .deleteConfig(auctionId)
        .thenAccept(resp -> Platform.runLater(() -> handleDeleteAutoBid(resp)))
        .exceptionally(
            ex -> {
              Throwable cause = BaseClientService.extractFailure(ex);
              String message =
                  cause.getMessage() != null ? cause.getMessage() : "Failed to cancel auto-bid";
              Platform.runLater(() -> showBidError(message));
              return null;
            });
  }

  private void handleDeleteAutoBid(AutoBidConfigResponse response) {
    if (response == null) {
      return;
    }
    clearBidError();
    showAutoBidStatus("Auto-bid stopped.");
    hideAutoBidConfig();
  }

  private void showAutoBidConfig(BigDecimal increment, AutoBidConfigDetailResponse config) {
    clearBidError();
    if (lblAutoBidCurrentBid != null) {
      lblAutoBidCurrentBid.setText(lblCurrentBid != null ? lblCurrentBid.getText() : "$0");
      activeAutoBidCurrentBidLabel = lblAutoBidCurrentBid;
    }
    if (lblAutoBidIncrement != null) {
      lblAutoBidIncrement.setText(DisplayFormatters.money(increment));
    }
    if (txtAutoBidMax != null) {
      txtAutoBidMax.clear();
    }
    if (config != null
        && config.isConfigured()
        && config.getMaxAmount() != null
        && txtAutoBidMax != null) {
      txtAutoBidMax.setText(config.getMaxAmount());
    }
    setAutoBidConfigVisible(true);
  }

  private void onSaveAutoBid() {
    String auctionId = AppContext.getSelectedAuctionId();
    if (auctionId == null || auctionId.isBlank()) {
      return;
    }
    BigDecimal increment =
        parseDisplayedMoney(lblAutoBidIncrement != null ? lblAutoBidIncrement.getText() : null);
    if (increment == null || increment.compareTo(BigDecimal.ZERO) <= 0) {
      showBidError("Could not read increment value from auction.");
      return;
    }

    BigDecimal maxAmount;
    try {
      String text = txtAutoBidMax != null ? txtAutoBidMax.getText() : "";
      maxAmount = new BigDecimal(text.trim());
      if (maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
        throw new NumberFormatException();
      }
    } catch (Exception ex) {
      showBidError("Please enter a valid auto-bid limit.");
      if (txtAutoBidMax != null) {
        txtAutoBidMax.getStyleClass().remove("bid-input-error");
        txtAutoBidMax.getStyleClass().add("bid-input-error");
      }
      return;
    }

    if (txtAutoBidMax != null) {
      txtAutoBidMax.getStyleClass().remove("bid-input-error");
    }
    if (btnSaveAutoBid != null) {
      btnSaveAutoBid.setDisable(true);
    }
    showAutoBidStatus("Saving auto-bid...");
    autoBidService
        .saveConfig(auctionId, maxAmount, increment)
        .thenAccept(
            resp ->
                Platform.runLater(
                    () -> {
                      if (btnSaveAutoBid != null) {
                        btnSaveAutoBid.setDisable(false);
                      }
                      if (resp != null) {
                        clearBidError();
                        showAutoBidStatus(
                            "Auto-bid is active up to " + DisplayFormatters.money(maxAmount) + ".");
                        hideAutoBidConfig();
                      }
                    }))
        .exceptionally(
            ex -> {
              Throwable cause = BaseClientService.extractFailure(ex);
              String message =
                  cause.getMessage() != null
                      ? cause.getMessage()
                      : "Failed to save auto-bid config";
              Platform.runLater(
                  () -> {
                    if (btnSaveAutoBid != null) {
                      btnSaveAutoBid.setDisable(false);
                    }
                    showBidError(message);
                  });
              return null;
            });
  }

  private void hideAutoBidConfig() {
    activeAutoBidCurrentBidLabel = null;
    setAutoBidConfigVisible(false);
  }

  private void setAutoBidConfigVisible(boolean visible) {
    if (autoBidConfigBox == null) {
      return;
    }
    autoBidConfigBox.setVisible(visible);
    autoBidConfigBox.setManaged(visible);
  }

  private void handlePlaceBidSuccess(PlaceBidResponse response) {
    if (response == null) {
      return;
    }
    if (txtBidInput != null) {
      txtBidInput.setText("");
    }
  }

  private BigDecimal parseDisplayedMoney(String display) {
    if (display == null || display.isBlank()) {
      return null;
    }
    String normalized = display.replace("$", "").replace(",", "").trim();
    try {
      return new BigDecimal(normalized);
    } catch (Exception e) {
      return null;
    }
  }
}
