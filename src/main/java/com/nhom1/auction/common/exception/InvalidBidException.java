package com.nhom1.auction.common.exception;

import com.nhom1.auction.common.protocol.ErrorCode;

public class InvalidBidException extends AppException {

    public InvalidBidException(String message) {
        super(ErrorCode.INVALID_BID, message);
    }
}
