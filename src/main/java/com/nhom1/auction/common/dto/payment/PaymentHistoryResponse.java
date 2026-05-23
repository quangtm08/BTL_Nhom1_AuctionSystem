package com.nhom1.auction.common.dto.payment;

import java.util.List;

public class PaymentHistoryResponse {
  private List<PaymentHistoryEntryDto> entries;

  public PaymentHistoryResponse() {}

  public PaymentHistoryResponse(List<PaymentHistoryEntryDto> entries) {
    this.entries = entries;
  }

  public List<PaymentHistoryEntryDto> getEntries() {
    return entries;
  }

  public void setEntries(List<PaymentHistoryEntryDto> entries) {
    this.entries = entries;
  }
}
