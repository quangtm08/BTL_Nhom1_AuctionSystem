package com.nhom1.auction.common.dto.payment;

import java.util.List;

public class PaymentListResponse {
    private List<PaymentItemDto> payments;

    public PaymentListResponse() {}

    public PaymentListResponse(List<PaymentItemDto> payments) {
        this.payments = payments;
    }

    public List<PaymentItemDto> getPayments() {
        return payments;
    }

    public void setPayments(List<PaymentItemDto> payments) {
        this.payments = payments;
    }
}
