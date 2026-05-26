package com.nhom1.auction.client.user.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.Socket;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServerConnectionTest {

  private java.io.InputStream getBlockingInputStream() {
    return new java.io.InputStream() {
      @Override
      public int read() throws java.io.IOException {
        try {
          Thread.sleep(10000);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
        return -1;
      }
    };
  }

  @BeforeEach
  public void resetSingleton() throws Exception {
    Field instanceField = ServerConnection.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);
  }

  @Test
  public void testConnectFailureAndFallback() {
    try (var socketMock =
        mockConstruction(
            Socket.class,
            (mock, context) -> {
              doThrow(new IOException("Simulated Cloud Connection Failure"))
                  .when(mock)
                  .connect(any(), anyInt());
            })) {
      ServerConnection conn = ServerConnection.getInstance();
      assertNotNull(conn);
      assertFalse(conn.isConnected());
    }
  }

  @Test
  public void testConnectSuccessCloud() throws Exception {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    java.io.InputStream inStream = getBlockingInputStream();

    try (var socketMock =
        mockConstruction(
            Socket.class,
            (mock, context) -> {
              // Do not throw on connect, which simulates success
              when(mock.getOutputStream()).thenReturn(outStream);
              when(mock.getInputStream()).thenReturn(inStream);
            })) {
      ServerConnection conn = ServerConnection.getInstance();
      assertTrue(conn.isConnected());
    }
  }

  @Test
  public void testConnectFallbackSuccessLocal() throws Exception {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    java.io.InputStream inStream = getBlockingInputStream();

    AtomicReference<Integer> connectCalls = new AtomicReference<>(0);

    try (var socketMock =
        mockConstruction(
            Socket.class,
            (mock, context) -> {
              // Throw only on the first socket connection, succeed on the second one
              doAnswer(
                      invocation -> {
                        int calls = connectCalls.get() + 1;
                        connectCalls.set(calls);
                        if (calls == 1) {
                          throw new IOException("Cloud connection failed");
                        }
                        return null; // Local connect succeeds
                      })
                  .when(mock)
                  .connect(any(), anyInt());

              when(mock.getOutputStream()).thenReturn(outStream);
              when(mock.getInputStream()).thenReturn(inStream);
            })) {
      ServerConnection conn = ServerConnection.getInstance();
      assertTrue(conn.isConnected());
      assertEquals(2, connectCalls.get());
    }
  }

  @Test
  public void testSendRequest_NotConnected() {
    try (var socketMock =
        mockConstruction(
            Socket.class,
            (mock, context) -> {
              doThrow(new IOException("Connection failed")).when(mock).connect(any(), anyInt());
            })) {
      ServerConnection conn = ServerConnection.getInstance();
      RequestMessage<String> req = new RequestMessage<>(MessageType.LOGIN, "data");
      CompletableFuture<ResponseMessage<String>> future = conn.sendRequest(req, String.class);
      assertTrue(future.isCompletedExceptionally());
    }
  }

  @Test
  public void testSendRequest_NoRequestIdGenerated() throws Exception {
    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
    java.io.InputStream inStream = getBlockingInputStream();

    try (var socketMock =
        mockConstruction(
            Socket.class,
            (mock, context) -> {
              when(mock.getOutputStream()).thenReturn(outStream);
              when(mock.getInputStream()).thenReturn(inStream);
            })) {
      ServerConnection conn = ServerConnection.getInstance();

      RequestMessage<String> req = new RequestMessage<>(MessageType.LOGIN, "data");
      req.setRequestId(null); // Force auto generation of ID

      CompletableFuture<ResponseMessage<String>> future = conn.sendRequest(req, String.class);
      assertNotNull(req.getRequestId());
      assertFalse(req.getRequestId().isEmpty());
    }
  }
}
