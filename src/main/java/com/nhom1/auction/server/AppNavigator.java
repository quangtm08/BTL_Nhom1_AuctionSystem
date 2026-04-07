package com.nhom1.auction.server;

import com.nhom1.auction.common.classes.AppContext;
import com.nhom1.auction.common.classes.BaseMainController;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;


public class AppNavigator {

    private static AppView currentView;
    private static BaseMainController mainController;

    public static void setRoot(BaseMainController controller) {
        mainController = controller;
    }

    public static AppView getCurrentView() {
        return currentView;
    }

    public static void navigateTo(AppView view) {
        try {

            if (view == currentView) return;

            currentView = view;

            AppContext.setServer(true);
            FXMLLoader loader = new FXMLLoader(
                AppNavigator.class.getResource(view.getFxml())
            );

            loader.setControllerFactory(clazz -> {
                try {
                    String base = "com.nhom1.auction." +
                            (AppContext.isServer() ? "server" : "client") +
                            ".Controller.";

                    return Class.forName(base + clazz.getSimpleName())
                            .getDeclaredConstructor()
                            .newInstance();

                } catch (Exception e) {
                    try {
                        return clazz.getDeclaredConstructor().newInstance();
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            });

            Parent root = loader.load();


            mainController.setView(root);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
