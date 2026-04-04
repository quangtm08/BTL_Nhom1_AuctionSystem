package com.nhom1.auction.client.Controller;


import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.util.Duration;


public class MainDashboardListingController{

    @FXML Button btnExplore;
    @FXML Button btnPayment;

    @FXML
    public void initialize(){

        btnExplore.setOnAction(e -> {
            AppNavigator.navigateTo(AppView.LOADING);

            PauseTransition delay = new PauseTransition(Duration.seconds(2));

            delay.setOnFinished(event -> {
                    AppNavigator.navigateTo(AppView.MAIN_DASHBOARD);
                });
                delay.play();

        });

        btnPayment.setOnAction(e -> {
            AppNavigator.navigateTo(AppView.LOADING);

            PauseTransition delay = new PauseTransition(Duration.seconds(2));

            delay.setOnFinished(event -> {
                    AppNavigator.navigateTo(AppView.LISTINGS);
                });
                delay.play();
        } );

    }

}
