package com.nhom1.auction.common.protocol;

/*
Generic container for all server responses to client.
parameter <T> The success payload type (e.g., AuthResponse)
*/

public class ResponseMessage<T> {
  private String requestId;
  private boolean success;
  private T payload;
  private ErrorResponse error;
  private MessageType type;

  public ResponseMessage() {}

  // Convenience constructor for success
  public ResponseMessage(String requestId, T payload) {
    this.requestId = requestId;
    this.success = true;
    this.payload = payload;
  }

  // Convenience constructor for error
  public ResponseMessage(String requestId, String errorCode, String errorMessage) {
    this.requestId = requestId;
    this.success = false;
    this.error = new ErrorResponse(errorCode, errorMessage);
  }

  // Getters and Setters
  public String getRequestId() {
    return requestId;
  }

  public void setRequestId(String requestId) {
    this.requestId = requestId;
  }

  public boolean isSuccess() {
    return success;
  }

  public void setSuccess(boolean success) {
    this.success = success;
  }

  public T getPayload() {
    return payload;
  }

  public void setPayload(T payload) {
    this.payload = payload;
  }

  public ErrorResponse getError() {
    return error;
  }

  public void setError(ErrorResponse error) {
    this.error = error;
  }

  public MessageType getType() {
    return type;
  }

  public void setType(MessageType type) {
    this.type = type;
  }
}
