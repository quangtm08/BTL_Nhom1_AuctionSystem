package com.nhom1.auction.common.dto.notification;

public class UserDeletedEvent {
  private String userId;

  public UserDeletedEvent() {}

  public UserDeletedEvent(String userId) {
    this.userId = userId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }
}
