package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.util.Duration;


public class MyBidsController {
    @FXML Button btnRaiseBid;

    @FXML
    public void initialize(){
        btnRaiseBid.setOnAction(e -> navigateWithLoading(AppView.AUCTION_DETAIL));
    }

    private void navigateWithLoading(AppView targetView) {
        // Không reload nếu đang ở màn hiện tại
        if (AppNavigator.getCurrentView() == targetView) return;

        // Bỏ qua màn LOADING và delay, chuyển thẳng đến view đích
        AppNavigator.navigateTo(targetView);
    }

}
