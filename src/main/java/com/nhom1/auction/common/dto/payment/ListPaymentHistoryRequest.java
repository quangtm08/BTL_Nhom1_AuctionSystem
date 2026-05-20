package com.nhom1.auction.common.dto.payment;

public class ListPaymentHistoryRequest {
    private String userId;

    public ListPaymentHistoryRequest() {
    }

    public ListPaymentHistoryRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
