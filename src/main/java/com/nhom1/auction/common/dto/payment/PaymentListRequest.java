package com.nhom1.auction.common.dto.payment;

public class PaymentListRequest {
    private String bidderId;

    public PaymentListRequest() {}

    public PaymentListRequest(String bidderId) {
        this.bidderId = bidderId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
}
