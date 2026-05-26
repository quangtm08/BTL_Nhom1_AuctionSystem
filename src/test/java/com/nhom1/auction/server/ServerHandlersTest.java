package com.nhom1.auction.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.dto.admin.UserSummaryDto;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigDetailResponse;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.ListAuctionsResponse;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;
import com.nhom1.auction.common.dto.payment.PaymentHistoryResponse;
import com.nhom1.auction.common.dto.payment.PendingPaymentsResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.server.admin.AdminHandler;
import com.nhom1.auction.server.admin.AdminService;
import com.nhom1.auction.server.auction.AuctionHandler;
import com.nhom1.auction.server.auction.AuctionService;
import com.nhom1.auction.server.auth.AuthHandler;
import com.nhom1.auction.server.auth.AuthService;
import com.nhom1.auction.server.automation.AutoBidHandler;
import com.nhom1.auction.server.automation.AutoBidService;
import com.nhom1.auction.server.bidding.BidHandler;
import com.nhom1.auction.server.bidding.BidService;
import com.nhom1.auction.server.infrastructure.ClientHandler;
import com.nhom1.auction.server.infrastructure.ClientRegistry;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import com.nhom1.auction.server.payment.PaymentHandler;
import com.nhom1.auction.server.payment.PaymentService;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ServerHandlersTest {

  private final ObjectMapper mapper = new ObjectMapper();

  @Test
  public void testAuthHandler() throws Exception {
    AuthService mockAuthService = mock(AuthService.class);
    AuthHandler handler = new AuthHandler(mockAuthService);

    MessageRouter router = new MessageRouter();
    handler.register(router);

    // Test Login
    User mockUser =
        new User(
            UUID.randomUUID(),
            "alice",
            "alice@mail.com",
            "pass",
            UserRole.USER,
            LocalDateTime.now(),
            LocalDateTime.now());

    when(mockAuthService.login("alice", "pass")).thenReturn(mockUser);

    String loginReq =
        "{\"type\":\"LOGIN\",\"requestId\":\"r-1\",\"payload\":{\"identifier\":\"alice\",\"password\":\"pass\"}}";
    String response = router.handleRequest(loginReq);

    assertTrue(response.contains("alice@mail.com"));
    verify(mockAuthService).login("alice", "pass");

    // Test Register
    when(mockAuthService.register("bob", "bob@mail.com", "pass")).thenReturn(mockUser);

    String regReq =
        "{\"type\":\"REGISTER\",\"requestId\":\"r-2\",\"payload\":{\"username\":\"bob\",\"email\":\"bob@mail.com\",\"password\":\"pass\"}}";
    String regResp = router.handleRequest(regReq);

    assertTrue(regResp.contains("success"));
    verify(mockAuthService).register("bob", "bob@mail.com", "pass");
  }

  @Test
  public void testAdminHandler() throws Exception {
    AdminService mockAdminService = mock(AdminService.class);
    AdminHandler handler = new AdminHandler(mockAdminService);

    MessageRouter router = new MessageRouter();
    handler.register(router);

    // List Users
    AdminUserListResponse ulr = new AdminUserListResponse();
    ulr.setUsers(
        List.of(
            new UserSummaryDto("u-1", "user1", "u1@mail.com", UserRole.USER, LocalDateTime.now())));
    when(mockAdminService.getAllUsers("caller-1")).thenReturn(ulr);

    String listUsersReq =
        "{\"type\":\"ADMIN_LIST_USERS\",\"requestId\":\"r-1\",\"payload\":{\"callerId\":\"caller-1\"}}";
    String resp1 = router.handleRequest(listUsersReq);
    assertTrue(resp1.contains("user1"));

    // List Auctions
    AdminAuctionListResponse alr = new AdminAuctionListResponse();
    when(mockAdminService.getAllAuctions("caller-1")).thenReturn(alr);

    String listAuctionsReq =
        "{\"type\":\"ADMIN_LIST_AUCTIONS\",\"requestId\":\"r-2\",\"payload\":{\"callerId\":\"caller-1\"}}";
    String resp2 = router.handleRequest(listAuctionsReq);
    assertTrue(resp2.contains("success"));

    // Delete User
    when(mockAdminService.deleteUser("target-1", "caller-1")).thenReturn("Deleted");
    String delUserReq =
        "{\"type\":\"ADMIN_DELETE_USER\",\"requestId\":\"r-3\",\"payload\":{\"targetUserId\":\"target-1\",\"callerId\":\"caller-1\"}}";
    String resp3 = router.handleRequest(delUserReq);
    assertTrue(resp3.contains("Deleted"));

    // Cancel Auction
    when(mockAdminService.cancelAuction("auc-1", "caller-1")).thenReturn("Canceled");
    String cancelAucReq =
        "{\"type\":\"ADMIN_CANCEL_AUCTION\",\"requestId\":\"r-4\",\"payload\":{\"auctionId\":\"auc-1\",\"callerId\":\"caller-1\"}}";
    String resp4 = router.handleRequest(cancelAucReq);
    assertTrue(resp4.contains("Canceled"));
  }

  @Test
  public void testAuctionHandler() throws Exception {
    AuctionService mockAuctionService = mock(AuctionService.class);
    NotificationService mockNotification = mock(NotificationService.class);
    AuctionHandler handler = new AuctionHandler(mockAuctionService, mockNotification);

    MessageRouter router = new MessageRouter();
    handler.register(router);

    // Create Auction
    Auction mockAuction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            BigDecimal.TEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(3),
            null,
            null,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now(),
            null);

    when(mockAuctionService.createAuction(anyString(), any())).thenReturn(mockAuction);

    String createReq =
        "{\"type\":\"CREATE_AUCTION\",\"requestId\":\"r-1\",\"payload\":{\"sellerId\":\"00000000-0000-0000-0000-000000000001\",\"name\":\"Table\",\"description\":\"Wood\",\"startingPrice\":\"10.0\",\"category\":\"ART\",\"condition\":\"NEW\",\"startTime\":\"2026-05-22T15:00:00\",\"endTime\":\"2026-05-25T15:00:00\"}}";
    String resp1 = router.handleRequest(createReq);
    assertTrue(resp1.contains("\"success\":true"));

    // List My Listings
    when(mockAuctionService.getMyListings("00000000-0000-0000-0000-000000000001"))
        .thenReturn(Collections.emptyList());
    String listReq =
        "{\"type\":\"LIST_MY_LISTINGS\",\"requestId\":\"r-2\",\"payload\":{\"sellerId\":\"00000000-0000-0000-0000-000000000001\"}}";
    String resp2 = router.handleRequest(listReq);
    assertTrue(resp2.contains("\"success\":true"));

    // Delete Auction
    String delReq =
        "{\"type\":\"DELETE_AUCTION\",\"requestId\":\"r-3\",\"payload\":{\"sellerId\":\"00000000-0000-0000-0000-000000000001\",\"auctionId\":\"00000000-0000-0000-0000-000000000002\"}}";
    String resp3 = router.handleRequest(delReq);
    assertTrue(resp3.contains("\"success\":true"));
    verify(mockAuctionService)
        .deleteAuction(
            "00000000-0000-0000-0000-000000000001", "00000000-0000-0000-0000-000000000002");
  }

  @Test
  public void testAutoBidHandler() throws Exception {
    AutoBidService mockAutoBidService = mock(AutoBidService.class);
    AutoBidHandler handler = new AutoBidHandler(mockAutoBidService);

    MessageRouter router = new MessageRouter();
    handler.register(router);

    AutoBidConfigResponse configResp = new AutoBidConfigResponse("CONFIG_SAVED");
    when(mockAutoBidService.saveConfig(any())).thenReturn(configResp);

    String configReq =
        "{\"type\":\"AUTO_BID_CONFIG\",\"requestId\":\"r-1\",\"payload\":{\"auctionId\":\"auc-1\",\"bidderId\":\"user-1\",\"maxAmount\":100.0,\"increment\":5.0}}";
    String resp = router.handleRequest(configReq);
    assertTrue(resp.contains("CONFIG_SAVED"));

    // Get config success
    AutoBidConfigDetailResponse detailResp =
        new AutoBidConfigDetailResponse("auc-1", "user-1", "100.0", "5.0", true);
    when(mockAutoBidService.getConfig("auc-1", "user-1")).thenReturn(detailResp);
    String getReq =
        "{\"type\":\"GET_AUTO_BID_CONFIG\",\"requestId\":\"r-2\",\"payload\":{\"auctionId\":\"auc-1\",\"bidderId\":\"user-1\"}}";
    String getResp = router.handleRequest(getReq);
    assertTrue(getResp.contains("true"));

    // Delete config success
    AutoBidConfigResponse delResp = new AutoBidConfigResponse("CONFIG_DELETED");
    when(mockAutoBidService.deleteConfig("auc-1", "user-1")).thenReturn(delResp);
    String delReq =
        "{\"type\":\"DELETE_AUTO_BID_CONFIG\",\"requestId\":\"r-3\",\"payload\":{\"auctionId\":\"auc-1\",\"bidderId\":\"user-1\"}}";
    String delRespStr = router.handleRequest(delReq);
    assertTrue(delRespStr.contains("CONFIG_DELETED"));

    // Error paths
    when(mockAutoBidService.saveConfig(any()))
        .thenThrow(new IllegalArgumentException("Invalid config"));
    String configReqErr =
        "{\"type\":\"AUTO_BID_CONFIG\",\"requestId\":\"r-4\",\"payload\":{\"auctionId\":\"auc-1\",\"bidderId\":\"user-1\",\"maxAmount\":100.0,\"increment\":5.0}}";
    assertTrue(router.handleRequest(configReqErr).contains("Invalid config"));

    when(mockAutoBidService.getConfig(anyString(), anyString()))
        .thenThrow(new IllegalArgumentException("Get config error"));
    String getReqErr =
        "{\"type\":\"GET_AUTO_BID_CONFIG\",\"requestId\":\"r-5\",\"payload\":{\"auctionId\":\"auc-1\",\"bidderId\":\"user-1\"}}";
    assertTrue(router.handleRequest(getReqErr).contains("Get config error"));

    when(mockAutoBidService.deleteConfig(anyString(), anyString()))
        .thenThrow(new IllegalArgumentException("Delete config error"));
    String delReqErr =
        "{\"type\":\"DELETE_AUTO_BID_CONFIG\",\"requestId\":\"r-6\",\"payload\":{\"auctionId\":\"auc-1\",\"bidderId\":\"user-1\"}}";
    assertTrue(router.handleRequest(delReqErr).contains("Delete config error"));
  }

  @Test
  public void testBidHandler() throws Exception {
    BidService mockBidService = mock(BidService.class);
    NotificationService mockNotification = mock(NotificationService.class);
    AutoBidService mockAutoBidService = mock(AutoBidService.class);

    BidHandler handler = new BidHandler(mockBidService, mockNotification);
    handler.setAutoBidService(mockAutoBidService);

    MessageRouter router = new MessageRouter();
    handler.register(router);

    UUID bidTxId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();

    // Place Bid
    BidTransaction mockTx =
        new BidTransaction(
            bidTxId,
            auctionId,
            bidderId,
            new BigDecimal("150.0"),
            BidType.MANUAL,
            LocalDateTime.now(),
            LocalDateTime.now());
    when(mockBidService.placeBid(
            eq(bidderId), eq(auctionId), eq(new BigDecimal("150.0")), eq(BidType.MANUAL)))
        .thenReturn(mockTx);

    String placeBidReq =
        String.format(
            "{\"type\":\"PLACE_BID\",\"requestId\":\"r-1\",\"payload\":{\"auctionId\":\"%s\",\"bidderId\":\"%s\",\"bidAmount\":\"150.0\"}}",
            auctionId, bidderId);
    String resp1 = router.handleRequest(placeBidReq);
    assertTrue(resp1.contains("150.0"));

    // List Auctions
    ListAuctionsResponse lar = new ListAuctionsResponse();
    lar.setAuctions(Collections.emptyList());
    when(mockBidService.listAllAuctions()).thenReturn(lar);

    String listReq = "{\"type\":\"LIST_AUCTIONS\",\"requestId\":\"r-2\",\"payload\":{}}";
    String resp2 = router.handleRequest(listReq);
    assertTrue(resp2.contains("success"));

    // Get Auction Detail
    AuctionDetailDto add =
        new AuctionDetailDto(
            auctionId.toString(),
            UUID.randomUUID().toString(),
            "Item",
            "Desc",
            ItemCategory.ART,
            ItemCondition.NEW,
            UUID.randomUUID().toString(),
            BigDecimal.TEN,
            BigDecimal.TEN,
            UUID.randomUUID().toString(),
            BigDecimal.ONE,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(2),
            Collections.emptyList());
    when(mockBidService.getAuctionDetail(eq(auctionId))).thenReturn(add);

    String getDetailReq =
        String.format(
            "{\"type\":\"GET_AUCTION_DETAIL\",\"requestId\":\"r-3\",\"payload\":{\"auctionId\":\"%s\"}}",
            auctionId);
    String resp3 = router.handleRequest(getDetailReq);
    assertTrue(resp3.contains(auctionId.toString()));

    // List My Bids
    MyBidsResponse mbr = new MyBidsResponse(Collections.emptyList());
    when(mockBidService.getMyBids(eq(bidderId))).thenReturn(mbr);

    String listMyBidsReq =
        String.format(
            "{\"type\":\"LIST_MY_BIDS\",\"requestId\":\"r-4\",\"payload\":{\"bidderId\":\"%s\"}}",
            bidderId);
    String resp4 = router.handleRequest(listMyBidsReq);
    assertTrue(resp4.contains("success"));
  }

  @Test
  public void testPaymentHandler() throws Exception {
    PaymentService mockPaymentService = mock(PaymentService.class);
    PaymentHandler handler = new PaymentHandler(mockPaymentService);

    MessageRouter router = new MessageRouter();
    handler.register(router);

    // Process Payment
    ProcessPaymentResponse ppr =
        new ProcessPaymentResponse(
            "auc-1", new BigDecimal("100.0"), "SUCCESS", LocalDateTime.now());
    when(mockPaymentService.processPayment("auc-1", "user-1")).thenReturn(ppr);

    String processReq =
        "{\"type\":\"PROCESS_PAYMENT\",\"requestId\":\"r-1\",\"payload\":{\"auctionId\":\"auc-1\",\"bidderId\":\"user-1\"}}";
    String resp1 = router.handleRequest(processReq);
    assertTrue(resp1.contains("SUCCESS"));

    // List Pending Payments
    PendingPaymentsResponse pprs = new PendingPaymentsResponse(Collections.emptyList());
    when(mockPaymentService.listPendingPayments("user-1")).thenReturn(pprs);

    String listPendingReq =
        "{\"type\":\"LIST_PENDING_PAYMENTS\",\"requestId\":\"r-2\",\"payload\":{\"bidderId\":\"user-1\"}}";
    String resp2 = router.handleRequest(listPendingReq);
    assertTrue(resp2.contains("success"));

    // List Payment History
    PaymentHistoryResponse phr = new PaymentHistoryResponse(Collections.emptyList());
    when(mockPaymentService.listPaymentHistory("user-1")).thenReturn(phr);

    String listHistReq =
        "{\"type\":\"LIST_PAYMENT_HISTORY\",\"requestId\":\"r-3\",\"payload\":{\"userId\":\"user-1\"}}";
    String resp3 = router.handleRequest(listHistReq);
    assertTrue(resp3.contains("success"));
  }

  @Test
  public void testClientHandlerAndRegistry() throws Exception {
    Socket mockSocket = mock(Socket.class);

    // Setup input/output streams for socket
    byte[] inputBytes =
        "{\"type\":\"LOGIN\",\"requestId\":\"r-1\",\"payload\":{\"identifier\":\"alice\",\"password\":\"pass\"}}\n"
            .getBytes();
    InputStream is = new ByteArrayInputStream(inputBytes);
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    when(mockSocket.getInputStream()).thenReturn(is);
    when(mockSocket.getOutputStream()).thenReturn(os);

    MessageRouter mockRouter = mock(MessageRouter.class);
    when(mockRouter.handleRequest(anyString())).thenReturn("{\"success\":true}");

    ClientRegistry registry = new ClientRegistry();
    ClientHandler handler = new ClientHandler(mockSocket, mockRouter, registry);

    handler.run();

    // Check if output stream received response
    String output = os.toString();
    assertTrue(output.contains("{\"success\":true}"));

    // Verify unregistration on disconnect
    assertNotNull(handler.getClientId());

    // Test linkUser and sendToUser / broadcast in registry
    UUID userId = UUID.randomUUID();
    registry.register(handler);
    registry.linkUser(userId, handler.getClientId());

    // Reset output stream
    os.reset();
    registry.sendToUser(userId, "DirectMsg");
    assertTrue(os.toString().contains("DirectMsg"));

    os.reset();
    registry.broadcast("BroadMsg");
    Thread.sleep(50); // Wait for async broadcast to complete
    assertTrue(os.toString().contains("BroadMsg"));

    // Test sendToUser where clientId is null
    UUID unlinkedUser = UUID.randomUUID();
    registry.sendToUser(unlinkedUser, "NullClientMsg");

    // Test sendToUser where handler is null (user linked but client handler not registered)
    UUID linkedButNoHandlerUser = UUID.randomUUID();
    UUID fakeClientId = UUID.randomUUID();
    registry.linkUser(linkedButNoHandlerUser, fakeClientId);
    registry.sendToUser(linkedButNoHandlerUser, "NullHandlerMsg");

    registry.unregister(handler.getClientId());
  }

  @Test
  public void testHandlersErrorPaths() throws Exception {
    // 1. AuthHandler errors
    AuthService mockAuthService = mock(AuthService.class);
    AuthHandler authHandler = new AuthHandler(mockAuthService);
    MessageRouter router = new MessageRouter();
    authHandler.register(router);

    // Invalid JSON Login
    String badLoginReq = "{\"type\":\"LOGIN\",\"requestId\":\"r-1\",\"payload\":\"bad_json\"}";
    String badLoginResp = router.handleRequest(badLoginReq);
    assertTrue(badLoginResp.contains("Invalid Login JSON"));

    // Invalid JSON Register
    String badRegReq = "{\"type\":\"REGISTER\",\"requestId\":\"r-2\",\"payload\":\"bad_json\"}";
    String badRegResp = router.handleRequest(badRegReq);
    assertTrue(badRegResp.contains("Invalid Register JSON"));

    // Service Exception Login
    when(mockAuthService.login(anyString(), anyString()))
        .thenThrow(new IllegalArgumentException("Service login error"));
    String loginReq =
        "{\"type\":\"LOGIN\",\"requestId\":\"r-3\",\"payload\":{\"identifier\":\"alice\",\"password\":\"pass\"}}";
    String loginResp = router.handleRequest(loginReq);
    assertTrue(loginResp.contains("Service login error"));

    // Service Exception Register
    when(mockAuthService.register(anyString(), anyString(), anyString()))
        .thenThrow(new IllegalArgumentException("Service register error"));
    String regReq =
        "{\"type\":\"REGISTER\",\"requestId\":\"r-4\",\"payload\":{\"username\":\"bob\",\"email\":\"bob@mail.com\",\"password\":\"pass\"}}";
    String regResp = router.handleRequest(regReq);
    assertTrue(regResp.contains("Service register error"));

    // 2. AdminHandler errors
    AdminService mockAdminService = mock(AdminService.class);
    AdminHandler adminHandler = new AdminHandler(mockAdminService);
    adminHandler.register(router);

    // List Users Service Exception
    when(mockAdminService.getAllUsers(anyString()))
        .thenThrow(new IllegalArgumentException("Admin users error"));
    String listUsersReq =
        "{\"type\":\"ADMIN_LIST_USERS\",\"requestId\":\"r-5\",\"payload\":{\"callerId\":\"caller-1\"}}";
    String listUsersResp = router.handleRequest(listUsersReq);
    assertTrue(listUsersResp.contains("Admin users error"));

    // List Auctions Service Exception
    when(mockAdminService.getAllAuctions(anyString()))
        .thenThrow(new IllegalArgumentException("Admin auctions error"));
    String listAuctionsReq =
        "{\"type\":\"ADMIN_LIST_AUCTIONS\",\"requestId\":\"r-6\",\"payload\":{\"callerId\":\"caller-1\"}}";
    String listAuctionsResp = router.handleRequest(listAuctionsReq);
    assertTrue(listAuctionsResp.contains("Admin auctions error"));

    // Delete User Service Exception
    when(mockAdminService.deleteUser(anyString(), anyString()))
        .thenThrow(new IllegalArgumentException("Admin delete user error"));
    String delUserReq =
        "{\"type\":\"ADMIN_DELETE_USER\",\"requestId\":\"r-7\",\"payload\":{\"targetUserId\":\"target-1\",\"callerId\":\"caller-1\"}}";
    String delUserResp = router.handleRequest(delUserReq);
    assertTrue(delUserResp.contains("Admin delete user error"));

    // Cancel Auction Service Exception
    when(mockAdminService.cancelAuction(anyString(), anyString()))
        .thenThrow(new IllegalArgumentException("Admin cancel error"));
    String cancelAucReq =
        "{\"type\":\"ADMIN_CANCEL_AUCTION\",\"requestId\":\"r-8\",\"payload\":{\"auctionId\":\"auc-1\",\"callerId\":\"caller-1\"}}";
    String cancelAucResp = router.handleRequest(cancelAucReq);
    assertTrue(cancelAucResp.contains("Admin cancel error"));

    // 3. AuctionHandler errors
    AuctionService mockAuctionService = mock(AuctionService.class);
    NotificationService mockNotification = mock(NotificationService.class);
    AuctionHandler auctionHandler = new AuctionHandler(mockAuctionService, mockNotification);
    auctionHandler.register(router);

    // Create Auction Service Exception
    when(mockAuctionService.createAuction(anyString(), any()))
        .thenThrow(new IllegalArgumentException("Create auction error"));
    String createReq =
        "{\"type\":\"CREATE_AUCTION\",\"requestId\":\"r-9\",\"payload\":{\"sellerId\":\"00000000-0000-0000-0000-000000000001\",\"name\":\"Table\",\"description\":\"Wood\",\"startingPrice\":\"10.0\",\"category\":\"ART\",\"condition\":\"NEW\",\"startTime\":\"2026-05-22T15:00:00\",\"endTime\":\"2026-05-25T15:00:00\"}}";
    String createResp = router.handleRequest(createReq);
    assertTrue(createResp.contains("Create auction error"));

    // List Listings Service Exception
    when(mockAuctionService.getMyListings(anyString()))
        .thenThrow(new IllegalArgumentException("My listings error"));
    String listReq =
        "{\"type\":\"LIST_MY_LISTINGS\",\"requestId\":\"r-10\",\"payload\":{\"sellerId\":\"seller-1\"}}";
    String listResp = router.handleRequest(listReq);
    assertTrue(listResp.contains("My listings error"));

    // Delete Auction Service Exception
    doThrow(new IllegalArgumentException("Delete auction error"))
        .when(mockAuctionService)
        .deleteAuction(anyString(), anyString());
    String delReq =
        "{\"type\":\"DELETE_AUCTION\",\"requestId\":\"r-11\",\"payload\":{\"sellerId\":\"seller-1\",\"auctionId\":\"auc-1\"}}";
    String delResp = router.handleRequest(delReq);
    assertTrue(delResp.contains("Delete auction error"));

    // 4. AutoBidHandler errors
    AutoBidService mockAutoBidService = mock(AutoBidService.class);
    AutoBidHandler autoBidHandler = new AutoBidHandler(mockAutoBidService);
    autoBidHandler.register(router);

    when(mockAutoBidService.saveConfig(any()))
        .thenThrow(new IllegalArgumentException("Auto bid config error"));
    String configReq =
        "{\"type\":\"AUTO_BID_CONFIG\",\"requestId\":\"r-12\",\"payload\":{\"auctionId\":\"auc-1\",\"bidderId\":\"user-1\",\"maxAmount\":100.0,\"increment\":5.0}}";
    String configResp = router.handleRequest(configReq);
    assertTrue(configResp.contains("Auto bid config error"));

    // 5. BidHandler errors
    BidService mockBidService = mock(BidService.class);
    BidHandler bidHandler = new BidHandler(mockBidService, mockNotification);
    bidHandler.register(router);

    // Place Bid Service Exception
    when(mockBidService.placeBid(any(), any(), any(), any()))
        .thenThrow(new com.nhom1.auction.common.exception.ValidationException("Place bid error"));
    String placeBidReq =
        "{\"type\":\"PLACE_BID\",\"requestId\":\"r-13\",\"payload\":{\"auctionId\":\""
            + UUID.randomUUID()
            + "\",\"bidderId\":\""
            + UUID.randomUUID()
            + "\",\"bidAmount\":\"150.0\"}}";
    String placeBidResp = router.handleRequest(placeBidReq);
    assertTrue(placeBidResp.contains("Place bid error"));

    // List Auctions Service Exception
    when(mockBidService.listAllAuctions())
        .thenThrow(new IllegalArgumentException("List auctions error"));
    String listAllReq = "{\"type\":\"LIST_AUCTIONS\",\"requestId\":\"r-14\",\"payload\":{}}";
    String listAllResp = router.handleRequest(listAllReq);
    assertTrue(listAllResp.contains("List auctions error"));

    // Get Auction Detail Service Exception
    when(mockBidService.getAuctionDetail(any()))
        .thenThrow(
            new com.nhom1.auction.common.exception.ValidationException("Get auction detail error"));
    String detailReq =
        "{\"type\":\"GET_AUCTION_DETAIL\",\"requestId\":\"r-15\",\"payload\":{\"auctionId\":\""
            + UUID.randomUUID()
            + "\"}}";
    String detailResp = router.handleRequest(detailReq);
    assertTrue(detailResp.contains("Get auction detail error"));

    // List My Bids Service Exception
    when(mockBidService.getMyBids(any()))
        .thenThrow(
            new com.nhom1.auction.common.exception.ValidationException("List my bids error"));
    String myBidsReq =
        "{\"type\":\"LIST_MY_BIDS\",\"requestId\":\"r-16\",\"payload\":{\"bidderId\":\""
            + UUID.randomUUID()
            + "\"}}";
    String myBidsResp = router.handleRequest(myBidsReq);
    assertTrue(myBidsResp.contains("List my bids error"));

    // 6. PaymentHandler errors
    PaymentService mockPaymentService = mock(PaymentService.class);
    PaymentHandler paymentHandler = new PaymentHandler(mockPaymentService);
    paymentHandler.register(router);

    // Process Payment Service Exception
    when(mockPaymentService.processPayment(anyString(), anyString()))
        .thenThrow(new IllegalArgumentException("Process payment error"));
    String processReq =
        "{\"type\":\"PROCESS_PAYMENT\",\"requestId\":\"r-17\",\"payload\":{\"auctionId\":\"auc-1\",\"bidderId\":\"user-1\"}}";
    String processResp = router.handleRequest(processReq);
    assertTrue(processResp.contains("Process payment error"));

    // List Pending Payments Service Exception
    when(mockPaymentService.listPendingPayments(anyString()))
        .thenThrow(new IllegalArgumentException("Pending payments error"));
    String pendingReq =
        "{\"type\":\"LIST_PENDING_PAYMENTS\",\"requestId\":\"r-18\",\"payload\":{\"bidderId\":\"user-1\"}}";
    String pendingResp = router.handleRequest(pendingReq);
    assertTrue(pendingResp.contains("Pending payments error"));

    // List Payment History Service Exception
    when(mockPaymentService.listPaymentHistory(anyString()))
        .thenThrow(new IllegalArgumentException("Payment history error"));
    String histReq =
        "{\"type\":\"LIST_PAYMENT_HISTORY\",\"requestId\":\"r-19\",\"payload\":{\"userId\":\"user-1\"}}";
    String histResp = router.handleRequest(histReq);
    assertTrue(histResp.contains("Payment history error"));
  }

  @Test
  public void testMessageRouterEdgeCases() {
    MessageRouter router = new MessageRouter();

    // 1. Missing message type
    String missingTypeJson = "{\"requestId\":\"r-1\",\"payload\":{}}";
    String resp1 = router.handleRequest(missingTypeJson);
    assertTrue(resp1.contains("INVALID_FORMAT"));
    assertTrue(resp1.contains("Missing message type"));

    // 2. Malformed JSON
    String malformedJson = "{\"type\":\"LOGIN\",";
    String resp2 = router.handleRequest(malformedJson);
    assertTrue(resp2.contains("INVALID_FORMAT"));
    assertTrue(resp2.contains("Malformed JSON request"));

    // 3. Unknown/Invalid Type
    String unknownTypeJson = "{\"type\":\"INVALID_MESSAGE_TYPE_ENUM_VALUE\",\"requestId\":\"r-1\"}";
    String resp3 = router.handleRequest(unknownTypeJson);
    assertTrue(resp3.contains("INVALID_TYPE"));
    assertTrue(resp3.contains("Unknown message type"));

    // 4. Action throws generic Exception
    router.register(
        MessageType.LOGIN,
        (requestId, payloadJson) -> {
          throw new RuntimeException("Direct router exception");
        });
    String directExReq = "{\"type\":\"LOGIN\",\"requestId\":\"r-4\"}";
    String resp4 = router.handleRequest(directExReq);
    assertTrue(resp4.contains("SERVER_ERROR"));
    assertTrue(resp4.contains("Direct router exception"));
  }
}
