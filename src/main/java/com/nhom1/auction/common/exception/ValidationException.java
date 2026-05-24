package com.nhom1.auction.common.exception;

import com.nhom1.auction.common.protocol.ErrorCode;

public class ValidationException extends AppException {

  public ValidationException(String message) {
    super(ErrorCode.VALIDATION_ERROR, message);
  }
}
