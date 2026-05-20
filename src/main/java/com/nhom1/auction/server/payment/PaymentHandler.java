package com.nhom1.auction.server.payment;

import com.nhom1.auction.common.dto.payment.ListPaymentHistoryRequest;
import com.nhom1.auction.common.dto.payment.ListPendingPaymentsRequest;
import com.nhom1.auction.common.dto.payment.PaymentHistoryResponse;
import com.nhom1.auction.common.dto.payment.PendingPaymentsResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentRequest;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.ResponseFactory;

public class PaymentHandler {
    private final PaymentService paymentService;

    public PaymentHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void register(MessageRouter router) {
        router.register(MessageType.PROCESS_PAYMENT, (requestId, payloadJson) -> {
            try {
                ProcessPaymentRequest dto = JsonUtil.fromJson(payloadJson, ProcessPaymentRequest.class);
                return handleProcessPayment(requestId, dto);
            } catch (Exception e) {
                return ResponseFactory.invalidFormat(requestId, "Invalid process payment JSON");
            }
        });

        router.register(MessageType.LIST_PENDING_PAYMENTS, (requestId, payloadJson) -> {
            try {
                ListPendingPaymentsRequest dto = JsonUtil.fromJson(payloadJson, ListPendingPaymentsRequest.class);
                return handleListPendingPayments(requestId, dto);
            } catch (Exception e) {
                return ResponseFactory.invalidFormat(requestId, "Invalid pending payments JSON");
            }
        });

        router.register(MessageType.LIST_PAYMENT_HISTORY, (requestId, payloadJson) -> {
            try {
                ListPaymentHistoryRequest dto = JsonUtil.fromJson(payloadJson, ListPaymentHistoryRequest.class);
                return handlePaymentHistory(requestId, dto);
            } catch (Exception e) {
                return ResponseFactory.invalidFormat(requestId, "Invalid payment history JSON");
            }
        });
    }

    private ResponseMessage<ProcessPaymentResponse> handleProcessPayment(String requestId, ProcessPaymentRequest dto) {
        try {
            if (dto == null) {
                return ResponseFactory.invalidFormat(requestId, "Missing process payment payload.");
            }
            return ResponseFactory.success(requestId, paymentService.processPayment(dto.getAuctionId(), dto.getBidderId()));
        } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
        }
    }

    private ResponseMessage<PendingPaymentsResponse> handleListPendingPayments(String requestId, ListPendingPaymentsRequest dto) {
        try {
            if (dto == null) {
                return ResponseFactory.invalidFormat(requestId, "Missing pending payments payload.");
            }
            return ResponseFactory.success(requestId, paymentService.listPendingPayments(dto.getBidderId()));
        } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
        }
    }

    private ResponseMessage<PaymentHistoryResponse> handlePaymentHistory(String requestId, ListPaymentHistoryRequest dto) {
        try {
            if (dto == null) {
                return ResponseFactory.invalidFormat(requestId, "Missing payment history payload.");
            }
            return ResponseFactory.success(requestId, paymentService.listPaymentHistory(dto.getUserId()));
        } catch (Exception e) {
            return ResponseFactory.fromException(requestId, e);
        }
    }
}
