package com.nhom1.auction.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.admin.AdminHandler;
import com.nhom1.auction.server.admin.AdminService;
import com.nhom1.auction.server.auction.AuctionHandler;
import com.nhom1.auction.server.auction.AuctionService;
import com.nhom1.auction.server.bidding.BidHandler;
import com.nhom1.auction.server.bidding.BidService;
import com.nhom1.auction.server.infrastructure.ClientRegistry;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import com.nhom1.auction.server.payment.PaymentHandler;
import com.nhom1.auction.server.payment.PaymentService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

public class ServerHandlersAdditionalTest {

  @Test
  public void testAdminHandlerAdditional() {
    AdminService mockAdminService = mock(AdminService.class);
    AdminHandler handler = new AdminHandler(mockAdminService);
    MessageRouter router = new MessageRouter();
    handler.register(router);

    // 1. Dto is null checks (by sending null payload in envelope)
    String listUsersNull = "{\"type\":\"ADMIN_LIST_USERS\",\"requestId\":\"r-1\",\"payload\":null}";
    String listUsersResp = router.handleRequest(listUsersNull);
    assertTrue(listUsersResp.contains("Missing admin list users payload."));

    String listAuctionsNull =
        "{\"type\":\"ADMIN_LIST_AUCTIONS\",\"requestId\":\"r-2\",\"payload\":null}";
    String listAuctionsResp = router.handleRequest(listAuctionsNull);
    assertTrue(listAuctionsResp.contains("Missing admin list auctions payload."));

    String deleteUserNull =
        "{\"type\":\"ADMIN_DELETE_USER\",\"requestId\":\"r-3\",\"payload\":null}";
    String deleteUserResp = router.handleRequest(deleteUserNull);
    assertTrue(deleteUserResp.contains("Missing delete user payload."));

    String cancelAuctionNull =
        "{\"type\":\"ADMIN_CANCEL_AUCTION\",\"requestId\":\"r-4\",\"payload\":null}";
    String cancelAuctionResp = router.handleRequest(cancelAuctionNull);
    assertTrue(cancelAuctionResp.contains("Missing cancel auction payload."));

    // 2. Exception in deserialization (use JSON arrays which fail to map to POJOs)
    String malformedJson = "{\"type\":\"ADMIN_LIST_USERS\",\"requestId\":\"r-5\",\"payload\":[]}";
    String respMalformed = router.handleRequest(malformedJson);
    assertTrue(respMalformed.contains("Invalid admin list users JSON"));

    String malformedJson2 =
        "{\"type\":\"ADMIN_LIST_AUCTIONS\",\"requestId\":\"r-6\",\"payload\":[]}";
    assertTrue(router.handleRequest(malformedJson2).contains("Invalid admin list auctions JSON"));

    String malformedJson3 = "{\"type\":\"ADMIN_DELETE_USER\",\"requestId\":\"r-7\",\"payload\":[]}";
    assertTrue(router.handleRequest(malformedJson3).contains("Invalid delete user JSON"));

    String malformedJson4 =
        "{\"type\":\"ADMIN_CANCEL_AUCTION\",\"requestId\":\"r-8\",\"payload\":[]}";
    assertTrue(router.handleRequest(malformedJson4).contains("Invalid cancel auction JSON"));
  }

  @Test
  public void testBidHandlerAdditional() {
    BidService mockBidService = mock(BidService.class);
    NotificationService mockNotification = mock(NotificationService.class);
    BidHandler handler = new BidHandler(mockBidService, mockNotification);
    MessageRouter router = new MessageRouter();
    handler.register(router);

    // 1. PLACE_BID invalid UUID format in request
    String placeBidBadUUID =
        "{\"type\":\"PLACE_BID\",\"requestId\":\"r-1\",\"payload\":{\"auctionId\":\"bad-uuid\",\"bidderId\":\"bad-uuid\",\"bidAmount\":10.0}}";
    String resp1 = router.handleRequest(placeBidBadUUID);
    assertTrue(resp1.contains("Invalid UUID format in request"));

    // 2. GET_AUCTION_DETAIL invalid UUID format in request
    String getDetailBadUUID =
        "{\"type\":\"GET_AUCTION_DETAIL\",\"requestId\":\"r-2\",\"payload\":{\"auctionId\":\"bad-uuid\"}}";
    String resp2 = router.handleRequest(getDetailBadUUID);
    assertTrue(resp2.contains("Invalid UUID format in request"));

    // 3. LIST_MY_BIDS invalid UUID format in request
    String listMyBidsBadUUID =
        "{\"type\":\"LIST_MY_BIDS\",\"requestId\":\"r-3\",\"payload\":{\"bidderId\":\"bad-uuid\"}}";
    String resp3 = router.handleRequest(listMyBidsBadUUID);
    assertTrue(resp3.contains("Invalid UUID format in request"));

    // 4. Malformed JSON payloads (use JSON arrays which fail to map to POJOs)
    String placeBidMalformed = "{\"type\":\"PLACE_BID\",\"requestId\":\"r-4\",\"payload\":[]}";
    assertTrue(router.handleRequest(placeBidMalformed).contains("Invalid PLACE_BID payload"));

    String getDetailMalformed =
        "{\"type\":\"GET_AUCTION_DETAIL\",\"requestId\":\"r-5\",\"payload\":[]}";
    assertTrue(
        router.handleRequest(getDetailMalformed).contains("Invalid GET_AUCTION_DETAIL payload"));

    String listMyBidsMalformed = "{\"type\":\"LIST_MY_BIDS\",\"requestId\":\"r-6\",\"payload\":[]}";
    assertTrue(router.handleRequest(listMyBidsMalformed).contains("Invalid LIST_MY_BIDS payload"));
  }

  @Test
  public void testPaymentHandlerAdditional() {
    PaymentService mockPaymentService = mock(PaymentService.class);
    PaymentHandler handler = new PaymentHandler(mockPaymentService);
    MessageRouter router = new MessageRouter();
    handler.register(router);

    // 1. Dto is null checks
    String processPaymentNull =
        "{\"type\":\"PROCESS_PAYMENT\",\"requestId\":\"r-1\",\"payload\":null}";
    assertTrue(
        router.handleRequest(processPaymentNull).contains("Missing process payment payload."));

    String listPendingNull =
        "{\"type\":\"LIST_PENDING_PAYMENTS\",\"requestId\":\"r-2\",\"payload\":null}";
    assertTrue(router.handleRequest(listPendingNull).contains("Missing pending payments payload."));

    String paymentHistoryNull =
        "{\"type\":\"LIST_PAYMENT_HISTORY\",\"requestId\":\"r-3\",\"payload\":null}";
    assertTrue(
        router.handleRequest(paymentHistoryNull).contains("Missing payment history payload."));

    // 2. Exception in deserialization (use JSON arrays which fail to map to POJOs)
    String processMalformed = "{\"type\":\"PROCESS_PAYMENT\",\"requestId\":\"r-4\",\"payload\":[]}";
    assertTrue(router.handleRequest(processMalformed).contains("Invalid process payment JSON"));

    String listPendingMalformed =
        "{\"type\":\"LIST_PENDING_PAYMENTS\",\"requestId\":\"r-5\",\"payload\":[]}";
    assertTrue(
        router.handleRequest(listPendingMalformed).contains("Invalid pending payments JSON"));

    String historyMalformed =
        "{\"type\":\"LIST_PAYMENT_HISTORY\",\"requestId\":\"r-6\",\"payload\":[]}";
    assertTrue(router.handleRequest(historyMalformed).contains("Invalid payment history JSON"));
  }

  @Test
  public void testAuctionHandlerAdditional() {
    AuctionService mockAuctionService = mock(AuctionService.class);
    NotificationService mockNotification = mock(NotificationService.class);
    AuctionHandler handler = new AuctionHandler(mockAuctionService, mockNotification);
    MessageRouter router = new MessageRouter();
    handler.register(router);

    // 1. Malformed JSON (use JSON arrays for CREATE_AUCTION and omit payload for others to trigger
    // null argument exception in JsonUtil)
    String createMalformed = "{\"type\":\"CREATE_AUCTION\",\"requestId\":\"r-1\",\"payload\":[]}";
    assertTrue(router.handleRequest(createMalformed).contains("Invalid CreateAuction JSON"));

    String listMalformed = "{\"type\":\"LIST_MY_LISTINGS\",\"requestId\":\"r-2\"}";
    assertTrue(router.handleRequest(listMalformed).contains("Invalid ListMyListings JSON"));

    String deleteMalformed = "{\"type\":\"DELETE_AUCTION\",\"requestId\":\"r-3\"}";
    assertTrue(router.handleRequest(deleteMalformed).contains("Invalid DeleteAuction JSON"));
  }

  @Test
  public void testNotificationServiceAdditional() {
    ClientRegistry mockRegistry = mock(ClientRegistry.class);
    NotificationService service = new NotificationService(mockRegistry);

    // Test other methods of NotificationService
    service.broadcastUserDeleted("user-1");
    verify(mockRegistry).broadcast(contains("user-1"));

    service.broadcastUserCreated("user-2", "username", "email");
    verify(mockRegistry).broadcast(contains("user-2"));

    service.broadcastAuctionEnded(UUID.randomUUID(), UUID.randomUUID(), BigDecimal.TEN);
    verify(mockRegistry, times(3)).broadcast(anyString());

    // Test catch block inside sendPush by mocking static JsonUtil.toJson
    try (MockedStatic<JsonUtil> mockedJsonUtil = mockStatic(JsonUtil.class)) {
      mockedJsonUtil
          .when(() -> JsonUtil.toJson(any()))
          .thenThrow(new RuntimeException("Serialization failure"));
      // This should not throw, just print to stderr
      assertDoesNotThrow(() -> service.broadcastUserDeleted("user-1"));
    }
  }
}
