package com.nhom1.auction.client.admin.Controller;

import com.nhom1.auction.common.classes.BaseMainController;

import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class MainController implements BaseMainController{

    @FXML
    private StackPane rootPane;

    @Override
    public void setView(Parent view) {
        rootPane.getChildren().setAll(view);
    }
}