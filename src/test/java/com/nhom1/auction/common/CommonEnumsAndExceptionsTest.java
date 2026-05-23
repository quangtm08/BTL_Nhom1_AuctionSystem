package com.nhom1.auction.common;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.enums.*;
import com.nhom1.auction.common.exception.*;
import com.nhom1.auction.common.protocol.*;
import com.nhom1.auction.common.utils.*;
import com.nhom1.auction.common.value.*;
import java.lang.reflect.Constructor;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class CommonEnumsAndExceptionsTest {

  private static class TestAppException extends AppException {
    public TestAppException(String code, String message) {
      super(code, message);
    }
  }

  @Test
  public void testEnums() {
    // AuctionStatus
    assertNotNull(AuctionStatus.values());
    assertEquals(AuctionStatus.OPEN, AuctionStatus.valueOf("OPEN"));

    // BidType
    assertNotNull(BidType.values());
    assertEquals(BidType.MANUAL, BidType.valueOf("MANUAL"));

    // ItemCategory
    assertNotNull(ItemCategory.values());
    assertEquals(ItemCategory.ART, ItemCategory.valueOf("ART"));

    // ItemCondition
    assertNotNull(ItemCondition.values());
    assertEquals(ItemCondition.NEW, ItemCondition.valueOf("NEW"));

    // UserRole
    assertNotNull(UserRole.values());
    assertEquals(UserRole.USER, UserRole.valueOf("USER"));

    // MessageType
    assertNotNull(MessageType.values());
    assertEquals(MessageType.LOGIN, MessageType.valueOf("LOGIN"));
  }

  @Test
  public void testErrorCodePrivateConstructor() throws Exception {
    Constructor<ErrorCode> constructor = ErrorCode.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    ErrorCode instance = constructor.newInstance();
    assertNotNull(instance);

    // Access a constant to ensure it loads
    assertEquals("VALIDATION_ERROR", ErrorCode.VALIDATION_ERROR);
  }

  @Test
  public void testExceptions() {
    String msg = "test error";
    String code = "TEST_CODE";

    // AppException
    AppException ae1 = new TestAppException(code, msg);
    assertEquals(msg, ae1.getMessage());
    assertEquals(code, ae1.getCode());

    // AuctionClosedException
    AuctionClosedException ace = new AuctionClosedException(msg);
    assertEquals(msg, ace.getMessage());

    // AuthenticationException
    AuthenticationException authEx = new AuthenticationException(msg);
    assertEquals(msg, authEx.getMessage());

    // ConflictException
    ConflictException ce = new ConflictException(msg);
    assertEquals(msg, ce.getMessage());

    // InvalidAuctionStateException
    InvalidAuctionStateException iase = new InvalidAuctionStateException(msg);
    assertEquals(msg, iase.getMessage());

    // InvalidBidException
    InvalidBidException ibe = new InvalidBidException(msg);
    assertEquals(msg, ibe.getMessage());

    // NotFoundException
    NotFoundException nfe = new NotFoundException(msg);
    assertEquals(msg, nfe.getMessage());

    // PaymentException
    PaymentException pe1 = new PaymentException(msg);
    assertEquals(msg, pe1.getMessage());

    // ServerException
    ServerException se1 = new ServerException(code, msg);
    assertEquals(msg, se1.getMessage());
    assertEquals(code, se1.getCode());

    // UnauthorizedActionException
    UnauthorizedActionException uae = new UnauthorizedActionException(msg);
    assertEquals(msg, uae.getMessage());

    // UserAlreadyExistsException
    UserAlreadyExistsException uaee = new UserAlreadyExistsException(msg);
    assertEquals(msg, uaee.getMessage());

    // ValidationException
    ValidationException ve = new ValidationException(msg);
    assertEquals(msg, ve.getMessage());
  }

  @Test
  public void testValueObjects() {
    Money money = new Money();
    assertNotNull(money);

    TimeRange timeRange = new TimeRange();
    assertNotNull(timeRange);
  }

  @Test
  public void testAppContext() {
    AppContext.setServer(true);
    assertTrue(AppContext.isServer());
    AppContext.setServer(false);
    assertFalse(AppContext.isServer());

    AuthResponse user = new AuthResponse();
    user.setUsername("john_doe");
    AppContext.setCurrentUser(user);
    assertEquals(user, AppContext.getCurrentUser());

    String auctionId = UUID.randomUUID().toString();
    AppContext.setSelectedAuctionId(auctionId);
    assertEquals(auctionId, AppContext.getSelectedAuctionId());

    AppContext.clearSession();
    assertNull(AppContext.getCurrentUser());
  }

  @Test
  public void testJsonUtil() throws Exception {
    // Constructor test via reflection
    Constructor<JsonUtil> constructor = JsonUtil.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    JsonUtil instance = constructor.newInstance();
    assertNotNull(instance);

    // Serialize / Deserialize
    AuthResponse res = new AuthResponse();
    res.setUsername("jack");
    res.setUserID("my-userID");

    String json = JsonUtil.toJson(res);
    assertNotNull(json);
    assertTrue(json.contains("jack"));

    AuthResponse deserialized = JsonUtil.fromJson(json, AuthResponse.class);
    assertEquals("jack", deserialized.getUsername());
    assertEquals("my-userID", deserialized.getUserID());
  }

  @Test
  public void testProtocol() {
    ErrorResponse er = new ErrorResponse(ErrorCode.VALIDATION_ERROR, "Invalid details");
    assertEquals(ErrorCode.VALIDATION_ERROR, er.getCode());
    assertEquals("Invalid details", er.getMessage());
    er.setCode(ErrorCode.SERVER_ERROR);
    er.setMessage("Oops");
    er.setDetails("More details");
    assertEquals(ErrorCode.SERVER_ERROR, er.getCode());
    assertEquals("Oops", er.getMessage());
    assertEquals("More details", er.getDetails());

    ErrorResponse er3 = new ErrorResponse("CODE", "MSG", "DT");
    assertEquals("CODE", er3.getCode());
    assertEquals("MSG", er3.getMessage());
    assertEquals("DT", er3.getDetails());

    RequestMessage<String> req = new RequestMessage<>();
    req.setType(MessageType.LOGIN);
    req.setPayload("payload");
    req.setRequestId("req-id");
    assertEquals(MessageType.LOGIN, req.getType());
    assertEquals("payload", req.getPayload());
    assertEquals("req-id", req.getRequestId());

    RequestMessage<String> req2 = new RequestMessage<>(MessageType.LOGIN, "payload");
    assertEquals(MessageType.LOGIN, req2.getType());
    assertEquals("payload", req2.getPayload());
    assertNotNull(req2.getRequestId());

    ResponseMessage<String> resp = new ResponseMessage<>();
    resp.setType(MessageType.LOGIN);
    resp.setPayload("payload");
    resp.setSuccess(true);

    ErrorResponse errorResponse = new ErrorResponse(ErrorCode.VALIDATION_ERROR, "error");
    resp.setError(errorResponse);

    assertEquals(MessageType.LOGIN, resp.getType());
    assertEquals("payload", resp.getPayload());
    assertTrue(resp.isSuccess());
    assertEquals(errorResponse, resp.getError());

    ResponseMessage<String> errorResp =
        new ResponseMessage<>("req-id", ErrorCode.SERVER_ERROR, "Fatal");
    assertEquals("req-id", errorResp.getRequestId());
    assertFalse(errorResp.isSuccess());
    assertEquals(ErrorCode.SERVER_ERROR, errorResp.getError().getCode());
    assertEquals("Fatal", errorResp.getError().getMessage());

    ResponseMessage<String> successResp = new ResponseMessage<>("req-id", "Done");
    assertEquals("req-id", successResp.getRequestId());
    assertTrue(successResp.isSuccess());
    assertEquals("Done", successResp.getPayload());
  }
}
