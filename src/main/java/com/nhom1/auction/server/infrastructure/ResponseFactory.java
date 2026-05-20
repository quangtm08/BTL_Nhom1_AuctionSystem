package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.common.exception.AppException;
import com.nhom1.auction.common.protocol.ErrorCode;
import com.nhom1.auction.common.protocol.ResponseMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ResponseFactory {
    private static final Logger LOGGER = Logger.getLogger(ResponseFactory.class.getName());

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

        IllegalArgumentException illegalArgumentException = findIllegalArgumentException(throwable);
        if (illegalArgumentException != null) {
            return new ResponseMessage<>(
                    requestId,
                    ErrorCode.VALIDATION_ERROR,
                    illegalArgumentException.getMessage()
            );
        }

        logUnexpectedException(requestId, throwable);
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

    private static IllegalArgumentException findIllegalArgumentException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != current) {
            if (current instanceof IllegalArgumentException illegalArgumentException) {
                return illegalArgumentException;
            }
            current = current.getCause();
        }
        return null;
    }

    private static void logUnexpectedException(String requestId, Throwable throwable) {
        if (throwable == null) {
            return;
        }
        LOGGER.log(
                Level.SEVERE,
                "Unexpected exception while handling requestId=" + requestId,
                throwable
        );
    }
}
