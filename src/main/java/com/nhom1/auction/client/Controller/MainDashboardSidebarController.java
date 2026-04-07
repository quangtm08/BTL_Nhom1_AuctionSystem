package com.nhom1.auction.client.Controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.util.Duration;


public class MainDashboardSidebarController {
    
    @FXML Button btnExplore;
    @FXML Button btnBids;
    @FXML Button btnListings;
    @FXML Button btnPayment;
    @FXML Button btnLogout;
    
    
    @FXML
    public void initialize(){

        btnExplore.setOnAction(e -> navigateWithLoading(AppView.MAIN_DASHBOARD));
        btnBids.setOnAction(e -> navigateWithLoading(AppView.BIDS));
        btnListings.setOnAction(e -> navigateWithLoading(AppView.LISTINGS));
        btnPayment.setOnAction(e -> navigateWithLoading(AppView.PAYMENT));
        btnLogout.setOnAction(e -> navigateWithLoading(AppView.SIGN_IN));

        updateActiveButton();

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
        
        btnExplore.getStyleClass().remove("side-btn-active");
        btnBids.getStyleClass().remove("side-btn-active");
        btnListings.getStyleClass().remove("side-btn-active");
        btnPayment.getStyleClass().remove("side-btn-active");

            if (current == null) return;

    
        switch (current) {
            case MAIN_DASHBOARD -> btnExplore.getStyleClass().add("side-btn-active");
            case BIDS -> btnBids.getStyleClass().add("side-btn-active");
            case LISTINGS -> btnListings.getStyleClass().add("side-btn-active");
            case PAYMENT -> btnPayment.getStyleClass().add("side-btn-active");
            default -> throw new IllegalArgumentException("Unexpected value: " + current);
        }
    }

}
