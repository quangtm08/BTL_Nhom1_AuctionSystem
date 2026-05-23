package com.nhom1.auction.common.dto.admin;

public class AdminDeleteUserRequest {
  private String targetUserId;
  private String callerId;

  public AdminDeleteUserRequest() {}

  public AdminDeleteUserRequest(String targetUserId, String callerId) {
    this.targetUserId = targetUserId;
    this.callerId = callerId;
  }

  public String getTargetUserId() {
    return targetUserId;
  }

  public void setTargetUserId(String targetUserId) {
    this.targetUserId = targetUserId;
  }

  public String getCallerId() {
    return callerId;
  }

  public void setCallerId(String callerId) {
    this.callerId = callerId;
  }
}
