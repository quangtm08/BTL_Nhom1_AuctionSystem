package com.nhom1.auction.client.admin.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.common.utils.AppContext;

import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class AdminSidebarController {
    
    @FXML Button btnDashboard;
    @FXML Button btnUsers;
    @FXML Button btnAuctions;
    @FXML Button btnLogout;

    @FXML
    public void initialize(){

        updateActiveButton();

        btnDashboard.setOnAction(e -> navigateWithLoading(AppView.ADMIN_OVERVIEW));
        btnUsers.setOnAction(e -> navigateWithLoading(AppView.USER_MANAGEMENT));
        btnAuctions.setOnAction(e -> navigateWithLoading(AppView.AUCTION_MANAGEMENT));
        btnLogout.setOnAction(e -> logout());


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
        
        if (current == null) return;

        btnDashboard.getStyleClass().remove("side-btn-active");
        btnUsers.getStyleClass().remove("side-btn-active");
        btnAuctions.getStyleClass().remove("side-btn-active");

        switch (current) {
            case ADMIN_OVERVIEW -> btnDashboard.getStyleClass().add("side-btn-active");
            case USER_MANAGEMENT -> btnUsers.getStyleClass().add("side-btn-active");
            case AUCTION_MANAGEMENT -> btnAuctions.getStyleClass().add("side-btn-active");
            default -> {
            }
        }
    }

}
