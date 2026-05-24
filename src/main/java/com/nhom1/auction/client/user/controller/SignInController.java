package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.AuthClientService;
import com.nhom1.auction.common.enums.UserRole;
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

public class SignInController {

    private final AuthClientService authService = new AuthClientService();
    private Stage alertStage;

    @FXML
    private Button btnSignIn;

    @FXML
    private Button btnRegister;

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    public void initialize() {
        btnRegister.setOnAction(e -> AppNavigator.navigateTo(AppView.REGISTER));

        btnSignIn.setOnAction(e ->
            authService
                .login(txtUsername.getText().trim(), txtPassword.getText())
                .thenAccept(authData ->
                    Platform.runLater(() -> {
                        if (authData.getRole() == UserRole.ADMIN) {
                            AppNavigator.navigateTo(AppView.ADMIN_OVERVIEW);
                        } else {
                            AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
                        }
                    })
                )
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        showError(
                            "Login Failed",
                            AuthClientService.extractFailure(ex).getMessage()
                        );
                        txtPassword.clear();
                    });
                    return null;
                })
        );
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
