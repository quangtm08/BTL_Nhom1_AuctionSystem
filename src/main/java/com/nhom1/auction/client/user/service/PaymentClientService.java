package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.dto.payment.ListPaymentHistoryRequest;
import com.nhom1.auction.common.dto.payment.ListPendingPaymentsRequest;
import com.nhom1.auction.common.dto.payment.PaymentHistoryResponse;
import com.nhom1.auction.common.dto.payment.PendingPaymentsResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentRequest;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.util.concurrent.CompletableFuture;

public class PaymentClientService extends BaseClientService {

  public CompletableFuture<PendingPaymentsResponse> listPendingPayments() {
    AuthResponse currentUser = AppContext.getCurrentUser();
    if (currentUser == null
        || currentUser.getUserID() == null
        || currentUser.getUserID().isBlank()) {
      return validationError("You must sign in before viewing pending payments.");
    }
    return send(
        new RequestMessage<>(
            MessageType.LIST_PENDING_PAYMENTS,
            new ListPendingPaymentsRequest(currentUser.getUserID())),
        PendingPaymentsResponse.class);
  }

  public CompletableFuture<PaymentHistoryResponse> listPaymentHistory() {
    AuthResponse currentUser = AppContext.getCurrentUser();
    if (currentUser == null
        || currentUser.getUserID() == null
        || currentUser.getUserID().isBlank()) {
      return validationError("You must sign in before viewing payment history.");
    }
    return send(
        new RequestMessage<>(
            MessageType.LIST_PAYMENT_HISTORY,
            new ListPaymentHistoryRequest(currentUser.getUserID())),
        PaymentHistoryResponse.class);
  }

  public CompletableFuture<ProcessPaymentResponse> processPayment(String auctionId) {
    if (auctionId == null || auctionId.isBlank()) {
      return validationError("Auction ID is required.");
    }
    AuthResponse currentUser = AppContext.getCurrentUser();
    if (currentUser == null
        || currentUser.getUserID() == null
        || currentUser.getUserID().isBlank()) {
      return validationError("You must sign in before paying.");
    }
    return send(
        new RequestMessage<>(
            MessageType.PROCESS_PAYMENT,
            new ProcessPaymentRequest(auctionId, currentUser.getUserID())),
        ProcessPaymentResponse.class);
  }
}
