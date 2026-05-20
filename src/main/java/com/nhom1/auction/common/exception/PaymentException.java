package com.nhom1.auction.common.exception;

import com.nhom1.auction.common.protocol.ErrorCode;

public class PaymentException extends AppException {

    public PaymentException(String message) {
        super(ErrorCode.PAYMENT_FAILED, message);
    }
}
