package com.nhom1.auction.client.user.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.BaseShellController;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.client.user.controller.components.AuctionCardComponentController;
import com.nhom1.auction.client.user.controller.components.BidCardComponentController;
import com.nhom1.auction.client.user.controller.components.ListingCardComponentController;
import com.nhom1.auction.client.user.service.AuthClientService;
import com.nhom1.auction.client.user.service.BiddingClientService;
import com.nhom1.auction.client.user.service.CreateAuctionClientService;
import com.nhom1.auction.client.user.service.PaymentClientService;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.auction.CreateAuctionResponse;
import com.nhom1.auction.common.dto.auction.MyListingsResponse;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.BidSummaryDto;
import com.nhom1.auction.common.dto.bidding.BidWithAuctionDto;
import com.nhom1.auction.common.dto.bidding.ListAuctionsResponse;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;
import com.nhom1.auction.common.dto.bidding.PlaceBidResponse;
import com.nhom1.auction.common.dto.payment.PaymentHistoryEntryDto;
import com.nhom1.auction.common.dto.payment.PaymentHistoryResponse;
import com.nhom1.auction.common.dto.payment.PendingPaymentDto;
import com.nhom1.auction.common.dto.payment.PendingPaymentsResponse;
import com.nhom1.auction.common.dto.payment.ProcessPaymentResponse;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class UserControllerTest {

  private static ServerConnection mockConnection;
  private static BaseShellController mockShell;
  private final Map<MessageType, java.util.function.Consumer<String>> registeredPushHandlers =
      new java.util.concurrent.ConcurrentHashMap<>();

  @BeforeAll
  public static void initJavaFX() throws Exception {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException e) {
      // Already initialized
    }
    mockConnection = mock(ServerConnection.class);
    java.lang.reflect.Field field = ServerConnection.class.getDeclaredField("instance");
    field.setAccessible(true);
    field.set(null, mockConnection);

    mockShell = mock(BaseShellController.class);
    AppNavigator.setRoot(mockShell);
  }

  @BeforeEach
  public void setUp() throws Exception {
    AppContext.clearSession();
    AppContext.setSelectedAuctionId(null);
    reset(mockConnection);
    reset(mockShell);
    registeredPushHandlers.clear();

    // Reset ClientPushService singleton to force registration of push handlers
    java.lang.reflect.Field pushInstanceField =
        com.nhom1.auction.client.service.ClientPushService.class.getDeclaredField("instance");
    pushInstanceField.setAccessible(true);
    pushInstanceField.set(null, null);

    org.mockito.Mockito.doAnswer(
            invocation -> {
              MessageType type = invocation.getArgument(0);
              java.util.function.Consumer<String> handler = invocation.getArgument(1);
              registeredPushHandlers.put(type, handler);
              return null;
            })
        .when(mockConnection)
        .registerPushHandler(any(MessageType.class), any(java.util.function.Consumer.class));
  }

  private void injectField(Object target, String fieldName, Object value) throws Exception {
    java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
    field.setAccessible(true);
    field.set(target, value);
  }

  private void waitForRunLater() throws Exception {
    CompletableFuture<Void> future = new CompletableFuture<>();
    Platform.runLater(() -> future.complete(null));
    future.get(3, TimeUnit.SECONDS);
  }

  @Test
  public void testSignInControllerSuccessUser() throws Exception {
    SignInController controller = new SignInController();

    AuthClientService mockAuthService = mock(AuthClientService.class);
    AuthResponse response = new AuthResponse();
    response.setUserID("user-1");
    response.setUsername("john_doe");
    response.setRole(UserRole.USER);

    when(mockAuthService.login(eq("john_doe"), eq("password")))
        .thenReturn(CompletableFuture.completedFuture(response));

    Button btnSignIn = new Button();
    Button btnRegister = new Button();
    TextField txtUsername = new TextField("john_doe");
    PasswordField txtPassword = new PasswordField();
    txtPassword.setText("password");

    injectField(controller, "authService", mockAuthService);
    injectField(controller, "btnSignIn", btnSignIn);
    injectField(controller, "btnRegister", btnRegister);
    injectField(controller, "txtUsername", txtUsername);
    injectField(controller, "txtPassword", txtPassword);

    controller.initialize();

    // Click register button
    btnRegister.getOnAction().handle(null);
    // Click sign in button
    btnSignIn.getOnAction().handle(null);

    waitForRunLater();

    verify(mockAuthService).login("john_doe", "password");
  }

  @Test
  public void testSignInControllerSuccessAdmin() throws Exception {
    SignInController controller = new SignInController();

    AuthClientService mockAuthService = mock(AuthClientService.class);
    AuthResponse response = new AuthResponse();
    response.setUserID("admin-1");
    response.setUsername("admin");
    response.setRole(UserRole.ADMIN);

    when(mockAuthService.login(eq("admin"), eq("adminpass")))
        .thenReturn(CompletableFuture.completedFuture(response));

    Button btnSignIn = new Button();
    Button btnRegister = new Button();
    TextField txtUsername = new TextField("admin");
    PasswordField txtPassword = new PasswordField();
    txtPassword.setText("adminpass");

    injectField(controller, "authService", mockAuthService);
    injectField(controller, "btnSignIn", btnSignIn);
    injectField(controller, "btnRegister", btnRegister);
    injectField(controller, "txtUsername", txtUsername);
    injectField(controller, "txtPassword", txtPassword);

    controller.initialize();

    btnSignIn.getOnAction().handle(null);

    waitForRunLater();

    verify(mockAuthService).login("admin", "adminpass");
  }

  @Test
  public void testSignInControllerFailure() throws Exception {
    SignInController controller = new SignInController();

    AuthClientService mockAuthService = mock(AuthClientService.class);
    when(mockAuthService.login(anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Invalid credentials")));

    Button btnSignIn = new Button();
    Button btnRegister = new Button();
    TextField txtUsername = new TextField("invalid");
    PasswordField txtPassword = new PasswordField();
    txtPassword.setText("wrong");

    injectField(controller, "authService", mockAuthService);
    injectField(controller, "btnSignIn", btnSignIn);
    injectField(controller, "btnRegister", btnRegister);
    injectField(controller, "txtUsername", txtUsername);
    injectField(controller, "txtPassword", txtPassword);

    controller.initialize();

    btnSignIn.getOnAction().handle(null);

    waitForRunLater();

    verify(mockAuthService).login("invalid", "wrong");
    assertEquals("", txtPassword.getText());
  }

  @Test
  public void testRegisterControllerSuccess() throws Exception {
    RegisterController controller = new RegisterController();

    AuthClientService mockAuthService = mock(AuthClientService.class);
    AuthResponse response = new AuthResponse();
    response.setUserID("user-2");
    response.setUsername("new_user");
    response.setRole(UserRole.USER);

    when(mockAuthService.register(eq("new_user"), eq("email@domain.com"), eq("pass"), eq("pass")))
        .thenReturn(CompletableFuture.completedFuture(response));

    Button btnRegister = new Button();
    Button btnSignIn = new Button();
    TextField txtUsername = new TextField("new_user");
    TextField txtEmail = new TextField("email@domain.com");
    PasswordField txtPassword = new PasswordField();
    txtPassword.setText("pass");
    PasswordField txtRepeatPassword = new PasswordField();
    txtRepeatPassword.setText("pass");

    injectField(controller, "authService", mockAuthService);
    injectField(controller, "btnRegister", btnRegister);
    injectField(controller, "btnSignIn", btnSignIn);
    injectField(controller, "txtUsername", txtUsername);
    injectField(controller, "txtEmail", txtEmail);
    injectField(controller, "txtPassword", txtPassword);
    injectField(controller, "txtRepeatPassword", txtRepeatPassword);

    controller.initialize();

    // Test navigation to sign in
    btnSignIn.getOnAction().handle(null);

    // Test registration
    btnRegister.getOnAction().handle(null);

    waitForRunLater();

    verify(mockAuthService).register("new_user", "email@domain.com", "pass", "pass");
  }

  @Test
  public void testRegisterControllerFailure() throws Exception {
    RegisterController controller = new RegisterController();

    AuthClientService mockAuthService = mock(AuthClientService.class);
    when(mockAuthService.register(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Username taken")));

    Button btnRegister = new Button();
    Button btnSignIn = new Button();
    TextField txtUsername = new TextField("taken");
    TextField txtEmail = new TextField("");
    PasswordField txtPassword = new PasswordField();
    txtPassword.setText("pass");
    PasswordField txtRepeatPassword = new PasswordField();
    txtRepeatPassword.setText("pass");

    injectField(controller, "authService", mockAuthService);
    injectField(controller, "btnRegister", btnRegister);
    injectField(controller, "btnSignIn", btnSignIn);
    injectField(controller, "txtUsername", txtUsername);
    injectField(controller, "txtEmail", txtEmail);
    injectField(controller, "txtPassword", txtPassword);
    injectField(controller, "txtRepeatPassword", txtRepeatPassword);

    controller.initialize();

    btnRegister.getOnAction().handle(null);

    waitForRunLater();

    verify(mockAuthService).register("taken", "", "pass", "pass");
  }

  @Test
  public void testUserSidebarController() throws Exception {
    UserSidebarController controller = new UserSidebarController();

    AuthResponse user = new AuthResponse();
    user.setUsername("sally");
    AppContext.setCurrentUser(user);

    Button btnExplore = new Button();
    Button btnBids = new Button();
    Button btnListings = new Button();
    Button btnPayment = new Button();
    Button btnWallet = new Button();
    Button btnLogout = new Button();
    Label usernameLabel = new Label();
    Label balanceLabel = new Label();

    injectField(controller, "btnExplore", btnExplore);
    injectField(controller, "btnBids", btnBids);
    injectField(controller, "btnListings", btnListings);
    injectField(controller, "btnPayment", btnPayment);
    injectField(controller, "btnWallet", btnWallet);
    injectField(controller, "btnLogout", btnLogout);
    injectField(controller, "usernameLabel", usernameLabel);
    injectField(controller, "balanceLabel", balanceLabel);

    controller.initialize();

    assertEquals("sally", usernameLabel.getText());

    // Test navigation flows
    btnExplore.getOnAction().handle(null);
    btnBids.getOnAction().handle(null);
    btnListings.getOnAction().handle(null);
    btnPayment.getOnAction().handle(null);
    btnWallet.getOnAction().handle(null);
    btnLogout.getOnAction().handle(null);

    // Test with empty/null user
    AppContext.clearSession();
    controller.initialize();
    assertEquals("Guest", usernameLabel.getText());
  }

  @Test
  public void testUserSidebarControllerEdgeCases() throws Exception {
    UserSidebarController controller = new UserSidebarController();

    Button btnExplore = new Button();
    Button btnBids = new Button();
    Button btnListings = new Button();
    Button btnPayment = new Button();
    Button btnWallet = new Button();
    Button btnLogout = new Button();
    Label usernameLabel = new Label();
    Label balanceLabel = new Label();

    injectField(controller, "btnExplore", btnExplore);
    injectField(controller, "btnBids", btnBids);
    injectField(controller, "btnListings", btnListings);
    injectField(controller, "btnPayment", btnPayment);
    injectField(controller, "btnWallet", btnWallet);
    injectField(controller, "btnLogout", btnLogout);
    injectField(controller, "usernameLabel", usernameLabel);
    injectField(controller, "balanceLabel", balanceLabel);

    // Case 1: current user is not null, but username is null
    AuthResponse userNullName = new AuthResponse();
    userNullName.setUsername(null);
    AppContext.setCurrentUser(userNullName);
    controller.initialize();
    assertEquals("Guest", usernameLabel.getText());

    // Case 2: current user is not null, username is blank
    AuthResponse userBlankName = new AuthResponse();
    userBlankName.setUsername("   ");
    AppContext.setCurrentUser(userBlankName);
    controller.initialize();
    assertEquals("Guest", usernameLabel.getText());

    // Case 3: updateActiveButton switch cases
    java.lang.reflect.Field fCurrentView = AppNavigator.class.getDeclaredField("currentView");
    fCurrentView.setAccessible(true);
    java.lang.reflect.Method mUpdateActive =
        UserSidebarController.class.getDeclaredMethod("updateActiveButton");
    mUpdateActive.setAccessible(true);

    fCurrentView.set(null, AppView.AUCTION_BROWSE);
    mUpdateActive.invoke(controller);
    assertTrue(btnExplore.getStyleClass().contains("btn-ghost-active"));

    fCurrentView.set(null, AppView.MY_BIDS);
    mUpdateActive.invoke(controller);
    assertTrue(btnBids.getStyleClass().contains("btn-ghost-active"));

    fCurrentView.set(null, AppView.MY_LISTINGS);
    mUpdateActive.invoke(controller);
    assertTrue(btnListings.getStyleClass().contains("btn-ghost-active"));

    fCurrentView.set(null, AppView.PAYMENT);
    mUpdateActive.invoke(controller);
    assertTrue(btnPayment.getStyleClass().contains("btn-ghost-active"));

    fCurrentView.set(null, AppView.WALLET);
    mUpdateActive.invoke(controller);
    assertTrue(btnWallet.getStyleClass().contains("btn-ghost-active"));

    fCurrentView.set(null, AppView.SIGN_IN);
    mUpdateActive.invoke(controller);
    assertFalse(btnExplore.getStyleClass().contains("btn-ghost-active"));
    assertFalse(btnBids.getStyleClass().contains("btn-ghost-active"));
    assertFalse(btnListings.getStyleClass().contains("btn-ghost-active"));
    assertFalse(btnPayment.getStyleClass().contains("btn-ghost-active"));
    assertFalse(btnWallet.getStyleClass().contains("btn-ghost-active"));

    fCurrentView.set(null, null);
    mUpdateActive.invoke(controller);
    assertFalse(btnExplore.getStyleClass().contains("btn-ghost-active"));

    // Test navigateWithLoading when targetView == currentView
    java.lang.reflect.Method mNavigate =
        UserSidebarController.class.getDeclaredMethod("navigateWithLoading", AppView.class);
    mNavigate.setAccessible(true);
    fCurrentView.set(null, AppView.PAYMENT);
    mNavigate.invoke(controller, AppView.PAYMENT); // should return early
  }

  @Test
  public void testAuctionBrowseController() throws Exception {
    AuctionBrowseController controller = new AuctionBrowseController();

    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    user.setUsername("alice");
    AppContext.setCurrentUser(user);

    BiddingClientService mockBiddingService = mock(BiddingClientService.class);

    AuctionSummaryDto auction = new AuctionSummaryDto();
    auction.setId("auc-1");
    auction.setItemName("Cool Painting");
    auction.setStartingPrice(BigDecimal.TEN);
    auction.setCurrentHighestBid(BigDecimal.ZERO);
    auction.setStatus(AuctionStatus.OPEN);
    auction.setSellerId("user-2"); // Not seller

    ListAuctionsResponse ar = new ListAuctionsResponse();
    ar.setAuctions(List.of(auction));

    when(mockBiddingService.listAuctions()).thenReturn(CompletableFuture.completedFuture(ar));
    when(mockBiddingService.getMyBids())
        .thenReturn(CompletableFuture.completedFuture(new MyBidsResponse(Collections.emptyList())));

    Label welcomeLabel = new Label();
    HBox mainContainer = new HBox();
    GridPane cardsGridPane = new GridPane();

    injectField(controller, "biddingService", mockBiddingService);
    injectField(controller, "welcomeLabel", welcomeLabel);
    injectField(controller, "mainContainer", mainContainer);
    injectField(controller, "cardsGridPane", cardsGridPane);

    controller.initialize();

    waitForRunLater();

    assertEquals("Hunt for the next deal, alice!", welcomeLabel.getText());
    verify(mockBiddingService).listAuctions();

    // Test handleBidUpdatePush
    String pushJson = "{\"payload\":{\"auctionId\":\"auc-1\",\"newHighestBid\":\"15.00\"}}";
    // Just call to verify coverage of JSON parsing
    injectField(controller, "priceLabels", Map.of("auc-1", new Label()));
    java.lang.reflect.Method m =
        AuctionBrowseController.class.getDeclaredMethod("handleBidUpdatePush", String.class);
    m.setAccessible(true);
    m.invoke(controller, pushJson);

    // Test handleAuctionDeletedPush
    String deleteJson = "{\"payload\":{\"auctionId\":\"auc-1\"}}";
    java.lang.reflect.Method m2 =
        AuctionBrowseController.class.getDeclaredMethod("handleAuctionDeletedPush", String.class);
    m2.setAccessible(true);
    m2.invoke(controller, deleteJson);
  }

  @Test
  public void testMyBidsController() throws Exception {
    MyBidsController controller = new MyBidsController();

    BiddingClientService mockBiddingService = mock(BiddingClientService.class);

    BidWithAuctionDto bid =
        new BidWithAuctionDto(
            "auc-1",
            "Item 1",
            BigDecimal.TEN,
            BigDecimal.TEN,
            AuctionStatus.OPEN,
            LocalDateTime.now().plusDays(1),
            true);

    MyBidsResponse mbr = new MyBidsResponse(List.of(bid));
    when(mockBiddingService.getMyBids()).thenReturn(CompletableFuture.completedFuture(mbr));

    GridPane cardsGridPane = new GridPane();
    injectField(controller, "biddingService", mockBiddingService);
    injectField(controller, "cardsGridPane", cardsGridPane);

    controller.initialize();
    waitForRunLater();

    verify(mockBiddingService).getMyBids();
  }

  @Test
  public void testMyListingsController() throws Exception {
    MyListingsController controller = new MyListingsController();

    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    AuctionSummaryDto summary = new AuctionSummaryDto();
    summary.setId("auc-1");
    summary.setItemName("My Item");
    summary.setStatus(AuctionStatus.OPEN);

    MyListingsResponse mlr = new MyListingsResponse();
    mlr.setListings(List.of(summary));

    ResponseMessage<MyListingsResponse> respMsg = new ResponseMessage<>();
    respMsg.setSuccess(true);
    respMsg.setPayload(mlr);

    when(mockConnection.sendRequest(any(RequestMessage.class), eq(MyListingsResponse.class)))
        .thenReturn(CompletableFuture.completedFuture(respMsg));

    Label activeListingsLabel = new Label();
    GridPane listingsGrid = new GridPane();

    injectField(controller, "activeListingsLabel", activeListingsLabel);
    injectField(controller, "listingsGrid", listingsGrid);

    // Test initialize
    java.lang.reflect.Method mInit = MyListingsController.class.getDeclaredMethod("initialize");
    mInit.setAccessible(true);
    mInit.invoke(controller);

    waitForRunLater();

    assertEquals("1", activeListingsLabel.getText());

    // Test handlers
    java.lang.reflect.Method hCreate =
        MyListingsController.class.getDeclaredMethod("handleCreateListing");
    hCreate.setAccessible(true);
    hCreate.invoke(controller);

    java.lang.reflect.Method hEdit =
        MyListingsController.class.getDeclaredMethod("handleEditListing");
    hEdit.setAccessible(true);
    hEdit.invoke(controller);

    // Test push bid update
    com.nhom1.auction.common.dto.notification.BidUpdateEvent eventObj =
        new com.nhom1.auction.common.dto.notification.BidUpdateEvent(
            "auc-1", BigDecimal.valueOf(25.00), (java.util.UUID) null);
    java.lang.reflect.Method hBidPush =
        MyListingsController.class.getDeclaredMethod(
            "handleBidUpdatePush", com.nhom1.auction.common.dto.notification.BidUpdateEvent.class);
    hBidPush.setAccessible(true);
    hBidPush.invoke(controller, eventObj);
  }

  @Test
  public void testPaymentController() throws Exception {
    PaymentController controller = new PaymentController();

    PaymentClientService mockPaymentService = mock(PaymentClientService.class);

    PendingPaymentDto pending =
        new PendingPaymentDto("auc-1", "PaidItem", "ART", BigDecimal.TEN, LocalDateTime.now());
    PendingPaymentsResponse ppr = new PendingPaymentsResponse(List.of(pending));

    PaymentHistoryEntryDto history =
        new PaymentHistoryEntryDto("auc-2", "HistItem", BigDecimal.ONE, "PAY", LocalDateTime.now());
    PaymentHistoryResponse phr = new PaymentHistoryResponse(List.of(history));

    when(mockPaymentService.listPendingPayments())
        .thenReturn(CompletableFuture.completedFuture(ppr));
    when(mockPaymentService.listPaymentHistory())
        .thenReturn(CompletableFuture.completedFuture(phr));

    Label lblPaymentStatus = new Label();
    VBox pendingPaymentsBox = new VBox();
    VBox historyBox = new VBox();

    injectField(controller, "paymentClientService", mockPaymentService);
    injectField(controller, "lblPaymentStatus", lblPaymentStatus);
    injectField(controller, "pendingPaymentsBox", pendingPaymentsBox);
    injectField(controller, "historyBox", historyBox);

    controller.initialize();
    waitForRunLater();

    verify(mockPaymentService).listPendingPayments();
    verify(mockPaymentService).listPaymentHistory();

    // Process payment mock
    ProcessPaymentResponse pprMock =
        new ProcessPaymentResponse("auc-1", BigDecimal.TEN, "SUCCESS", LocalDateTime.now());
    when(mockPaymentService.processPayment(eq("auc-1")))
        .thenReturn(CompletableFuture.completedFuture(pprMock));
    java.lang.reflect.Method processMethod =
        PaymentController.class.getDeclaredMethod("processPayment", String.class, Button.class);
    processMethod.setAccessible(true);
    processMethod.invoke(controller, "auc-1", new Button());
    waitForRunLater();
    verify(mockPaymentService).processPayment("auc-1");
  }

  @Test
  public void testCreateAuctionController() throws Exception {
    CreateAuctionController controller = new CreateAuctionController();

    CreateAuctionClientService mockCreateService = mock(CreateAuctionClientService.class);

    ComboBox<ItemCategory> categoryComboBox = new ComboBox<>();
    ComboBox<ItemCondition> conditionComboBox = new ComboBox<>();
    Label uploadCountLabel = new Label();
    Button duration1Btn = new Button();
    Button duration3Btn = new Button();
    Button duration7Btn = new Button();
    Button duration14Btn = new Button();
    Button duration30Btn = new Button();
    TextField customDurationField = new TextField();
    TextField titleField = new TextField("Super Art");
    TextArea descriptionArea = new TextArea("Nice painting");
    TextField startingBidField = new TextField("100");
    TextField reservePriceField = new TextField("150");
    DatePicker openingDatePicker = new DatePicker();
    openingDatePicker.setValue(java.time.LocalDate.now());

    injectField(controller, "createAuctionService", mockCreateService);
    injectField(controller, "categoryComboBox", categoryComboBox);
    injectField(controller, "conditionComboBox", conditionComboBox);
    injectField(controller, "uploadCountLabel", uploadCountLabel);
    injectField(controller, "duration1Btn", duration1Btn);
    injectField(controller, "duration3Btn", duration3Btn);
    injectField(controller, "duration7Btn", duration7Btn);
    injectField(controller, "duration14Btn", duration14Btn);
    injectField(controller, "duration30Btn", duration30Btn);
    injectField(controller, "customDurationField", customDurationField);
    injectField(controller, "titleField", titleField);
    injectField(controller, "descriptionArea", descriptionArea);
    injectField(controller, "startingBidField", startingBidField);
    injectField(controller, "openingDatePicker", openingDatePicker);

    java.lang.reflect.Method mInit = CreateAuctionController.class.getDeclaredMethod("initialize");
    mInit.setAccessible(true);
    mInit.invoke(controller);

    // Test handleDurationPreset
    ActionEvent event = new ActionEvent(duration3Btn, null);
    java.lang.reflect.Method preset =
        CreateAuctionController.class.getDeclaredMethod("handleDurationPreset", ActionEvent.class);
    preset.setAccessible(true);
    preset.invoke(controller, event);
    assertTrue(duration3Btn.getStyleClass().contains("duration-chip-active"));

    // Test handleBackToListings
    java.lang.reflect.Method back =
        CreateAuctionController.class.getDeclaredMethod("handleBackToListings");
    back.setAccessible(true);
    back.invoke(controller);

    // Test publish
    when(mockCreateService.validateInput(any(), any(), any(), any(), anyInt(), any()))
        .thenReturn(null);
    CreateAuctionResponse response = new CreateAuctionResponse();
    when(mockCreateService.createAuction(any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenReturn(CompletableFuture.completedFuture(response));

    java.lang.reflect.Method publish =
        CreateAuctionController.class.getDeclaredMethod("handlePublishListing");
    publish.setAccessible(true);
    publish.invoke(controller);
    waitForRunLater();

    verify(mockCreateService)
        .createAuction(any(), any(), any(), any(), any(), anyInt(), any(), any());
  }

  @Test
  public void testEditAuctionController() throws Exception {
    EditAuctionController controller = new EditAuctionController();

    ComboBox<ItemCategory> categoryComboBox = new ComboBox<>();
    ComboBox<ItemCondition> conditionComboBox = new ComboBox<>();
    Button duration1Btn = new Button();
    Button duration3Btn = new Button();
    Button duration7Btn = new Button();
    Button duration14Btn = new Button();
    Button duration30Btn = new Button();
    TextField customDurationField = new TextField();
    TextField titleField = new TextField();
    TextArea descriptionArea = new TextArea();
    Label statusLabel = new Label();
    Label metaLabel = new Label();
    Label statusSubLabel = new Label();
    TextField startingBidField = new TextField();
    Button saveChangesButton = new Button();

    injectField(controller, "categoryComboBox", categoryComboBox);
    injectField(controller, "conditionComboBox", conditionComboBox);
    injectField(controller, "duration1Btn", duration1Btn);
    injectField(controller, "duration3Btn", duration3Btn);
    injectField(controller, "duration7Btn", duration7Btn);
    injectField(controller, "duration14Btn", duration14Btn);
    injectField(controller, "duration30Btn", duration30Btn);
    injectField(controller, "customDurationField", customDurationField);
    injectField(controller, "titleField", titleField);
    injectField(controller, "descriptionArea", descriptionArea);
    injectField(controller, "statusLabel", statusLabel);
    injectField(controller, "metaLabel", metaLabel);
    injectField(controller, "statusSubLabel", statusSubLabel);
    injectField(controller, "startingBidField", startingBidField);
    injectField(controller, "saveChangesButton", saveChangesButton);

    java.lang.reflect.Method mInit = EditAuctionController.class.getDeclaredMethod("initialize");
    mInit.setAccessible(true);
    mInit.invoke(controller);

    ActionEvent event = new ActionEvent(duration7Btn, null);
    java.lang.reflect.Method preset =
        EditAuctionController.class.getDeclaredMethod("handleDurationPreset", ActionEvent.class);
    preset.setAccessible(true);
    preset.invoke(controller, event);

    java.lang.reflect.Method back =
        EditAuctionController.class.getDeclaredMethod("handleBackToListings");
    back.setAccessible(true);
    back.invoke(controller);
  }

  @Test
  public void testAuctionDetailController() throws Exception {
    AuctionDetailController controller = new AuctionDetailController();

    BiddingClientService mockBiddingService = mock(BiddingClientService.class);
    AppContext.setSelectedAuctionId("auc-123");

    BidSummaryDto bid =
        new BidSummaryDto(
            "bid-123",
            "john",
            BigDecimal.valueOf(1200),
            BidType.MANUAL,
            LocalDateTime.now(),
            "John Doe");
    AuctionDetailDto detail =
        new AuctionDetailDto(
            "auc-123",
            "item-123",
            "Awesome Car",
            "V8 engine",
            ItemCategory.ART,
            ItemCondition.USED,
            "user-2",
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(1200),
            "john",
            BigDecimal.valueOf(50),
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            List.of(bid));
    detail.setSellerName("Seller Bob");

    when(mockBiddingService.getAuctionDetail(eq("auc-123")))
        .thenReturn(CompletableFuture.completedFuture(detail));

    TextField txtBidInput = new TextField();
    Label lblBidError = new Label();
    Button btnBid = new Button();
    Label lblCurrentBid = new Label();
    Label lblMinIncrement = new Label();
    Button btnBack = new Button();
    VBox bidHistoryList = new VBox();
    Label lblTitle = new Label();
    Label lblShortDesc = new Label();
    Label lblSellerName = new Label();
    Label lblDescription = new Label();

    injectField(controller, "biddingService", mockBiddingService);
    injectField(controller, "txtBidInput", txtBidInput);
    injectField(controller, "lblBidError", lblBidError);
    injectField(controller, "btnBid", btnBid);
    injectField(controller, "lblCurrentBid", lblCurrentBid);
    injectField(controller, "lblMinIncrement", lblMinIncrement);
    injectField(controller, "btnBack", btnBack);
    injectField(controller, "bidHistoryList", bidHistoryList);
    injectField(controller, "lblTitle", lblTitle);
    injectField(controller, "lblShortDesc", lblShortDesc);
    injectField(controller, "lblSellerName", lblSellerName);
    injectField(controller, "lblDescription", lblDescription);

    controller.initialize();
    waitForRunLater();

    verify(mockBiddingService).getAuctionDetail("auc-123");
    assertEquals("Awesome Car", lblTitle.getText());

    // Test invalid bid input
    txtBidInput.setText("not-a-number");
    btnBid.getOnAction().handle(null);
    assertTrue(lblBidError.getText().contains("Invalid amount"));

    // Test valid bid input
    txtBidInput.setText("1300");
    PlaceBidResponse pbr = new PlaceBidResponse();
    when(mockBiddingService.placeBid(eq("auc-123"), eq(BigDecimal.valueOf(1300))))
        .thenReturn(CompletableFuture.completedFuture(pbr));

    btnBid.getOnAction().handle(null);
    waitForRunLater();
    verify(mockBiddingService).placeBid("auc-123", BigDecimal.valueOf(1300));
  }

  @Test
  public void testCardComponents() throws Exception {
    AuctionCardComponentController c1 = new AuctionCardComponentController();
    injectField(c1, "titleLabel", new Label());
    injectField(c1, "categoryLabel", new Label());
    injectField(c1, "statusBadgeLabel", new Label());
    injectField(c1, "priceValueLabel", new Label());
    injectField(c1, "timeLeftLabel", new Label());
    injectField(c1, "actionButton", new Button());

    c1.bind(new AuctionSummaryDto(), "Active", "$100", "3 days left", id -> {});
    assertNotNull(c1.getPriceValueLabel());

    BidCardComponentController c2 = new BidCardComponentController();
    injectField(c2, "titleLabel", new Label());
    injectField(c2, "yourBidLabel", new Label());
    injectField(c2, "statusBadgeLabel", new Label());
    injectField(c2, "currentBidLabel", new Label());
    injectField(c2, "timeLeftLabel", new Label());
    injectField(c2, "raiseBidButton", new Button());

    c2.bind(new BidWithAuctionDto(), "$100", "$150", "2 days left", id -> {});

    ListingCardComponentController c3 = new ListingCardComponentController();
    injectField(c3, "titleLabel", new Label());
    injectField(c3, "subLabel", new Label());
    injectField(c3, "statusLabel", new Label());
    injectField(c3, "priceLabel", new Label());
    injectField(c3, "remainingLabel", new Label());
    injectField(c3, "editButton", new Button());
    injectField(c3, "deleteButton", new Button());

    c3.bind(new AuctionSummaryDto(), "Active", "$200", "5 days left", false, () -> {}, () -> {});
    assertNotNull(c3.getPriceLabel());
  }

  @Test
  public void testMyListingsControllerDelete() throws Exception {
    MyListingsController controller = new MyListingsController();

    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    AuctionSummaryDto summary = new AuctionSummaryDto();
    summary.setId("auc-1");
    summary.setItemName("My Item");
    summary.setStatus(AuctionStatus.OPEN);

    MyListingsResponse mlr = new MyListingsResponse();
    mlr.setListings(List.of(summary));

    ResponseMessage<MyListingsResponse> respMsg = new ResponseMessage<>();
    respMsg.setSuccess(true);
    respMsg.setPayload(mlr);

    when(mockConnection.sendRequest(
            argThat(r -> r != null && r.getType() == MessageType.LIST_MY_LISTINGS),
            eq(MyListingsResponse.class)))
        .thenReturn(CompletableFuture.completedFuture(respMsg));

    ResponseMessage<String> deleteSuccess = new ResponseMessage<>();
    deleteSuccess.setSuccess(true);
    deleteSuccess.setPayload("Deleted");
    when(mockConnection.sendRequest(
            argThat(r -> r != null && r.getType() == MessageType.DELETE_AUCTION), eq(String.class)))
        .thenReturn(CompletableFuture.completedFuture(deleteSuccess));

    Label activeListingsLabel = new Label();
    GridPane listingsGrid = new GridPane();
    injectField(controller, "activeListingsLabel", activeListingsLabel);
    injectField(controller, "listingsGrid", listingsGrid);

    ObservableList<ButtonType> buttonTypes = FXCollections.observableArrayList();
    DialogPane mockDialogPane = mock(DialogPane.class);
    ObservableList<String> stylesheets = FXCollections.observableArrayList();
    when(mockDialogPane.getStylesheets()).thenReturn(stylesheets);
    Button mockYesButton = new Button();
    Button mockNoButton = new Button();
    when(mockDialogPane.lookupButton(any(ButtonType.class)))
        .thenAnswer(
            inv -> {
              ButtonType bt = inv.getArgument(0);
              if (bt != null && "Yes".equals(bt.getText())) {
                return mockYesButton;
              }
              return mockNoButton;
            });

    try (MockedConstruction<Alert> mockedAlert =
        mockConstruction(
            Alert.class,
            (mockAlert, context) -> {
              when(mockAlert.getButtonTypes()).thenReturn(buttonTypes);
              when(mockAlert.getDialogPane()).thenReturn(mockDialogPane);
              when(mockAlert.showAndWait())
                  .thenAnswer(
                      inv -> {
                        if (!buttonTypes.isEmpty()) {
                          return Optional.of(buttonTypes.get(0));
                        }
                        return Optional.empty();
                      });
            })) {
      java.lang.reflect.Method mInit = MyListingsController.class.getDeclaredMethod("initialize");
      mInit.setAccessible(true);
      mInit.invoke(controller);
      waitForRunLater();

      java.lang.reflect.Method mDelete =
          MyListingsController.class.getDeclaredMethod(
              "handleDeleteListing", AuctionSummaryDto.class);
      mDelete.setAccessible(true);
      mDelete.invoke(controller, summary);
      waitForRunLater();
    }

    verify(mockConnection)
        .sendRequest(
            argThat(r -> r != null && r.getType() == MessageType.DELETE_AUCTION), eq(String.class));
  }

  @Test
  public void testMyListingsControllerDeleteFailures() throws Exception {
    MyListingsController controller = new MyListingsController();

    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    AuctionSummaryDto summary = new AuctionSummaryDto();
    summary.setId("auc-1");

    ResponseMessage<String> deleteFail = new ResponseMessage<>();
    deleteFail.setSuccess(false);
    com.nhom1.auction.common.protocol.ErrorResponse err =
        new com.nhom1.auction.common.protocol.ErrorResponse();
    err.setMessage("Failed to delete");
    deleteFail.setError(err);
    when(mockConnection.sendRequest(
            argThat(r -> r != null && r.getType() == MessageType.DELETE_AUCTION), eq(String.class)))
        .thenReturn(CompletableFuture.completedFuture(deleteFail));

    Label activeListingsLabel = new Label();
    GridPane listingsGrid = new GridPane();
    injectField(controller, "activeListingsLabel", activeListingsLabel);
    injectField(controller, "listingsGrid", listingsGrid);

    ObservableList<ButtonType> buttonTypes = FXCollections.observableArrayList();
    DialogPane mockDialogPane = mock(DialogPane.class);
    ObservableList<String> stylesheets = FXCollections.observableArrayList();
    when(mockDialogPane.getStylesheets()).thenReturn(stylesheets);
    Button mockYesButton = new Button();
    Button mockNoButton = new Button();
    when(mockDialogPane.lookupButton(any(ButtonType.class)))
        .thenAnswer(
            inv -> {
              ButtonType bt = inv.getArgument(0);
              if (bt != null && "Yes".equals(bt.getText())) return mockYesButton;
              return mockNoButton;
            });

    try (MockedConstruction<Alert> mockedAlert =
        mockConstruction(
            Alert.class,
            (mockAlert, context) -> {
              when(mockAlert.getButtonTypes()).thenReturn(buttonTypes);
              when(mockAlert.getDialogPane()).thenReturn(mockDialogPane);
              when(mockAlert.showAndWait())
                  .thenAnswer(
                      inv -> {
                        if (!buttonTypes.isEmpty()) return Optional.of(buttonTypes.get(0));
                        return Optional.empty();
                      });
            })) {
      java.lang.reflect.Method mDelete =
          MyListingsController.class.getDeclaredMethod(
              "handleDeleteListing", AuctionSummaryDto.class);
      mDelete.setAccessible(true);
      mDelete.invoke(controller, summary);
      waitForRunLater();

      reset(mockConnection);
      when(mockConnection.sendRequest(
              argThat(r -> r != null && r.getType() == MessageType.DELETE_AUCTION),
              eq(String.class)))
          .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Network down")));
      mDelete.invoke(controller, summary);
      waitForRunLater();
    }
  }

  @Test
  public void testMyListingsControllerLoadErrors() throws Exception {
    MyListingsController controller = new MyListingsController();
    Label activeListingsLabel = new Label();
    GridPane listingsGrid = new GridPane();
    injectField(controller, "activeListingsLabel", activeListingsLabel);
    injectField(controller, "listingsGrid", listingsGrid);

    AppContext.clearSession();
    java.lang.reflect.Method mLoad = MyListingsController.class.getDeclaredMethod("loadMyListings");
    mLoad.setAccessible(true);
    mLoad.invoke(controller);
    waitForRunLater();
    assertEquals("0", activeListingsLabel.getText());

    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    ResponseMessage<MyListingsResponse> respFail = new ResponseMessage<>();
    respFail.setSuccess(false);
    com.nhom1.auction.common.protocol.ErrorResponse err =
        new com.nhom1.auction.common.protocol.ErrorResponse();
    err.setMessage("Failed load");
    respFail.setError(err);

    when(mockConnection.sendRequest(any(RequestMessage.class), eq(MyListingsResponse.class)))
        .thenReturn(CompletableFuture.completedFuture(respFail));
    mLoad.invoke(controller);
    waitForRunLater();
    assertEquals("0", activeListingsLabel.getText());

    when(mockConnection.sendRequest(any(RequestMessage.class), eq(MyListingsResponse.class)))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Timeout")));
    mLoad.invoke(controller);
    waitForRunLater();
    assertEquals("0", activeListingsLabel.getText());
  }

  @Test
  public void testCreateAuctionControllerFlows() throws Exception {
    CreateAuctionController controller = new CreateAuctionController();
    CreateAuctionClientService mockCreateService = mock(CreateAuctionClientService.class);
    injectField(controller, "createAuctionService", mockCreateService);

    ComboBox<ItemCategory> categoryComboBox = new ComboBox<>();
    ComboBox<ItemCondition> conditionComboBox = new ComboBox<>();
    Label uploadCountLabel = new Label();
    Button duration1Btn = new Button();
    Button duration3Btn = new Button();
    Button duration7Btn = new Button();
    Button duration14Btn = new Button();
    Button duration30Btn = new Button();
    TextField customDurationField = new TextField();
    TextField titleField = new TextField();
    TextArea descriptionArea = new TextArea();
    TextField startingBidField = new TextField();
    TextField reservePriceField = new TextField();
    DatePicker openingDatePicker = new DatePicker();
    openingDatePicker.setValue(java.time.LocalDate.now());

    injectField(controller, "categoryComboBox", categoryComboBox);
    injectField(controller, "conditionComboBox", conditionComboBox);
    injectField(controller, "uploadCountLabel", uploadCountLabel);
    injectField(controller, "duration1Btn", duration1Btn);
    injectField(controller, "duration3Btn", duration3Btn);
    injectField(controller, "duration7Btn", duration7Btn);
    injectField(controller, "duration14Btn", duration14Btn);
    injectField(controller, "duration30Btn", duration30Btn);
    injectField(controller, "customDurationField", customDurationField);
    injectField(controller, "titleField", titleField);
    injectField(controller, "descriptionArea", descriptionArea);
    injectField(controller, "startingBidField", startingBidField);
    injectField(controller, "openingDatePicker", openingDatePicker);

    java.lang.reflect.Method mInit = CreateAuctionController.class.getDeclaredMethod("initialize");
    mInit.setAccessible(true);
    mInit.invoke(controller);

    duration3Btn.getStyleClass().add("duration-chip-active");
    customDurationField.setText("10");
    assertFalse(duration3Btn.getStyleClass().contains("duration-chip-active"));

    java.util.concurrent.atomic.AtomicInteger callCount =
        new java.util.concurrent.atomic.AtomicInteger(0);
    try (MockedConstruction<FileChooser> mockedFC =
        mockConstruction(
            FileChooser.class,
            (mockFC, context) -> {
              ObservableList<FileChooser.ExtensionFilter> filters =
                  FXCollections.observableArrayList();
              when(mockFC.getExtensionFilters()).thenReturn(filters);
              when(mockFC.showOpenMultipleDialog(any()))
                  .thenAnswer(
                      inv -> {
                        int count = callCount.getAndIncrement();
                        if (count == 0) {
                          return null;
                        } else if (count == 1) {
                          return List.of(new File("image1.png"));
                        } else {
                          return List.of(
                              new File("image1.png"),
                              new File("image2.png"),
                              new File("image3.png"));
                        }
                      });
            })) {
      java.lang.reflect.Method mChoose =
          CreateAuctionController.class.getDeclaredMethod("handleChoosePhotos");
      mChoose.setAccessible(true);
      mChoose.invoke(controller);
      assertEquals("No photo selected", uploadCountLabel.getText());

      mChoose.invoke(controller);
      assertTrue(uploadCountLabel.getText().contains("1 photo"));

      mChoose.invoke(controller);
      assertTrue(
          uploadCountLabel.getText().contains("3 photo")
              && uploadCountLabel.getText().contains("+1 more"));
    }

    customDurationField.setText("not-a-number");
    java.lang.reflect.Method mResolveDuration =
        CreateAuctionController.class.getDeclaredMethod("resolveDurationDays");
    mResolveDuration.setAccessible(true);
    int d1 = (int) mResolveDuration.invoke(controller);
    assertEquals(-1, d1);

    customDurationField.setText("");
    duration1Btn.getStyleClass().add("duration-chip-active");
    assertEquals(1, mResolveDuration.invoke(controller));
    duration1Btn.getStyleClass().remove("duration-chip-active");

    duration14Btn.getStyleClass().add("duration-chip-active");
    assertEquals(14, mResolveDuration.invoke(controller));
    duration14Btn.getStyleClass().remove("duration-chip-active");

    duration30Btn.getStyleClass().add("duration-chip-active");
    assertEquals(30, mResolveDuration.invoke(controller));
    duration30Btn.getStyleClass().remove("duration-chip-active");

    assertEquals(7, mResolveDuration.invoke(controller));

    when(mockCreateService.validateInput(any(), any(), any(), any(), anyInt(), any()))
        .thenReturn("Invalid title");
    java.lang.reflect.Method mPublish =
        CreateAuctionController.class.getDeclaredMethod("handlePublishListing");
    mPublish.setAccessible(true);
    mPublish.invoke(controller);
    assertEquals("Invalid title", uploadCountLabel.getText());

    when(mockCreateService.validateInput(any(), any(), any(), any(), anyInt(), any()))
        .thenReturn(null);
    customDurationField.setText("0");
    mPublish.invoke(controller);
    assertEquals("Duration must be greater than 0.", uploadCountLabel.getText());

    customDurationField.setText("5");
    when(mockCreateService.createAuction(any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("IMGBB_API_KEY missing")));
    mPublish.invoke(controller);
    waitForRunLater();
    assertTrue(uploadCountLabel.getText().contains("IMGBB_API_KEY"));

    when(mockCreateService.createAuction(any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenReturn(
            CompletableFuture.failedFuture(
                new com.nhom1.auction.common.exception.ValidationException(
                    "Custom validation failed")));
    mPublish.invoke(controller);
    waitForRunLater();
    assertEquals("Custom validation failed", uploadCountLabel.getText());
  }

  @Test
  public void testAuctionDetailControllerFlows() throws Exception {
    AuctionDetailController controller = new AuctionDetailController();
    BiddingClientService mockBiddingService = mock(BiddingClientService.class);
    injectField(controller, "biddingService", mockBiddingService);

    AppContext.setSelectedAuctionId("auc-123");
    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    user.setUsername("john");
    AppContext.setCurrentUser(user);

    AuctionDetailDto ownDetail =
        new AuctionDetailDto(
            "auc-123",
            "item-123",
            "Awesome Car",
            "V8 engine",
            ItemCategory.ART,
            ItemCondition.USED,
            "user-1",
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(1200),
            "john",
            BigDecimal.valueOf(50),
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            Collections.emptyList());
    ownDetail.setImageUrls(List.of("http://dummyimage.com/img.png"));

    when(mockBiddingService.getAuctionDetail(eq("auc-123")))
        .thenReturn(CompletableFuture.completedFuture(ownDetail));

    TextField txtBidInput = new TextField();
    Label lblBidError = new Label();
    Button btnBid = new Button();
    Label lblCurrentBid = new Label();
    Label lblMinIncrement = new Label();
    Button btnBack = new Button();
    VBox bidHistoryList = new VBox();
    Label lblTitle = new Label();
    ImageView itemImageView = new ImageView();

    injectField(controller, "txtBidInput", txtBidInput);
    injectField(controller, "lblBidError", lblBidError);
    injectField(controller, "btnBid", btnBid);
    injectField(controller, "lblCurrentBid", lblCurrentBid);
    injectField(controller, "lblMinIncrement", lblMinIncrement);
    injectField(controller, "btnBack", btnBack);
    injectField(controller, "bidHistoryList", bidHistoryList);
    injectField(controller, "lblTitle", lblTitle);
    injectField(controller, "itemImageView", itemImageView);

    SimpleDoubleProperty progressProperty = new SimpleDoubleProperty(0.0);
    SimpleBooleanProperty errorProperty = new SimpleBooleanProperty(false);
    try (MockedConstruction<Image> mockedImage =
        mockConstruction(
            Image.class,
            (mockImg, context) -> {
              when(mockImg.progressProperty()).thenReturn(progressProperty);
              when(mockImg.errorProperty()).thenReturn(errorProperty);
              when(mockImg.getWidth()).thenReturn(420.0);
              when(mockImg.getHeight()).thenReturn(320.0);
            })) {
      controller.initialize();
      waitForRunLater();

      progressProperty.set(1.0);
      waitForRunLater();

      errorProperty.set(true);
      waitForRunLater();

      assertTrue(btnBid.isDisable());
      assertEquals("You cannot bid on your own auction.", lblBidError.getText());

      List<BidSummaryDto> bids = new java.util.ArrayList<>();
      for (int i = 0; i < 12; i++) {
        bids.add(
            new BidSummaryDto(
                "b-" + i,
                "bidder",
                BigDecimal.valueOf(1100 + i * 10),
                BidType.MANUAL,
                LocalDateTime.now(),
                "Bidder"));
      }

      AuctionDetailDto ownDetail2 =
          new AuctionDetailDto(
              "auc-123",
              "item-123",
              "Awesome Car",
              "V8 engine",
              ItemCategory.ART,
              ItemCondition.USED,
              "user-2",
              BigDecimal.valueOf(1000),
              BigDecimal.valueOf(1200),
              "john",
              BigDecimal.valueOf(50),
              AuctionStatus.OPEN,
              LocalDateTime.now(),
              LocalDateTime.now().plusDays(1),
              bids);
      ownDetail2.setImageUrls(List.of("http://dummyimage.com/img.png"));

      reset(mockBiddingService);
      when(mockBiddingService.getAuctionDetail(eq("auc-123")))
          .thenReturn(CompletableFuture.completedFuture(ownDetail2));

      progressProperty.set(0.0);
      controller.initialize();
      waitForRunLater();

      progressProperty.set(1.0);
      waitForRunLater();

      assertFalse(btnBid.isDisable());
      assertFalse(lblBidError.isVisible());

      com.nhom1.auction.common.dto.notification.BidUpdateEvent pushEvent =
          new com.nhom1.auction.common.dto.notification.BidUpdateEvent(
              "auc-123", new BigDecimal("1500"), java.util.UUID.randomUUID());
      java.lang.reflect.Method mPush =
          AuctionDetailController.class.getDeclaredMethod(
              "handleBidUpdatePush",
              com.nhom1.auction.common.dto.notification.BidUpdateEvent.class);
      mPush.setAccessible(true);
      mPush.invoke(controller, pushEvent);
      waitForRunLater();

      assertEquals("$1,500", lblCurrentBid.getText());

      txtBidInput.setText("");
      java.lang.reflect.Method mPlace =
          AuctionDetailController.class.getDeclaredMethod("onPlaceBid");
      mPlace.setAccessible(true);
      mPlace.invoke(controller);
      assertEquals("Please enter a bid amount.", lblBidError.getText());

      txtBidInput.setText("2000");
      when(mockBiddingService.placeBid(any(), any()))
          .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Amount too low")));
      mPlace.invoke(controller);
      waitForRunLater();
      assertEquals("Amount too low", lblBidError.getText());
    }
  }

  @Test
  public void testAuctionBrowseControllerEdgeCases() throws Exception {
    AuctionBrowseController controller = new AuctionBrowseController();

    BiddingClientService mockBiddingService = mock(BiddingClientService.class);
    ListAuctionsResponse lar = new ListAuctionsResponse();
    lar.setAuctions(Collections.emptyList());
    when(mockBiddingService.listAuctions()).thenReturn(CompletableFuture.completedFuture(lar));
    when(mockBiddingService.getMyBids())
        .thenReturn(CompletableFuture.completedFuture(new MyBidsResponse(Collections.emptyList())));
    injectField(controller, "biddingService", mockBiddingService);

    // 1. AppContext.getCurrentUser() is null
    AppContext.clearSession();
    Label welcomeLabel = new Label();
    injectField(controller, "welcomeLabel", welcomeLabel);
    controller.initialize();
    assertEquals("", welcomeLabel.getText());

    // 2. AppContext.getCurrentUser().getUsername() is blank or null
    AuthResponse userBlank = new AuthResponse();
    userBlank.setUserID("user-1");
    userBlank.setUsername("   ");
    AppContext.setCurrentUser(userBlank);
    welcomeLabel.setText("default");
    controller.initialize();
    assertEquals("default", welcomeLabel.getText()); // Shouldn't change welcome label

    // 3. formatStatus, formatMoney, resolveDisplayCurrentBid, formatTimeLeft reflection tests
    java.lang.reflect.Method formatStatus =
        AuctionBrowseController.class.getDeclaredMethod("formatStatus", Object.class);
    formatStatus.setAccessible(true);
    assertEquals("Unknown", formatStatus.invoke(controller, (Object) null));
    assertEquals("Open", formatStatus.invoke(controller, AuctionStatus.OPEN));
    assertEquals("Ended", formatStatus.invoke(controller, AuctionStatus.FINISHED));
    assertEquals("Ended", formatStatus.invoke(controller, AuctionStatus.PAID));
    assertEquals("Hello", formatStatus.invoke(controller, "Hello"));

    java.lang.reflect.Method formatMoney =
        AuctionBrowseController.class.getDeclaredMethod("formatMoney", BigDecimal.class);
    formatMoney.setAccessible(true);
    assertEquals("$0", formatMoney.invoke(controller, (BigDecimal) null));
    assertEquals("$12,345.67", formatMoney.invoke(controller, new BigDecimal("12345.67")));

    java.lang.reflect.Method resolveDisplayCurrentBid =
        AuctionBrowseController.class.getDeclaredMethod(
            "resolveDisplayCurrentBid", AuctionSummaryDto.class);
    resolveDisplayCurrentBid.setAccessible(true);
    assertEquals(
        BigDecimal.ZERO, resolveDisplayCurrentBid.invoke(controller, (AuctionSummaryDto) null));

    AuctionSummaryDto dto = new AuctionSummaryDto();
    dto.setCurrentHighestBid(BigDecimal.TEN);
    assertEquals(BigDecimal.TEN, resolveDisplayCurrentBid.invoke(controller, dto));

    dto.setCurrentHighestBid(BigDecimal.ZERO);
    dto.setStartingPrice(BigDecimal.ONE);
    assertEquals(BigDecimal.ONE, resolveDisplayCurrentBid.invoke(controller, dto));

    dto.setStartingPrice(null);
    assertEquals(BigDecimal.ZERO, resolveDisplayCurrentBid.invoke(controller, dto));

    java.lang.reflect.Method formatTimeLeft =
        AuctionBrowseController.class.getDeclaredMethod("formatTimeLeft", LocalDateTime.class);
    formatTimeLeft.setAccessible(true);
    assertEquals("N/A", formatTimeLeft.invoke(controller, (LocalDateTime) null));
    assertEquals("Ended", formatTimeLeft.invoke(controller, LocalDateTime.now().minusMinutes(5)));
    assertEquals("Ended", formatTimeLeft.invoke(controller, LocalDateTime.now()));
    assertEquals(
        "1 day", formatTimeLeft.invoke(controller, LocalDateTime.now().plusDays(1).plusSeconds(1)));
    assertEquals(
        "2 days",
        formatTimeLeft.invoke(controller, LocalDateTime.now().plusDays(2).plusSeconds(1)));
    assertEquals(
        "1 hour",
        formatTimeLeft.invoke(controller, LocalDateTime.now().plusHours(1).plusSeconds(1)));
    assertEquals(
        "2 hours",
        formatTimeLeft.invoke(controller, LocalDateTime.now().plusHours(2).plusSeconds(1)));
    assertEquals(
        "1 min",
        formatTimeLeft.invoke(controller, LocalDateTime.now().plusMinutes(1).plusSeconds(1)));
    assertEquals(
        "2 mins",
        formatTimeLeft.invoke(controller, LocalDateTime.now().plusMinutes(2).plusSeconds(1)));

    // 4. navigateToDetail edge case where view is already AUCTION_DETAIL
    java.lang.reflect.Field cvField = AppNavigator.class.getDeclaredField("currentView");
    cvField.setAccessible(true);
    cvField.set(null, AppView.AUCTION_DETAIL);
    controller.navigateToDetail("auc-1"); // Should return early, not trigger navigation
    cvField.set(null, null);

    // 5. handleBidUpdatePush JSON variations
    java.lang.reflect.Method handleBidUpdatePush =
        AuctionBrowseController.class.getDeclaredMethod("handleBidUpdatePush", String.class);
    handleBidUpdatePush.setAccessible(true);
    handleBidUpdatePush.invoke(controller, "{invalid json}");
    handleBidUpdatePush.invoke(controller, "{}");
    handleBidUpdatePush.invoke(controller, "{\"auctionId\": \"auc-1\"}");
    handleBidUpdatePush.invoke(controller, "{\"newHighestBid\": \"100\"}");
    handleBidUpdatePush.invoke(
        controller, "{\"auctionId\": \"auc-1\", \"newHighestBid\": \"100\"}");

    // 6. handleAuctionDeletedPush JSON variations
    java.lang.reflect.Method handleAuctionDeletedPush =
        AuctionBrowseController.class.getDeclaredMethod("handleAuctionDeletedPush", String.class);
    handleAuctionDeletedPush.setAccessible(true);
    handleAuctionDeletedPush.invoke(controller, "{invalid json}");
    handleAuctionDeletedPush.invoke(controller, "{}");
    handleAuctionDeletedPush.invoke(controller, "{\"auctionId\": \"auc-1\"}");
  }

  @Test
  public void testAuctionBrowseControllerLoadAuctionsVariations() throws Exception {
    AuctionBrowseController controller = new AuctionBrowseController();

    BiddingClientService mockBiddingService = mock(BiddingClientService.class);
    injectField(controller, "biddingService", mockBiddingService);

    // Case 1: listAuctions fails
    when(mockBiddingService.listAuctions())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API error")));
    when(mockBiddingService.getMyBids())
        .thenReturn(CompletableFuture.completedFuture(new MyBidsResponse(Collections.emptyList())));

    java.lang.reflect.Method loadAuctions =
        AuctionBrowseController.class.getDeclaredMethod("loadAuctions");
    loadAuctions.setAccessible(true);
    loadAuctions.invoke(controller);
    waitForRunLater();

    // Case 2: listAuctions returns null response, myBids is null
    reset(mockBiddingService);
    when(mockBiddingService.listAuctions()).thenReturn(CompletableFuture.completedFuture(null));
    when(mockBiddingService.getMyBids()).thenReturn(CompletableFuture.completedFuture(null));
    loadAuctions.invoke(controller);
    waitForRunLater();

    // Case 3: listAuctions has null auctions, myBids has null bids list
    reset(mockBiddingService);
    ListAuctionsResponse lar = new ListAuctionsResponse();
    lar.setAuctions(null);
    MyBidsResponse mbr = new MyBidsResponse();
    mbr.setBids(null);
    when(mockBiddingService.listAuctions()).thenReturn(CompletableFuture.completedFuture(lar));
    when(mockBiddingService.getMyBids()).thenReturn(CompletableFuture.completedFuture(mbr));
    loadAuctions.invoke(controller);
    waitForRunLater();
  }

  @Test
  public void testAuctionDetailControllerEdgeCases() throws Exception {
    AuctionDetailController controller = new AuctionDetailController();

    // 1. sel is null or blank -> navigates to AUCTION_BROWSE
    AppContext.setSelectedAuctionId(null);
    controller.initialize();
    assertEquals(AppView.AUCTION_BROWSE, AppNavigator.getCurrentView());

    AppContext.setSelectedAuctionId("   ");
    controller.initialize();
    assertEquals(AppView.AUCTION_BROWSE, AppNavigator.getCurrentView());

    // Restore context
    AppContext.setSelectedAuctionId("auc-123");

    // 2. formatMoney, renderBidHistory, applyCoverViewport tests
    assertEquals("$0", com.nhom1.auction.client.util.DisplayFormatters.money((BigDecimal) null));
    assertEquals(
        "$150", com.nhom1.auction.client.util.DisplayFormatters.money(new BigDecimal("150")));

    // renderBidHistory empty or null
    VBox bidHistoryList = new VBox();
    injectField(controller, "bidHistoryList", bidHistoryList);
    java.lang.reflect.Method renderBidHistory =
        AuctionDetailController.class.getDeclaredMethod("renderBidHistory", List.class);
    renderBidHistory.setAccessible(true);
    renderBidHistory.invoke(controller, (List) null);
    assertTrue(bidHistoryList.getChildren().isEmpty());
    renderBidHistory.invoke(controller, Collections.emptyList());
    assertTrue(bidHistoryList.getChildren().isEmpty());

    // renderBidHistory details validation (null bidder name, null bid type, null createdAt)
    BidSummaryDto incompleteBid = new BidSummaryDto(null, null, BigDecimal.TEN, null, null, null);
    renderBidHistory.invoke(controller, List.of(incompleteBid));
    assertEquals(1, bidHistoryList.getChildren().size());

    // applyCoverViewport logic with invalid values
    ImageView itemImageView = new ImageView();
    injectField(controller, "itemImageView", itemImageView);
    Image mockImage = mock(Image.class);
    when(mockImage.getWidth()).thenReturn(0.0);
    when(mockImage.getHeight()).thenReturn(100.0);
    java.lang.reflect.Method applyCoverViewport =
        AuctionDetailController.class.getDeclaredMethod("applyCoverViewport", Image.class);
    applyCoverViewport.setAccessible(true);
    applyCoverViewport.invoke(controller, mockImage);
    assertNull(itemImageView.getViewport());

    // applyCoverViewport where sourceRatio > targetRatio
    when(mockImage.getWidth()).thenReturn(1000.0);
    when(mockImage.getHeight()).thenReturn(500.0);
    applyCoverViewport.invoke(controller, mockImage);
    assertNotNull(itemImageView.getViewport());

    // applyCoverViewport where sourceRatio <= targetRatio
    when(mockImage.getWidth()).thenReturn(200.0);
    when(mockImage.getHeight()).thenReturn(500.0);
    applyCoverViewport.invoke(controller, mockImage);
    assertNotNull(itemImageView.getViewport());

    // 3. onPlaceBid edge cases
    BiddingClientService mockBiddingService = mock(BiddingClientService.class);
    when(mockBiddingService.getAuctionDetail(anyString()))
        .thenReturn(CompletableFuture.completedFuture(new AuctionDetailDto()));
    injectField(controller, "biddingService", mockBiddingService);

    TextField txtBidInput = new TextField();
    Label lblBidError = new Label();
    injectField(controller, "txtBidInput", txtBidInput);
    injectField(controller, "lblBidError", lblBidError);

    // auctionId is null
    AppContext.setSelectedAuctionId(null);
    java.lang.reflect.Method onPlaceBid =
        AuctionDetailController.class.getDeclaredMethod("onPlaceBid");
    onPlaceBid.setAccessible(true);
    onPlaceBid.invoke(controller);

    // restore selected auction
    AppContext.setSelectedAuctionId("auc-123");

    // txtBidInput text is null
    txtBidInput.setText(null);
    onPlaceBid.invoke(controller);
    assertEquals("Please enter a bid amount.", lblBidError.getText());

    // txtBidInput text is blank
    txtBidInput.setText("   ");
    onPlaceBid.invoke(controller);
    assertEquals("Please enter a bid amount.", lblBidError.getText());

    // txtBidInput has invalid text format
    txtBidInput.setText("123.45.67");
    onPlaceBid.invoke(controller);
    assertTrue(lblBidError.getText().contains("Invalid amount"));

    // handlePlaceBidSuccess null check
    java.lang.reflect.Method handlePlaceBidSuccess =
        AuctionDetailController.class.getDeclaredMethod(
            "handlePlaceBidSuccess", PlaceBidResponse.class);
    handlePlaceBidSuccess.setAccessible(true);
    txtBidInput.setText("dirty text");
    handlePlaceBidSuccess.invoke(controller, (PlaceBidResponse) null);
    assertEquals("dirty text", txtBidInput.getText()); // Should not clear if response is null

    // 4. applyDetail variations
    AuthResponse currentUser = new AuthResponse();
    currentUser.setUserID("user-1");
    AppContext.setCurrentUser(currentUser);

    AuctionDetailDto dto =
        new AuctionDetailDto(
            "auc-123", // auctionId
            "item-123", // itemID
            null, // itemName
            null, // itemDescription
            null, // itemCategory
            null, // itemCondition
            "user-2", // sellerID
            null, // startingPrice
            null, // currentHighestBid
            null, // currentHighestBidderId
            null, // minBidIncrement
            null, // status
            null, // startTime
            null, // endTime
            null // bidHistory
            );
    dto.setSellerName(null);
    dto.setImageUrls(null);

    Label lblTitle = new Label();
    Label lblShortDesc = new Label();
    Label lblDescription = new Label();
    Label lblSellerName = new Label();
    Label lblCurrentBid = new Label();
    Label lblMinIncrement = new Label();
    injectField(controller, "lblTitle", lblTitle);
    injectField(controller, "lblShortDesc", lblShortDesc);
    injectField(controller, "lblDescription", lblDescription);
    injectField(controller, "lblSellerName", lblSellerName);
    injectField(controller, "lblCurrentBid", lblCurrentBid);
    injectField(controller, "lblMinIncrement", lblMinIncrement);

    java.lang.reflect.Method applyDetail =
        AuctionDetailController.class.getDeclaredMethod("applyDetail", AuctionDetailDto.class);
    applyDetail.setAccessible(true);
    applyDetail.invoke(controller, (AuctionDetailDto) null); // should not crash

    applyDetail.invoke(controller, dto);
    assertEquals("", lblTitle.getText());
    assertEquals("Unknown", lblSellerName.getText());
    assertEquals("$0", lblCurrentBid.getText());

    // image list with blank element
    dto.setImageUrls(List.of("   "));
    applyDetail.invoke(controller, dto);

    // 5. handleBidUpdatePush typed variations
    java.lang.reflect.Method handleBidUpdatePush =
        AuctionDetailController.class.getDeclaredMethod(
            "handleBidUpdatePush", com.nhom1.auction.common.dto.notification.BidUpdateEvent.class);
    handleBidUpdatePush.setAccessible(true);
    handleBidUpdatePush.invoke(
        controller, (com.nhom1.auction.common.dto.notification.BidUpdateEvent) null);
    handleBidUpdatePush.invoke(
        controller,
        new com.nhom1.auction.common.dto.notification.BidUpdateEvent(
            null, BigDecimal.TEN, (java.util.UUID) null));
    handleBidUpdatePush.invoke(
        controller,
        new com.nhom1.auction.common.dto.notification.BidUpdateEvent(
            "different-auc", BigDecimal.TEN, (java.util.UUID) null));
    handleBidUpdatePush.invoke(
        controller,
        new com.nhom1.auction.common.dto.notification.BidUpdateEvent(
            "auc-123", null, (java.util.UUID) null));

    // when biddingService.getAuctionDetail fails during push refresh
    reset(mockBiddingService);
    when(mockBiddingService.getAuctionDetail(anyString()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Refresh error")));
    handleBidUpdatePush.invoke(
        controller,
        new com.nhom1.auction.common.dto.notification.BidUpdateEvent(
            "auc-123", BigDecimal.valueOf(200), (java.util.UUID) null));
    waitForRunLater();
  }

  @Test
  public void testMyListingsControllerEdgeCases() throws Exception {
    MyListingsController controller = new MyListingsController();

    // 1. formatStatus, formatMoney, resolveDisplayCurrentBid, formatTimeLeft tests
    assertEquals(
        "Unknown",
        com.nhom1.auction.client.util.DisplayFormatters.auctionStatusLabel((AuctionStatus) null));
    assertEquals(
        "Open",
        com.nhom1.auction.client.util.DisplayFormatters.auctionStatusLabel(AuctionStatus.OPEN));
    assertEquals(
        "Ended",
        com.nhom1.auction.client.util.DisplayFormatters.auctionStatusLabel(AuctionStatus.FINISHED));

    assertEquals("$0", com.nhom1.auction.client.util.DisplayFormatters.money((BigDecimal) null));
    assertEquals(
        "$120", com.nhom1.auction.client.util.DisplayFormatters.money(new BigDecimal("120.00")));

    java.lang.reflect.Method resolveDisplayCurrentBid =
        MyListingsController.class.getDeclaredMethod(
            "resolveDisplayCurrentBid", AuctionSummaryDto.class);
    resolveDisplayCurrentBid.setAccessible(true);
    assertEquals(
        BigDecimal.ZERO, resolveDisplayCurrentBid.invoke(controller, (AuctionSummaryDto) null));

    AuctionSummaryDto dto = new AuctionSummaryDto();
    dto.setCurrentHighestBid(BigDecimal.valueOf(-1));
    dto.setStartingPrice(BigDecimal.valueOf(10));
    assertEquals(BigDecimal.valueOf(10), resolveDisplayCurrentBid.invoke(controller, dto));

    dto.setCurrentHighestBid(null);
    dto.setStartingPrice(null);
    assertEquals(BigDecimal.ZERO, resolveDisplayCurrentBid.invoke(controller, dto));

    assertEquals(
        "N/A", com.nhom1.auction.client.util.DisplayFormatters.timeLeft((LocalDateTime) null));
    assertEquals(
        "Ended",
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().minusSeconds(1)));
    assertEquals(
        "Ended", com.nhom1.auction.client.util.DisplayFormatters.timeLeft(LocalDateTime.now()));
    assertEquals(
        "2 days left",
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().plusDays(2).plusSeconds(5)));
    assertEquals(
        "2 hours left",
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().plusHours(2).plusSeconds(5)));
    assertEquals(
        "2 min left",
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().plusMinutes(2).plusSeconds(5)));

    // 2. loadMyListings variations
    Label activeListingsLabel = new Label();
    GridPane listingsGrid = new GridPane();
    injectField(controller, "activeListingsLabel", activeListingsLabel);
    injectField(controller, "listingsGrid", listingsGrid);

    // User is null
    AppContext.clearSession();
    java.lang.reflect.Method loadMyListings =
        MyListingsController.class.getDeclaredMethod("loadMyListings");
    loadMyListings.setAccessible(true);
    loadMyListings.invoke(controller);
    waitForRunLater();
    assertEquals("0", activeListingsLabel.getText());

    // User ID is blank
    AuthResponse userBlank = new AuthResponse();
    userBlank.setUserID("   ");
    AppContext.setCurrentUser(userBlank);
    loadMyListings.invoke(controller);
    waitForRunLater();
    assertEquals("0", activeListingsLabel.getText());

    // Restore user
    AuthResponse userValid = new AuthResponse();
    userValid.setUserID("user-1");
    AppContext.setCurrentUser(userValid);

    // Response is success, but listings is empty
    ResponseMessage<MyListingsResponse> respSuccessEmpty = new ResponseMessage<>();
    respSuccessEmpty.setSuccess(true);
    respSuccessEmpty.setPayload(new MyListingsResponse(Collections.emptyList()));
    when(mockConnection.sendRequest(any(RequestMessage.class), eq(MyListingsResponse.class)))
        .thenReturn(CompletableFuture.completedFuture(respSuccessEmpty));
    loadMyListings.invoke(controller);
    waitForRunLater();
    assertEquals("0", activeListingsLabel.getText());

    // Response is null
    reset(mockConnection);
    when(mockConnection.sendRequest(any(RequestMessage.class), eq(MyListingsResponse.class)))
        .thenReturn(CompletableFuture.completedFuture(null));
    loadMyListings.invoke(controller);
    waitForRunLater();
    assertEquals("0", activeListingsLabel.getText());

    // Response is failure with no error object
    reset(mockConnection);
    ResponseMessage<MyListingsResponse> respFail = new ResponseMessage<>();
    respFail.setSuccess(false);
    respFail.setError(null);
    when(mockConnection.sendRequest(any(RequestMessage.class), eq(MyListingsResponse.class)))
        .thenReturn(CompletableFuture.completedFuture(respFail));
    loadMyListings.invoke(controller);
    waitForRunLater();
    assertEquals("0", activeListingsLabel.getText());

    // 3. handleBidUpdatePush JSON variations
    java.lang.reflect.Method handleBidUpdatePush =
        MyListingsController.class.getDeclaredMethod(
            "handleBidUpdatePush", com.nhom1.auction.common.dto.notification.BidUpdateEvent.class);
    handleBidUpdatePush.setAccessible(true);
    handleBidUpdatePush.invoke(
        controller, (com.nhom1.auction.common.dto.notification.BidUpdateEvent) null);
    handleBidUpdatePush.invoke(
        controller,
        new com.nhom1.auction.common.dto.notification.BidUpdateEvent(
            null, BigDecimal.TEN, (java.util.UUID) null));
    handleBidUpdatePush.invoke(
        controller,
        new com.nhom1.auction.common.dto.notification.BidUpdateEvent(
            "auc-1", null, (java.util.UUID) null));
    handleBidUpdatePush.invoke(
        controller,
        new com.nhom1.auction.common.dto.notification.BidUpdateEvent(
            "auc-1", BigDecimal.valueOf(150), (java.util.UUID) null));

    // 4. handleDeleteListing confirmation NO option clicked
    ObservableList<ButtonType> buttonTypes = FXCollections.observableArrayList();
    DialogPane mockDialogPane = mock(DialogPane.class);
    ObservableList<String> stylesheets = FXCollections.observableArrayList();
    when(mockDialogPane.getStylesheets()).thenReturn(stylesheets);
    Button mockYesButton = new Button();
    Button mockNoButton = new Button();
    when(mockDialogPane.lookupButton(any(ButtonType.class)))
        .thenAnswer(
            inv -> {
              ButtonType bt = inv.getArgument(0);
              if (bt != null && "Yes".equals(bt.getText())) return mockYesButton;
              return mockNoButton;
            });

    try (MockedConstruction<Alert> mockedAlert =
        mockConstruction(
            Alert.class,
            (mockAlert, context) -> {
              when(mockAlert.getButtonTypes()).thenReturn(buttonTypes);
              when(mockAlert.getDialogPane()).thenReturn(mockDialogPane);
              when(mockAlert.showAndWait())
                  .thenAnswer(
                      inv -> {
                        // Return 'No' (which is the second button in the list)
                        if (buttonTypes.size() > 1) {
                          return Optional.of(buttonTypes.get(1));
                        }
                        return Optional.empty();
                      });
            })) {
      java.lang.reflect.Method handleDeleteListing =
          MyListingsController.class.getDeclaredMethod(
              "handleDeleteListing", AuctionSummaryDto.class);
      handleDeleteListing.setAccessible(true);
      handleDeleteListing.invoke(controller, dto);
    }

    // 5. handleDeleteListing with session expired or missing
    try (MockedConstruction<Alert> mockedAlert =
        mockConstruction(
            Alert.class,
            (mockAlert, context) -> {
              when(mockAlert.getButtonTypes()).thenReturn(buttonTypes);
              when(mockAlert.getDialogPane()).thenReturn(mockDialogPane);
              when(mockAlert.showAndWait())
                  .thenAnswer(
                      inv -> {
                        if (!buttonTypes.isEmpty()) return Optional.of(buttonTypes.get(0));
                        return Optional.empty();
                      });
            })) {
      AppContext.clearSession();
      java.lang.reflect.Method handleDeleteListing =
          MyListingsController.class.getDeclaredMethod(
              "handleDeleteListing", AuctionSummaryDto.class);
      handleDeleteListing.setAccessible(true);
      handleDeleteListing.invoke(controller, dto);

      // User ID is blank
      userBlank.setUserID("   ");
      AppContext.setCurrentUser(userBlank);
      handleDeleteListing.invoke(controller, dto);
    }
  }

  @Test
  public void testPaymentControllerEdgeCases() throws Exception {
    PaymentController controller = new PaymentController();
    PaymentClientService mockPaymentService = mock(PaymentClientService.class);
    injectField(controller, "paymentClientService", mockPaymentService);

    Label lblPaymentStatus = new Label();
    VBox pendingPaymentsBox = new VBox();
    VBox historyBox = new VBox();
    injectField(controller, "lblPaymentStatus", lblPaymentStatus);
    injectField(controller, "pendingPaymentsBox", pendingPaymentsBox);
    injectField(controller, "historyBox", historyBox);

    // 1. reload with various nulls and empties in responses
    PendingPaymentsResponse pprNull = new PendingPaymentsResponse(null);
    PaymentHistoryResponse phrNull = new PaymentHistoryResponse(null);
    when(mockPaymentService.listPendingPayments())
        .thenReturn(CompletableFuture.completedFuture(pprNull));
    when(mockPaymentService.listPaymentHistory())
        .thenReturn(CompletableFuture.completedFuture(phrNull));

    java.lang.reflect.Method reload = PaymentController.class.getDeclaredMethod("reload");
    reload.setAccessible(true);
    reload.invoke(controller);
    waitForRunLater();
    assertTrue(lblPaymentStatus.getText().contains("0 pending payment(s)"));

    // reload exceptional path
    reset(mockPaymentService);
    when(mockPaymentService.listPendingPayments())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Service offline")));
    when(mockPaymentService.listPaymentHistory())
        .thenReturn(CompletableFuture.completedFuture(phrNull));
    reload.invoke(controller);
    waitForRunLater();
    assertTrue(lblPaymentStatus.getText().contains("Service offline"));

    // 2. Row styling and null scenarios (paidAt is null, amount is null, direction is not RECEIVE)
    PendingPaymentDto pending = new PendingPaymentDto("auc-1", "PaidItem", "ART", null, null);
    PaymentHistoryEntryDto history =
        new PaymentHistoryEntryDto("auc-2", "HistItem", null, "PAY", null);
    PendingPaymentsResponse ppr = new PendingPaymentsResponse(List.of(pending));
    PaymentHistoryResponse phr = new PaymentHistoryResponse(List.of(history));

    reset(mockPaymentService);
    when(mockPaymentService.listPendingPayments())
        .thenReturn(CompletableFuture.completedFuture(ppr));
    when(mockPaymentService.listPaymentHistory())
        .thenReturn(CompletableFuture.completedFuture(phr));
    reload.invoke(controller);
    waitForRunLater();
    assertTrue(lblPaymentStatus.getText().contains("1 pending payment(s)"));

    // historyEntries size == 1 (singular) vs other (plural)
    assertTrue(lblPaymentStatus.getText().contains("1 history entry"));

    // 3. processPayment failure
    reset(mockPaymentService);
    when(mockPaymentService.processPayment(anyString()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Insufficient funds")));
    java.lang.reflect.Method processPayment =
        PaymentController.class.getDeclaredMethod("processPayment", String.class, Button.class);
    processPayment.setAccessible(true);
    Button payButton = new Button();
    processPayment.invoke(controller, "auc-1", payButton);
    waitForRunLater();
    assertTrue(lblPaymentStatus.getText().contains("Insufficient funds"));
    assertFalse(payButton.isDisabled());
  }

  @Test
  public void testCreateAuctionControllerEdgeCases() throws Exception {
    CreateAuctionController controller = new CreateAuctionController();
    CreateAuctionClientService mockCreateService = mock(CreateAuctionClientService.class);
    injectField(controller, "createAuctionService", mockCreateService);

    ComboBox<ItemCategory> categoryComboBox = new ComboBox<>();
    ComboBox<ItemCondition> conditionComboBox = new ComboBox<>();
    Label uploadCountLabel = new Label();
    Button duration1Btn = new Button();
    Button duration3Btn = new Button();
    Button duration7Btn = new Button();
    Button duration14Btn = new Button();
    Button duration30Btn = new Button();
    TextField customDurationField = new TextField();
    TextField titleField = new TextField("Super Art");
    TextArea descriptionArea = new TextArea("Nice painting");
    TextField startingBidField = new TextField("100");
    TextField reservePriceField = new TextField("150");
    DatePicker openingDatePicker = new DatePicker();
    openingDatePicker.setValue(java.time.LocalDate.now());

    injectField(controller, "categoryComboBox", categoryComboBox);
    injectField(controller, "conditionComboBox", conditionComboBox);
    injectField(controller, "uploadCountLabel", uploadCountLabel);
    injectField(controller, "duration1Btn", duration1Btn);
    injectField(controller, "duration3Btn", duration3Btn);
    injectField(controller, "duration7Btn", duration7Btn);
    injectField(controller, "duration14Btn", duration14Btn);
    injectField(controller, "duration30Btn", duration30Btn);
    injectField(controller, "customDurationField", customDurationField);
    injectField(controller, "titleField", titleField);
    injectField(controller, "descriptionArea", descriptionArea);
    injectField(controller, "startingBidField", startingBidField);
    injectField(controller, "openingDatePicker", openingDatePicker);

    java.lang.reflect.Method mInit = CreateAuctionController.class.getDeclaredMethod("initialize");
    mInit.setAccessible(true);
    mInit.invoke(controller);

    // handleDurationPreset edge case when source is not Button
    java.lang.reflect.Method handleDurationPreset =
        CreateAuctionController.class.getDeclaredMethod("handleDurationPreset", ActionEvent.class);
    handleDurationPreset.setAccessible(true);
    handleDurationPreset.invoke(controller, new ActionEvent(new Object(), null));

    // handlePublishListing when createAuction returns null response
    when(mockCreateService.validateInput(any(), any(), any(), any(), anyInt(), any()))
        .thenReturn(null);
    when(mockCreateService.createAuction(any(), any(), any(), any(), any(), anyInt(), any(), any()))
        .thenReturn(CompletableFuture.completedFuture(null));
    java.lang.reflect.Method handlePublishListing =
        CreateAuctionController.class.getDeclaredMethod("handlePublishListing");
    handlePublishListing.setAccessible(true);
    handlePublishListing.invoke(controller);
    waitForRunLater();
    assertEquals("Failed to publish listing.", uploadCountLabel.getText());

    // resolveErrorMessage scenarios
    java.lang.reflect.Method resolveErrorMessage =
        CreateAuctionController.class.getDeclaredMethod("resolveErrorMessage", Throwable.class);
    resolveErrorMessage.setAccessible(true);

    // AppException with null message
    String msg1 =
        (String)
            resolveErrorMessage.invoke(
                controller,
                new com.nhom1.auction.common.exception.AppException("ERR_CODE", null) {});
    assertEquals("Connection error.", msg1);

    // Normal Exception with blank message
    String msg2 = (String) resolveErrorMessage.invoke(controller, new RuntimeException("  "));
    assertEquals("Connection error.", msg2);

    // Normal Exception with arbitrary message
    String msg3 =
        (String) resolveErrorMessage.invoke(controller, new RuntimeException("Server error"));
    assertEquals("Server error", msg3);
  }

  @Test
  public void testEditAuctionControllerEdgeCases() throws Exception {
    EditAuctionController controller = new EditAuctionController();
    ComboBox<ItemCategory> categoryComboBox = new ComboBox<>();
    ComboBox<ItemCondition> conditionComboBox = new ComboBox<>();
    Button duration1Btn = new Button();
    Button duration3Btn = new Button();
    Button duration7Btn = new Button();
    Button duration14Btn = new Button();
    Button duration30Btn = new Button();
    TextField customDurationField = new TextField();
    TextField titleField = new TextField();
    TextArea descriptionArea = new TextArea();
    Label statusLabel = new Label();
    Label metaLabel = new Label();
    Label statusSubLabel = new Label();
    TextField startingBidField = new TextField();
    Button saveChangesButton = new Button();

    injectField(controller, "categoryComboBox", categoryComboBox);
    injectField(controller, "conditionComboBox", conditionComboBox);
    injectField(controller, "duration1Btn", duration1Btn);
    injectField(controller, "duration3Btn", duration3Btn);
    injectField(controller, "duration7Btn", duration7Btn);
    injectField(controller, "duration14Btn", duration14Btn);
    injectField(controller, "duration30Btn", duration30Btn);
    injectField(controller, "customDurationField", customDurationField);
    injectField(controller, "titleField", titleField);
    injectField(controller, "descriptionArea", descriptionArea);
    injectField(controller, "statusLabel", statusLabel);
    injectField(controller, "metaLabel", metaLabel);
    injectField(controller, "statusSubLabel", statusSubLabel);
    injectField(controller, "startingBidField", startingBidField);
    injectField(controller, "saveChangesButton", saveChangesButton);

    java.lang.reflect.Method mInit = EditAuctionController.class.getDeclaredMethod("initialize");
    mInit.setAccessible(true);
    mInit.invoke(controller);

    // trigger listener with null text
    customDurationField.setText(null);

    // trigger handleDurationPreset with non-Button source
    java.lang.reflect.Method handleDurationPreset =
        EditAuctionController.class.getDeclaredMethod("handleDurationPreset", ActionEvent.class);
    handleDurationPreset.setAccessible(true);
    handleDurationPreset.invoke(controller, new ActionEvent(new Object(), null));
  }

  @Test
  public void testMyBidsControllerEdgeCases() throws Exception {
    MyBidsController controller = new MyBidsController();
    BiddingClientService mockBiddingService = mock(BiddingClientService.class);
    injectField(controller, "biddingService", mockBiddingService);

    GridPane cardsGridPane = new GridPane();
    injectField(controller, "cardsGridPane", cardsGridPane);

    // Case 1: successful loading with 5 bids to hit all formatTimeLeft branches
    MyBidsResponse mbr = new MyBidsResponse();
    BidWithAuctionDto bid1 =
        new BidWithAuctionDto(
            "auc-1",
            "Car",
            BigDecimal.valueOf(100),
            BigDecimal.valueOf(200),
            AuctionStatus.OPEN,
            LocalDateTime.now().plusDays(2),
            true);
    BidWithAuctionDto bid2 =
        new BidWithAuctionDto(
            "auc-2",
            "Bike",
            BigDecimal.valueOf(50),
            BigDecimal.valueOf(80),
            AuctionStatus.OPEN,
            LocalDateTime.now().minusDays(1),
            false);
    BidWithAuctionDto bid3 =
        new BidWithAuctionDto(
            "auc-3",
            "Watch",
            BigDecimal.valueOf(10),
            BigDecimal.valueOf(15),
            AuctionStatus.OPEN,
            LocalDateTime.now().plusHours(3),
            true);
    BidWithAuctionDto bid4 =
        new BidWithAuctionDto(
            "auc-4",
            "Book",
            BigDecimal.valueOf(5),
            BigDecimal.valueOf(6),
            AuctionStatus.OPEN,
            LocalDateTime.now().plusMinutes(10),
            false);
    BidWithAuctionDto bid5 =
        new BidWithAuctionDto(
            "auc-5",
            "Phone",
            BigDecimal.valueOf(500),
            BigDecimal.valueOf(600),
            AuctionStatus.OPEN,
            null,
            false);
    mbr.setBids(List.of(bid1, bid2, bid3, bid4, bid5));

    when(mockBiddingService.getMyBids()).thenReturn(CompletableFuture.completedFuture(mbr));

    controller.initialize();
    waitForRunLater();

    assertEquals(5, cardsGridPane.getChildren().size());

    // Test navigateToDetail from bid card callback
    java.lang.reflect.Method mNav =
        MyBidsController.class.getDeclaredMethod("navigateToDetail", String.class);
    mNav.setAccessible(true);

    // 1. null id
    mNav.invoke(controller, (String) null);

    // 2. non-null id when current view is AUCTION_DETAIL
    java.lang.reflect.Field cvField = AppNavigator.class.getDeclaredField("currentView");
    cvField.setAccessible(true);
    cvField.set(null, AppView.AUCTION_DETAIL);
    mNav.invoke(controller, "auc-1");
    assertEquals("auc-1", AppContext.getSelectedAuctionId());

    // 3. non-null id when current view is not AUCTION_DETAIL
    cvField.set(null, AppView.AUCTION_BROWSE);
    mNav.invoke(controller, "auc-2");
    assertEquals("auc-2", AppContext.getSelectedAuctionId());

    // Restore context
    cvField.set(null, null);

    // Case 2: failed loading (exceptionally path)
    reset(mockBiddingService);
    when(mockBiddingService.getMyBids())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API error")));
    controller.initialize();
    waitForRunLater();

    // Case 3: null/empty bids response
    reset(mockBiddingService);
    when(mockBiddingService.getMyBids()).thenReturn(CompletableFuture.completedFuture(null));
    controller.initialize();
    waitForRunLater();

    MyBidsResponse emptyMbr = new MyBidsResponse();
    emptyMbr.setBids(null);
    when(mockBiddingService.getMyBids()).thenReturn(CompletableFuture.completedFuture(emptyMbr));
    controller.initialize();
    waitForRunLater();
  }

  @Test
  public void testBidCardComponentController() throws Exception {
    BidCardComponentController controller = new BidCardComponentController();

    Label titleLabel = new Label();
    Label yourBidLabel = new Label();
    Label statusBadgeLabel = new Label();
    Label currentBidLabel = new Label();
    Label timeLeftLabel = new Label();
    Button raiseBidButton = new Button();

    injectField(controller, "titleLabel", titleLabel);
    injectField(controller, "yourBidLabel", yourBidLabel);
    injectField(controller, "statusBadgeLabel", statusBadgeLabel);
    injectField(controller, "currentBidLabel", currentBidLabel);
    injectField(controller, "timeLeftLabel", timeLeftLabel);
    injectField(controller, "raiseBidButton", raiseBidButton);

    // Case 1: Winning, ItemName is null
    BidWithAuctionDto bid1 =
        new BidWithAuctionDto(
            "auc-1",
            null,
            BigDecimal.TEN,
            BigDecimal.TEN,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            true);
    java.util.concurrent.atomic.AtomicReference<String> clickedId =
        new java.util.concurrent.atomic.AtomicReference<>();
    controller.bind(bid1, "$10", "$10", "2 days", clickedId::set);

    assertEquals("Unknown item", titleLabel.getText());
    assertEquals("Winning", statusBadgeLabel.getText());
    raiseBidButton.fire();
    assertEquals("auc-1", clickedId.get());

    // Case 2: Outbid, ItemName is non-null
    BidWithAuctionDto bid2 =
        new BidWithAuctionDto(
            "auc-2",
            "Art Piece",
            BigDecimal.TEN,
            BigDecimal.TEN,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            false);
    controller.bind(bid2, "$10", "$12", "Ended", clickedId::set);
    assertEquals("Art Piece", titleLabel.getText());
    assertEquals("Outbid", statusBadgeLabel.getText());
    raiseBidButton.fire();
    assertEquals("auc-2", clickedId.get());
  }

  @Test
  public void testAuctionCardComponentController() throws Exception {
    AuctionCardComponentController controller = new AuctionCardComponentController();

    Label titleLabel = new Label();
    Label categoryLabel = new Label();
    Label statusBadgeLabel = new Label();
    Label priceValueLabel = new Label();
    Label timeLeftLabel = new Label();
    Button actionButton = new Button();

    injectField(controller, "titleLabel", titleLabel);
    injectField(controller, "categoryLabel", categoryLabel);
    injectField(controller, "statusBadgeLabel", statusBadgeLabel);
    injectField(controller, "priceValueLabel", priceValueLabel);
    injectField(controller, "timeLeftLabel", timeLeftLabel);
    injectField(controller, "actionButton", actionButton);

    // Case 1: ItemName / Category not null
    AuctionSummaryDto dto1 = new AuctionSummaryDto();
    dto1.setId("auc-1");
    dto1.setItemName("Golden Car");
    dto1.setItemCategory("VEHICLES");
    java.util.concurrent.atomic.AtomicReference<String> clickedId =
        new java.util.concurrent.atomic.AtomicReference<>();
    controller.bind(dto1, "Running", "$100", "5h", clickedId::set);

    assertEquals("Golden Car", titleLabel.getText());
    assertEquals("VEHICLES", categoryLabel.getText());
    assertEquals(priceValueLabel, controller.getPriceValueLabel());
    actionButton.fire();
    assertEquals("auc-1", clickedId.get());

    // Case 2: ItemName / Category null
    AuctionSummaryDto dto2 = new AuctionSummaryDto();
    dto2.setId("auc-2");
    dto2.setItemName(null);
    dto2.setItemCategory(null);
    controller.bind(dto2, "Ended", "$0", "Ended", clickedId::set);
    assertEquals("Untitled", titleLabel.getText());
    assertEquals("Uncategorized", categoryLabel.getText());
  }

  @Test
  public void testListingCardComponentController() throws Exception {
    ListingCardComponentController controller = new ListingCardComponentController();

    Label titleLabel = new Label();
    Label subLabel = new Label();
    Label statusLabel = new Label();
    Label priceLabel = new Label();
    Label remainingLabel = new Label();
    Button editButton = new Button();
    Button deleteButton = new Button();

    injectField(controller, "titleLabel", titleLabel);
    injectField(controller, "subLabel", subLabel);
    injectField(controller, "statusLabel", statusLabel);
    injectField(controller, "priceLabel", priceLabel);
    injectField(controller, "remainingLabel", remainingLabel);
    injectField(controller, "editButton", editButton);
    injectField(controller, "deleteButton", deleteButton);

    // Case 1: ItemName is not null, ended is false
    AuctionSummaryDto dto1 = new AuctionSummaryDto();
    dto1.setItemName("Modern painting");
    java.util.concurrent.atomic.AtomicBoolean editClicked =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    java.util.concurrent.atomic.AtomicBoolean deleteClicked =
        new java.util.concurrent.atomic.AtomicBoolean(false);
    controller.bind(
        dto1,
        "Running",
        "$1,000",
        "3 days left",
        false,
        () -> editClicked.set(true),
        () -> deleteClicked.set(true));

    assertEquals("Modern painting", titleLabel.getText());
    assertFalse(editButton.isDisabled());
    editButton.fire();
    assertTrue(editClicked.get());
    deleteButton.fire();
    assertTrue(deleteClicked.get());
    assertEquals(priceLabel, controller.getPriceLabel());

    // Case 2: ItemName is null, ended is true
    AuctionSummaryDto dto2 = new AuctionSummaryDto();
    dto2.setItemName(null);
    statusLabel
        .getStyleClass()
        .add("status-badge-ended"); // trigger coverage flow of style class checks
    controller.bind(dto2, "Ended", "$1,000", "Ended", true, () -> {}, () -> {});
    assertEquals("Untitled listing", titleLabel.getText());
    assertTrue(editButton.isDisabled());
    assertNull(editButton.getOnAction());
  }

  @Test
  public void testSignInControllerShowErrorEdgeCases() throws Exception {
    SignInController controller = new SignInController();
    java.lang.reflect.Method mShowError =
        SignInController.class.getDeclaredMethod("showError", String.class, String.class);
    mShowError.setAccessible(true);

    java.lang.reflect.Field fAlertStage = SignInController.class.getDeclaredField("alertStage");
    fAlertStage.setAccessible(true);

    java.util.concurrent.atomic.AtomicReference<Stage> alertStageRef =
        new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicReference<Button> closeBtnRef =
        new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicReference<Label> titleRef =
        new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicReference<Label> msgRef =
        new java.util.concurrent.atomic.AtomicReference<>();

    Platform.runLater(
        () -> {
          try {
            try (MockedConstruction<FXMLLoader> mockLoader =
                mockConstruction(
                    FXMLLoader.class,
                    (mock, context) -> {
                      when(mock.load())
                          .thenAnswer(
                              inv -> {
                                VBox r = new VBox();
                                Label lblTitle = new Label();
                                lblTitle.setId("lblTitle");
                                Label lblMessage = new Label();
                                lblMessage.setId("lblMessage");
                                Button btnClose = new Button();
                                btnClose.setId("btnClose");
                                r.getChildren().addAll(lblTitle, lblMessage, btnClose);

                                closeBtnRef.set(btnClose);
                                titleRef.set(lblTitle);
                                msgRef.set(lblMessage);
                                return r;
                              });
                    })) {
              mShowError.invoke(controller, "Test Error", "Test Message");

              Stage alertStage = (Stage) fAlertStage.get(controller);
              alertStageRef.set(alertStage);

              mShowError.invoke(controller, "Test Error 2", "Test Message 2");

              // Test the branch: alertStage is not null but is not showing
              Stage dummyStage = new Stage();
              fAlertStage.set(controller, dummyStage);

              mShowError.invoke(controller, "Test Error 3", "Test Message 3");
              Stage alertStage3 = (Stage) fAlertStage.get(controller);
              assertNotNull(alertStage3);
              assertTrue(dummyStage != alertStage3);
              alertStageRef.set(alertStage3);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
    waitForRunLater();

    Stage alertStage = alertStageRef.get();
    assertNotNull(alertStage);
    assertEquals("Test Error 3", titleRef.get().getText());
    assertEquals("Test Message 3", msgRef.get().getText());
    assertTrue(alertStage.isShowing());

    Platform.runLater(
        () -> {
          closeBtnRef.get().fire();
          try {
            Stage stage = (Stage) fAlertStage.get(controller);
            if (stage != null && stage.getOnHidden() != null) {
              stage.getOnHidden().handle(null);
            }
          } catch (Exception e) {
          }
        });
    waitForRunLater();

    assertNull(fAlertStage.get(controller));
  }

  @Test
  public void testSignInControllerShowErrorFXMLLoadFailure() throws Exception {
    SignInController controller = new SignInController();
    try (MockedConstruction<FXMLLoader> mockLoader =
        mockConstruction(
            FXMLLoader.class,
            (mock, context) -> {
              when(mock.load()).thenThrow(new IOException("Simulated load error"));
            })) {
      java.lang.reflect.Method mShowError =
          SignInController.class.getDeclaredMethod("showError", String.class, String.class);
      mShowError.setAccessible(true);
      mShowError.invoke(controller, "Test Error", "Test Message");

      java.lang.reflect.Field fAlertStage = SignInController.class.getDeclaredField("alertStage");
      fAlertStage.setAccessible(true);
      assertNull(fAlertStage.get(controller));
    }
  }

  @Test
  public void testRegisterControllerShowErrorEdgeCases() throws Exception {
    RegisterController controller = new RegisterController();
    java.lang.reflect.Method mShowError =
        RegisterController.class.getDeclaredMethod("showError", String.class, String.class);
    mShowError.setAccessible(true);

    java.lang.reflect.Field fAlertStage = RegisterController.class.getDeclaredField("alertStage");
    fAlertStage.setAccessible(true);

    java.util.concurrent.atomic.AtomicReference<Stage> alertStageRef =
        new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicReference<Button> closeBtnRef =
        new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicReference<Label> titleRef =
        new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicReference<Label> msgRef =
        new java.util.concurrent.atomic.AtomicReference<>();

    Platform.runLater(
        () -> {
          try {
            try (MockedConstruction<FXMLLoader> mockLoader =
                mockConstruction(
                    FXMLLoader.class,
                    (mock, context) -> {
                      when(mock.load())
                          .thenAnswer(
                              inv -> {
                                VBox r = new VBox();
                                Label lblTitle = new Label();
                                lblTitle.setId("lblTitle");
                                Label lblMessage = new Label();
                                lblMessage.setId("lblMessage");
                                Button btnClose = new Button();
                                btnClose.setId("btnClose");
                                r.getChildren().addAll(lblTitle, lblMessage, btnClose);

                                closeBtnRef.set(btnClose);
                                titleRef.set(lblTitle);
                                msgRef.set(lblMessage);
                                return r;
                              });
                    })) {
              mShowError.invoke(controller, "Reg Error", "Reg Message");

              Stage alertStage = (Stage) fAlertStage.get(controller);
              alertStageRef.set(alertStage);

              mShowError.invoke(controller, "Reg Error 2", "Reg Message 2");

              // Test the branch: alertStage is not null but is not showing
              Stage dummyStage = new Stage();
              fAlertStage.set(controller, dummyStage);

              mShowError.invoke(controller, "Reg Error 3", "Reg Message 3");
              Stage alertStage3 = (Stage) fAlertStage.get(controller);
              assertNotNull(alertStage3);
              assertTrue(dummyStage != alertStage3);
              alertStageRef.set(alertStage3);
            }
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
    waitForRunLater();

    Stage alertStage = alertStageRef.get();
    assertNotNull(alertStage);
    assertEquals("Reg Error 3", titleRef.get().getText());
    assertEquals("Reg Message 3", msgRef.get().getText());
    assertTrue(alertStage.isShowing());

    Platform.runLater(
        () -> {
          closeBtnRef.get().fire();
          try {
            Stage stage = (Stage) fAlertStage.get(controller);
            if (stage != null && stage.getOnHidden() != null) {
              stage.getOnHidden().handle(null);
            }
          } catch (Exception e) {
          }
        });
    waitForRunLater();

    assertNull(fAlertStage.get(controller));
  }

  @Test
  public void testRegisterControllerShowErrorFXMLLoadFailure() throws Exception {
    RegisterController controller = new RegisterController();
    try (MockedConstruction<FXMLLoader> mockLoader =
        mockConstruction(
            FXMLLoader.class,
            (mock, context) -> {
              when(mock.load()).thenThrow(new IOException("Simulated load error"));
            })) {
      java.lang.reflect.Method mShowError =
          RegisterController.class.getDeclaredMethod("showError", String.class, String.class);
      mShowError.setAccessible(true);
      mShowError.invoke(controller, "Reg Error", "Reg Message");

      java.lang.reflect.Field fAlertStage = RegisterController.class.getDeclaredField("alertStage");
      fAlertStage.setAccessible(true);
      assertNull(fAlertStage.get(controller));
    }
  }

  @Test
  public void testRegisterControllerNullEmail() throws Exception {
    RegisterController controller = new RegisterController();
    AuthClientService mockAuthService = mock(AuthClientService.class);
    when(mockAuthService.register(anyString(), anyString(), anyString(), anyString()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Error")));

    Button btnRegister = new Button();
    Button btnSignIn = new Button();
    TextField txtUsername = new TextField("user");
    PasswordField txtPassword = new PasswordField();
    txtPassword.setText("pass");
    PasswordField txtRepeatPassword = new PasswordField();
    txtRepeatPassword.setText("pass");

    injectField(controller, "authService", mockAuthService);
    injectField(controller, "btnRegister", btnRegister);
    injectField(controller, "btnSignIn", btnSignIn);
    injectField(controller, "txtUsername", txtUsername);
    injectField(controller, "txtEmail", null);
    injectField(controller, "txtPassword", txtPassword);
    injectField(controller, "txtRepeatPassword", txtRepeatPassword);

    controller.initialize();
    btnRegister.getOnAction().handle(null);
    waitForRunLater();

    verify(mockAuthService).register("user", "", "pass", "pass");
  }

  @Test
  public void testMyBidsControllerTimeFormatting() throws Exception {
    String d1 =
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().plusDays(1).plusSeconds(10));
    assertEquals("1 days left", d1);

    String h1 =
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().plusHours(1).plusSeconds(10));
    assertEquals("1 hours left", h1);

    String m1 =
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().plusMinutes(1).plusSeconds(10));
    assertEquals("1 min left", m1);
  }

  @Test
  public void testMyBidsControllerCreateBidCardFailure() throws Exception {
    MyBidsController controller = new MyBidsController();
    BidWithAuctionDto bid =
        new BidWithAuctionDto(
            "auc-1",
            "Item",
            BigDecimal.TEN,
            BigDecimal.TEN,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            true);

    try (MockedConstruction<FXMLLoader> mockLoader =
        mockConstruction(
            FXMLLoader.class,
            (mock, context) -> {
              when(mock.load()).thenThrow(new IOException("Simulated load failure"));
            })) {
      java.lang.reflect.Method mCreate =
          MyBidsController.class.getDeclaredMethod("createBidCard", BidWithAuctionDto.class);
      mCreate.setAccessible(true);

      try {
        mCreate.invoke(controller, bid);
        fail("Should have thrown an exception");
      } catch (java.lang.reflect.InvocationTargetException e) {
        assertTrue(e.getCause() instanceof RuntimeException);
        assertTrue(e.getCause().getMessage().contains("Failed to load bid card component"));
      }
    }
  }

  @Test
  public void testAuctionBrowseControllerCreateAuctionCardFailure() throws Exception {
    AuctionBrowseController controller = new AuctionBrowseController();
    AuctionSummaryDto dto = new AuctionSummaryDto();
    dto.setId("auc-1");

    try (MockedConstruction<FXMLLoader> mockLoader =
        mockConstruction(
            FXMLLoader.class,
            (mock, context) -> {
              when(mock.load()).thenThrow(new IOException("Simulated load failure"));
            })) {
      java.lang.reflect.Method mCreate =
          AuctionBrowseController.class.getDeclaredMethod(
              "createAuctionCard", AuctionSummaryDto.class);
      mCreate.setAccessible(true);

      try {
        mCreate.invoke(controller, dto);
        fail("Should have thrown an exception");
      } catch (java.lang.reflect.InvocationTargetException e) {
        assertTrue(e.getCause() instanceof RuntimeException);
        assertTrue(e.getCause().getMessage().contains("Failed to load auction card component"));
      }
    }
  }

  @Test
  public void testAuctionBrowseControllerNavigateToDetailTransition() throws Exception {
    AuctionBrowseController controller = new AuctionBrowseController();

    java.lang.reflect.Field cvField = AppNavigator.class.getDeclaredField("currentView");
    cvField.setAccessible(true);
    cvField.set(null, AppView.SIGN_IN);

    controller.navigateToDetail("auc-1");
    assertEquals(AppNavigator.getCurrentView(), AppView.LOADING);

    Thread.sleep(500);
    waitForRunLater();

    assertEquals(AppNavigator.getCurrentView(), AppView.AUCTION_DETAIL);
    cvField.set(null, null);
  }

  @Test
  public void testEditAuctionControllerDurationPresetAlreadyActive() throws Exception {
    EditAuctionController controller = new EditAuctionController();

    Button duration1Btn = new Button();
    Button duration3Btn = new Button();
    Button duration7Btn = new Button();
    Button duration14Btn = new Button();
    Button duration30Btn = new Button();
    TextField customDurationField = new TextField();

    injectField(controller, "duration1Btn", duration1Btn);
    injectField(controller, "duration3Btn", duration3Btn);
    injectField(controller, "duration7Btn", duration7Btn);
    injectField(controller, "duration14Btn", duration14Btn);
    injectField(controller, "duration30Btn", duration30Btn);
    injectField(controller, "customDurationField", customDurationField);

    Button foreignButton = new Button();
    foreignButton.getStyleClass().add("duration-chip-active");

    ActionEvent event = new ActionEvent(foreignButton, null);
    java.lang.reflect.Method preset =
        EditAuctionController.class.getDeclaredMethod("handleDurationPreset", ActionEvent.class);
    preset.setAccessible(true);

    preset.invoke(controller, event);
    assertTrue(foreignButton.getStyleClass().contains("duration-chip-active"));
  }

  @Test
  public void testAuctionBrowseControllerPushHandlers() throws Exception {
    AuctionBrowseController controller = new AuctionBrowseController();

    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    user.setUsername("alice");
    AppContext.setCurrentUser(user);

    BiddingClientService mockBiddingService = mock(BiddingClientService.class);

    AuctionSummaryDto auction = new AuctionSummaryDto();
    auction.setId("auc-1");
    auction.setItemName("Cool Painting");
    auction.setStartingPrice(BigDecimal.TEN);
    auction.setCurrentHighestBid(BigDecimal.ZERO);
    auction.setStatus(AuctionStatus.OPEN);
    auction.setSellerId("user-2");

    ListAuctionsResponse ar = new ListAuctionsResponse();
    ar.setAuctions(List.of(auction));

    when(mockBiddingService.listAuctions()).thenReturn(CompletableFuture.completedFuture(ar));
    when(mockBiddingService.getMyBids())
        .thenReturn(CompletableFuture.completedFuture(new MyBidsResponse(Collections.emptyList())));

    Label welcomeLabel = new Label();
    HBox mainContainer = new HBox();
    GridPane cardsGridPane = new GridPane();

    injectField(controller, "biddingService", mockBiddingService);
    injectField(controller, "welcomeLabel", welcomeLabel);
    injectField(controller, "mainContainer", mainContainer);
    injectField(controller, "cardsGridPane", cardsGridPane);

    controller.initialize();
    waitForRunLater();

    // Verify push handlers were registered
    assertTrue(registeredPushHandlers.containsKey(MessageType.PUSH_NEW_AUCTION));
    assertTrue(registeredPushHandlers.containsKey(MessageType.PUSH_BID_UPDATE));
    assertTrue(registeredPushHandlers.containsKey(MessageType.PUSH_AUCTION_DELETED));

    // 1. Trigger PUSH_NEW_AUCTION
    reset(mockBiddingService);
    when(mockBiddingService.listAuctions()).thenReturn(CompletableFuture.completedFuture(ar));
    when(mockBiddingService.getMyBids())
        .thenReturn(CompletableFuture.completedFuture(new MyBidsResponse(Collections.emptyList())));
    registeredPushHandlers.get(MessageType.PUSH_NEW_AUCTION).accept("{}");
    waitForRunLater();
    waitForRunLater();
    verify(mockBiddingService).listAuctions();

    // 2. Trigger PUSH_BID_UPDATE
    // Set up the price labels map manually so we can verify the text changes
    Label priceLabel = new Label("$10");
    java.util.Map<String, Label> priceLabels = new java.util.HashMap<>();
    priceLabels.put("auc-1", priceLabel);
    injectField(controller, "priceLabels", priceLabels);

    registeredPushHandlers
        .get(MessageType.PUSH_BID_UPDATE)
        .accept("{\"payload\":{\"auctionId\":\"auc-1\",\"newHighestBid\":\"25.00\"}}");
    waitForRunLater();
    waitForRunLater();
    assertEquals("$25", priceLabel.getText());

    // 3. Trigger PUSH_AUCTION_DELETED
    List<AuctionSummaryDto> currentAuctions = new java.util.ArrayList<>();
    currentAuctions.add(auction);
    injectField(controller, "currentAuctions", currentAuctions);

    registeredPushHandlers
        .get(MessageType.PUSH_AUCTION_DELETED)
        .accept("{\"payload\":{\"auctionId\":\"auc-1\"}}");
    waitForRunLater();
    waitForRunLater();
    java.lang.reflect.Field currentAuctionsField =
        controller.getClass().getDeclaredField("currentAuctions");
    currentAuctionsField.setAccessible(true);
    List<?> updatedAuctions = (List<?>) currentAuctionsField.get(controller);
    assertTrue(updatedAuctions.isEmpty());

    // Exception/Null paths in loadAuctions
    reset(mockBiddingService);
    when(mockBiddingService.listAuctions())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API error")));
    when(mockBiddingService.getMyBids())
        .thenReturn(CompletableFuture.completedFuture(new MyBidsResponse(Collections.emptyList())));
    java.lang.reflect.Method loadAuctions =
        AuctionBrowseController.class.getDeclaredMethod("loadAuctions");
    loadAuctions.setAccessible(true);
    loadAuctions.invoke(controller);
    waitForRunLater();

    // Failure with cause = null
    reset(mockBiddingService);
    CompletableFuture<ListAuctionsResponse> failedFuture = new CompletableFuture<>();
    failedFuture.completeExceptionally(new RuntimeException("Immediate failure without cause"));
    when(mockBiddingService.listAuctions()).thenReturn(failedFuture);
    when(mockBiddingService.getMyBids())
        .thenReturn(CompletableFuture.completedFuture(new MyBidsResponse(Collections.emptyList())));
    loadAuctions.invoke(controller);
    waitForRunLater();
  }

  @Test
  public void testAuctionDetailControllerPushAndFocus() throws Exception {
    AuctionDetailController controller = new AuctionDetailController();
    BiddingClientService mockBiddingService = mock(BiddingClientService.class);
    injectField(controller, "biddingService", mockBiddingService);

    AppContext.setSelectedAuctionId("auc-123");
    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    user.setUsername("john");
    AppContext.setCurrentUser(user);

    BidSummaryDto bid =
        new BidSummaryDto(
            "b-1",
            "john",
            BigDecimal.valueOf(1200),
            BidType.MANUAL,
            LocalDateTime.now(),
            "John Doe");
    AuctionDetailDto detail =
        new AuctionDetailDto(
            "auc-123",
            "item-123",
            "Awesome Car",
            "V8 engine",
            ItemCategory.ART,
            ItemCondition.USED,
            "user-2",
            BigDecimal.valueOf(1000),
            BigDecimal.valueOf(1200),
            "john",
            BigDecimal.valueOf(50),
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(1),
            List.of(bid));
    detail.setSellerName("Seller Bob");
    detail.setImageUrls(List.of("http://dummyimage.com/img.png"));

    when(mockBiddingService.getAuctionDetail(eq("auc-123")))
        .thenReturn(CompletableFuture.completedFuture(detail));

    TextField txtBidInput = new TextField();
    Label lblBidError = new Label();
    Button btnBid = new Button();
    Label lblCurrentBid = new Label();
    Label lblMinIncrement = new Label();
    Button btnBack = new Button();
    VBox bidHistoryList = new VBox();
    Label lblTitle = new Label();
    ImageView itemImageView = new ImageView();

    injectField(controller, "txtBidInput", txtBidInput);
    injectField(controller, "lblBidError", lblBidError);
    injectField(controller, "btnBid", btnBid);
    injectField(controller, "lblCurrentBid", lblCurrentBid);
    injectField(controller, "lblMinIncrement", lblMinIncrement);
    injectField(controller, "btnBack", btnBack);
    injectField(controller, "bidHistoryList", bidHistoryList);
    injectField(controller, "lblTitle", lblTitle);
    injectField(controller, "itemImageView", itemImageView);

    SimpleDoubleProperty progressProperty = new SimpleDoubleProperty(0.0);
    SimpleBooleanProperty errorProperty = new SimpleBooleanProperty(false);
    try (MockedConstruction<Image> mockedImage =
        mockConstruction(
            Image.class,
            (mockImg, context) -> {
              when(mockImg.progressProperty()).thenReturn(progressProperty);
              when(mockImg.errorProperty()).thenReturn(errorProperty);
              when(mockImg.getWidth()).thenReturn(420.0);
              when(mockImg.getHeight()).thenReturn(320.0);
            })) {
      controller.initialize();
      waitForRunLater();

      progressProperty.set(1.0);
      waitForRunLater();

      // Test txtBidInput focus gained to clear bid error
      lblBidError.setText("Some Error");
      lblBidError.setVisible(true);

      // Call setFocused(true) via reflection
      java.lang.reflect.Method setFocusedMethod =
          javafx.scene.Node.class.getDeclaredMethod("setFocused", boolean.class);
      setFocusedMethod.setAccessible(true);
      setFocusedMethod.invoke(txtBidInput, true);
      waitForRunLater();
      assertFalse(lblBidError.isVisible());

      // Verify PUSH_BID_UPDATE push handler was registered and trigger it
      assertTrue(registeredPushHandlers.containsKey(MessageType.PUSH_BID_UPDATE));

      // Set up getAuctionDetail mock for the refresh call that push update does
      reset(mockBiddingService);
      when(mockBiddingService.getAuctionDetail(eq("auc-123")))
          .thenReturn(CompletableFuture.completedFuture(detail));

      registeredPushHandlers
          .get(MessageType.PUSH_BID_UPDATE)
          .accept("{\"payload\":{\"auctionId\":\"auc-123\",\"newHighestBid\":\"1500\"}}");
      waitForRunLater();
      assertEquals("$1,500", lblCurrentBid.getText());
    }
  }

  @Test
  public void testMyListingsControllerFormatMethods() throws Exception {
    assertTrue(com.nhom1.auction.client.util.DisplayFormatters.isEnded(AuctionStatus.FINISHED));
    assertTrue(com.nhom1.auction.client.util.DisplayFormatters.isEnded(AuctionStatus.CANCELED));
    assertTrue(com.nhom1.auction.client.util.DisplayFormatters.isEnded(AuctionStatus.PAID));
    assertFalse(com.nhom1.auction.client.util.DisplayFormatters.isEnded(AuctionStatus.OPEN));

    assertEquals(
        "N/A", com.nhom1.auction.client.util.DisplayFormatters.timeLeft((LocalDateTime) null));
    assertEquals(
        "Ended",
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().minusSeconds(1)));
    assertEquals(
        "Ended", com.nhom1.auction.client.util.DisplayFormatters.timeLeft(LocalDateTime.now()));
    assertEquals(
        "2 days left",
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().plusDays(2).plusSeconds(5)));
    assertEquals(
        "2 hours left",
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().plusHours(2).plusSeconds(5)));
    assertEquals(
        "1 min left",
        com.nhom1.auction.client.util.DisplayFormatters.timeLeft(
            LocalDateTime.now().plusMinutes(1).plusSeconds(5)));
  }

  @Test
  public void testMyListingsControllerCreateListingCardFailure() throws Exception {
    MyListingsController controller = new MyListingsController();
    AuctionSummaryDto dto = new AuctionSummaryDto();
    dto.setId("auc-1");

    try (MockedConstruction<FXMLLoader> mockLoader =
        mockConstruction(
            FXMLLoader.class,
            (mock, context) -> {
              when(mock.load()).thenThrow(new IOException("Simulated FXML load failure"));
            })) {
      java.lang.reflect.Method mCreate =
          MyListingsController.class.getDeclaredMethod(
              "createListingCard", AuctionSummaryDto.class);
      mCreate.setAccessible(true);
      try {
        mCreate.invoke(controller, dto);
        fail("Should have thrown an exception");
      } catch (java.lang.reflect.InvocationTargetException e) {
        assertTrue(e.getCause() instanceof RuntimeException);
        assertTrue(e.getCause().getMessage().contains("Failed to load listing card component"));
      }
    }
  }

  @Test
  public void testMyListingsControllerDeleteEdgeCases() throws Exception {
    MyListingsController controller = new MyListingsController();
    AuctionSummaryDto summary = new AuctionSummaryDto();
    summary.setId("auc-1");

    Label activeListingsLabel = new Label();
    GridPane listingsGrid = new GridPane();
    injectField(controller, "activeListingsLabel", activeListingsLabel);
    injectField(controller, "listingsGrid", listingsGrid);

    ObservableList<ButtonType> buttonTypes = FXCollections.observableArrayList();
    DialogPane mockDialogPane = mock(DialogPane.class);
    ObservableList<String> stylesheets = FXCollections.observableArrayList();
    when(mockDialogPane.getStylesheets()).thenReturn(stylesheets);
    Button mockYesButton = new Button();
    Button mockNoButton = new Button();
    when(mockDialogPane.lookupButton(any(ButtonType.class)))
        .thenAnswer(
            inv -> {
              ButtonType bt = inv.getArgument(0);
              if (bt != null && "Yes".equals(bt.getText())) return mockYesButton;
              return mockNoButton;
            });

    // 1. confirm dialog returns yes, but AppContext has no user session
    AppContext.clearSession();
    try (MockedConstruction<Alert> mockedAlert =
        mockConstruction(
            Alert.class,
            (mockAlert, context) -> {
              when(mockAlert.getButtonTypes()).thenReturn(buttonTypes);
              when(mockAlert.getDialogPane()).thenReturn(mockDialogPane);
              when(mockAlert.showAndWait())
                  .thenAnswer(
                      inv -> {
                        if (buttonTypes.size() > 0) return Optional.of(buttonTypes.get(0)); // yes
                        return Optional.empty();
                      });
            })) {
      java.lang.reflect.Method mDelete =
          MyListingsController.class.getDeclaredMethod(
              "handleDeleteListing", AuctionSummaryDto.class);
      mDelete.setAccessible(true);
      mDelete.invoke(controller, summary);
      waitForRunLater();
    }

    // 2. confirm dialog returns yes, user has session, but delete response has success = false and
    // error = null
    AuthResponse user = new AuthResponse();
    user.setUserID("user-1");
    AppContext.setCurrentUser(user);

    ResponseMessage<String> deleteFailNullError = new ResponseMessage<>();
    deleteFailNullError.setSuccess(false);
    deleteFailNullError.setError(null);
    when(mockConnection.sendRequest(
            argThat(r -> r != null && r.getType() == MessageType.DELETE_AUCTION), eq(String.class)))
        .thenReturn(CompletableFuture.completedFuture(deleteFailNullError));

    try (MockedConstruction<Alert> mockedAlert =
        mockConstruction(
            Alert.class,
            (mockAlert, context) -> {
              when(mockAlert.getButtonTypes()).thenReturn(buttonTypes);
              when(mockAlert.getDialogPane()).thenReturn(mockDialogPane);
              when(mockAlert.showAndWait())
                  .thenAnswer(
                      inv -> {
                        if (buttonTypes.size() > 0) return Optional.of(buttonTypes.get(0)); // yes
                        return Optional.empty();
                      });
            })) {
      java.lang.reflect.Method mDelete =
          MyListingsController.class.getDeclaredMethod(
              "handleDeleteListing", AuctionSummaryDto.class);
      mDelete.setAccessible(true);
      mDelete.invoke(controller, summary);
      waitForRunLater();
    }
  }
}
