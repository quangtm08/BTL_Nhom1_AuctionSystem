package com.nhom1.auction.client.user.controller;

import java.io.IOException;
import java.text.NumberFormat;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.service.ClientPushService;
import com.nhom1.auction.client.user.controller.components.AuctionCardComponentController;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.notification.AuctionDeletedEvent;
import com.nhom1.auction.common.dto.notification.BidUpdateEvent;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.utils.AppContext;

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

    @FXML
    public void initialize() {
        if (AppContext.getCurrentUser() != null) {
            if (AppContext.getCurrentUser().getUsername() != null && !AppContext.getCurrentUser().getUsername().isBlank()) {
                welcomeLabel.setText("Hunt for the next deal, " + AppContext.getCurrentUser().getUsername() + "!");
            }
            loadAuctions();
            pushService.onNewAuction(event -> Platform.runLater(this::loadAuctions));
            pushService.onBidUpdate(event -> Platform.runLater(() -> handleBidUpdatePush(event)));
            pushService.onAuctionDeleted(event -> Platform.runLater(() -> handleAuctionDeletedPush(event)));
        }
    }

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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/components/auction_card.fxml"));
            Parent card = loader.load();
            AuctionCardComponentController c = loader.getController();
            c.bind(dto, formatStatus(dto.getStatus()), formatMoney(resolveDisplayCurrentBid(dto)), formatTimeLeft(dto.getEndTime()), this::navigateToDetail);
            if (dto.getId() != null) priceLabels.put(dto.getId(), c.getPriceValueLabel());
            return card;
        } catch (IOException e) {
            throw new RuntimeException("Failed to load auction card component", e);
        }
    }

    private void handleBidUpdatePush(BidUpdateEvent event) {
        if (event == null || event.getAuctionId() == null || event.getNewHighestBid() == null) return;
        Label label = priceLabels.get(event.getAuctionId());
        if (label != null) label.setText(formatMoney(event.getNewHighestBid()));
    }

    private void loadAuctions() { /* unchanged behavior */
        biddingService.listAuctions().thenCombine(biddingService.getMyBids().exceptionally(ex -> new com.nhom1.auction.common.dto.bidding.MyBidsResponse(Collections.emptyList())), (auctionsResp, myBidsResp) -> {
            if (auctionsResp == null || auctionsResp.getAuctions() == null) return java.util.List.<AuctionSummaryDto>of();
            String currentUserId = AppContext.getCurrentUser() != null ? AppContext.getCurrentUser().getUserID() : null;
            Set<String> myBidAuctionIds = myBidsResp == null || myBidsResp.getBids() == null ? Set.of() : myBidsResp.getBids().stream().map(b -> b.getAuctionId()).filter(id -> id != null && !id.isBlank()).collect(Collectors.toSet());
            return auctionsResp.getAuctions().stream()
                    .filter(a -> a.getId() != null && !myBidAuctionIds.contains(a.getId()))
                    .filter(a -> currentUserId == null || a.getSellerId() == null || !currentUserId.equals(a.getSellerId()))
                    .toList();
        }).thenAccept(filtered -> Platform.runLater(() -> handleFilteredAuctions(filtered))).exceptionally(ex -> { Platform.runLater(() -> showError("Load auctions failed", ex.getCause().getMessage())); return null; });
    }

    private void handleFilteredAuctions(List<AuctionSummaryDto> auctions) {
        if (auctions == null || auctions.isEmpty()) { currentAuctions = new ArrayList<>(); renderAuctionCards(currentAuctions); AppContext.setSelectedAuctionId(null); return; }
        currentAuctions = new ArrayList<>(auctions);
        renderAuctionCards(currentAuctions);
        AppContext.setSelectedAuctionId(auctions.get(0).getId());
    }

    private void handleAuctionDeletedPush(AuctionDeletedEvent event) {
        if (event == null || event.getAuctionId() == null) return;
        currentAuctions.removeIf(a -> event.getAuctionId().equals(a.getId()));
        renderAuctionCards(currentAuctions);
    }

    private void showError(String title, String message) { System.err.println(title + ": " + message); }
    public void navigateToDetail(String auctionId) { if (auctionId != null) AppContext.setSelectedAuctionId(auctionId); if (AppNavigator.getCurrentView() == AppView.AUCTION_DETAIL) return; AppNavigator.navigateTo(AppView.LOADING); PauseTransition d = new PauseTransition(Duration.seconds(0.4)); d.setOnFinished(e -> AppNavigator.navigateTo(AppView.AUCTION_DETAIL)); d.play(); }
    private String formatStatus(Object status) {
        if (status == null) return "Unknown";
        if (status instanceof AuctionStatus auctionStatus) {
            return switch (auctionStatus) {
                case OPEN, RUNNING -> "Running";
                case FINISHED, CANCELED, PAID -> "Ended";
            };
        }
        return status.toString();
    }
    private String formatMoney(BigDecimal amount) { return amount == null ? "$0" : "$" + NumberFormat.getNumberInstance(Locale.US).format(amount); }
    private BigDecimal resolveDisplayCurrentBid(AuctionSummaryDto dto) {
        if (dto == null) return BigDecimal.ZERO;
        BigDecimal current = dto.getCurrentHighestBid();
        if (current != null && current.compareTo(BigDecimal.ZERO) > 0) return current;
        return dto.getStartingPrice() != null ? dto.getStartingPrice() : BigDecimal.ZERO;
    }
    private String formatTimeLeft(LocalDateTime endTime) { if (endTime == null) return "N/A"; long d = ChronoUnit.DAYS.between(LocalDateTime.now(), endTime); long h = ChronoUnit.HOURS.between(LocalDateTime.now(), endTime); long m = ChronoUnit.MINUTES.between(LocalDateTime.now(), endTime); if (d > 0) return d + " day" + (d > 1 ? "s" : ""); if (h > 0) return h + " hour" + (h > 1 ? "s" : ""); if (m > 0) return m + " min" + (m > 1 ? "s" : ""); return "Ended"; }
}
