package com.nhom1.auction.client.Controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.util.Duration;



public class MainDashboardController {
    
    @FXML Button btnPayment;
    @FXML Button btnListings;
    
    @FXML
    public void initialize(){

        btnPayment.setOnAction(e -> {
            AppNavigator.navigateTo(AppView.LOADING);

            PauseTransition delay = new PauseTransition(Duration.seconds(2));

            delay.setOnFinished(event -> {
                    AppNavigator.navigateTo(AppView.PAYMENT);
                });
                delay.play();

        }); 
    
        btnListings.setOnAction(e -> {
            AppNavigator.navigateTo(AppView.LOADING);

            PauseTransition delay = new PauseTransition(Duration.seconds(2));

            delay.setOnFinished(event -> {
                    AppNavigator.navigateTo(AppView.LISTINGS);
                });
                delay.play();
        });
    
    }
}
