package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.common.exception.AuthenticationException;
import com.nhom1.auction.common.exception.AppException;
import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.common.protocol.ErrorCode;
import com.nhom1.auction.common.protocol.ResponseMessage;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;

public class ResponseFactoryTest {

    @Test
    public void testSuccess_ReturnsSuccessfulResponse() {
        String requestId = "req-123";
        String payload = "data";
        ResponseMessage<String> response = ResponseFactory.success(requestId, payload);

        assertTrue(response.isSuccess());
        assertEquals(requestId, response.getRequestId());
        assertEquals(payload, response.getPayload());
        assertNull(response.getError());
    }

    @Test
    public void testInvalidFormat_ReturnsErrorResponse() {
        String requestId = "req-456";
        String message = "Bad JSON";
        ResponseMessage<Object> response = ResponseFactory.invalidFormat(requestId, message);

        assertFalse(response.isSuccess());
        assertEquals(requestId, response.getRequestId());
        assertEquals(ErrorCode.INVALID_FORMAT, response.getError().getCode());
        assertEquals(message, response.getError().getMessage());
    }

    @Test
    public void testFromException_ValidationException_MapsToValidationError() {
        String requestId = "req-789";
        ValidationException ex = new ValidationException("Invalid price");
        ResponseMessage<Object> response = ResponseFactory.fromException(requestId, ex);

        assertFalse(response.isSuccess());
        assertEquals(ErrorCode.VALIDATION_ERROR, response.getError().getCode());
        assertEquals("Invalid price", response.getError().getMessage());
        assertEquals(ErrorCode.VALIDATION_ERROR, ex.getCode());
    }

    @Test
    public void testFromException_AuthenticationException_MapsToAuthenticationFailed() {
        String requestId = "req-000";
        AuthenticationException ex = new AuthenticationException("Wrong password");
        ResponseMessage<Object> response = ResponseFactory.fromException(requestId, ex);

        assertFalse(response.isSuccess());
        assertEquals(ErrorCode.AUTHENTICATION_FAILED, response.getError().getCode());
        assertEquals("Wrong password", response.getError().getMessage());
        assertEquals(ErrorCode.AUTHENTICATION_FAILED, ex.getCode());
    }

    @Test
    public void testFromException_NotFoundException_MapsToNotFound() {
        Exception ex = new NotFoundException("User not found");
        ResponseMessage<Object> response = ResponseFactory.fromException("id", ex);

        assertEquals(ErrorCode.NOT_FOUND, response.getError().getCode());
    }

    @Test
    public void testFromException_IllegalArgumentException_MapsToValidationError() {
        Exception ex = new IllegalArgumentException("amount must be positive");
        ResponseMessage<Object> response = ResponseFactory.fromException("id", ex);

        assertEquals(ErrorCode.VALIDATION_ERROR, response.getError().getCode());
        assertEquals("amount must be positive", response.getError().getMessage());
    }

    @Test
    public void testFromException_NestedIllegalArgumentException_MapsToValidationError() {
        Exception root = new IllegalArgumentException("auctionId is invalid UUID");
        Exception wrapper = new RuntimeException("Wrapper", root);
        ResponseMessage<Object> response = ResponseFactory.fromException("id", wrapper);

        assertEquals(ErrorCode.VALIDATION_ERROR, response.getError().getCode());
        assertEquals("auctionId is invalid UUID", response.getError().getMessage());
    }

    @Test
    public void testFromException_SQLException_MapsToServerError() {
        Exception ex = new SQLException("Connection lost");
        ResponseMessage<Object> response = ResponseFactory.fromException("id", ex);

        assertEquals(ErrorCode.SERVER_ERROR, response.getError().getCode());
        assertEquals("Unexpected server error", response.getError().getMessage());
        assertNull(response.getError().getDetails());
    }

    @Test
    public void testFromException_RuntimeException_MapsToServerError() {
        Exception ex = new RuntimeException("Something went wrong");
        ResponseMessage<Object> response = ResponseFactory.fromException("id", ex);

        assertEquals(ErrorCode.SERVER_ERROR, response.getError().getCode());
        assertEquals("Unexpected server error", response.getError().getMessage());
    }

    @Test
    public void testFindAppException_NestedException_ReturnsAppException() {
        Exception root = new ValidationException("Root error");
        Exception wrapper = new RuntimeException("Wrapper", root);

        AppException result = ResponseFactory.findAppException(wrapper);

        assertEquals(root, result);
    }

    @Test
    public void testFindAppException_NullOrSelfReferencing() {
        assertNull(ResponseFactory.findAppException(null));

        // Create self-referencing exception using reflection or subclassing
        class SelfReferencingException extends RuntimeException {
            @Override
            public synchronized Throwable getCause() {
                return this;
            }
        }
        SelfReferencingException ex = new SelfReferencingException();
        assertNull(ResponseFactory.findAppException(ex));
        assertNull(ResponseFactory.fromException("id", ex).getPayload());
        
        // Test logUnexpectedException null check
        assertDoesNotThrow(() -> ResponseFactory.fromException("id", null));
    }
}
