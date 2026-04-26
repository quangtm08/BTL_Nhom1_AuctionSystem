package com.nhom1.auction.common.dto.payment;

public class ProcessPaymentResponse {
    private String status;

    public ProcessPaymentResponse() {}

    public ProcessPaymentResponse(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
