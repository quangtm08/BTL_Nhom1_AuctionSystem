package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.common.exception.AppException;
import com.nhom1.auction.common.protocol.ErrorCode;
import com.nhom1.auction.common.protocol.ResponseMessage;

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
        AppException appException = findAppException(throwable);
        if (appException != null) {
            return new ResponseMessage<>(
                    requestId,
                    appException.getCode(),
                    appException.getMessage()
            );
        }

        if (throwable != null) {
            throwable.printStackTrace();
        }
        return new ResponseMessage<>(
                requestId,
                ErrorCode.SERVER_ERROR,
                "Unexpected server error"
        );
    }

    public static AppException findAppException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            if (current instanceof AppException appException) {
                return appException;
            }
            current = current.getCause();
        }
        return null;
    }
}
