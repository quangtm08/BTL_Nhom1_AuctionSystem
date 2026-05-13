package com.nhom1.auction.server.payment;

import com.nhom1.auction.common.dto.payment.PaymentListRequest;
import com.nhom1.auction.common.dto.payment.PaymentListResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentRequest;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;

public class PaymentHandler {
    private final PaymentService paymentService;

    public PaymentHandler(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    public void register(MessageRouter router) {
        router.register(MessageType.LIST_PENDING_PAYMENTS, (requestId, payloadJson) -> {
            try {
                return handlePendingPayments(requestId, JsonUtil.fromJson(payloadJson, PaymentListRequest.class));
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid pending payments JSON");
            }
        });

        router.register(MessageType.LIST_PAYMENT_HISTORY, (requestId, payloadJson) -> {
            try {
                return handlePaymentHistory(requestId, JsonUtil.fromJson(payloadJson, PaymentListRequest.class));
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid payment history JSON");
            }
        });

        router.register(MessageType.PROCESS_PAYMENT, (requestId, payloadJson) -> {
            try {
                return handleProcessPayment(requestId, JsonUtil.fromJson(payloadJson, ProcessPaymentRequest.class));
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Invalid process payment JSON");
            }
        });
    }

    private ResponseMessage<PaymentListResponse> handlePendingPayments(String requestId, PaymentListRequest dto) {
        try {
            if (dto == null) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Missing pending payments payload.");
            }
            return new ResponseMessage<>(requestId, paymentService.getPendingPayments(dto.getBidderId()));
        } catch (Exception e) {
            return new ResponseMessage<>(requestId, "PAYMENT_ACTION_FAILED", e.getMessage());
        }
    }

    private ResponseMessage<PaymentListResponse> handlePaymentHistory(String requestId, PaymentListRequest dto) {
        try {
            if (dto == null) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Missing payment history payload.");
            }
            return new ResponseMessage<>(requestId, paymentService.getPaymentHistory(dto.getBidderId()));
        } catch (Exception e) {
            return new ResponseMessage<>(requestId, "PAYMENT_ACTION_FAILED", e.getMessage());
        }
    }

    private ResponseMessage<ProcessPaymentResponse> handleProcessPayment(String requestId, ProcessPaymentRequest dto) {
        try {
            if (dto == null) {
                return new ResponseMessage<>(requestId, "INVALID_FORMAT", "Missing process payment payload.");
            }
            return new ResponseMessage<>(requestId, paymentService.processPayment(dto.getAuctionId(), dto.getBidderId()));
        } catch (Exception e) {
            return new ResponseMessage<>(requestId, "PAYMENT_ACTION_FAILED", e.getMessage());
        }
    }
}
