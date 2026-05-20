package com.nhom1.auction.client.user.service;

import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.exception.AuctionClosedException;
import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.ConflictException;
import com.nhom1.auction.common.exception.InvalidAuctionStateException;
import com.nhom1.auction.common.exception.InvalidBidException;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.PaymentException;
import com.nhom1.auction.common.exception.ServerException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.common.protocol.ErrorCode;
import com.nhom1.auction.common.protocol.ErrorResponse;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/*
 * Abstract base for all client-side services.
 * Responsibilities handled here so subclasses don't repeat them:
 *   1. Sending a RequestMessage through ServerConnection.
 *   2. Unwrapping ResponseMessage<T> -> T on success.
 *   3. Mapping standardized server errors to typed exceptions.
 */
public abstract class BaseClientService {

    protected final ServerConnection connection;

    protected BaseClientService() {
        this.connection = ServerConnection.getInstance();
    }

    protected <T> CompletableFuture<T> send(
        RequestMessage<?> request,
        Class<T> responseClass
    ) {
        return connection
            .sendRequest(request, responseClass)
            .thenApply(this::unwrap)
            .exceptionally(ex -> {
                Throwable cause = extractFailure(ex);
                if (cause instanceof Exception typedException) {
                    throw new CompletionException(typedException);
                }
                throw new CompletionException(
                    new ServerException(
                        ErrorCode.SERVER_ERROR,
                        "Server unreachable: " + cause.getMessage()
                    )
                );
            });
    }

    private <T> T unwrap(ResponseMessage<T> response) {
        if (response.isSuccess()) {
            return response.getPayload();
        }
        throw new CompletionException(mapServerError(response.getError()));
    }

    protected <T> CompletableFuture<T> validationError(String message) {
        return CompletableFuture.failedFuture(new ValidationException(message));
    }

    public static Throwable extractFailure(Throwable throwable) {
        Throwable current = throwable;
        while (
            current.getCause() != null &&
            current.getCause() != current &&
            current instanceof CompletionException
        ) {
            current = current.getCause();
        }
        return current;
    }

    private Exception mapServerError(ErrorResponse error) {
        String message =
            error != null && error.getMessage() != null
                ? error.getMessage()
                : "Unknown server error";
        String code = error != null ? error.getCode() : ErrorCode.SERVER_ERROR;

        return switch (code) {
            case
                ErrorCode.VALIDATION_ERROR,
                ErrorCode.INVALID_FORMAT -> new ValidationException(message);
            case ErrorCode.AUTHENTICATION_FAILED -> new AuthenticationException(
                message
            );
            case ErrorCode.UNAUTHORIZED -> new UnauthorizedActionException(
                message
            );
            case ErrorCode.NOT_FOUND -> new NotFoundException(message);
            case ErrorCode.INVALID_BID -> new InvalidBidException(message);
            case ErrorCode.AUCTION_CLOSED -> new AuctionClosedException(
                message
            );
            case ErrorCode.INVALID_AUCTION_STATE -> new InvalidAuctionStateException(
                message
            );
            case ErrorCode.PAYMENT_FAILED -> new PaymentException(message);
            case ErrorCode.CONFLICT -> new ConflictException(message);
            default -> new ServerException(code, message);
        };
    }
}
