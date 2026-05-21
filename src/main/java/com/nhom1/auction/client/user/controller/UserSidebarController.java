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
    private Button btnExplore;

    @FXML
    private Button btnBids;

    @FXML
    private Button btnListings;

    @FXML
    private Button btnPayment;

    @FXML
    private Button btnLogout;

    @FXML
    private Label usernameLabel;

    @FXML
    public void initialize() {
        bindCurrentUsername();

        btnExplore.setOnAction(e ->
            navigateWithLoading(AppView.AUCTION_BROWSE)
        );
        btnBids.setOnAction(e -> navigateWithLoading(AppView.MY_BIDS));
        btnListings.setOnAction(e -> navigateWithLoading(AppView.MY_LISTINGS));
        btnPayment.setOnAction(e -> navigateWithLoading(AppView.PAYMENT));
        btnLogout.setOnAction(e -> logout());

        updateActiveButton();
    }

    private void bindCurrentUsername() {
        AuthResponse currentUser = AppContext.getCurrentUser();
        if (
            currentUser != null &&
            currentUser.getUsername() != null &&
            !currentUser.getUsername().isBlank()
        ) {
            usernameLabel.setText(currentUser.getUsername());
            return;
        }
        usernameLabel.setText("Guest");
    }

    private void navigateWithLoading(AppView targetView) {
        if (AppNavigator.getCurrentView() == targetView) return;

        AppNavigator.navigateTo(targetView);
    }

    private void logout() {
        AppContext.clearSession();
        AppNavigator.navigateTo(AppView.SIGN_IN);
    }

    private void updateActiveButton() {
        AppView current = AppNavigator.getCurrentView();

        btnExplore.getStyleClass().remove("btn-ghost-active");
        btnBids.getStyleClass().remove("btn-ghost-active");
        btnListings.getStyleClass().remove("btn-ghost-active");
        btnPayment.getStyleClass().remove("btn-ghost-active");

        if (current == null) return;

        switch (current) {
            case AUCTION_BROWSE -> btnExplore
                .getStyleClass()
                .add("btn-ghost-active");
            case MY_BIDS -> btnBids.getStyleClass().add("btn-ghost-active");
            case MY_LISTINGS -> btnListings
                .getStyleClass()
                .add("btn-ghost-active");
            case PAYMENT -> btnPayment.getStyleClass().add("btn-ghost-active");
            default -> {
            }
        }
    }
}
