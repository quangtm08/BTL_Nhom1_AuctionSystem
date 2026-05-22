package com.nhom1.auction.client.user.controller;

import java.io.IOException;
import java.util.List;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.controller.components.BidCardComponentController;
import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.client.util.DisplayFormatters;
import com.nhom1.auction.common.dto.bidding.BidWithAuctionDto;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;
import com.nhom1.auction.common.utils.AppContext;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.GridPane;

public class MyBidsController {
    @FXML private GridPane cardsGridPane;
    private final BiddingClientService biddingService = new BiddingClientService();

    @FXML
    public void initialize() {
        biddingService.getMyBids().thenAccept(resp -> Platform.runLater(() -> renderMyBids(resp))).exceptionally(ex -> { Throwable cause = BaseClientService.extractFailure(ex); Platform.runLater(() -> System.err.println("Failed to load my bids: " + cause.getMessage())); return null; });
    }

    private void renderMyBids(MyBidsResponse resp) {
        cardsGridPane.getChildren().clear();
        if (resp == null || resp.getBids() == null || resp.getBids().isEmpty()) return;
        List<BidWithAuctionDto> bids = resp.getBids();
        for (int i = 0; i < bids.size(); i++) cardsGridPane.add(createBidCard(bids.get(i)), i % 2, i / 2);
    }

    private Parent createBidCard(BidWithAuctionDto bid) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/user/components/bid_card.fxml"));
            Parent card = loader.load();
            BidCardComponentController c = loader.getController();
            c.bind(bid, DisplayFormatters.money(bid.getYourBid()), DisplayFormatters.money(bid.getCurrentHighestBid()), DisplayFormatters.timeLeft(bid.getEndTime()), this::navigateToDetail);
            return card;
        } catch (IOException e) { throw new RuntimeException("Failed to load bid card component", e); }
    }

    private void navigateToDetail(String auctionId) { if (auctionId != null) AppContext.setSelectedAuctionId(auctionId); if (AppNavigator.getCurrentView() == AppView.AUCTION_DETAIL) return; AppNavigator.navigateTo(AppView.AUCTION_DETAIL); }
}
