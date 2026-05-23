package com.nhom1.auction.common.dto.payment;

public class ListPendingPaymentsRequest {
  private String bidderId;

  public ListPendingPaymentsRequest() {}

  public ListPendingPaymentsRequest(String bidderId) {
    this.bidderId = bidderId;
  }

  public String getBidderId() {
    return bidderId;
  }

  public void setBidderId(String bidderId) {
    this.bidderId = bidderId;
  }
}
