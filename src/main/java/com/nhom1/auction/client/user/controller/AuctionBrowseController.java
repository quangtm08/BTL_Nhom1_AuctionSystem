package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.service.ClientPushService;
import com.nhom1.auction.client.user.controller.components.AuctionCardComponentController;
import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.client.util.DisplayFormatters;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.notification.AuctionDeletedEvent;
import com.nhom1.auction.common.dto.notification.BidUpdateEvent;
import com.nhom1.auction.common.utils.AppContext;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Duration;

public class AuctionBrowseController {

  private final BiddingClientService biddingService = new BiddingClientService();
  private final ClientPushService pushService = ClientPushService.getInstance();
  private final Map<String, Label> priceLabels = new HashMap<>();
  private List<AuctionSummaryDto> currentAuctions = new ArrayList<>();

  @FXML private Label welcomeLabel;

  @FXML private HBox mainContainer;

  @FXML private GridPane cardsGridPane;

  // Initialization
  @FXML
  public void initialize() {
    if (AppContext.getCurrentUser() != null) {
      if (AppContext.getCurrentUser().getUsername() != null
          && !AppContext.getCurrentUser().getUsername().isBlank()) {
        welcomeLabel.setText(
            "Hunt for the next deal, " + AppContext.getCurrentUser().getUsername() + "!");
      }
      loadAuctions();
      pushService.onNewAuction(event -> Platform.runLater(this::loadAuctions));
      pushService.onBidUpdate(event -> Platform.runLater(() -> handleBidUpdatePush(event)));
      pushService.onAuctionDeleted(
          event -> Platform.runLater(() -> handleAuctionDeletedPush(event)));
      pushService.onAuctionEnded(event -> Platform.runLater(this::loadAuctions));
    }
  }

  // Rendering
  private void renderAuctionCards(List<AuctionSummaryDto> auctions) {
    priceLabels.clear();
    cardsGridPane.getChildren().clear();
    for (int i = 0; i < auctions.size(); i++) {
      Parent card = createAuctionCard(auctions.get(i));
      cardsGridPane.add(card, i % 2, i / 2);
    }
  }

  private Parent createAuctionCard(AuctionSummaryDto dto) {
    try {
      FXMLLoader loader =
          new FXMLLoader(getClass().getResource("/views/user/components/auction_card.fxml"));
      Parent card = loader.load();
      AuctionCardComponentController c = loader.getController();
      c.bind(
          dto,
          DisplayFormatters.auctionStatusLabel(dto.getStatus()),
          DisplayFormatters.money(resolveDisplayCurrentBid(dto)),
          DisplayFormatters.timeLeft(dto.getEndTime()),
          this::navigateToDetail);
      if (dto.getId() != null) priceLabels.put(dto.getId(), c.getPriceValueLabel());
      return card;
    } catch (IOException e) {
      throw new RuntimeException("Failed to load auction card component", e);
    }
  }

  // Data loading
  private void loadAuctions() {
    biddingService
        .listBrowseAuctions()
        .thenAccept(filtered -> Platform.runLater(() -> handleFilteredAuctions(filtered)))
        .exceptionally(
            ex -> {
              Throwable cause = BaseClientService.extractFailure(ex);
              Platform.runLater(() -> showError("Load auctions failed", cause.getMessage()));
              return null;
            });
  }

  private void handleFilteredAuctions(List<AuctionSummaryDto> auctions) {
    if (auctions == null || auctions.isEmpty()) {
      currentAuctions = new ArrayList<>();
      renderAuctionCards(currentAuctions);
      AppContext.setSelectedAuctionId(null);
      return;
    }
    currentAuctions = new ArrayList<>(auctions);
    renderAuctionCards(currentAuctions);
    AppContext.setSelectedAuctionId(auctions.get(0).getId());
  }

  // Event handlers
  private void handleBidUpdatePush(BidUpdateEvent event) {
    String auctionId = event.getAuctionId();
    BigDecimal newBid = event.getNewHighestBid();
    if (auctionId == null || newBid == null) return;
    Label label = priceLabels.get(auctionId);
    if (label != null) label.setText(DisplayFormatters.money(newBid));
  }

  private void handleAuctionDeletedPush(AuctionDeletedEvent event) {
    String auctionId = event.getAuctionId();
    if (auctionId == null) return;
    currentAuctions.removeIf(a -> auctionId.equals(a.getId()));
    renderAuctionCards(currentAuctions);
  }

  // Navigation
  public void navigateToDetail(String auctionId) {
    if (auctionId != null) AppContext.setSelectedAuctionId(auctionId);
    if (AppNavigator.getCurrentView() == AppView.AUCTION_DETAIL) return;
    AppNavigator.navigateTo(AppView.LOADING);
    PauseTransition d = new PauseTransition(Duration.seconds(0.4));
    d.setOnFinished(e -> AppNavigator.navigateTo(AppView.AUCTION_DETAIL));
    d.play();
  }

  // Helpers
  private void showError(String title, String message) {
    System.err.println(title + ": " + message);
  }

  private BigDecimal resolveDisplayCurrentBid(AuctionSummaryDto dto) {
    if (dto == null) return BigDecimal.ZERO;
    BigDecimal current = dto.getCurrentHighestBid();
    if (current != null && current.compareTo(BigDecimal.ZERO) > 0) return current;
    return dto.getStartingPrice() != null ? dto.getStartingPrice() : BigDecimal.ZERO;
  }
}
