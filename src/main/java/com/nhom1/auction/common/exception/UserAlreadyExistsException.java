package com.nhom1.auction.common.exception;

import com.nhom1.auction.common.protocol.ErrorCode;

public class UserAlreadyExistsException extends AppException {

  public UserAlreadyExistsException(String message) {
    super(ErrorCode.VALIDATION_ERROR, message);
  }
}
