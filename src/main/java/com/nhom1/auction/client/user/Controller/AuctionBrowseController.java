package com.nhom1.auction.client.user.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.utils.AppContext;
import com.nhom1.auction.common.protocol.MessageType;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

public class AuctionBrowseController {

    private final BiddingClientService biddingService =
        new BiddingClientService();

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Label> priceLabels = new HashMap<>();
    private List<AuctionSummaryDto> currentAuctions = new ArrayList<>();

    @FXML
    private Label welcomeLabel;

    @FXML
    private HBox mainContainer;

    @FXML
    private GridPane cardsGridPane;

    @FXML
    public void initialize() {
        if (AppContext.getCurrentUser() != null) {
            if (
                AppContext.getCurrentUser().getUsername() != null &&
                !AppContext.getCurrentUser().getUsername().isBlank()
            ) {
                welcomeLabel.setText(
                    "Hunt for the next deal, " +
                        AppContext.getCurrentUser().getUsername() +
                        "!"
                );
            } else {
                System.err.println("No user logged in!");
            }

            loadAuctions();

            ServerConnection.getInstance().registerPushHandler(
                MessageType.PUSH_NEW_AUCTION,
                json -> Platform.runLater(this::loadAuctions)
            );

            ServerConnection.getInstance().registerPushHandler(
                MessageType.PUSH_BID_UPDATE,
                json -> handleBidUpdatePush(json)
            );

            ServerConnection.getInstance().registerPushHandler(
                MessageType.PUSH_AUCTION_DELETED,
                json -> handleAuctionDeletedPush(json)
            );
        }
    }

    private void handleBidUpdatePush(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode node = root.has("payload") && !root.get("payload").isNull()
                ? root.get("payload") : root;
            String auctionId = node.has("auctionId") ? node.get("auctionId").asText() : null;
            if (auctionId == null) return;
            BigDecimal newBid = node.has("newHighestBid")
                ? new BigDecimal(node.get("newHighestBid").asText()) : null;
            if (newBid == null) return;
            final BigDecimal bid = newBid;
            Platform.runLater(() -> {
                Label label = priceLabels.get(auctionId);
                if (label != null) label.setText(formatMoney(bid));
            });
        } catch (Exception e) {
            System.err.println("Error parsing bid update push: " + e.getMessage());
        }
    }

    private void loadAuctions() {
        biddingService
            .listAuctions()
            .thenCombine(biddingService
                .getMyBids()
                .exceptionally(ex -> {
                    String msg =
                        ex != null && ex.getCause() != null
                            ? ex.getCause().getMessage()
                            : "Unknown error";
                    System.err.println(
                        "Failed to load my bids for explore filter: " + msg
                    );
                    return new com.nhom1.auction.common.dto.bidding.MyBidsResponse(
                        Collections.emptyList()
                    );
                }), (auctionsResp, myBidsResp) -> {
                if (
                    auctionsResp == null ||
                    auctionsResp.getAuctions() == null
                ) {
                    return java.util.List.<AuctionSummaryDto>of();
                }
                Set<String> myBidAuctionIds =
                    myBidsResp == null || myBidsResp.getBids() == null
                        ? Set.of()
                        : myBidsResp
                              .getBids()
                              .stream()
                              .map(b -> b.getAuctionId())
                              .filter(id -> id != null && !id.isBlank())
                              .collect(Collectors.toSet());

                return auctionsResp
                    .getAuctions()
                    .stream()
                    .filter(
                        a ->
                            a.getId() != null &&
                            !myBidAuctionIds.contains(a.getId())
                    )
                    .toList();
            })
            .thenAccept(filtered ->
                Platform.runLater(() -> handleFilteredAuctions(filtered))
            )
            .exceptionally(ex -> {
                Platform.runLater(() ->
                    showError(
                        "Load auctions failed",
                        ex.getCause().getMessage()
                    )
                );
                return null;
            });
    }

    private void handleFilteredAuctions(List<AuctionSummaryDto> auctions) {
        if (auctions == null || auctions.isEmpty()) return;

        currentAuctions = new ArrayList<>(auctions);
        renderAuctionCards(auctions);

        String firstAuctionId = auctions.get(0).getId();
        com.nhom1.auction.common.utils.AppContext.setSelectedAuctionId(
            firstAuctionId
        );
    }

    private void handleAuctionDeletedPush(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            JsonNode node = root.has("payload") && !root.get("payload").isNull()
                ? root.get("payload") : root;
            String auctionId = node.has("auctionId") ? node.get("auctionId").asText() : null;
            if (auctionId == null) return;
            final String id = auctionId;
            Platform.runLater(() -> {
                currentAuctions.removeIf(a -> id.equals(a.getId()));
                renderAuctionCards(currentAuctions);
            });
        } catch (Exception e) {
            System.err.println("Error parsing auction deleted push: " + e.getMessage());
        }
    }

    /**
     * Render auction cards dynamically in the GridPane
     */
    private void renderAuctionCards(List<AuctionSummaryDto> auctions) {
        priceLabels.clear();
        cardsGridPane.getChildren().clear();

        for (int i = 0; i < auctions.size(); i++) {
            AuctionSummaryDto auction = auctions.get(i);
            Node card = createAuctionCard(auction);
            int col = i % 2;
            int row = i / 2;
            cardsGridPane.add(card, col, row);
        }
    }

    /**
     * Create a single auction card VBox with all components
     */
    private Node createAuctionCard(AuctionSummaryDto dto) {
        VBox card = new VBox(12);
        card.getStyleClass().add("product-card");

        // Top row: Title + Status badge
        HBox topRow = new HBox();
        topRow.setAlignment(Pos.CENTER_LEFT);

        VBox titleBox = new VBox(2);
        Label title = new Label(
            dto.getItemName() != null ? dto.getItemName() : "Untitled"
        );
        title.getStyleClass().add("card-title-text");
        Label category = new Label(
            dto.getItemCategory() != null
                ? dto.getItemCategory()
                : "Uncategorized"
        );
        category.getStyleClass().add("card-sub-text");
        titleBox.getChildren().addAll(title, category);

        Label statusBadge = new Label(formatStatus(dto.getStatus()));
        statusBadge.getStyleClass().add("status-badge");

        HBox.setHgrow(titleBox, Priority.ALWAYS);
        topRow.getChildren().addAll(titleBox, statusBadge);

        // Spacer
        VBox spacer = new VBox();
        spacer.setPrefHeight(50.0);

        // Bottom row: Price + Time + Button
        HBox bottomRow = new HBox();
        bottomRow.setAlignment(Pos.BOTTOM_LEFT);
        bottomRow.setSpacing(10);

        VBox priceSection = new VBox(2);
        Label priceLabel = new Label("Current bid");
        priceLabel.getStyleClass().add("card-sub-text");
        Label priceValue = new Label(formatMoney(dto.getCurrentHighestBid()));
        priceValue.getStyleClass().add("card-price-text");
        priceValue.setMaxWidth(Double.MAX_VALUE);
        if (dto.getId() != null) priceLabels.put(dto.getId(), priceValue);
        priceSection.getChildren().addAll(priceLabel, priceValue);

        VBox timeSection = new VBox(2);
        timeSection.setAlignment(Pos.BOTTOM_RIGHT);
        timeSection.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Label timeLeftLabel = new Label(formatTimeLeft(dto.getEndTime()));
        timeLeftLabel.getStyleClass().add("card-sub-text");

        Button bidButton = new Button("Raise bid");
        bidButton.getStyleClass().add("btn-primary");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        timeSection.getChildren().addAll(timeLeftLabel, bidButton);

        HBox.setHgrow(priceSection, Priority.ALWAYS);
        bottomRow.getChildren().addAll(priceSection, timeSection);

        // Wire navigation button
        bidButton.setOnAction(e -> navigateToDetail(dto.getId()));

        // Assemble card
        card.getChildren().addAll(topRow, spacer, bottomRow);
        return card;
    }

    private void showError(String title, String message) {
        // lightweight: navigate to loading or show console; reuse AppNavigator to
        // remain non-blocking
        System.err.println(title + ": " + message);
    }

    // Navigation helper used by UI buttons (can be wired later)
    public void navigateToDetail(String auctionId) {
        if (
            auctionId != null
        ) com.nhom1.auction.common.utils.AppContext.setSelectedAuctionId(
            auctionId
        );
        if (AppNavigator.getCurrentView() == AppView.AUCTION_DETAIL) return;
        AppNavigator.navigateTo(AppView.LOADING);
        PauseTransition delay = new PauseTransition(Duration.seconds(0.4));
        delay.setOnFinished(e ->
            AppNavigator.navigateTo(AppView.AUCTION_DETAIL)
        );
        delay.play();
    }

    // ================= HELPER METHODS =================
    // Helper method để format status, money và thời gian còn lại cho hiển thị đẹp
    // hơn
    private String formatStatus(Object status) {
        if (status == null) return "Unknown";
        return status.toString();
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) return "$0";
        return "$" + NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    private String formatTimeLeft(LocalDateTime endTime) {
        if (endTime == null) return "N/A";

        long daysLeft = ChronoUnit.DAYS.between(LocalDateTime.now(), endTime);
        long hoursLeft = ChronoUnit.HOURS.between(LocalDateTime.now(), endTime);
        long minutesLeft = ChronoUnit.MINUTES.between(
            LocalDateTime.now(),
            endTime
        );

        if (daysLeft > 0) return daysLeft + " day" + (daysLeft > 1 ? "s" : "");
        if (hoursLeft > 0) return (
            hoursLeft + " hour" + (hoursLeft > 1 ? "s" : "")
        );
        if (minutesLeft > 0) return (
            minutesLeft + " min" + (minutesLeft > 1 ? "s" : "")
        );

        return "Ended";
    }
}
