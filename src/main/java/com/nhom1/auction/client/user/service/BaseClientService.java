package com.nhom1.auction.client.user.service;

import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.exception.AuctionException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/*
 * Abstract base for all client-side services.
 * Responsibilities handled here so subclasses don't repeat them:
 *   1. Sending a RequestMessage through ServerConnection.
 *   2. Unwrapping ResponseMessage<T> → T on success.
 *   3. Wrapping server errors and network failures into AuctionException.
 *
 * Subclasses: build the RequestMessage, call send(), return the future.
 * Controllers: only do Platform.runLater() + UI updates.
 *
 * TODO: once team agrees on standardized server error codes, add typed
 *       mapping in unwrap() (e.g. "AUTH_FAILED" → AuthenticationException).
 */
public abstract class BaseClientService {

    protected final ServerConnection connection;

    protected BaseClientService() {
        this.connection = ServerConnection.getInstance();
    }

    protected <T> CompletableFuture<T> send(RequestMessage<?> request, Class<T> responseClass) {
        return connection
            .sendRequest(request, responseClass)
            .thenApply(this::unwrap)
            .exceptionally(ex -> {
                // Keep validation errors intact so controllers can show the
                // original fail-fast message instead of a generic network error.
                Throwable cause = extractFailure(ex);
                if (cause instanceof AuctionException ae) {
                    throw new CompletionException(ae);
                }
                if (cause instanceof ValidationException ve) {
                    throw new CompletionException(ve);
                }
                throw new CompletionException(new AuctionException("Server unreachable: " + cause.getMessage()));
            });
    }

    private <T> T unwrap(ResponseMessage<T> response) {
        if (response.isSuccess()) {
            return response.getPayload();
        }
        String message = response.getError() != null ? response.getError().getMessage() : "Unknown server error";
        throw new CompletionException(new AuctionException(message));
    }

    // Fail fast before hitting the network. Usage: if (x.isBlank()) return validationError("...");
    protected <T> CompletableFuture<T> validationError(String message) {
        return CompletableFuture.failedFuture(new ValidationException(message));
    }

    // CompletableFuture often wraps the real failure in CompletionException.
    // Controllers/services can reuse this instead of duplicating unwrap logic.
    public static Throwable extractFailure(Throwable throwable) {
        return throwable.getCause() != null ? throwable.getCause() : throwable;
    }
}
