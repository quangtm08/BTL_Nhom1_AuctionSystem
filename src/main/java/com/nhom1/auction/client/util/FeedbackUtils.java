package com.nhom1.auction.client.util;

import javafx.scene.control.Label;

/**
 * Standardized utility for showing and clearing user feedback (errors/status) via Labels. Used to
 * ensure consistent UI behavior across the application.
 */
public class FeedbackUtils {

  public static void showError(Label label, String message) {
    if (label == null) return;
    label.setText(message);
    label.getStyleClass().remove("error-label");
    label.getStyleClass().add("error-label");
    label.setVisible(true);
    label.setManaged(true);
  }

  public static void clearError(Label label) {
    if (label == null) return;
    label.setText("");
    label.setVisible(false);
    label.setManaged(false);
    label.getStyleClass().remove("error-label");
  }

  public static void showStatus(Label label, String message) {
    if (label == null) return;
    label.setText(message);
    label.getStyleClass().remove("error-label");
    label.setVisible(true);
    label.setManaged(true);
  }
}
