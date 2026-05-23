package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.exception.*;
import com.nhom1.auction.common.protocol.ErrorCode;
import com.nhom1.auction.common.protocol.ErrorResponse;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.client.user.connection.ServerConnection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BaseClientServiceTest {

    private ServerConnection mockConnection;

    // Concrete implementation for testing
    private static class TestClientService extends BaseClientService {
        public TestClientService() {
            super();
        }

        public CompletableFuture<String> publicSend(RequestMessage<?> request) {
            return super.send(request, String.class);
        }

        public Exception publicMapServerError(ErrorResponse error) throws Exception {
            Method method = BaseClientService.class.getDeclaredMethod("mapServerError", ErrorResponse.class);
            method.setAccessible(true);
            return (Exception) method.invoke(this, error);
        }

        public Object publicUnwrap(ResponseMessage<?> response) throws Exception {
             Method method = BaseClientService.class.getDeclaredMethod("unwrap", ResponseMessage.class);
             method.setAccessible(true);
             try {
                 return method.invoke(this, response);
             } catch (java.lang.reflect.InvocationTargetException e) {
                 throw (Exception) e.getCause();
             }
        }
    }

    @BeforeEach
    public void setUp() throws Exception {
        mockConnection = mock(ServerConnection.class);
        Field instanceField = ServerConnection.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, mockConnection);
    }

    @Test
    public void testMapServerErrorAllCodes() throws Exception {
        TestClientService service = new TestClientService();

        // 1. Validation Error
        assertTrue(service.publicMapServerError(new ErrorResponse(ErrorCode.VALIDATION_ERROR, "msg")) instanceof ValidationException);
        // 2. Invalid Format
        assertTrue(service.publicMapServerError(new ErrorResponse(ErrorCode.INVALID_FORMAT, "msg")) instanceof ValidationException);
        // 3. Authentication Failed
        assertTrue(service.publicMapServerError(new ErrorResponse(ErrorCode.AUTHENTICATION_FAILED, "msg")) instanceof AuthenticationException);
        // 4. Unauthorized
        assertTrue(service.publicMapServerError(new ErrorResponse(ErrorCode.UNAUTHORIZED, "msg")) instanceof UnauthorizedActionException);
        // 5. Not Found
        assertTrue(service.publicMapServerError(new ErrorResponse(ErrorCode.NOT_FOUND, "msg")) instanceof NotFoundException);
        // 6. Invalid Bid
        assertTrue(service.publicMapServerError(new ErrorResponse(ErrorCode.INVALID_BID, "msg")) instanceof InvalidBidException);
        // 7. Auction Closed
        assertTrue(service.publicMapServerError(new ErrorResponse(ErrorCode.AUCTION_CLOSED, "msg")) instanceof AuctionClosedException);
        // 8. Invalid Auction State
        assertTrue(service.publicMapServerError(new ErrorResponse(ErrorCode.INVALID_AUCTION_STATE, "msg")) instanceof InvalidAuctionStateException);
        // 9. Payment Failed
        assertTrue(service.publicMapServerError(new ErrorResponse(ErrorCode.PAYMENT_FAILED, "msg")) instanceof PaymentException);
        // 10. Conflict
        assertTrue(service.publicMapServerError(new ErrorResponse(ErrorCode.CONFLICT, "msg")) instanceof ConflictException);
        // 11. Unknown / Default Code
        Exception defaultEx = service.publicMapServerError(new ErrorResponse("SOMETHING_ELSE", "msg"));
        assertTrue(defaultEx instanceof ServerException);
        assertEquals("SOMETHING_ELSE", ((ServerException) defaultEx).getCode());

        // Test null ErrorResponse or null message inside ErrorResponse
        Exception nullErrorEx = service.publicMapServerError(null);
        assertTrue(nullErrorEx instanceof ServerException);
        assertEquals("Unknown server error", nullErrorEx.getMessage());

        Exception nullMsgEx = service.publicMapServerError(new ErrorResponse("TEST", null));
        assertEquals("Unknown server error", nullMsgEx.getMessage());
    }

    @Test
    public void testUnwrapSuccess() throws Exception {
        TestClientService service = new TestClientService();
        ResponseMessage<String> response = new ResponseMessage<>("id", "hello");
        assertEquals("hello", service.publicUnwrap(response));
    }

    @Test
    public void testValidationErrorMethod() {
        TestClientService service = new TestClientService();
        CompletableFuture<Object> future = service.validationError("Error message");
        assertTrue(future.isCompletedExceptionally());
        assertThrows(CompletionException.class, future::join);
        try {
            future.join();
        } catch (CompletionException e) {
            assertTrue(e.getCause() instanceof ValidationException);
            assertEquals("Error message", e.getCause().getMessage());
        }
    }

    @Test
    public void testExtractFailure() {
        // Test non-CompletionException
        RuntimeException root = new RuntimeException("root");
        assertEquals(root, BaseClientService.extractFailure(root));

        // Test nested CompletionException
        CompletionException nested = new CompletionException(root);
        assertEquals(root, BaseClientService.extractFailure(nested));

        // Test deeply nested
        CompletionException deeplyNested = new CompletionException(new CompletionException(root));
        assertEquals(root, BaseClientService.extractFailure(deeplyNested));

        // Test self-referencing exception cause loop
        // We cannot easily create cause loop in standard Exception class, but let's mock it
        CompletionException cyclic = mock(CompletionException.class);
        when(cyclic.getCause()).thenReturn(cyclic);
        assertEquals(cyclic, BaseClientService.extractFailure(cyclic));
    }

    @Test
    public void testSendSuccess() throws Exception {
        TestClientService service = new TestClientService();
        RequestMessage<String> request = new RequestMessage<>(MessageType.LOGIN, "payload");
        ResponseMessage<String> response = new ResponseMessage<>("id", "result");

        when(mockConnection.sendRequest(any(RequestMessage.class), eq(String.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        String result = service.publicSend(request).get();
        assertEquals("result", result);
    }

    @Test
    public void testSendUnwrapError() {
        TestClientService service = new TestClientService();
        RequestMessage<String> request = new RequestMessage<>(MessageType.LOGIN, "payload");
        ResponseMessage<String> response = new ResponseMessage<>("id", ErrorCode.CONFLICT, "conflict message");

        when(mockConnection.sendRequest(any(RequestMessage.class), eq(String.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        CompletableFuture<String> future = service.publicSend(request);
        assertThrows(CompletionException.class, future::join);
        try {
            future.join();
        } catch (CompletionException e) {
            assertTrue(e.getCause() instanceof ConflictException);
            assertEquals("conflict message", e.getCause().getMessage());
        }
    }

    @Test
    public void testSendConnectionException() {
        TestClientService service = new TestClientService();
        RequestMessage<String> request = new RequestMessage<>(MessageType.LOGIN, "payload");

        when(mockConnection.sendRequest(any(RequestMessage.class), eq(String.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Connection lost")));

        CompletableFuture<String> future = service.publicSend(request);
        assertThrows(CompletionException.class, future::join);
        try {
            future.join();
        } catch (CompletionException e) {
            assertTrue(e.getCause() instanceof RuntimeException);
            assertEquals("Connection lost", e.getCause().getMessage());
        }
    }

    @Test
    public void testSendConnectionNonExceptionThrowable() {
        TestClientService service = new TestClientService();
        RequestMessage<String> request = new RequestMessage<>(MessageType.LOGIN, "payload");

        // Throw an Error (which is a Throwable but not an Exception)
        when(mockConnection.sendRequest(any(RequestMessage.class), eq(String.class)))
                .thenReturn(CompletableFuture.failedFuture(new LinkageError("Linkage issue")));

        CompletableFuture<String> future = service.publicSend(request);
        assertThrows(CompletionException.class, future::join);
        try {
            future.join();
        } catch (CompletionException e) {
            assertTrue(e.getCause() instanceof ServerException);
            assertEquals("Server unreachable: Linkage issue", e.getCause().getMessage());
        }
    }
}
