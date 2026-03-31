package com.nhom1.auction.client.Controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.util.Duration;

public class RegisterController{

    @FXML Button btnRegister;
    @FXML Button btnSignIn; 

    @FXML
    public void initialize(){

        btnRegister.setOnAction((e) -> {

            AppNavigator.navigateTo(AppView.LOADING);

            PauseTransition delay = new PauseTransition(Duration.seconds(2));


            // Check if the account exists.

            delay.setOnFinished(event -> {
                    AppNavigator.navigateTo(AppView.SIGN_IN);
                });
            delay.play();
        });

        btnSignIn.setOnAction((e) -> {
            AppNavigator.navigateTo(AppView.LOADING);

            PauseTransition delay = new PauseTransition(Duration.seconds(2));

            delay.setOnFinished(event -> {
                    AppNavigator.navigateTo(AppView.SIGN_IN);
                });
            delay.play();
        });

    }

}
