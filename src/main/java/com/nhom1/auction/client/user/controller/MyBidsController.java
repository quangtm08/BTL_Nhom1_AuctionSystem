package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.service.ClientPushService;
import com.nhom1.auction.client.user.controller.components.BidCardComponentController;
import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.client.util.DisplayFormatters;
import com.nhom1.auction.common.dto.bidding.BidWithAuctionDto;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.utils.AppContext;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

public class MyBidsController {

  @FXML private GridPane cardsGridPane;

  @FXML private Label activeBidCountLabel;

  @FXML private Label endingSoonCountLabel;

  @FXML private javafx.scene.layout.VBox loadingBox;

  @FXML private javafx.scene.control.ScrollPane contentBox;

  private final BiddingClientService biddingService = new BiddingClientService();
  private final ClientPushService pushService = ClientPushService.getInstance();

  @FXML
  public void initialize() {
    loadMyBids();
    pushService.onAuctionEnded(event -> Platform.runLater(this::loadMyBids));
  }

  private void showContent() {
    if (loadingBox != null) {
      loadingBox.setVisible(false);
      loadingBox.setManaged(false);
    }
    if (contentBox != null) {
      contentBox.setVisible(true);
      contentBox.setManaged(true);
    }
  }

  private void loadMyBids() {
    biddingService
        .getMyBids()
        .thenAccept(resp -> Platform.runLater(() -> renderMyBids(resp)))
        .exceptionally(
            ex -> {
              Throwable cause = BaseClientService.extractFailure(ex);
              Platform.runLater(
                  () -> {
                    showContent();
                    System.err.println("Failed to load my bids: " + cause.getMessage());
                  });
              return null;
            });
  }

  private void renderMyBids(MyBidsResponse resp) {
    showContent();
    cardsGridPane.getChildren().clear();
    if (resp == null || resp.getBids() == null || resp.getBids().isEmpty()) {
      updateCounts(0, 0);
      return;
    }

    List<BidWithAuctionDto> bids = resp.getBids();
    Set<String> activeAuctions = new HashSet<>();
    Set<String> endingSoonAuctions = new HashSet<>();

    for (BidWithAuctionDto bid : bids) {
      if (bid == null || bid.getAuctionId() == null) continue;
      if (bid.getStatus() == AuctionStatus.RUNNING) {
        activeAuctions.add(bid.getAuctionId());
        if (isEndingSoon(bid.getEndTime())) {
          endingSoonAuctions.add(bid.getAuctionId());
        }
      }
    }

    updateCounts(activeAuctions.size(), endingSoonAuctions.size());
    for (int i = 0; i < bids.size(); i++) {
      cardsGridPane.add(createBidCard(bids.get(i)), i % 2, i / 2);
    }
  }

  private Parent createBidCard(BidWithAuctionDto bid) {
    try {
      FXMLLoader loader =
          new FXMLLoader(getClass().getResource("/views/user/components/bid_card.fxml"));
      Parent card = loader.load();
      BidCardComponentController c = loader.getController();
      c.bind(
          bid,
          DisplayFormatters.money(bid.getYourBid()),
          DisplayFormatters.money(bid.getCurrentHighestBid()),
          formatBidTimeLeft(bid.getStatus(), bid.getEndTime()),
          this::navigateToDetail);
      return card;
    } catch (IOException e) {
      throw new RuntimeException("Failed to load bid card component", e);
    }
  }

  private void updateCounts(int activeCount, int endingSoonCount) {
    if (activeBidCountLabel != null) activeBidCountLabel.setText(String.valueOf(activeCount));
    if (endingSoonCountLabel != null) endingSoonCountLabel.setText(String.valueOf(endingSoonCount));
  }

  private boolean isEndingSoon(LocalDateTime endTime) {
    if (endTime == null) return false;
    Duration remaining = Duration.between(LocalDateTime.now(), endTime);
    return (!remaining.isNegative() && remaining.compareTo(Duration.ofHours(24)) < 0);
  }

  private void navigateToDetail(String auctionId) {
    if (auctionId != null) AppContext.setSelectedAuctionId(auctionId);
    if (AppNavigator.getCurrentView() == AppView.AUCTION_DETAIL) return;
    AppNavigator.navigateTo(AppView.AUCTION_DETAIL);
  }

  private String formatBidTimeLeft(AuctionStatus status, LocalDateTime endTime) {
    if (DisplayFormatters.isEnded(status)) {
      return "Ended";
    }
    return DisplayFormatters.timeLeft(endTime);
  }
}
