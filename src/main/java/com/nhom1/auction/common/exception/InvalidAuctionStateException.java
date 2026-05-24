package com.nhom1.auction.common.exception;

import com.nhom1.auction.common.protocol.ErrorCode;

public class InvalidAuctionStateException extends AppException {

  public InvalidAuctionStateException(String message) {
    super(ErrorCode.INVALID_AUCTION_STATE, message);
  }
}
