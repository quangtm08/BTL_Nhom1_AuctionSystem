package com.nhom1.auction.common.exception;

import com.nhom1.auction.common.protocol.ErrorCode;

public class ConflictException extends AppException {

  public ConflictException(String message) {
    super(ErrorCode.CONFLICT, message);
  }
}
