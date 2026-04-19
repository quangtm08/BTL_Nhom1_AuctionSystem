package com.nhom1.auction.client;

import com.nhom1.auction.client.BaseShellController;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class ShellController implements BaseShellController {

    @FXML
    private StackPane rootPane;

    @Override
    public void setView(Parent view) {
        rootPane.getChildren().setAll(view);
    }
}
