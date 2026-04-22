package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.dto.auth.LoginRequest;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
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

    private Stage alertStage;

    @FXML private Button btnSignIn;
    @FXML private Button btnRegister;
    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;

    @FXML
    public void initialize() {

        btnRegister.setOnAction(e -> {
            AppNavigator.navigateTo(AppView.REGISTER);
        });

        btnSignIn.setOnAction((var e) -> {
            String username = txtUsername.getText();
            String password = txtPassword.getText();

            if (username.isEmpty() || password.isEmpty()) {
                showError("Missing Info", "Please enter both username and password.");
                return;
            }

            // 1. Package the request
            LoginRequest loginRequest = new LoginRequest(username, password);
            RequestMessage<LoginRequest> loginRequestMessage = new RequestMessage<>(MessageType.LOGIN, loginRequest);

        // 2. Send and handle response
        ServerConnection.getInstance().sendRequest(loginRequestMessage, AuthResponse.class)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    if (response.isSuccess()) {
                        AuthResponse authData = (AuthResponse) response.getPayload();
                        AppContext.setCurrentUser(authData);

                        if (authData.getRole() == UserRole.ADMIN) {
                            AppNavigator.navigateTo(AppView.ADMIN_OVERVIEW);
                        } else {
                            AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
                        }
                    } else {
                        showError("Login Failed", response.getError().getMessage());
                        txtPassword.clear();
                    }
                });
            })
            .exceptionally(ex -> {
                Platform.runLater(() -> showError("Connection Error", "Server is down."));
                return null;
            });

        });
    }

    private void showError(String title, String message) {
        try {
           // 🔥 nếu đang mở thì không tạo mới
            if (alertStage != null && alertStage.isShowing()) {
                return;
            }

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/views/custom_alert.fxml")
            );

            Parent root = loader.load();

            Label lblTitle = (Label) root.lookup("#lblTitle");
            Label lblMessage = (Label) root.lookup("#lblMessage");

            lblTitle.setText(title);
            lblMessage.setText(message);

            Scene scene = new Scene(root);
            scene.setFill(null);

            alertStage = new Stage();
            alertStage.setScene(scene);

            alertStage.initStyle(StageStyle.TRANSPARENT);

            Button btnClose = (Button) root.lookup("#btnClose");
            btnClose.setOnAction(e -> alertStage.close());

            // 🔥 khi đóng thì reset
            alertStage.setOnHidden(e -> alertStage = null);

            alertStage.show();

            

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}