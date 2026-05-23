package com.nhom1.auction.common.exception;

import com.nhom1.auction.common.protocol.ErrorCode;

public class UnauthorizedActionException extends AppException {

  public UnauthorizedActionException(String message) {
    super(ErrorCode.UNAUTHORIZED, message);
  }
}
