package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.PaymentClientService;
import com.nhom1.auction.common.dto.payment.PaymentHistoryEntryDto;
import com.nhom1.auction.common.dto.payment.PendingPaymentDto;
import com.nhom1.auction.common.dto.payment.PaymentHistoryResponse;
import com.nhom1.auction.common.dto.payment.PendingPaymentsResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PaymentController {
    private static final DateTimeFormatter HISTORY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final PaymentClientService paymentClientService = new PaymentClientService();
    private String processingAuctionId;

    @FXML private Label lblPaymentStatus;
    @FXML private VBox pendingPaymentsBox;
    @FXML private VBox historyBox;

    @FXML
    public void initialize() {
        reload();
    }

    private void reload() {
        lblPaymentStatus.setText("Loading payment data...");
        paymentClientService.listPendingPayments()
                .thenCombine(paymentClientService.listPaymentHistory(), PaymentSnapshot::new)
                .thenAccept(snapshot -> Platform.runLater(() -> render(snapshot.pendingPayments(), snapshot.paymentHistory())))
                .exceptionally(ex -> {
                    Throwable cause = BaseClientService.extractFailure(ex);
                    Platform.runLater(() -> renderFailure(cause));
                    return null;
                });
    }

    private void render(PendingPaymentsResponse pendingResponse, PaymentHistoryResponse historyResponse) {
        List<PendingPaymentDto> pendingPayments = pendingResponse != null && pendingResponse.getPayments() != null
                ? pendingResponse.getPayments()
                : List.of();
        List<PaymentHistoryEntryDto> historyEntries = historyResponse != null && historyResponse.getEntries() != null
                ? historyResponse.getEntries()
                : List.of();

        pendingPaymentsBox.getChildren().clear();
        historyBox.getChildren().clear();

        if (pendingPayments.isEmpty()) {
            pendingPaymentsBox.getChildren().add(createEmptyState("No auctions are waiting for payment."));
        } else {
            pendingPayments.forEach(payment -> pendingPaymentsBox.getChildren().add(createPendingRow(payment)));
        }

        if (historyEntries.isEmpty()) {
            historyBox.getChildren().add(createEmptyState("No payment history yet."));
        } else {
            historyEntries.forEach(entry -> historyBox.getChildren().add(createHistoryRow(entry)));
        }

        lblPaymentStatus.setText(pendingPayments.size() + " pending payment(s) | " + historyEntries.size() + " history entr" +
                (historyEntries.size() == 1 ? "y" : "ies"));
    }

    private void renderFailure(Throwable cause) {
        pendingPaymentsBox.getChildren().setAll(createEmptyState("Could not load pending payments."));
        historyBox.getChildren().setAll(createEmptyState("Could not load payment history."));
        lblPaymentStatus.setText("Payment center unavailable: " + cause.getMessage());
    }

    private VBox createPendingRow(PendingPaymentDto payment) {
        VBox itemInfo = new VBox(
                label(payment.getItemName(), "item-name"),
                label(payment.getItemCategory(), "item-category")
        );
        HBox.setHgrow(itemInfo, Priority.ALWAYS);

        Label price = label(formatAmount(payment.getAmount()), "price-tag");
        Button payNow = new Button("Pay now");
        payNow.getStyleClass().add("btn-primary");
        payNow.setDisable(payment.getAuctionId().equals(processingAuctionId));
        payNow.setOnAction(event -> processPayment(payment.getAuctionId(), payNow));

        HBox row = new HBox(12, itemInfo, price, payNow);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox wrapper = new VBox(row);
        wrapper.getStyleClass().add("payment-card");
        return wrapper;
    }

    private HBox createHistoryRow(PaymentHistoryEntryDto entry) {
        VBox left = new VBox(
                label(entry.getItemName(), "history-name"),
                label(formatDate(entry.getPaidAt()), "history-date")
        );
        HBox.setHgrow(left, Priority.ALWAYS);

        Label badge = new Label("RECEIVE".equals(entry.getDirection()) ? "Receive" : "Pay");
        badge.getStyleClass().add("RECEIVE".equals(entry.getDirection()) ? "badge-receive" : "badge-pay");

        VBox right = new VBox(
                badge,
                label(formatAmount(entry.getAmount()), "history-price")
        );
        right.setAlignment(Pos.CENTER_RIGHT);
        right.setSpacing(5);

        HBox row = new HBox(left, right);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");
        return row;
    }

    private void processPayment(String auctionId, Button payNowButton) {
        processingAuctionId = auctionId;
        payNowButton.setDisable(true);
        lblPaymentStatus.setText("Processing payment...");

        paymentClientService.processPayment(auctionId)
                .thenAccept(response -> Platform.runLater(() -> {
                    processingAuctionId = null;
                    lblPaymentStatus.setText("Payment completed for auction " + response.getAuctionId() + ".");
                    reload();
                }))
                .exceptionally(ex -> {
                    Throwable cause = BaseClientService.extractFailure(ex);
                    Platform.runLater(() -> {
                        processingAuctionId = null;
                        payNowButton.setDisable(false);
                        lblPaymentStatus.setText("Payment failed: " + cause.getMessage());
                    });
                    return null;
                });
    }

    private Label createEmptyState(String message) {
        Label label = new Label(message);
        label.getStyleClass().add("item-category");
        return label;
    }

    private Label label(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        return label;
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? "$0" : "$" + amount.toPlainString();
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : dateTime.format(HISTORY_FORMATTER);
    }

    private record PaymentSnapshot(
            PendingPaymentsResponse pendingPayments,
            PaymentHistoryResponse paymentHistory) {}
}
