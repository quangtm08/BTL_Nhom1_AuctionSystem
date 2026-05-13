package com.nhom1.auction.client.user.controller;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import com.nhom1.auction.client.user.service.PaymentClientService;
import com.nhom1.auction.common.dto.payment.PaymentItemDto;
import com.nhom1.auction.common.dto.payment.PaymentListResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public class PaymentController {
    private static final DateTimeFormatter HISTORY_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy");

    private final PaymentClientService paymentClientService = new PaymentClientService();
    private String feedbackMessage = "Loading payments...";

    @FXML
    private Label lblPendingSummary;

    @FXML
    private Label lblHistorySummary;

    @FXML
    private Label lblFeedback;

    @FXML
    private Label lblPendingAmount;

    @FXML
    private Label lblHistoryAmount;

    @FXML
    private VBox pendingContainer;

    @FXML
    private VBox historyContainer;

    @FXML
    public void initialize() {
        reload();
    }

    private void reload() {
        renderFeedback(feedbackMessage, "feedback-banner");
        paymentClientService.listPendingPayments()
                .thenCombine(paymentClientService.listPaymentHistory(), this::renderAll)
                .exceptionally(ex -> {
                    Throwable cause = PaymentClientService.extractFailure(ex);
                    Platform.runLater(() -> {
                        feedbackMessage = "Failed to load payments: " + cause.getMessage();
                        renderFeedback(feedbackMessage, "feedback-banner", "feedback-error");
                    });
                    return null;
                });
    }

    private Void renderAll(PaymentListResponse pendingResponse, PaymentListResponse historyResponse) {
        Platform.runLater(() -> {
            List<PaymentItemDto> pending = pendingResponse != null && pendingResponse.getPayments() != null
                    ? pendingResponse.getPayments()
                    : List.of();
            List<PaymentItemDto> history = historyResponse != null && historyResponse.getPayments() != null
                    ? historyResponse.getPayments()
                    : List.of();

            renderPending(pending);
            renderHistory(history);
            if (!feedbackMessage.startsWith("Payment recorded")) {
                feedbackMessage = pending.isEmpty()
                        ? "No pending payments right now."
                        : "Ready to settle your won auctions.";
            }
            renderFeedback(feedbackMessage, "feedback-banner");
        });
        return null;
    }

    private void renderPending(List<PaymentItemDto> payments) {
        pendingContainer.getChildren().clear();
        lblPendingSummary.setText(payments.size() + " pending");
        lblPendingAmount.setText(formatMoney(sumAmounts(payments)));

        if (payments.isEmpty()) {
            pendingContainer.getChildren().add(createEmptyLabel("You have no auctions waiting for payment."));
            return;
        }

        for (PaymentItemDto payment : payments) {
            pendingContainer.getChildren().add(createPendingRow(payment, !pendingContainer.getChildren().isEmpty()));
        }
    }

    private void renderHistory(List<PaymentItemDto> payments) {
        historyContainer.getChildren().clear();
        lblHistorySummary.setText(payments.size() + " completed");
        lblHistoryAmount.setText(formatMoney(sumAmounts(payments)));

        if (payments.isEmpty()) {
            historyContainer.getChildren().add(createEmptyLabel("Your payment history will appear here."));
            return;
        }

        for (PaymentItemDto payment : payments) {
            historyContainer.getChildren().add(createHistoryRow(payment));
        }
    }

    private HBox createPendingRow(PaymentItemDto payment, boolean withDivider) {
        HBox row = new HBox(12);
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("payment-row");
        if (withDivider) {
            row.getStyleClass().add("payment-row-divider");
        }

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label name = new Label(safeText(payment.getItemName(), "Unknown item"));
        name.getStyleClass().add("item-name");
        Label category = new Label(formatCategory(payment.getItemCategory()));
        category.getStyleClass().add("item-category");
        info.getChildren().addAll(name, category);

        Label price = new Label(formatMoney(payment.getAmount()));
        price.getStyleClass().add("price-tag");

        Button payButton = new Button("Pay now");
        payButton.getStyleClass().add("btn-primary");
        payButton.setOnAction(event -> submitPayment(payment.getAuctionId(), payButton));

        row.getChildren().addAll(info, price, payButton);
        return row;
    }

    private HBox createHistoryRow(PaymentItemDto payment) {
        HBox row = new HBox();
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        row.getStyleClass().add("history-row");

        VBox info = new VBox(2);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label name = new Label(safeText(payment.getItemName(), "Unknown item"));
        name.getStyleClass().add("history-name");
        Label date = new Label(formatDate(payment.getEventTime()));
        date.getStyleClass().add("history-date");
        info.getChildren().addAll(name, date);

        VBox right = new VBox(5);
        right.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Label badge = new Label(safeText(payment.getStatusLabel(), "Paid"));
        badge.getStyleClass().add("badge-receive");
        Label price = new Label(formatMoney(payment.getAmount()));
        price.getStyleClass().add("history-price");
        right.getChildren().addAll(badge, price);

        row.getChildren().addAll(info, right);
        return row;
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("empty-state");
        return label;
    }

    private void submitPayment(String auctionId, Button payButton) {
        payButton.setDisable(true);
        feedbackMessage = "Processing payment...";
        renderFeedback(feedbackMessage, "feedback-banner");

        paymentClientService.processPayment(auctionId)
                .thenAccept(response -> Platform.runLater(() -> handlePaymentSuccess(response)))
                .exceptionally(ex -> {
                    Throwable cause = PaymentClientService.extractFailure(ex);
                    Platform.runLater(() -> {
                        payButton.setDisable(false);
                        feedbackMessage = "Payment failed: " + cause.getMessage();
                        renderFeedback(feedbackMessage, "feedback-banner", "feedback-error");
                    });
                    return null;
                });
    }

    private void handlePaymentSuccess(ProcessPaymentResponse response) {
        feedbackMessage = response != null && response.getMessage() != null
                ? response.getMessage()
                : "Payment recorded successfully.";
        renderFeedback(feedbackMessage, "feedback-banner", "feedback-success");
        reload();
    }

    private void renderFeedback(String text, String... styleClasses) {
        lblFeedback.setText(text);
        lblFeedback.getStyleClass().setAll(styleClasses);
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatMoney(BigDecimal amount) {
        if (amount == null) {
            return "$0";
        }
        return "$" + NumberFormat.getNumberInstance(Locale.US).format(amount);
    }

    private String formatDate(LocalDateTime eventTime) {
        if (eventTime == null) {
            return "Unknown date";
        }
        return eventTime.format(HISTORY_FORMATTER);
    }

    private BigDecimal sumAmounts(List<PaymentItemDto> payments) {
        return payments.stream()
                .flatMap(payment -> Stream.ofNullable(payment.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatCategory(String raw) {
        String value = safeText(raw, "Unknown category").replace('_', ' ').toLowerCase(Locale.ROOT);
        if (value.isBlank()) {
            return "Unknown category";
        }

        String[] words = value.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) {
                builder.append(word.substring(1));
            }
        }
        return builder.toString();
    }

}
