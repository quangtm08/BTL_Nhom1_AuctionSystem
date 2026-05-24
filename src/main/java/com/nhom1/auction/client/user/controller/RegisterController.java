package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.AuthClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class RegisterController {

    private final AuthClientService authService = new AuthClientService();
    private Stage alertStage;

    @FXML
    private Button btnRegister;

    @FXML
    private Button btnSignIn;

    @FXML
    private TextField txtUsername;

    @FXML
    private TextField txtEmail;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtRepeatPassword;

    @FXML
    public void initialize() {
        btnSignIn.setOnAction(e -> AppNavigator.navigateTo(AppView.SIGN_IN));
        btnRegister.setOnAction(e -> handleRegister());
    }

    private void handleRegister() {
        authService
            .register(
                txtUsername.getText().trim(),
                txtEmail != null ? txtEmail.getText().trim() : "",
                txtPassword.getText(),
                txtRepeatPassword.getText()
            )
            .thenAccept(authData ->
                Platform.runLater(() ->
                    AppNavigator.navigateTo(AppView.AUCTION_BROWSE)
                )
            )
            .exceptionally(ex -> {
                Platform.runLater(() ->
                    showError(
                        "Registration Failed",
                        AuthClientService.extractFailure(ex).getMessage()
                    )
                );
                return null;
            });
    }

    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.ERROR
        );
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
