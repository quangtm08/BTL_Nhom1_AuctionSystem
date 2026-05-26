package com.nhom1.auction.client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.client.user.connection.ServerConnection;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ClientApplicationTest {

  @BeforeAll
  public static void initJavaFX() {
    try {
      // Mock ServerConnection singleton to prevent blocking network attempts
      ServerConnection mockConnection = mock(ServerConnection.class);
      // Return a non-null failed future by default so that .thenApply calls don't NPE
      lenient()
          .when(mockConnection.sendRequest(any(), any()))
          .thenReturn(
              CompletableFuture.failedFuture(new IOException("Mock connection: not connected")));

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
}
