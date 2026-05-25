package com.nhom1.auction.client.util;

import javafx.scene.Node;

/** Utility for managing skeleton loading state. */
public final class SkeletonUtils {

  private SkeletonUtils() {}

  /**
   * Shows the loading skeleton and hides the actual content.
   *
   * @param loadingBox the skeleton container
   * @param contentBox the actual content container
   */
  public static void showLoading(Node loadingBox, Node contentBox) {
    if (loadingBox != null) {
      loadingBox.setVisible(true);
      loadingBox.setManaged(true);
    }
    if (contentBox != null) {
      contentBox.setVisible(false);
      contentBox.setManaged(false);
    }
  }

  /**
   * Shows the actual content and hides the loading skeleton.
   *
   * @param loadingBox the skeleton container
   * @param contentBox the actual content container
   */
  public static void showContent(Node loadingBox, Node contentBox) {
    if (loadingBox != null) {
      loadingBox.setVisible(false);
      loadingBox.setManaged(false);
    }
    if (contentBox != null) {
      contentBox.setVisible(true);
      contentBox.setManaged(true);
    }
  }
}
