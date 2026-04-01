package com.nhom1.auction.client.Controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.util.Duration;

public class SignInController {

    @FXML private Button btnSignIn;
    @FXML private Button btnRegister;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    public void initialize() {

        btnRegister.setOnAction(e -> {
            AppNavigator.navigateTo(AppView.LOADING);

            PauseTransition delay = new PauseTransition(Duration.seconds(2));

            delay.setOnFinished(event -> {
                    AppNavigator.navigateTo(AppView.REGISTER);
                });
                delay.play();
        });

        btnSignIn.setOnAction(e -> {
            if(txtUsername.getText().equals("user") &&
               txtPassword.getText().equals("123")) {
            }

            AppNavigator.navigateTo(AppView.LOADING);

            PauseTransition delay = new PauseTransition(Duration.seconds(2));

            delay.setOnFinished(event -> {
                AppNavigator.navigateTo(AppView.MAIN_DASHBOARD);
            });
            delay.play();
        });
    }
}