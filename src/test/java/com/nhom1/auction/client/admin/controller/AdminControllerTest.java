package com.nhom1.auction.client.admin.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.client.AppNavigator;
import com.nhom1.auction.client.AppView;
import com.nhom1.auction.client.BaseShellController;
import com.nhom1.auction.client.admin.service.AdminClientService;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.dto.admin.AdminAuctionListResponse;
import com.nhom1.auction.common.dto.admin.AdminUserListResponse;
import com.nhom1.auction.common.dto.admin.UserSummaryDto;
import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.UserRole;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.utils.AppContext;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AdminControllerTest {

  private static ServerConnection mockConnection;
  private static BaseShellController mockShell;

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
    reset(mockConnection);
    reset(mockShell);

    // Reset ClientPushService singleton to force registration of push handlers
    java.lang.reflect.Field pushInstanceField =
        com.nhom1.auction.client.service.ClientPushService.class.getDeclaredField("instance");
    pushInstanceField.setAccessible(true);
    pushInstanceField.set(null, null);
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
  public void testAdminOverviewController() throws Exception {
    AdminOverviewController controller = new AdminOverviewController();

    AdminClientService mockAdminService = mock(AdminClientService.class);

    AdminUserListResponse ulr = new AdminUserListResponse();
    UserSummaryDto user1 =
        new UserSummaryDto("u-1", "john", "john@gmail.com", UserRole.USER, LocalDateTime.now());
    UserSummaryDto user2 =
        new UserSummaryDto("u-2", "boss", "boss@gmail.com", UserRole.ADMIN, LocalDateTime.now());
    ulr.setUsers(List.of(user1, user2));

    AdminAuctionListResponse alr = new AdminAuctionListResponse();
    AuctionSummaryDto auc = new AuctionSummaryDto();
    auc.setId("a-1");
    auc.setItemName("Golden Ring");
    auc.setStatus(AuctionStatus.RUNNING);
    alr.setAuctions(List.of(auc));

    when(mockAdminService.listUsers()).thenReturn(CompletableFuture.completedFuture(ulr));
    when(mockAdminService.listAllAuctions()).thenReturn(CompletableFuture.completedFuture(alr));

    Label lblOverviewDate = new Label();
    Label lblTotalUsersValue = new Label();
    Label lblTotalUsersBreakdown = new Label();
    Label lblActiveAuctionsValue = new Label();
    Label lblActiveAuctionsBreakdown = new Label();
    Label lblRecentActivityBody = new Label();
    Label lblRecentActivityTime = new Label();
    Label lblSessionStatus = new Label();

    injectField(controller, "adminClientService", mockAdminService);
    injectField(controller, "lblOverviewDate", lblOverviewDate);
    injectField(controller, "lblTotalUsersValue", lblTotalUsersValue);
    injectField(controller, "lblTotalUsersBreakdown", lblTotalUsersBreakdown);
    injectField(controller, "lblActiveAuctionsValue", lblActiveAuctionsValue);
    injectField(controller, "lblActiveAuctionsBreakdown", lblActiveAuctionsBreakdown);
    injectField(controller, "lblRecentActivityBody", lblRecentActivityBody);
    injectField(controller, "lblRecentActivityTime", lblRecentActivityTime);
    injectField(controller, "lblSessionStatus", lblSessionStatus);

    controller.initialize();
    waitForRunLater();

    verify(mockAdminService).listUsers();
    verify(mockAdminService).listAllAuctions();

    assertEquals("2", lblTotalUsersValue.getText());
    assertEquals("1", lblActiveAuctionsValue.getText());
  }

  @Test
  public void testAdminOverviewControllerFailure() throws Exception {
    AdminOverviewController controller = new AdminOverviewController();

    AdminClientService mockAdminService = mock(AdminClientService.class);
    when(mockAdminService.listUsers())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("API Error")));
    when(mockAdminService.listAllAuctions())
        .thenReturn(CompletableFuture.completedFuture(new AdminAuctionListResponse()));

    Label lblOverviewDate = new Label();
    Label lblTotalUsersValue = new Label();
    Label lblTotalUsersBreakdown = new Label();
    Label lblActiveAuctionsValue = new Label();
    Label lblActiveAuctionsBreakdown = new Label();
    Label lblRecentActivityBody = new Label();
    Label lblRecentActivityTime = new Label();
    Label lblSessionStatus = new Label();

    injectField(controller, "adminClientService", mockAdminService);
    injectField(controller, "lblOverviewDate", lblOverviewDate);
    injectField(controller, "lblTotalUsersValue", lblTotalUsersValue);
    injectField(controller, "lblTotalUsersBreakdown", lblTotalUsersBreakdown);
    injectField(controller, "lblActiveAuctionsValue", lblActiveAuctionsValue);
    injectField(controller, "lblActiveAuctionsBreakdown", lblActiveAuctionsBreakdown);
    injectField(controller, "lblRecentActivityBody", lblRecentActivityBody);
    injectField(controller, "lblRecentActivityTime", lblRecentActivityTime);
    injectField(controller, "lblSessionStatus", lblSessionStatus);

    controller.initialize();
    waitForRunLater();

    assertEquals("--", lblTotalUsersValue.getText());
    assertEquals("Could not load users", lblTotalUsersBreakdown.getText());
    assertTrue(lblRecentActivityBody.getText().contains("Admin dashboard failed to load"));
  }

  @Test
  public void testAdminOverviewControllerNullLists() throws Exception {
    AdminOverviewController controller = new AdminOverviewController();

    AdminClientService mockAdminService = mock(AdminClientService.class);
    AdminUserListResponse ulr = new AdminUserListResponse();
    ulr.setUsers(null);
    AdminAuctionListResponse alr = new AdminAuctionListResponse();
    alr.setAuctions(null);

    when(mockAdminService.listUsers()).thenReturn(CompletableFuture.completedFuture(ulr));
    when(mockAdminService.listAllAuctions()).thenReturn(CompletableFuture.completedFuture(alr));

    Label lblOverviewDate = new Label();
    Label lblTotalUsersValue = new Label();
    Label lblTotalUsersBreakdown = new Label();
    Label lblActiveAuctionsValue = new Label();
    Label lblActiveAuctionsBreakdown = new Label();
    Label lblRecentActivityBody = new Label();
    Label lblRecentActivityTime = new Label();
    Label lblSessionStatus = new Label();

    injectField(controller, "adminClientService", mockAdminService);
    injectField(controller, "lblOverviewDate", lblOverviewDate);
    injectField(controller, "lblTotalUsersValue", lblTotalUsersValue);
    injectField(controller, "lblTotalUsersBreakdown", lblTotalUsersBreakdown);
    injectField(controller, "lblActiveAuctionsValue", lblActiveAuctionsValue);
    injectField(controller, "lblActiveAuctionsBreakdown", lblActiveAuctionsBreakdown);
    injectField(controller, "lblRecentActivityBody", lblRecentActivityBody);
    injectField(controller, "lblRecentActivityTime", lblRecentActivityTime);
    injectField(controller, "lblSessionStatus", lblSessionStatus);

    controller.initialize();
    waitForRunLater();

    assertEquals("0", lblTotalUsersValue.getText());
    assertEquals("0 members | 0 admins", lblTotalUsersBreakdown.getText());
  }

  @Test
  public void testAdminSidebarController() throws Exception {
    AdminSidebarController controller = new AdminSidebarController();

    Button btnDashboard = new Button();
    Button btnUsers = new Button();
    Button btnAuctions = new Button();
    Button btnLogout = new Button();

    injectField(controller, "btnDashboard", btnDashboard);
    injectField(controller, "btnUsers", btnUsers);
    injectField(controller, "btnAuctions", btnAuctions);
    injectField(controller, "btnLogout", btnLogout);

    controller.initialize();

    btnDashboard.getOnAction().handle(null);
    btnUsers.getOnAction().handle(null);
    btnAuctions.getOnAction().handle(null);
    btnLogout.getOnAction().handle(null);
  }

  @Test
  public void testAdminSidebarControllerEdgeCases() throws Exception {
    AdminSidebarController controller = new AdminSidebarController();

    Button btnDashboard = new Button();
    Button btnUsers = new Button();
    Button btnAuctions = new Button();
    Button btnLogout = new Button();

    injectField(controller, "btnDashboard", btnDashboard);
    injectField(controller, "btnUsers", btnUsers);
    injectField(controller, "btnAuctions", btnAuctions);
    injectField(controller, "btnLogout", btnLogout);

    java.lang.reflect.Field fCurrentView = AppNavigator.class.getDeclaredField("currentView");
    fCurrentView.setAccessible(true);
    java.lang.reflect.Method mUpdateActive =
        AdminSidebarController.class.getDeclaredMethod("updateActiveButton");
    mUpdateActive.setAccessible(true);

    fCurrentView.set(null, AppView.ADMIN_OVERVIEW);
    mUpdateActive.invoke(controller);
    assertTrue(btnDashboard.getStyleClass().contains("side-btn-active"));

    fCurrentView.set(null, AppView.USER_MANAGEMENT);
    mUpdateActive.invoke(controller);
    assertTrue(btnUsers.getStyleClass().contains("side-btn-active"));

    fCurrentView.set(null, AppView.AUCTION_MANAGEMENT);
    mUpdateActive.invoke(controller);
    assertTrue(btnAuctions.getStyleClass().contains("side-btn-active"));

    fCurrentView.set(null, AppView.SIGN_IN);
    mUpdateActive.invoke(controller);
    assertFalse(btnDashboard.getStyleClass().contains("side-btn-active"));

    fCurrentView.set(null, null);
    mUpdateActive.invoke(controller);
    assertFalse(btnDashboard.getStyleClass().contains("side-btn-active"));

    java.lang.reflect.Method mNavigate =
        AdminSidebarController.class.getDeclaredMethod("navigateWithLoading", AppView.class);
    mNavigate.setAccessible(true);
    fCurrentView.set(null, AppView.ADMIN_OVERVIEW);
    mNavigate.invoke(controller, AppView.ADMIN_OVERVIEW); // should return early
  }

  @Test
  public void testAuctionManagementController() throws Exception {
    AuctionManagementController controller = new AuctionManagementController();

    AdminClientService mockAdminService = mock(AdminClientService.class);

    AdminAuctionListResponse alr = new AdminAuctionListResponse();
    AuctionSummaryDto auc = new AuctionSummaryDto();
    auc.setId("a-1");
    auc.setItemName("Mona Lisa");
    auc.setStatus(AuctionStatus.OPEN);
    alr.setAuctions(List.of(auc));

    when(mockAdminService.listAllAuctions()).thenReturn(CompletableFuture.completedFuture(alr));

    Label lblAuctionSummary = new Label();
    GridPane auctionGrid = new GridPane();

    injectField(controller, "adminClientService", mockAdminService);
    injectField(controller, "lblAuctionSummary", lblAuctionSummary);
    injectField(controller, "auctionGrid", auctionGrid);

    controller.initialize();
    waitForRunLater();

    verify(mockAdminService).listAllAuctions();

    // Cancel auction test
    when(mockAdminService.cancelAuction(eq("a-1")))
        .thenReturn(CompletableFuture.completedFuture("CANCELED"));
    java.lang.reflect.Method cancelMethod =
        AuctionManagementController.class.getDeclaredMethod(
            "cancelAuction", String.class, Button.class);
    cancelMethod.setAccessible(true);
    cancelMethod.invoke(controller, "a-1", new Button());
    waitForRunLater();

    verify(mockAdminService).cancelAuction("a-1");
  }

  @Test
  public void testUserManagementController() throws Exception {
    UserManagementController controller = new UserManagementController();

    AdminClientService mockAdminService = mock(AdminClientService.class);

    AdminUserListResponse ulr = new AdminUserListResponse();
    UserSummaryDto u =
        new UserSummaryDto("u-1", "sally", "sally@mail.com", UserRole.USER, LocalDateTime.now());
    ulr.setUsers(List.of(u));

    when(mockAdminService.listUsers()).thenReturn(CompletableFuture.completedFuture(ulr));

    Label lblUserSummary = new Label();
    GridPane userGrid = new GridPane();

    injectField(controller, "adminClientService", mockAdminService);
    injectField(controller, "lblUserSummary", lblUserSummary);
    injectField(controller, "userGrid", userGrid);

    controller.initialize();
    waitForRunLater();

    verify(mockAdminService).listUsers();

    // Delete user test
    when(mockAdminService.deleteUser(eq("u-1")))
        .thenReturn(CompletableFuture.completedFuture("DELETED"));
    java.lang.reflect.Method deleteMethod =
        UserManagementController.class.getDeclaredMethod("deleteUser", String.class, Button.class);
    deleteMethod.setAccessible(true);
    deleteMethod.invoke(controller, "u-1", new Button());
    waitForRunLater();

    verify(mockAdminService).deleteUser("u-1");
  }

  @Test
  public void testAuctionManagementControllerExceptionsAndPush() throws Exception {
    AuctionManagementController controller = new AuctionManagementController();
    AdminClientService mockAdminService = mock(AdminClientService.class);
    injectField(controller, "adminClientService", mockAdminService);

    Label lblAuctionSummary = new Label();
    GridPane auctionGrid = new GridPane();
    injectField(controller, "lblAuctionSummary", lblAuctionSummary);
    injectField(controller, "auctionGrid", auctionGrid);

    // When listAllAuctions fails
    when(mockAdminService.listAllAuctions())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Simulated load error")));

    org.mockito.ArgumentCaptor<MessageType> msgTypeCaptor =
        org.mockito.ArgumentCaptor.forClass(MessageType.class);
    org.mockito.ArgumentCaptor<java.util.function.Consumer<String>> consumerCaptor =
        org.mockito.ArgumentCaptor.forClass(java.util.function.Consumer.class);

    controller.initialize();
    waitForRunLater();

    verify(mockConnection, atLeastOnce())
        .registerPushHandler(msgTypeCaptor.capture(), consumerCaptor.capture());
    assertTrue(lblAuctionSummary.getText().contains("Load auctions failed"));

    // Trigger push handlers and check reload is called
    reset(mockAdminService);
    when(mockAdminService.listAllAuctions())
        .thenReturn(CompletableFuture.completedFuture(new AdminAuctionListResponse()));

    List<MessageType> capturedTypes = msgTypeCaptor.getAllValues();
    List<java.util.function.Consumer<String>> capturedConsumers = consumerCaptor.getAllValues();

    for (int i = 0; i < capturedTypes.size(); i++) {
      if (capturedTypes.get(i) == MessageType.PUSH_NEW_AUCTION
          || capturedTypes.get(i) == MessageType.PUSH_BID_UPDATE
          || capturedTypes.get(i) == MessageType.PUSH_AUCTION_DELETED
          || capturedTypes.get(i) == MessageType.PUSH_AUCTION_ENDED) {
        capturedConsumers.get(i).accept("{\"success\":true,\"payload\":{}}");
        waitForRunLater();
      }
    }
    verify(mockAdminService, atLeastOnce()).listAllAuctions();

    // Cancel fails
    reset(mockAdminService);
    when(mockAdminService.cancelAuction(anyString()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Simulated cancel error")));

    java.lang.reflect.Method cancelMethod =
        AuctionManagementController.class.getDeclaredMethod(
            "cancelAuction", String.class, Button.class);
    cancelMethod.setAccessible(true);
    cancelMethod.invoke(controller, "a-123", new Button());
    waitForRunLater();
    assertTrue(lblAuctionSummary.getText().contains("Cancel failed"));
  }

  @Test
  public void testUserManagementControllerExceptionsAndPush() throws Exception {
    UserManagementController controller = new UserManagementController();
    AdminClientService mockAdminService = mock(AdminClientService.class);
    injectField(controller, "adminClientService", mockAdminService);

    Label lblUserSummary = new Label();
    GridPane userGrid = new GridPane();
    injectField(controller, "lblUserSummary", lblUserSummary);
    injectField(controller, "userGrid", userGrid);

    // When listUsers fails
    when(mockAdminService.listUsers())
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Simulated load error")));

    org.mockito.ArgumentCaptor<MessageType> msgTypeCaptor =
        org.mockito.ArgumentCaptor.forClass(MessageType.class);
    org.mockito.ArgumentCaptor<java.util.function.Consumer<String>> consumerCaptor =
        org.mockito.ArgumentCaptor.forClass(java.util.function.Consumer.class);

    controller.initialize();
    waitForRunLater();

    verify(mockConnection, atLeastOnce())
        .registerPushHandler(msgTypeCaptor.capture(), consumerCaptor.capture());
    assertTrue(lblUserSummary.getText().contains("Load users failed"));

    // Trigger push handlers and check reload is called
    reset(mockAdminService);
    when(mockAdminService.listUsers())
        .thenReturn(CompletableFuture.completedFuture(new AdminUserListResponse()));

    List<MessageType> capturedTypes = msgTypeCaptor.getAllValues();
    List<java.util.function.Consumer<String>> capturedConsumers = consumerCaptor.getAllValues();

    for (int i = 0; i < capturedTypes.size(); i++) {
      if (capturedTypes.get(i) == MessageType.PUSH_USER_CREATED
          || capturedTypes.get(i) == MessageType.PUSH_USER_DELETED) {
        capturedConsumers.get(i).accept("{\"success\":true,\"payload\":{}}");
        waitForRunLater();
      }
    }
    verify(mockAdminService, atLeastOnce()).listUsers();

    // Delete fails
    reset(mockAdminService);
    when(mockAdminService.deleteUser(anyString()))
        .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Simulated delete error")));

    java.lang.reflect.Method deleteMethod =
        UserManagementController.class.getDeclaredMethod("deleteUser", String.class, Button.class);
    deleteMethod.setAccessible(true);
    deleteMethod.invoke(controller, "u-123", new Button());
    waitForRunLater();
    assertTrue(lblUserSummary.getText().contains("Delete failed"));
  }

  @Test
  public void testAuctionManagementControllerHelperMethodsAndStatusPills() throws Exception {
    AuctionManagementController controller = new AuctionManagementController();

    // shortId(String)
    java.lang.reflect.Method mShortId =
        AuctionManagementController.class.getDeclaredMethod("shortId", String.class);
    mShortId.setAccessible(true);
    assertEquals("-", mShortId.invoke(controller, (String) null));
    assertEquals("abc", mShortId.invoke(controller, "abc"));
    assertEquals("12345678...", mShortId.invoke(controller, "1234567890"));

    // nvl(String)
    java.lang.reflect.Method mNvl =
        AuctionManagementController.class.getDeclaredMethod("nvl", String.class);
    mNvl.setAccessible(true);
    assertEquals("-", mNvl.invoke(controller, (String) null));
    assertEquals("-", mNvl.invoke(controller, "   "));
    assertEquals("hello", mNvl.invoke(controller, "hello"));
  }

  @Test
  public void testUserManagementControllerHelperMethodsAndAdminRole() throws Exception {
    UserManagementController controller = new UserManagementController();

    // formatDate(LocalDateTime)
    assertEquals(
        "-", com.nhom1.auction.client.util.DisplayFormatters.shortDate((LocalDateTime) null));
    LocalDateTime time = LocalDateTime.of(2026, 5, 22, 10, 30);
    assertEquals("May 22, 2026", com.nhom1.auction.client.util.DisplayFormatters.shortDate(time));

    // nvl(String)
    java.lang.reflect.Method mNvl =
        UserManagementController.class.getDeclaredMethod("nvl", String.class);
    mNvl.setAccessible(true);
    assertEquals("-", mNvl.invoke(controller, (String) null));
    assertEquals("-", mNvl.invoke(controller, "   "));
    assertEquals("hello", mNvl.invoke(controller, "hello"));

    // Test addRow disables delete button for Admin role
    GridPane userGrid = new GridPane();
    injectField(controller, "userGrid", userGrid);

    UserSummaryDto adminUser =
        new UserSummaryDto(
            "u-admin", "admin_boss", "admin@mail.com", UserRole.ADMIN, LocalDateTime.now());
    java.lang.reflect.Method mAddRow =
        UserManagementController.class.getDeclaredMethod("addRow", int.class, UserSummaryDto.class);
    mAddRow.setAccessible(true);

    mAddRow.invoke(controller, 1, adminUser);

    // Find the delete button in the grid children to verify it is disabled
    boolean foundDisabledDelete = false;
    for (javafx.scene.Node node : userGrid.getChildren()) {
      if (node instanceof javafx.scene.layout.HBox) {
        javafx.scene.layout.HBox actions = (javafx.scene.layout.HBox) node;
        if (!actions.getChildren().isEmpty() && actions.getChildren().get(0) instanceof Button) {
          Button btn = (Button) actions.getChildren().get(0);
          if ("Delete".equals(btn.getText()) && btn.isDisable()) {
            foundDisabledDelete = true;
            break;
          }
        }
      }
    }
    assertTrue(foundDisabledDelete, "Delete button for admin should be disabled");
  }
}
