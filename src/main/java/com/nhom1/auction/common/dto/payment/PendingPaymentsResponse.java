package com.nhom1.auction.common.dto.payment;

import java.util.List;

public class PendingPaymentsResponse {
    private List<PendingPaymentDto> payments;

    public PendingPaymentsResponse() {
    }

    public PendingPaymentsResponse(List<PendingPaymentDto> payments) {
        this.payments = payments;
    }

    public List<PendingPaymentDto> getPayments() {
        return payments;
    }

    public void setPayments(List<PendingPaymentDto> payments) {
        this.payments = payments;
    }
}
