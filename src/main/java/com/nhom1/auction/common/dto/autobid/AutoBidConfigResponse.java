package com.nhom1.auction.common.dto.autobid;

public class AutoBidConfigResponse {
  private String status;

  public AutoBidConfigResponse() {}

  public AutoBidConfigResponse(String status) {
    this.status = status;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
