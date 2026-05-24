package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.WalletClientService;
import com.nhom1.auction.client.util.DisplayFormatters;
import com.nhom1.auction.common.dto.wallet.WalletResponse;
import com.nhom1.auction.common.dto.wallet.WalletTransactionDto;
import java.math.BigDecimal;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class WalletController {

    private final WalletClientService walletClientService =
        new WalletClientService();
    private Stage alertStage;

    @FXML
    private VBox skeletonBox;

    @FXML
    private ScrollPane contentBox;

    @FXML
    private Label lblStatus;

    @FXML
    private Label lblBalance;

    @FXML
    private TextField txtDepositAmount;

    @FXML
    private Button btnQuickDep100;

    @FXML
    private Button btnQuickDep500;

    @FXML
    private Button btnQuickDep1k;

    @FXML
    private Button btnQuickDep5k;

    @FXML
    private Button btnQuickDep10k;

    @FXML
    private Button btnDeposit;

    @FXML
    private TextField txtWithdrawAmount;

    @FXML
    private Button btnQuickWith100;

    @FXML
    private Button btnQuickWith500;

    @FXML
    private Button btnQuickWith1k;

    @FXML
    private Button btnQuickWith5k;

    @FXML
    private Button btnQuickWith10k;

    @FXML
    private Button btnWithdraw;

    @FXML
    private VBox historyBox;

    @FXML
    public void initialize() {
        setupQuickButtons();
        loadWalletData();

        btnDeposit.setOnAction(e -> handleDeposit());
        btnWithdraw.setOnAction(e -> handleWithdraw());
    }

    private void setupQuickButtons() {
        btnQuickDep100.setOnAction(e -> txtDepositAmount.setText("100"));
        btnQuickDep500.setOnAction(e -> txtDepositAmount.setText("500"));
        btnQuickDep1k.setOnAction(e -> txtDepositAmount.setText("1000"));
        btnQuickDep5k.setOnAction(e -> txtDepositAmount.setText("5000"));
        btnQuickDep10k.setOnAction(e -> txtDepositAmount.setText("10000"));

        btnQuickWith100.setOnAction(e -> txtWithdrawAmount.setText("100"));
        btnQuickWith500.setOnAction(e -> txtWithdrawAmount.setText("500"));
        btnQuickWith1k.setOnAction(e -> txtWithdrawAmount.setText("1000"));
        btnQuickWith5k.setOnAction(e -> txtWithdrawAmount.setText("5000"));
        btnQuickWith10k.setOnAction(e -> txtWithdrawAmount.setText("10000"));
    }

    private void showContent() {
        if (skeletonBox != null) {
            skeletonBox.setVisible(false);
            skeletonBox.setManaged(false);
        }
        if (contentBox != null) {
            contentBox.setVisible(true);
            contentBox.setManaged(true);
        }
    }

    private void loadWalletData() {
        walletClientService
            .getWallet()
            .thenAccept(wallet -> Platform.runLater(() -> render(wallet)))
            .exceptionally(ex -> {
                Throwable cause = BaseClientService.extractFailure(ex);
                Platform.runLater(() -> {
                    showContent();
                    lblBalance.setText("Error");
                    historyBox
                        .getChildren()
                        .setAll(
                            createLabel(
                                "Could not load transaction logs.",
                                "history-desc"
                            )
                        );
                    System.err.println(
                        "Failed to fetch wallet info: " + cause.getMessage()
                    );
                });
                return null;
            });
    }

    private void handleDeposit() {
        String input = txtDepositAmount.getText().trim();
        if (input.isEmpty()) {
            showAlert(
                "Invalid Amount",
                "Please enter an amount to deposit.",
                false
            );
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(input);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            showAlert(
                "Invalid Amount",
                "Please enter a valid positive number.",
                false
            );
            return;
        }

        btnDeposit.setDisable(true);
        walletClientService
            .deposit(amount)
            .thenAccept(wallet ->
                Platform.runLater(() -> {
                    btnDeposit.setDisable(false);
                    txtDepositAmount.clear();
                    render(wallet);
                    showAlert(
                        "Deposit Successful",
                        String.format(
                            "Successfully deposited %s to your wallet.",
                            DisplayFormatters.money(amount)
                        ),
                        true
                    );
                })
            )
            .exceptionally(ex -> {
                Throwable cause = BaseClientService.extractFailure(ex);
                Platform.runLater(() -> {
                    btnDeposit.setDisable(false);
                    showAlert("Deposit Failed", cause.getMessage(), false);
                });
                return null;
            });
    }

    private void handleWithdraw() {
        String input = txtWithdrawAmount.getText().trim();
        if (input.isEmpty()) {
            showAlert(
                "Invalid Amount",
                "Please enter an amount to withdraw.",
                false
            );
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(input);
            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            showAlert(
                "Invalid Amount",
                "Please enter a valid positive number.",
                false
            );
            return;
        }

        btnWithdraw.setDisable(true);
        walletClientService
            .withdraw(amount)
            .thenAccept(wallet ->
                Platform.runLater(() -> {
                    btnWithdraw.setDisable(false);
                    txtWithdrawAmount.clear();
                    render(wallet);
                    showAlert(
                        "Withdrawal Successful",
                        String.format(
                            "Successfully withdrew %s from your wallet.",
                            DisplayFormatters.money(amount)
                        ),
                        true
                    );
                })
            )
            .exceptionally(ex -> {
                Throwable cause = BaseClientService.extractFailure(ex);
                Platform.runLater(() -> {
                    btnWithdraw.setDisable(false);
                    showAlert("Withdrawal Failed", cause.getMessage(), false);
                });
                return null;
            });
    }

    private void render(WalletResponse wallet) {
        showContent();
        if (wallet == null) return;

        lblBalance.setText(DisplayFormatters.money(wallet.getBalance()));
        historyBox.getChildren().clear();

        List<WalletTransactionDto> transactions = wallet.getTransactions();
        if (transactions == null || transactions.isEmpty()) {
            historyBox
                .getChildren()
                .add(
                    createLabel("No transaction history yet.", "history-desc")
                );
            return;
        }

        transactions.sort((t1, t2) -> {
            if (
                t1.getCreatedAt() == null || t2.getCreatedAt() == null
            ) return 0;
            return t2.getCreatedAt().compareTo(t1.getCreatedAt());
        });

        for (WalletTransactionDto tx : transactions) {
            historyBox.getChildren().add(createTransactionRow(tx));
        }
    }

    private HBox createTransactionRow(WalletTransactionDto tx) {
        VBox leftBox = new VBox();
        leftBox.setSpacing(4);
        HBox.setHgrow(leftBox, Priority.ALWAYS);

        HBox typeAndDateBox = new HBox();
        typeAndDateBox.setSpacing(10);
        typeAndDateBox.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(tx.getTransactionType());
        badge.getStyleClass().add(getBadgeStyleClass(tx.getTransactionType()));

        Label dateLabel = new Label(
            DisplayFormatters.dateTime(tx.getCreatedAt())
        );
        dateLabel.getStyleClass().add("history-date");

        typeAndDateBox.getChildren().addAll(badge, dateLabel);

        Label descLabel = new Label(
            tx.getDescription() != null ? tx.getDescription() : "No description"
        );
        descLabel.getStyleClass().add("history-desc");

        Label refLabel = new Label("Ref ID: " + tx.getId());
        refLabel.getStyleClass().add("history-ref");

        leftBox.getChildren().addAll(typeAndDateBox, descLabel, refLabel);

        boolean isCredit = isCreditTransaction(tx.getTransactionType());
        String sign = isCredit ? "+" : "-";
        Label priceLabel = new Label(
            sign + " " + DisplayFormatters.money(tx.getAmount())
        );
        priceLabel.getStyleClass().add("history-price");
        priceLabel
            .getStyleClass()
            .add(isCredit ? "price-positive" : "price-negative");

        HBox row = new HBox();
        row.getStyleClass().add("history-row");
        row.setAlignment(Pos.CENTER_LEFT);
        row.getChildren().addAll(leftBox, priceLabel);

        return row;
    }

    private String getBadgeStyleClass(String type) {
        if (type == null) return "badge-deposit";
        return switch (type.toUpperCase()) {
            case "DEPOSIT" -> "badge-deposit";
            case "WITHDRAW" -> "badge-withdraw";
            case "PAYMENT" -> "badge-payment";
            case "RECEIPT" -> "badge-receipt";
            case "REFUND" -> "badge-refund";
            default -> "badge-deposit";
        };
    }

    private boolean isCreditTransaction(String type) {
        if (type == null) return true;
        return switch (type.toUpperCase()) {
            case "DEPOSIT", "RECEIPT", "REFUND" -> true;
            case "WITHDRAW", "PAYMENT" -> false;
            default -> true;
        };
    }

    private Label createLabel(String text, String styleClass) {
        Label lbl = new Label(text);
        lbl.getStyleClass().add(styleClass);
        return lbl;
    }

    private void showAlert(String title, String message, boolean isSuccess) {
        if (isSuccess) {
            com.nhom1.auction.client.util.FeedbackUtils.showStatus(
                lblStatus,
                title + ": " + message
            );
        } else {
            com.nhom1.auction.client.util.FeedbackUtils.showError(
                lblStatus,
                title + ": " + message
            );
        }
    }
}
