package com.nhom1.auction.common.dto.wallet;

public class GetWalletRequest {
  private String userId;

  public GetWalletRequest() {}

  public GetWalletRequest(String userId) {
    this.userId = userId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }
}
