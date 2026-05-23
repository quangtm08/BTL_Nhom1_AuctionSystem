package com.nhom1.auction.client.user.controller.components;

import com.nhom1.auction.common.dto.bidding.BidWithAuctionDto;
import java.util.function.Consumer;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class BidCardComponentController {
  @FXML private Label titleLabel;
  @FXML private Label yourBidLabel;
  @FXML private Label statusBadgeLabel;
  @FXML private Label currentBidLabel;
  @FXML private Label timeLeftLabel;
  @FXML private Button raiseBidButton;

  public void bind(
      BidWithAuctionDto dto,
      String yourBid,
      String currentBid,
      String timeLeft,
      Consumer<String> onRaiseBid) {
    titleLabel.setText(dto.getItemName() != null ? dto.getItemName() : "Unknown item");
    yourBidLabel.setText("Your bid: " + yourBid);
    statusBadgeLabel.setText(dto.isWinning() ? "Winning" : "Outbid");
    statusBadgeLabel.setStyle(
        dto.isWinning() ? "" : "-fx-text-fill: #ff4d4d; -fx-background-color: #331a1a;");
    currentBidLabel.setText(currentBid);
    timeLeftLabel.setText(timeLeft);
    raiseBidButton.setOnAction(e -> onRaiseBid.accept(dto.getAuctionId()));
  }
}
