package com.nhom1.auction.common.dto.admin;

public class AdminListAuctionsRequest {
  private String callerId;

  public AdminListAuctionsRequest() {}

  public AdminListAuctionsRequest(String callerId) {
    this.callerId = callerId;
  }

  public String getCallerId() {
    return callerId;
  }

  public void setCallerId(String callerId) {
    this.callerId = callerId;
  }
}
