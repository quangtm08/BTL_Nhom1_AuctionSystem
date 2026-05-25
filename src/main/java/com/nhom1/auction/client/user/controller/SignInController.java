package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.service.AuthClientService;
import com.nhom1.auction.client.util.FeedbackUtils;
import com.nhom1.auction.common.enums.UserRole;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

public class SignInController {

  private final AuthClientService authService = new AuthClientService();

  @FXML private Button btnSignIn;

  @FXML private Button btnRegister;

  @FXML private TextField txtUsername;

  @FXML private PasswordField txtPassword;

  @FXML private Label lblStatus;

  @FXML
  public void initialize() {
    btnRegister.setOnAction(e -> AppNavigator.navigateTo(AppView.REGISTER));

    btnSignIn.setOnAction(
        e -> {
          FeedbackUtils.clear(lblStatus);
          authService
              .login(txtUsername.getText().trim(), txtPassword.getText())
              .thenAccept(
                  authData ->
                      Platform.runLater(
                          () -> {
                            if (authData.getRole() == UserRole.ADMIN) {
                              AppNavigator.navigateTo(AppView.ADMIN_OVERVIEW);
                            } else {
                              AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
                            }
                          }))
              .exceptionally(
                  ex -> {
                    Platform.runLater(
                        () -> {
                          FeedbackUtils.showError(
                              lblStatus, AuthClientService.extractFailure(ex).getMessage());
                          txtPassword.clear();
                        });
                    return null;
                  });
        });
  }
}
