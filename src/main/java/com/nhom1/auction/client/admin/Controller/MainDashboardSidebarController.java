package com.nhom1.auction.client.admin.Controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.util.Duration;

public class MainDashboardSidebarController {
    
    @FXML Button btnDashboard;
    @FXML Button btnUsers;
    @FXML Button btnAuctions;
    @FXML Button btnLogout;

    @FXML
    public void initialize(){

        updateActiveButton();

        btnDashboard.setOnAction(e -> navigateWithLoading(AppView.MAIN_DASHBOARD));
        btnUsers.setOnAction(e -> navigateWithLoading(AppView.MAIN_DASHBOARD_USER_MANAGEMENT));
        btnAuctions.setOnAction(e -> navigateWithLoading(AppView.MAIN_DASHBOARD_AUCTION_MANAGEMENT));
        btnLogout.setOnAction(e -> navigateWithLoading(AppView.SIGN_IN));


    }

    private void navigateWithLoading(AppView targetView) {
        // Không reload nếu đang ở màn hiện tại
        if (AppNavigator.getCurrentView() == targetView) return;

        AppNavigator.navigateTo(AppView.LOADING);

        PauseTransition delay = new PauseTransition(Duration.seconds(0.5));
        delay.setOnFinished(e -> AppNavigator.navigateTo(targetView));
        delay.play();
    }

    private void updateActiveButton() {
        AppView current = AppNavigator.getCurrentView();
        
        if (current == null) return;

        btnDashboard.getStyleClass().remove("side-btn-active");
        btnUsers.getStyleClass().remove("side-btn-active");

        switch (current) {
            case MAIN_DASHBOARD -> btnDashboard.getStyleClass().add("side-btn-active");
            case MAIN_DASHBOARD_USER_MANAGEMENT -> btnUsers.getStyleClass().add("side-btn-active");
            case MAIN_DASHBOARD_AUCTION_MANAGEMENT -> btnAuctions.getStyleClass().add("side-btn-active");
            default -> throw new IllegalArgumentException("Unexpected value: " + current);
        }
    }

}
