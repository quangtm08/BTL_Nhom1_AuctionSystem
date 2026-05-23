package com.nhom1.auction.common.protocol;

public final class ErrorCode {
  public static final String INVALID_FORMAT = "INVALID_FORMAT";
  public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
  public static final String AUTHENTICATION_FAILED = "AUTHENTICATION_FAILED";
  public static final String UNAUTHORIZED = "UNAUTHORIZED";
  public static final String NOT_FOUND = "NOT_FOUND";
  public static final String INVALID_AUCTION_STATE = "INVALID_AUCTION_STATE";
  public static final String INVALID_BID = "INVALID_BID";
  public static final String AUCTION_CLOSED = "AUCTION_CLOSED";
  public static final String PAYMENT_FAILED = "PAYMENT_FAILED";
  public static final String CONFLICT = "CONFLICT";
  public static final String SERVER_ERROR = "SERVER_ERROR";

  private ErrorCode() {}
}
