package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.util.Duration;


public class MyBidsController {
    @FXML Button btnRaiseBid;

    private final BiddingClientService biddingService = new BiddingClientService();

    @FXML
    public void initialize(){
        btnRaiseBid.setOnAction(e -> navigateWithLoading(AppView.AUCTION_DETAIL));

        // Load my bids to populate counts/statistics
        biddingService.getMyBids()
            .thenAccept(resp -> Platform.runLater(() -> applyStats(resp)))
            .exceptionally(ex -> {
                Platform.runLater(() -> System.err.println("Failed to load my bids: " + ex.getCause().getMessage()));
                return null;
            });
    }

    private void applyStats(MyBidsResponse resp) {
        if (resp == null) return;
        // UI updates for stats exist in FXML but not fx:id-bound; keep minimal for now
    }

    private void navigateWithLoading(AppView targetView) {
        // Không reload nếu đang ở màn hiện tại
        if (AppNavigator.getCurrentView() == targetView) return;

        // Bỏ qua màn LOADING và delay, chuyển thẳng đến view đích
        AppNavigator.navigateTo(targetView);
    }

}
