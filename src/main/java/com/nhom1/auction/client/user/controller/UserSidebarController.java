package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.service.ClientPushService;
import com.nhom1.auction.client.user.service.WalletClientService;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.utils.AppContext;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class UserSidebarController {

  @FXML private Button btnExplore;

  @FXML private Button btnBids;

  @FXML private Button btnListings;

  @FXML private Button btnPayment;

  @FXML private Button btnWallet;

  @FXML private Button btnLogout;

  @FXML private Label usernameLabel;

  @FXML private Label balanceLabel;

  private final WalletClientService walletClientService = new WalletClientService();

  @FXML
  public void initialize() {
    bindCurrentUsername();
    loadInitialBalance();
    subscribeToWalletUpdates();

    btnExplore.setOnAction(e -> navigateWithLoading(AppView.AUCTION_BROWSE));
    btnBids.setOnAction(e -> navigateWithLoading(AppView.MY_BIDS));
    btnListings.setOnAction(e -> navigateWithLoading(AppView.MY_LISTINGS));
    btnPayment.setOnAction(e -> navigateWithLoading(AppView.PAYMENT));
    btnWallet.setOnAction(e -> navigateWithLoading(AppView.WALLET));
    btnLogout.setOnAction(e -> logout());

    updateActiveButton();
  }

  private void bindCurrentUsername() {
    AuthResponse currentUser = AppContext.getCurrentUser();
    if (currentUser != null
        && currentUser.getUsername() != null
        && !currentUser.getUsername().isBlank()) {
      usernameLabel.setText(currentUser.getUsername());
      return;
    }
    usernameLabel.setText("Guest");
  }

  private void loadInitialBalance() {
    AuthResponse currentUser = AppContext.getCurrentUser();
    if (currentUser != null && currentUser.getUserID() != null) {
      walletClientService
          .getWallet(currentUser.getUserID())
          .thenAccept(
              wallet -> {
                javafx.application.Platform.runLater(
                    () -> {
                      balanceLabel.setText(String.format("$%,.2f", wallet.getBalance()));
                    });
              })
          .exceptionally(
              ex -> {
                System.err.println("Failed to load initial wallet balance: " + ex.getMessage());
                return null;
              });
    }
  }

  private void subscribeToWalletUpdates() {
    ClientPushService.getInstance()
        .addWalletUpdateListener(
            event -> {
              AuthResponse currentUser = AppContext.getCurrentUser();
              if (currentUser != null
                  && currentUser.getUserID() != null
                  && currentUser.getUserID().equals(event.getUserId())) {
                javafx.application.Platform.runLater(
                    () -> {
                      balanceLabel.setText(String.format("$%,.2f", event.getNewBalance()));
                    });
              }
            });
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

    btnExplore.getStyleClass().remove("btn-ghost-active");
    btnBids.getStyleClass().remove("btn-ghost-active");
    btnListings.getStyleClass().remove("btn-ghost-active");
    btnPayment.getStyleClass().remove("btn-ghost-active");
    btnWallet.getStyleClass().remove("btn-ghost-active");

    if (current == null) return;

    switch (current) {
      case AUCTION_BROWSE -> btnExplore.getStyleClass().add("btn-ghost-active");
      case MY_BIDS -> btnBids.getStyleClass().add("btn-ghost-active");
      case MY_LISTINGS -> btnListings.getStyleClass().add("btn-ghost-active");
      case PAYMENT -> btnPayment.getStyleClass().add("btn-ghost-active");
      case WALLET -> btnWallet.getStyleClass().add("btn-ghost-active");
      default -> {}
    }
  }
}
