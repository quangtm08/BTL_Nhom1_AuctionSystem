package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.AuctionClosedException;
import com.nhom1.auction.common.exception.ConflictException;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.InvalidBidException;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.PaymentException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.UserAlreadyExistsException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.common.protocol.ErrorCode;
import com.nhom1.auction.common.protocol.ResponseMessage;
import java.sql.SQLException;

public final class ResponseFactory {

    private ResponseFactory() {
    }

    public static <T> ResponseMessage<T> success(String requestId, T payload) {
        return new ResponseMessage<>(requestId, payload);
    }

    public static <T> ResponseMessage<T> invalidFormat(String requestId, String message) {
        return new ResponseMessage<>(requestId, ErrorCode.INVALID_FORMAT, message);
    }

    public static <T> ResponseMessage<T> fromException(String requestId, Throwable throwable) {
        Throwable cause = unwrap(throwable);
        String code = mapCode(cause);
        String message = cause.getMessage() == null || cause.getMessage().isBlank()
                ? "Unexpected server error"
                : cause.getMessage();
        ResponseMessage<T> response = new ResponseMessage<>(requestId, code, message);
        if (response.getError() != null && code.equals(ErrorCode.SERVER_ERROR) && cause.getClass() != RuntimeException.class) {
            response.getError().setDetails(cause.getClass().getSimpleName());
        }
        return response;
    }

    public static Throwable unwrap(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null
                && current.getCause() != current
                && (current instanceof RuntimeException
                || current instanceof java.util.concurrent.CompletionException)) {
            current = current.getCause();
        }
        return current;
    }

    private static String mapCode(Throwable cause) {
        if (cause instanceof ValidationException || cause instanceof IllegalArgumentException || cause instanceof UserAlreadyExistsException) {
            return ErrorCode.VALIDATION_ERROR;
        }
        if (cause instanceof AuthenticationException) {
            return ErrorCode.AUTHENTICATION_FAILED;
        }
        if (cause instanceof UnauthorizedActionException) {
            return ErrorCode.UNAUTHORIZED;
        }
        if (cause instanceof NotFoundException) {
            return ErrorCode.NOT_FOUND;
        }
        if (cause instanceof InvalidAuctionStateException) {
            return ErrorCode.INVALID_AUCTION_STATE;
        }
        if (cause instanceof InvalidBidException) {
            return ErrorCode.INVALID_BID;
        }
        if (cause instanceof AuctionClosedException) {
            return ErrorCode.AUCTION_CLOSED;
        }
        if (cause instanceof PaymentException) {
            return ErrorCode.PAYMENT_FAILED;
        }
        if (cause instanceof ConflictException) {
            return ErrorCode.CONFLICT;
        }
        if (cause instanceof SQLException) {
            return ErrorCode.SERVER_ERROR;
        }
        return ErrorCode.SERVER_ERROR;
    }
}
