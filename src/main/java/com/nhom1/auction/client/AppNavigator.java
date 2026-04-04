package com.nhom1.auction.client;

import com.nhom1.auction.client.Controller.MainController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
public class AppNavigator {

    private static MainController mainController;

    public static void setRoot(MainController controller) {
        mainController = controller;
    }

    @SuppressWarnings({"UseSpecificCatch", "CallToPrintStackTrace"})
    public static void navigateTo(AppView view) {
        try {
            FXMLLoader loader = new FXMLLoader(
                AppNavigator.class.getResource(view.getFxml())
            );

            
            
            Parent root = loader.load();

            mainController.setView(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
