package com.nhom1.auction.client.user.service;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom1.auction.common.exception.NotFoundException;
import com.nhom1.auction.common.exception.ServerException;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.common.protocol.ErrorCode;
import com.nhom1.auction.common.protocol.ErrorResponse;
import com.nhom1.auction.common.protocol.ResponseMessage;
import java.lang.reflect.Method;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.Test;

public class BaseClientServiceTest {

  // Concrete implementation for testing
  private static class TestClientService extends BaseClientService {
    public TestClientService() {
      super();
    }

    // Expose protected mapServerError for testing
    public Exception publicMapServerError(ErrorResponse error) throws Exception {
      Method method =
          BaseClientService.class.getDeclaredMethod("mapServerError", ErrorResponse.class);
      method.setAccessible(true);
      return (Exception) method.invoke(this, error);
    }

    // Expose protected unwrap for testing
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

  @Test
  public void testMapServerError_NotFound_ReturnsNotFoundException() throws Exception {
    TestClientService service = new TestClientService();
    ErrorResponse error = new ErrorResponse(ErrorCode.NOT_FOUND, "Item missing");

    Exception ex = service.publicMapServerError(error);

    assertTrue(ex instanceof NotFoundException);
    assertEquals("Item missing", ex.getMessage());
  }

  @Test
  public void testMapServerError_Validation_ReturnsValidationException() throws Exception {
    TestClientService service = new TestClientService();
    ErrorResponse error = new ErrorResponse(ErrorCode.VALIDATION_ERROR, "Bad input");

    Exception ex = service.publicMapServerError(error);

    assertTrue(ex instanceof ValidationException);
    assertEquals("Bad input", ex.getMessage());
  }

  @Test
  public void testMapServerError_UnknownCode_ReturnsServerException() throws Exception {
    TestClientService service = new TestClientService();
    ErrorResponse error = new ErrorResponse("NEW_SERVER_CODE", "New server error");

    Exception ex = service.publicMapServerError(error);

    assertTrue(ex instanceof ServerException);
    assertEquals("NEW_SERVER_CODE", ((ServerException) ex).getCode());
    assertEquals("New server error", ex.getMessage());
  }

  @Test
  public void testUnwrap_ErrorResponse_ThrowsCompletionExceptionWithMappedError() throws Exception {
    TestClientService service = new TestClientService();
    ResponseMessage<Object> response =
        new ResponseMessage<>("id", ErrorCode.NOT_FOUND, "Not found");

    assertThrows(
        CompletionException.class,
        () -> {
          service.publicUnwrap(response);
        });

    try {
      service.publicUnwrap(response);
    } catch (CompletionException e) {
      assertTrue(e.getCause() instanceof NotFoundException);
      assertEquals("Not found", e.getCause().getMessage());
    }
  }
}
