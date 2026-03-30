package com.nhom1.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.util.Objects;

public class ClientApplication extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(
            ClientApplication.class.getResource("/views/register-view.fxml")
        );

        Scene scene = new Scene(fxmlLoader.load(), 1320, 855);
        scene.getStylesheets().add(
            Objects.requireNonNull(
                ClientApplication.class.getResource("/css/register.css")
            ).toExternalForm()
        );



        stage.setTitle("Auction System Client");
        stage.setMinWidth(1100);
        stage.setMinHeight(760);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
