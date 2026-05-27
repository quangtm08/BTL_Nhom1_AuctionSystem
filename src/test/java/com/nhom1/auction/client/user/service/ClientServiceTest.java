package com.nhom1.auction.client.user.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nhom1.auction.client.admin.service.AdminClientService;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.dto.admin.*;
import com.nhom1.auction.common.dto.auction.*;
import com.nhom1.auction.common.dto.auth.*;
import com.nhom1.auction.common.dto.autobid.*;
import com.nhom1.auction.common.dto.bidding.*;
import com.nhom1.auction.common.dto.payment.*;
import com.nhom1.auction.common.dto.wallet.*;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@SuppressWarnings({"rawtypes", "unchecked"})
public class ClientServiceTest {

  private ServerConnection mockConnection;

  @BeforeEach
  public void setUp() throws Exception {
    // Set up mock ServerConnection singleton
    mockConnection = mock(ServerConnection.class);
    lenient()
        .when(mockConnection.sendRequest(any(), any()))
        .thenReturn(
            CompletableFuture.failedFuture(new IOException("Mock connection: not connected")));
    Field instanceField = ServerConnection.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, mockConnection);

    // Reset AppContext session
    AppContext.clearSession();
  }

  @Test
  public void testAuthClientService() throws Exception {
    AuthClientService service = new AuthClientService();

    // 1. Login success
    AuthResponse loginRes = new AuthResponse();
    loginRes.setUsername("testuser");
    ResponseMessage<AuthResponse> successMsg = new ResponseMessage<>("id", loginRes);
    CompletableFuture rawFuture = CompletableFuture.completedFuture(successMsg);
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawFuture);

    AuthResponse resp = service.login("testuser", "password").get();
    assertEquals("testuser", resp.getUsername());

    // 2. Login validation failure
    assertThrows(CompletionException.class, () -> service.login(null, "password").join());
    assertThrows(CompletionException.class, () -> service.login("user", "").join());

    // 3. Register success
    ResponseMessage<AuthResponse> regMsg = new ResponseMessage<>("id", loginRes);
    CompletableFuture rawRegFuture = CompletableFuture.completedFuture(regMsg);
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawRegFuture);
    AuthResponse respReg =
        service.register("testuser", "email@test.com", "password", "password").get();
    assertEquals("testuser", respReg.getUsername());

    // 4. Register validation failure
    assertThrows(
        CompletionException.class,
        () -> service.register("", "email@test.com", "password", "password").join());
    assertThrows(
        CompletionException.class,
        () -> service.register("testuser2", null, "password", "password").join());
    assertThrows(
        CompletionException.class,
        () -> service.register("user", "email@test.com", "", "password").join());
    assertThrows(
        CompletionException.class,
        () -> service.register("user", "email@test.com", "password", "different").join());

    // 5. Logout
    service.logout();
    assertNull(AppContext.getCurrentUser());
  }

  @Test
  public void testAutoBidClientService() throws Exception {
    AutoBidClientService service = new AutoBidClientService();

    // 1. Validation failure: no user logged in
    assertThrows(
        CompletionException.class,
        () ->
            service
                .saveConfig("auction-1", new BigDecimal("100.00"), new BigDecimal("10.00"))
                .join());

    // Login user
    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    // 2. Configure AutoBid success
    AutoBidConfigResponse conf = new AutoBidConfigResponse();
    ResponseMessage<AutoBidConfigResponse> successMsg = new ResponseMessage<>("id", conf);
    CompletableFuture rawFuture = CompletableFuture.completedFuture(successMsg);
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawFuture);

    AutoBidConfigResponse resp =
        service.saveConfig("auction-1", new BigDecimal("100.00"), new BigDecimal("10.00")).get();
    assertNotNull(resp);

    // 3. Configure validation failure
    assertThrows(
        CompletionException.class,
        () -> service.saveConfig(null, new BigDecimal("100.00"), new BigDecimal("10.00")).join());
    assertThrows(
        CompletionException.class,
        () -> service.saveConfig("", new BigDecimal("100.00"), new BigDecimal("10.00")).join());
    assertThrows(
        CompletionException.class,
        () -> service.saveConfig("auction-1", BigDecimal.ZERO, new BigDecimal("10.00")).join());
    assertThrows(
        CompletionException.class,
        () ->
            service
                .saveConfig("auction-1", new BigDecimal("-5.00"), new BigDecimal("10.00"))
                .join());
    assertThrows(
        CompletionException.class,
        () -> service.saveConfig("auction-1", new BigDecimal("100.00"), BigDecimal.ZERO).join());
    assertThrows(
        CompletionException.class,
        () ->
            service
                .saveConfig("auction-1", new BigDecimal("100.00"), new BigDecimal("-2.00"))
                .join());
    assertThrows(
        CompletionException.class,
        () ->
            service
                .saveConfig("auction-1", new BigDecimal("50.00"), new BigDecimal("100.00"))
                .join());
  }

  @Test
  public void testBiddingClientService() throws Exception {
    BiddingClientService service = new BiddingClientService();

    // 1. List auctions
    ListAuctionsResponse listRes = new ListAuctionsResponse();
    CompletableFuture rawListFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", listRes));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawListFuture);
    assertNotNull(service.listAuctions().get());

    // 2. Get auction detail
    AuctionDetailDto detail = new AuctionDetailDto();
    CompletableFuture rawDetailFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", detail));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawDetailFuture);
    assertNotNull(service.getAuctionDetail("auction-1").get());
    assertThrows(CompletionException.class, () -> service.getAuctionDetail(null).join());

    // 3. Place bid validation when not logged in
    assertThrows(CompletionException.class, () -> service.placeBid("auc-1", BigDecimal.TEN).join());

    // 4. Log in and test place bid validation / success
    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    assertThrows(CompletionException.class, () -> service.placeBid(null, BigDecimal.TEN).join());
    assertThrows(
        CompletionException.class, () -> service.placeBid("auc-1", BigDecimal.ZERO).join());

    PlaceBidResponse bidResp = new PlaceBidResponse();
    CompletableFuture rawBidFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", bidResp));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawBidFuture);
    assertNotNull(service.placeBid("auc-1", BigDecimal.TEN).get());

    // 5. Get my bids
    MyBidsResponse myBids = new MyBidsResponse();
    CompletableFuture rawMyBidsFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", myBids));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawMyBidsFuture);
    assertNotNull(service.getMyBids().get());

    // Clear session and test get my bids fails
    AppContext.clearSession();
    assertThrows(CompletionException.class, () -> service.getMyBids().join());
  }

  @Test
  public void testCreateAuctionClientService() throws Exception {
    CreateAuctionClientService service = new CreateAuctionClientService();

    // Mock internal ImageUploadService using reflection
    ImageUploadService mockUploadService = mock(ImageUploadService.class);
    Field uploadServiceField =
        CreateAuctionClientService.class.getDeclaredField("imageUploadService");
    uploadServiceField.setAccessible(true);
    uploadServiceField.set(service, mockUploadService);

    LocalDate nextDay = LocalDate.now().plusDays(1);
    // 1. validateInput checks when not logged in
    assertEquals(
        "Please sign in again.",
        service.validateInput(
            "Title",
            "10.0",
            com.nhom1.auction.common.enums.ItemCategory.ELECTRONICS,
            com.nhom1.auction.common.enums.ItemCondition.NEW,
            7,
            nextDay));

    // Login user
    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    // validateInput checks
    assertNull(
        service.validateInput(
            "Title",
            "10.0",
            com.nhom1.auction.common.enums.ItemCategory.ELECTRONICS,
            com.nhom1.auction.common.enums.ItemCondition.NEW,
            7,
            nextDay));
    assertEquals(
        "Title is required.",
        service.validateInput(
            "",
            "10.0",
            com.nhom1.auction.common.enums.ItemCategory.ELECTRONICS,
            com.nhom1.auction.common.enums.ItemCondition.NEW,
            7,
            nextDay));
    assertEquals(
        "Category and condition are required.",
        service.validateInput(
            "Title", "10.0", null, com.nhom1.auction.common.enums.ItemCondition.NEW, 7, nextDay));
    assertEquals(
        "Starting bid must be a valid number.",
        service.validateInput(
            "Title",
            "invalid",
            com.nhom1.auction.common.enums.ItemCategory.ELECTRONICS,
            com.nhom1.auction.common.enums.ItemCondition.NEW,
            7,
            nextDay));
    assertEquals(
        "Duration must be greater than 0.",
        service.validateInput(
            "Title",
            "10.0",
            com.nhom1.auction.common.enums.ItemCategory.ELECTRONICS,
            com.nhom1.auction.common.enums.ItemCondition.NEW,
            0,
            nextDay));

    // 2. createAuction success
    CreateAuctionResponse resp = new CreateAuctionResponse();
    CompletableFuture rawCreateFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", resp));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawCreateFuture);

    File dummyFile = File.createTempFile("test-upload", ".jpg");
    dummyFile.deleteOnExit();
    when(mockUploadService.upload(dummyFile))
        .thenReturn(CompletableFuture.completedFuture("http://uploaded-url.com/1.jpg"));

    CreateAuctionResponse createResp =
        service
            .createAuction(
                "Title",
                "Desc",
                "10.0",
                com.nhom1.auction.common.enums.ItemCategory.ELECTRONICS,
                com.nhom1.auction.common.enums.ItemCondition.NEW,
                7,
                nextDay,
                java.util.List.of(dummyFile))
            .get();
    assertNotNull(createResp);
  }

  @Test
  public void testPaymentClientService() throws Exception {
    PaymentClientService service = new PaymentClientService();

    // 1. Get pending payments validation when not logged in
    assertThrows(CompletionException.class, () -> service.listPendingPayments().join());

    // Login as user
    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    // 2. Get pending payments success
    PendingPaymentsResponse pending = new PendingPaymentsResponse();
    CompletableFuture rawPendingFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", pending));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawPendingFuture);
    assertNotNull(service.listPendingPayments().get());

    // 3. Get payment history success
    PaymentHistoryResponse history = new PaymentHistoryResponse();
    CompletableFuture rawHistoryFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", history));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawHistoryFuture);
    assertNotNull(service.listPaymentHistory().get());

    // 4. Process payment validation
    assertThrows(CompletionException.class, () -> service.processPayment(null).join());

    // 5. Process payment success
    ProcessPaymentResponse payResp = new ProcessPaymentResponse();
    CompletableFuture rawPayFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", payResp));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawPayFuture);
    assertNotNull(service.processPayment("auc-1").get());
  }

  @Test
  public void testAdminClientService() throws Exception {
    AdminClientService service = new AdminClientService();

    // 1. Validation when not logged in as Admin
    assertThrows(CompletionException.class, () -> service.listUsers().join());
    assertThrows(CompletionException.class, () -> service.listAllAuctions().join());
    assertThrows(CompletionException.class, () -> service.deleteUser("user-1").join());
    assertThrows(CompletionException.class, () -> service.cancelAuction("auc-1").join());

    // Login as ADMIN user
    AuthResponse admin = new AuthResponse();
    admin.setUserID("admin-1");
    admin.setRole(UserRole.ADMIN);
    AppContext.setCurrentUser(admin);

    // 2. List users success
    AdminUserListResponse users = new AdminUserListResponse();
    CompletableFuture rawUsersFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", users));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawUsersFuture);
    assertNotNull(service.listUsers().get());

    // 3. List auctions success
    AdminAuctionListResponse listAuctions = new AdminAuctionListResponse();
    CompletableFuture rawAuctionsFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", listAuctions));
    when(mockConnection.sendRequest(any(RequestMessage.class), any()))
        .thenReturn(rawAuctionsFuture);
    assertNotNull(service.listAllAuctions().get());

    // 4. Delete user success
    CompletableFuture rawDelFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", "user-deleted"));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawDelFuture);
    assertEquals("user-deleted", service.deleteUser("user-1").get());
    assertThrows(CompletionException.class, () -> service.deleteUser(null).join());

    // 5. Cancel auction success
    CompletableFuture rawCancelFuture =
        CompletableFuture.completedFuture(new ResponseMessage<>("id", "canceled"));
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawCancelFuture);
    assertEquals("canceled", service.cancelAuction("auc-1").get());
    assertThrows(CompletionException.class, () -> service.cancelAuction(null).join());
  }

  @Test
  public void testImageUploadService() throws Exception {
    ImageUploadService service = new ImageUploadService();

    // Mock HttpClient inside ImageUploadService via reflection
    HttpClient mockClient = mock(HttpClient.class);
    Field clientField = ImageUploadService.class.getDeclaredField("httpClient");
    clientField.setAccessible(true);
    clientField.set(service, mockClient);

    // 1. Invalid input validation
    assertThrows(CompletionException.class, () -> service.upload(null).join());
    assertThrows(
        CompletionException.class, () -> service.upload(new File("nonexistent-file.jpg")).join());

    // Create temporary dummy file for testing file paths
    File dummyFile = File.createTempFile("test-image", ".jpg");
    dummyFile.deleteOnExit();

    // 2. Success upload path
    HttpResponse mockResponse = mock(HttpResponse.class);
    when(mockResponse.statusCode()).thenReturn(200);
    when(mockResponse.body())
        .thenReturn("{\"success\":true,\"data\":{\"display_url\":\"https://imgbb.com/test.jpg\"}}");
    when(mockClient.send(any(), any())).thenReturn(mockResponse);

    String result = service.upload(dummyFile).get();
    assertEquals("https://imgbb.com/test.jpg", result);

    // 3. Bad status code response path
    HttpResponse errorResponse = mock(HttpResponse.class);
    when(errorResponse.statusCode()).thenReturn(500);
    when(mockClient.send(any(), any())).thenReturn(errorResponse);

    assertThrows(CompletionException.class, () -> service.upload(dummyFile).join());

    // 4. Invalid JSON payload response path
    HttpResponse invalidJsonResponse = mock(HttpResponse.class);
    when(invalidJsonResponse.statusCode()).thenReturn(200);
    when(invalidJsonResponse.body()).thenReturn("{\"success\":false}");
    when(mockClient.send(any(), any())).thenReturn(invalidJsonResponse);

    assertThrows(CompletionException.class, () -> service.upload(dummyFile).join());

    // 5. InterruptedException path
    when(mockClient.send(any(), any())).thenThrow(new InterruptedException("Simulated interrupt"));
    assertThrows(CompletionException.class, () -> service.upload(dummyFile).join());
  }

  @Test
  public void testListMyBidsRequest() {
    com.nhom1.auction.common.dto.bidding.ListMyBidsRequest req1 =
        new com.nhom1.auction.common.dto.bidding.ListMyBidsRequest();
    assertNull(req1.getBidderId());
    req1.setBidderId("bidder-123");
    assertEquals("bidder-123", req1.getBidderId());

    com.nhom1.auction.common.dto.bidding.ListMyBidsRequest req2 =
        new com.nhom1.auction.common.dto.bidding.ListMyBidsRequest("bidder-456");
    assertEquals("bidder-456", req2.getBidderId());
  }

  @Test
  public void testWalletClientService() throws Exception {
    WalletClientService service = new WalletClientService();
    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    // 1. Get Wallet success
    WalletResponse walletRes =
        new WalletResponse("user-1", new BigDecimal("1000.00"), java.util.Collections.emptyList());
    ResponseMessage<WalletResponse> successMsg = new ResponseMessage<>("id", walletRes);
    CompletableFuture rawFuture = CompletableFuture.completedFuture(successMsg);
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawFuture);

    WalletResponse resp = service.getWallet().get();
    assertEquals("user-1", resp.getUserId());
    assertEquals(new BigDecimal("1000.00"), resp.getBalance());

    // 2. Get Wallet validation failure
    AppContext.clearSession();
    assertThrows(CompletionException.class, () -> service.getWallet().join());
    AppContext.setCurrentUser(user);

    // 3. Deposit success
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawFuture);
    WalletResponse respDep = service.deposit(new BigDecimal("500.00")).get();
    assertNotNull(respDep);

    // 4. Deposit validation failure
    AppContext.clearSession();
    assertThrows(CompletionException.class, () -> service.deposit(new BigDecimal("500.00")).join());
    AppContext.setCurrentUser(user);
    assertThrows(CompletionException.class, () -> service.deposit(null).join());
    assertThrows(CompletionException.class, () -> service.deposit(BigDecimal.ZERO).join());
    assertThrows(CompletionException.class, () -> service.deposit(new BigDecimal("-10.00")).join());

    // 5. Withdraw success
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawFuture);
    WalletResponse respWith = service.withdraw(new BigDecimal("200.00")).get();
    assertNotNull(respWith);

    // 6. Withdraw validation failure
    AppContext.clearSession();
    assertThrows(
        CompletionException.class, () -> service.withdraw(new BigDecimal("200.00")).join());
    AppContext.setCurrentUser(user);
    assertThrows(CompletionException.class, () -> service.withdraw(null).join());
    assertThrows(CompletionException.class, () -> service.withdraw(BigDecimal.ZERO).join());
    assertThrows(CompletionException.class, () -> service.withdraw(new BigDecimal("-5.00")).join());
  }

  @Test
  public void testMyListingsClientService() throws Exception {
    MyListingsClientService service = new MyListingsClientService();

    // 1. Not logged in
    assertThrows(CompletionException.class, () -> service.listMyListings().join());
    assertThrows(CompletionException.class, () -> service.deleteListing("auc-1").join());

    // 2. Log in
    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    // 3. List my listings success
    MyListingsResponse myRes = new MyListingsResponse();
    ResponseMessage<MyListingsResponse> successMsg = new ResponseMessage<>("id", myRes);
    CompletableFuture rawListFuture = CompletableFuture.completedFuture(successMsg);
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawListFuture);

    assertNotNull(service.listMyListings().get());

    // 4. Delete listing success
    ResponseMessage<String> successDel = new ResponseMessage<>("id", "deleted");
    CompletableFuture rawDelFuture = CompletableFuture.completedFuture(successDel);
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawDelFuture);

    assertEquals("deleted", service.deleteListing("auc-1").get());
    assertThrows(CompletionException.class, () -> service.deleteListing(null).join());
  }

  @Test
  public void testEditAuctionClientService() throws Exception {
    MyListingsClientService service = new MyListingsClientService();

    // 1. Not logged in
    assertThrows(
        CompletionException.class,
        () ->
            service
                .updateListing(
                    "auc-1",
                    "title",
                    "desc",
                    "10.0",
                    com.nhom1.auction.common.enums.ItemCategory.ART,
                    com.nhom1.auction.common.enums.ItemCondition.NEW,
                    LocalDateTime.now().plusDays(1))
                .join());

    // 2. Log in
    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    // 3. Validation failure
    assertThrows(
        CompletionException.class,
        () ->
            service
                .updateListing(
                    null,
                    "title",
                    "desc",
                    "10.0",
                    com.nhom1.auction.common.enums.ItemCategory.ART,
                    com.nhom1.auction.common.enums.ItemCondition.NEW,
                    LocalDateTime.now().plusDays(1))
                .join());
    assertThrows(
        CompletionException.class,
        () ->
            service
                .updateListing(
                    "auc-1",
                    "",
                    "desc",
                    "10.0",
                    com.nhom1.auction.common.enums.ItemCategory.ART,
                    com.nhom1.auction.common.enums.ItemCondition.NEW,
                    LocalDateTime.now().plusDays(1))
                .join());
    assertThrows(
        CompletionException.class,
        () ->
            service
                .updateListing(
                    "auc-1",
                    "title",
                    "desc",
                    "bad bid",
                    com.nhom1.auction.common.enums.ItemCategory.ART,
                    com.nhom1.auction.common.enums.ItemCondition.NEW,
                    LocalDateTime.now().plusDays(1))
                .join());
    assertThrows(
        CompletionException.class,
        () ->
            service
                .updateListing(
                    "auc-1",
                    "title",
                    "desc",
                    "10.0",
                    null,
                    com.nhom1.auction.common.enums.ItemCondition.NEW,
                    LocalDateTime.now().plusDays(1))
                .join());
    assertThrows(
        CompletionException.class,
        () ->
            service
                .updateListing(
                    "auc-1",
                    "title",
                    "desc",
                    "10.0",
                    com.nhom1.auction.common.enums.ItemCategory.ART,
                    com.nhom1.auction.common.enums.ItemCondition.NEW,
                    LocalDateTime.now().minusMinutes(5))
                .join());

    // 4. Update success
    ResponseMessage<String> successUpdate = new ResponseMessage<>("id", "updated");
    CompletableFuture rawUpdateFuture = CompletableFuture.completedFuture(successUpdate);
    when(mockConnection.sendRequest(any(RequestMessage.class), any())).thenReturn(rawUpdateFuture);

    assertEquals(
        "updated",
        service
            .updateListing(
                "auc-1",
                "title",
                "desc",
                "10.0",
                com.nhom1.auction.common.enums.ItemCategory.ART,
                com.nhom1.auction.common.enums.ItemCondition.NEW,
                LocalDateTime.now().plusDays(1))
            .get());
  }
}
