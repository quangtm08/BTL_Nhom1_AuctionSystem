package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.utils.AppContext;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;


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
        bindCurrentUsername();

        btnExplore.setOnAction(e -> navigateWithLoading(AppView.AUCTION_BROWSE));
        btnBids.setOnAction(e -> navigateWithLoading(AppView.MY_BIDS));
        btnListings.setOnAction(e -> navigateWithLoading(AppView.MY_LISTINGS));
        btnPayment.setOnAction(e -> navigateWithLoading(AppView.PAYMENT));
        btnLogout.setOnAction(e -> navigateWithLoading(AppView.SIGN_IN));

        updateActiveButton();
    }

    private void bindCurrentUsername() {
        AuthResponse currentUser = AppContext.getCurrentUser();
        if (currentUser != null && currentUser.getUsername() != null && !currentUser.getUsername().isBlank()) {
            usernameLabel.setText(currentUser.getUsername());
            return;
        }
        usernameLabel.setText("Guest");
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

        btnExplore.getStyleClass().remove("btn-ghost-active");
        btnBids.getStyleClass().remove("btn-ghost-active");
        btnListings.getStyleClass().remove("btn-ghost-active");
        btnPayment.getStyleClass().remove("btn-ghost-active");

        if (current == null)
            return;

        switch (current) {
            case AUCTION_BROWSE -> btnExplore.getStyleClass().add("btn-ghost-active");
            case MY_BIDS -> btnBids.getStyleClass().add("btn-ghost-active");
            case MY_LISTINGS -> btnListings.getStyleClass().add("btn-ghost-active");
            case PAYMENT -> btnPayment.getStyleClass().add("btn-ghost-active");
            default -> {
            }
        }
    }

}
