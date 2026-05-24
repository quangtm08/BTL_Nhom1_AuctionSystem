package com.nhom1.auction.common.protocol;

import java.util.UUID;

/*
Generic container for all client -> server requests.
parameter <T> The specific payload type (e.g., LoginRequest)
*/
public class RequestMessage<T> {
  private MessageType type;
  private String requestId;
  private T payload;

  public RequestMessage() {
    this.requestId = UUID.randomUUID().toString();
  }

  public RequestMessage(MessageType type, T payload) {
    this();
    this.type = type;
    this.payload = payload;
  }

  // Getters and Setters
  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }

  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public T getPayload() {
    return payload;
  }

  public void setPayload(T payload) {
    this.payload = payload;
  }
}
