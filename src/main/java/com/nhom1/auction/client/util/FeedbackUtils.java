package com.nhom1.auction.client.util;

import javafx.scene.control.Label;

/** Standardized utility for showing and clearing user feedback (errors/status) via Labels. */
public final class FeedbackUtils {

  private FeedbackUtils() {}

  public static void showError(Label label, String message) {
    if (label == null) return;
    label.setText(message);
    label.getStyleClass().remove("status-label");
    if (!label.getStyleClass().contains("error-label")) {
      label.getStyleClass().add("error-label");
    }
    label.setVisible(true);
    label.setManaged(true);
  }

  public static void showStatus(Label label, String message) {
    if (label == null) return;
    label.setText(message);
    label.getStyleClass().remove("error-label");
    if (!label.getStyleClass().contains("status-label")) {
      label.getStyleClass().add("status-label");
    }
    label.setVisible(true);
    label.setManaged(true);
  }

  public static void clear(Label label) {
    if (label == null) return;
    label.setText("");
    label.getStyleClass().removeAll("error-label", "status-label");
    label.setVisible(false);
    label.setManaged(false);
  }

  public static String messageOrFallback(Throwable throwable, String fallback) {
    if (throwable != null && throwable.getMessage() != null && !throwable.getMessage().isBlank()) {
      return throwable.getMessage();
    }
    return fallback;
  }
}
