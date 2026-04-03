package com.nhom1.auction.client.Controller;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class MainController {

    @FXML
    private StackPane rootPane;

    public void setView(Parent view) {
        rootPane.getChildren().setAll(view);
    }
}