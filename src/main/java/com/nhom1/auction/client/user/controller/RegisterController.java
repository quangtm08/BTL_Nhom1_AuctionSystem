package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.util.FeedbackUtils;
import com.nhom1.auction.client.user.service.AuthClientService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class RegisterController {

  private final AuthClientService authService = new AuthClientService();

  @FXML private Button btnRegister;

  @FXML private Button btnSignIn;

  @FXML private TextField txtUsername;

  @FXML private TextField txtEmail;

  @FXML private PasswordField txtPassword;

  @FXML private PasswordField txtRepeatPassword;

  @FXML private Label lblStatus;

  @FXML
  public void initialize() {
    btnSignIn.setOnAction(e -> AppNavigator.navigateTo(AppView.SIGN_IN));
    btnRegister.setOnAction(e -> handleRegister());
  }

  private void handleRegister() {
    FeedbackUtils.clear(lblStatus);
    authService
        .register(
            txtUsername.getText().trim(),
            txtEmail != null ? txtEmail.getText().trim() : "",
            txtPassword.getText(),
            txtRepeatPassword.getText())
        .thenAccept(
            authData -> Platform.runLater(() -> AppNavigator.navigateTo(AppView.AUCTION_BROWSE)))
        .exceptionally(
            ex -> {
              Platform.runLater(
                  () ->
                      FeedbackUtils.showError(
                          lblStatus, AuthClientService.extractFailure(ex).getMessage()));
              return null;
            });
  }
}
