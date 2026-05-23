package com.nhom1.auction.common.exception;

import com.nhom1.auction.common.protocol.ErrorCode;

public class AuctionClosedException extends AppException {

  public AuctionClosedException(String message) {
    super(ErrorCode.AUCTION_CLOSED, message);
  }
}
