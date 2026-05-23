package com.nhom1.auction.client;

import com.nhom1.auction.client.service.ClientPushService;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

public class AppNavigator {

  private static BaseShellController mainController;
  private static AppView currentView;

  public static void setRoot(BaseShellController controller) {
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
      ClientPushService.clearHandlersIfInitialized();

      FXMLLoader loader = new FXMLLoader(AppNavigator.class.getResource(view.getFxml()));

      Parent root = loader.load();

      mainController.setView(root);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
