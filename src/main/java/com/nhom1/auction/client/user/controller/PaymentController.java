package com.nhom1.auction.client.user.controller;

import com.nhom1.auction.client.user.service.BaseClientService;
import com.nhom1.auction.client.user.service.PaymentClientService;
import com.nhom1.auction.client.util.DisplayFormatters;
import com.nhom1.auction.common.dto.payment.PaymentHistoryEntryDto;
import com.nhom1.auction.common.dto.payment.PaymentHistoryResponse;
import com.nhom1.auction.common.dto.payment.PendingPaymentDto;
import com.nhom1.auction.common.dto.payment.PendingPaymentsResponse;
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

    private final PaymentClientService paymentClientService =
        new PaymentClientService();
    private String processingAuctionId;

    @FXML
    private VBox pendingPaymentsBox;

    @FXML
    private VBox historyBox;

    @FXML
    private javafx.scene.layout.VBox loadingBox;

    @FXML
    private javafx.scene.control.ScrollPane contentBox;

    private void showContent() {
        if (loadingBox != null) {
            loadingBox.setVisible(false);
            loadingBox.setManaged(false);
        }
        if (contentBox != null) {
            contentBox.setVisible(true);
            contentBox.setManaged(true);
        }
    }

    @FXML
    public void initialize() {
        reload();
    }

    private void reload() {
        paymentClientService
            .listPendingPayments()
            .thenCombine(
                paymentClientService.listPaymentHistory(),
                PaymentSnapshot::new
            )
            .thenAccept(snapshot ->
                Platform.runLater(() ->
                    render(
                        snapshot.pendingPayments(),
                        snapshot.paymentHistory()
                    )
                )
            )
            .exceptionally(ex -> {
                Throwable cause = BaseClientService.extractFailure(ex);
                Platform.runLater(() -> renderFailure(cause));
                return null;
            });
    }

    private void render(
        PendingPaymentsResponse pendingResponse,
        PaymentHistoryResponse historyResponse
    ) {
        showContent();
        List<PendingPaymentDto> pendingPayments =
            pendingResponse != null && pendingResponse.getPayments() != null
                ? pendingResponse.getPayments()
                : List.of();
        List<PaymentHistoryEntryDto> historyEntries =
            historyResponse != null && historyResponse.getEntries() != null
                ? historyResponse.getEntries()
                : List.of();

        pendingPaymentsBox.getChildren().clear();
        historyBox.getChildren().clear();

        if (pendingPayments.isEmpty()) {
            pendingPaymentsBox
                .getChildren()
                .add(createEmptyState("No auctions are waiting for payment."));
        } else {
            pendingPayments.forEach(payment ->
                pendingPaymentsBox.getChildren().add(createPendingRow(payment))
            );
        }

        if (historyEntries.isEmpty()) {
            historyBox
                .getChildren()
                .add(createEmptyState("No payment history yet."));
        } else {
            historyEntries.forEach(entry ->
                historyBox.getChildren().add(createHistoryRow(entry))
            );
        }
    }

    private void renderFailure(Throwable cause) {
        showContent();
        pendingPaymentsBox
            .getChildren()
            .setAll(createEmptyState("Could not load pending payments."));
        historyBox
            .getChildren()
            .setAll(createEmptyState("Could not load payment history."));
    }

    private VBox createPendingRow(PendingPaymentDto payment) {
        VBox itemInfo = new VBox(
            label(payment.getItemName(), "item-name"),
            label(payment.getItemCategory(), "item-category")
        );
        HBox.setHgrow(itemInfo, Priority.ALWAYS);

        Label price = label(
            DisplayFormatters.money(payment.getAmount()),
            "price-tag"
        );
        Button payNow = new Button("Pay now");
        payNow.getStyleClass().add("btn-primary");
        String auctionId = payment.getAuctionId();
        boolean missingAuctionId = auctionId == null || auctionId.isBlank();
        payNow.setDisable(
            missingAuctionId || auctionId.equals(processingAuctionId)
        );
        payNow.setOnAction(event -> processPayment(auctionId, payNow));

        HBox row = new HBox(12, itemInfo, price, payNow);
        row.setAlignment(Pos.CENTER_LEFT);

        VBox wrapper = new VBox(row);
        wrapper.getStyleClass().add("payment-card");
        return wrapper;
    }

    private HBox createHistoryRow(PaymentHistoryEntryDto entry) {
        VBox left = new VBox(
            label(entry.getItemName(), "history-name"),
            label(
                DisplayFormatters.shortDate(entry.getPaidAt()),
                "history-date"
            )
        );
        HBox.setHgrow(left, Priority.ALWAYS);

        Label badge = new Label(
            "RECEIVE".equals(entry.getDirection()) ? "Receive" : "Pay"
        );
        badge
            .getStyleClass()
            .add(
                "RECEIVE".equals(entry.getDirection())
                    ? "badge-receive"
                    : "badge-pay"
            );

        VBox right = new VBox(
            badge,
            label(DisplayFormatters.money(entry.getAmount()), "history-price")
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

        paymentClientService
            .processPayment(auctionId)
            .thenAccept(response ->
                Platform.runLater(() -> {
                    processingAuctionId = null;
                    reload();
                })
            )
            .exceptionally(ex -> {
                Throwable cause = BaseClientService.extractFailure(ex);
                Platform.runLater(() -> {
                    processingAuctionId = null;
                    payNowButton.setDisable(false);
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

    private record PaymentSnapshot(
        PendingPaymentsResponse pendingPayments,
        PaymentHistoryResponse paymentHistory
    ) {}
}
