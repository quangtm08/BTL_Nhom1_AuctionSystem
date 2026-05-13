package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.dto.payment.PaymentListRequest;
import com.nhom1.auction.common.dto.payment.PaymentListResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentRequest;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.util.concurrent.CompletableFuture;

public class PaymentClientService extends BaseClientService {

    public CompletableFuture<PaymentListResponse> listPendingPayments() {
        AuthResponse user = requireUser();
        if (user == null) {
            return validationError("User not logged in.");
        }

        return send(
                new RequestMessage<>(MessageType.LIST_PENDING_PAYMENTS, new PaymentListRequest(user.getUserID())),
                PaymentListResponse.class);
    }

    public CompletableFuture<PaymentListResponse> listPaymentHistory() {
        AuthResponse user = requireUser();
        if (user == null) {
            return validationError("User not logged in.");
        }

        return send(
                new RequestMessage<>(MessageType.LIST_PAYMENT_HISTORY, new PaymentListRequest(user.getUserID())),
                PaymentListResponse.class);
    }

    public CompletableFuture<ProcessPaymentResponse> processPayment(String auctionId) {
        AuthResponse user = requireUser();
        if (user == null) {
            return validationError("User not logged in.");
        }
        if (auctionId == null || auctionId.isBlank()) {
            return validationError("Auction ID is required.");
        }

        return send(
                new RequestMessage<>(MessageType.PROCESS_PAYMENT, new ProcessPaymentRequest(auctionId, user.getUserID())),
                ProcessPaymentResponse.class);
    }

    private AuthResponse requireUser() {
        AuthResponse currentUser = AppContext.getCurrentUser();
        if (currentUser == null || currentUser.getUserID() == null || currentUser.getUserID().isBlank()) {
            return null;
        }
        return currentUser;
    }
}
