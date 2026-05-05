package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.common.utils.AppContext;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.util.Duration;

public class UserSidebarController {

    @FXML
    Button btnExplore;
    @FXML
    Button btnBids;
    @FXML
    Button btnListings;
    @FXML
    Button btnPayment;
    @FXML
    Button btnLogout;
    @FXML
    Label usernameLabel;

    @FXML
    public void initialize() {
        btnExplore.setOnAction(e -> navigateWithLoading(AppView.AUCTION_BROWSE));
        btnBids.setOnAction(e -> navigateWithLoading(AppView.MY_BIDS));
        btnListings.setOnAction(e -> navigateWithLoading(AppView.MY_LISTINGS));
        btnPayment.setOnAction(e -> navigateWithLoading(AppView.PAYMENT));
        btnLogout.setOnAction(e -> navigateWithLoading(AppView.SIGN_IN));
        if (AppContext.getCurrentUser() != null) {
            usernameLabel.setText(AppContext.getCurrentUser().getUsername());
        }
        updateActiveButton();
    }

    private void navigateWithLoading(AppView targetView) {
        // Không reload nếu đang ở màn hiện tại
        if (AppNavigator.getCurrentView() == targetView)
            return;

        // Bỏ qua màn LOADING và delay, chuyển thẳng đến view đích
        AppNavigator.navigateTo(targetView);
    }

    private void updateActiveButton() {
        AppView current = AppNavigator.getCurrentView();

        btnExplore.getStyleClass().remove("side-btn-active");
        btnBids.getStyleClass().remove("side-btn-active");
        btnListings.getStyleClass().remove("side-btn-active");
        btnPayment.getStyleClass().remove("side-btn-active");

        if (current == null)
            return;

        switch (current) {
            case AUCTION_BROWSE -> btnExplore.getStyleClass().add("side-btn-active");
            case MY_BIDS -> btnBids.getStyleClass().add("side-btn-active");
            case MY_LISTINGS -> btnListings.getStyleClass().add("side-btn-active");
            case PAYMENT -> btnPayment.getStyleClass().add("side-btn-active");
            default -> {
            }
        }
    }

}
