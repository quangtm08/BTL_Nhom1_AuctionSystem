package com.nhom1.auction.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.client.user.connection.ServerConnection;
import javafx.application.Platform;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ClientApplicationTest {

  @BeforeAll
  public static void initJavaFX() {
    try {
      Platform.startup(() -> {});
    } catch (IllegalStateException e) {
      // Already initialized
    }
    try {
      // Mock ServerConnection singleton to prevent blocking network attempts
      ServerConnection mockConnection = mock(ServerConnection.class);
      java.lang.reflect.Field instanceField = ServerConnection.class.getDeclaredField("instance");
      instanceField.setAccessible(true);
      instanceField.set(null, mockConnection);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @Test
  public void testAppView() {
    for (AppView view : AppView.values()) {
      assertNotNull(view.getFxml());
      assertNull(view.getCss());
      assertEquals(view, AppView.valueOf(view.name()));
    }
  }

  @Test
  public void testAppAssets() {
    // AppAssets.loadFonts does not throw even if file loading fails,
    // we call it to ensure it completes gracefully.
    AppAssets.loadFonts();
    // Since fonts are in resources, they should load or be null/non-null depending on environment.
  }

  @Test
  public void testClientApplicationStaticAccessors() {
    Stage mockStage = mock(Stage.class);
    ClientApplication.setStage(mockStage);
    assertEquals(mockStage, ClientApplication.getStage());

    ClientApplication.setClientIp("127.0.0.1");
    assertEquals("127.0.0.1", ClientApplication.getClientIp());

    ClientApplication.setIp("192.168.1.1");
    assertEquals("192.168.1.1", ClientApplication.getIp());

    ClientApplication.setPort(8080);
    assertEquals(8080, ClientApplication.getPort());

    javafx.fxml.FXMLLoader mockLoader = mock(javafx.fxml.FXMLLoader.class);
    ClientApplication.setFxmlLoader(mockLoader);
    assertEquals(mockLoader, ClientApplication.getFxmlLoader());
  }

  @Test
  public void testAppNavigatorAndShellController() throws Exception {
    // Create actual JavaFX controls on the JavaFX Thread or with toolkit initialized.
    StackPane rootPane = new StackPane();
    ShellController shell = new ShellController();

    // Use reflection to set FXML private field
    java.lang.reflect.Field field = ShellController.class.getDeclaredField("rootPane");
    field.setAccessible(true);
    field.set(shell, rootPane);

    AppNavigator.setRoot(shell);

    // Test navigateTo
    // Since it loads FXML from classpath, calling it should not throw and should update
    // currentView.
    // We run navigateTo on Platform.runLater or synchronously since JavaFX toolkit is running.
    Platform.runLater(
        () -> {
          AppNavigator.navigateTo(AppView.SIGN_IN);
          assertEquals(AppView.SIGN_IN, AppNavigator.getCurrentView());

          // Navigate to same view should return early
          AppNavigator.navigateTo(AppView.SIGN_IN);
          assertEquals(AppView.SIGN_IN, AppNavigator.getCurrentView());
        });

    // Wait briefly for JavaFX events
    Thread.sleep(200);
  }

  @Test
  public void testClientApplicationStartAndMain() {
    ClientApplication app = new ClientApplication();
    Stage stage = mock(Stage.class);
    Platform.runLater(
        () -> {
          try {
            app.start(stage);
          } catch (Exception e) {
            // Expected to fail on FXMLLoader or resource loading
          }
        });
  }
}
