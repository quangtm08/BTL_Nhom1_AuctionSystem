package com.nhom1.auction.common.dto.admin;

public class AdminListUsersRequest {
  private String callerId;

  public AdminListUsersRequest() {}

  public AdminListUsersRequest(String callerId) {
    this.callerId = callerId;
  }

  public String getCallerId() {
    return callerId;
  }

  public void setCallerId(String callerId) {
    this.callerId = callerId;
  }
}
