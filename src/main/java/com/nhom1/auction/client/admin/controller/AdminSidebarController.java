package com.nhom1.auction.client.admin.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.common.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

public class AdminSidebarController {
  private static final String SIDE_ACTIVE_CLASS = "side-btn-active";
  private static final String GHOST_ACTIVE_CLASS = "btn-ghost-active";

  @FXML Button btnDashboard;
  @FXML Button btnUsers;
  @FXML Button btnAuctions;
  @FXML Button btnLogout;

  @FXML
  public void initialize() {

    updateActiveButton();

    btnDashboard.setOnAction(e -> navigateWithLoading(AppView.ADMIN_OVERVIEW));
    btnUsers.setOnAction(e -> navigateWithLoading(AppView.USER_MANAGEMENT));
    btnAuctions.setOnAction(e -> navigateWithLoading(AppView.AUCTION_MANAGEMENT));
    btnLogout.setOnAction(e -> logout());
  }

  private void navigateWithLoading(AppView targetView) {
    if (AppNavigator.getCurrentView() == targetView) return;

    AppNavigator.navigateTo(targetView);
  }

  private void logout() {
    AppContext.clearSession();
    AppNavigator.navigateTo(AppView.SIGN_IN);
  }

  private void updateActiveButton() {
    AppView current = AppNavigator.getCurrentView();

    if (current == null) return;

    removeActiveClasses(btnDashboard);
    removeActiveClasses(btnUsers);
    removeActiveClasses(btnAuctions);

    switch (current) {
      case ADMIN_OVERVIEW -> addActiveClasses(btnDashboard);
      case USER_MANAGEMENT -> addActiveClasses(btnUsers);
      case AUCTION_MANAGEMENT -> addActiveClasses(btnAuctions);
      default -> {}
    }
  }

  private void removeActiveClasses(Button button) {
    button.getStyleClass().remove(SIDE_ACTIVE_CLASS);
    button.getStyleClass().remove(GHOST_ACTIVE_CLASS);
  }

  private void addActiveClasses(Button button) {
    button.getStyleClass().add(SIDE_ACTIVE_CLASS);
    button.getStyleClass().add(GHOST_ACTIVE_CLASS);
  }
}
