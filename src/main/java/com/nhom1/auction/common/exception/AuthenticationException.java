package com.nhom1.auction.common.exception;

import com.nhom1.auction.common.protocol.ErrorCode;

public class AuthenticationException extends AppException {

  public AuthenticationException(String message) {
    super(ErrorCode.AUTHENTICATION_FAILED, message);
  }
}
