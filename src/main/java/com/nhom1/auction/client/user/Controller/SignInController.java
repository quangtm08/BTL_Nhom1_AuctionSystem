package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.common.service.AuthService;

import javafx.animation.PauseTransition;
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
import javafx.util.Duration;


public class SignInController {

    private Stage alertStage;

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

        btnSignIn.setOnAction((var e) -> {    

            AuthService authService = new AuthService();

            System.out.println("===== USERS =====");
            authService.getAllUsers().forEach(System.out::println);

            String username = txtUsername.getText();
            String password = txtPassword.getText();

            String USER_ROLE = authService.login(username, password);

            if(USER_ROLE != null){
                if(USER_ROLE.equals("ADMIN")){

                    AppNavigator.navigateTo(AppView.LOADING);

                    PauseTransition delay = new PauseTransition(Duration.seconds(2));

                    delay.setOnFinished(event -> {
                            AppNavigator.navigateTo(AppView.MAIN_DASHBOARD);
                    });

                    delay.play();
                }
                else{
                    AppNavigator.navigateTo(AppView.LOADING);

                    PauseTransition delay = new PauseTransition(Duration.seconds(2));

                    delay.setOnFinished(event -> {
                            AppNavigator.navigateTo(AppView.EXPLORE);
                    });

                    delay.play();
                }
            }
            else{
                showError("Ooops!", "Incorrect account or password");
                txtPassword.setText("");
                txtUsername.setText("");
            }

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