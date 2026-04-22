package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.dto.auth.RegisterRequest;
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

public class RegisterController {

    private Stage alertStage;

    @FXML private Button btnRegister;
    @FXML private Button btnSignIn; 
    @FXML private TextField txtUsername; 
    @FXML private TextField txtEmail; 
    @FXML private PasswordField txtPassword; 
    @FXML private PasswordField txtRepeatPassword; 

    @FXML
    public void initialize() {
        // Navigate back to Sign In
        btnSignIn.setOnAction(e -> {
            AppNavigator.navigateTo(AppView.SIGN_IN);
        });

        // Handle Register button click
        btnRegister.setOnAction(e -> handleRegister());
    }

    private void handleRegister() {
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        String repeatPassword = txtRepeatPassword.getText();

        // 1. Basic Validation
        if (username.isEmpty() || password.isEmpty()) {
            showError("Missing Info", "Username and password are required.");
            return;
        }

        // Get email from UI, or use a dummy if field is missing/empty
        String email = (txtEmail != null && !txtEmail.getText().isEmpty()) 
                       ? txtEmail.getText() 
                       : username + "@auction.com";

        if (!password.equals(repeatPassword)) {
            showError("Password Mismatch", "Passwords do not match.");
            return;
        }

        // 2. Package the request
        RegisterRequest registerDto = new RegisterRequest(username, email, password);
        RequestMessage<RegisterRequest> request = new RequestMessage<>(MessageType.REGISTER, registerDto);

        // 3. Send and handle response (Async)
        ServerConnection.getInstance().sendRequest(request, AuthResponse.class)
            .thenAccept(response -> {
                Platform.runLater(() -> {
                    try {
                        if (response.isSuccess()) {
                            System.out.println("[Register] Registration success. Payload type: " + response.getPayload().getClass().getName());

                            // Success path: Store session and go to Dashboard
                            AuthResponse authData = (AuthResponse) response.getPayload();
                            AppContext.setCurrentUser(authData);

                            System.out.println("Registration successful! Welcome, " + authData.getUsername());
                            AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
                        } else {
                            // Failure path: Show server error
                            showError("Registration Failed", response.getError().getMessage());
                        }
                    } catch (Exception ex) {
                        System.err.println("[Register] Error processing registration response: " + ex.getMessage());
                        ex.printStackTrace();
                        showError("System Error", "Error processing server response.");
                    }
                });
            })

            .exceptionally(ex -> {
                Platform.runLater(() -> showError("Connection Error", "Server is down."));
                return null;
            });
    }

    private void showError(String title, String message) {
        try {
            if (alertStage != null && alertStage.isShowing()) {
                return;
            }

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/custom_alert.fxml"));
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
            alertStage.setOnHidden(e -> alertStage = null);

            alertStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
