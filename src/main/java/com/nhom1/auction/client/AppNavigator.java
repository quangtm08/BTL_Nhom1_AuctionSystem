package com.nhom1.auction.client;

import com.nhom1.auction.common.classes.BaseMainController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
public class AppNavigator {

    private static BaseMainController mainController;
    private static AppView currentView;


    public static void setRoot(BaseMainController controller) {
        mainController = controller;
    }

    public static AppView getCurrentView() {
        return currentView;
    }

    @SuppressWarnings({"UseSpecificCatch", "CallToPrintStackTrace"})
    public static void navigateTo(AppView view) {
        try {
            
            if (view == currentView) return;

            currentView = view;

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
