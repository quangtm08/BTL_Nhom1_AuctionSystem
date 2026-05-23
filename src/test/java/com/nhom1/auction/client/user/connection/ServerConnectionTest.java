package com.nhom1.auction.client.user.connection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
  public void testSendRequest_SuccessAndReceiveResponse() throws Exception {
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
      req.setRequestId("test-req-id");

      CompletableFuture<ResponseMessage<String>> future = conn.sendRequest(req, String.class);

      Method handleMethod =
          ServerConnection.class.getDeclaredMethod("handleRawResponse", String.class);
      handleMethod.setAccessible(true);
      handleMethod.invoke(
          conn, "{\"requestId\":\"test-req-id\",\"success\":true,\"payload\":\"ok\"}");

      ResponseMessage<String> resp = future.get();
      assertNotNull(resp);
      assertTrue(resp.isSuccess());
      assertEquals("ok", resp.getPayload());
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

  @Test
  public void testSendRequest_Timeout() throws Exception {
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
      req.setRequestId("timeout-id");

      // We can shorten or test the timeout behavior or invoke the timeout check directly
      CompletableFuture<ResponseMessage<String>> future = conn.sendRequest(req, String.class);
      // Complete with timeout exception to trigger whenComplete block
      future.completeExceptionally(new java.util.concurrent.TimeoutException());

      // Should be removed from pendingRequests
      Field pendingRequestsField = ServerConnection.class.getDeclaredField("pendingRequests");
      pendingRequestsField.setAccessible(true);
      java.util.Map<?, ?> pending = (java.util.Map<?, ?>) pendingRequestsField.get(conn);
      assertFalse(pending.containsKey("timeout-id"));
    }
  }

  @Test
  public void testSendRequest_ExceptionDuringSerialization() throws Exception {
    ByteArrayOutputStream outStream = mock(ByteArrayOutputStream.class);
    // Throw an exception when trying to write to simulate printer error
    doThrow(new RuntimeException("Printer broke"))
        .when(outStream)
        .write(any(byte[].class), anyInt(), anyInt());
    doThrow(new RuntimeException("Printer broke")).when(outStream).write(anyInt());

    try (var socketMock =
        mockConstruction(
            Socket.class,
            (mock, context) -> {
              when(mock.getOutputStream()).thenReturn(outStream);
              when(mock.getInputStream()).thenReturn(getBlockingInputStream());
            })) {
      ServerConnection conn = ServerConnection.getInstance();
      RequestMessage<String> req = new RequestMessage<>(MessageType.LOGIN, "data");
      req.setRequestId("serial-fail-id");

      CompletableFuture<ResponseMessage<String>> future = conn.sendRequest(req, String.class);
      assertTrue(future.isCompletedExceptionally());
    }
  }

  @Test
  public void testPushNotificationHandler() throws Exception {
    try (var socketMock =
        mockConstruction(
            Socket.class,
            (mock, context) -> {
              when(mock.getOutputStream()).thenReturn(new ByteArrayOutputStream());
              when(mock.getInputStream()).thenReturn(getBlockingInputStream());
            })) {
      ServerConnection conn = ServerConnection.getInstance();

      AtomicReference<String> receivedJson = new AtomicReference<>();
      conn.registerPushHandler(
          MessageType.PUSH_NEW_AUCTION,
          json -> {
            receivedJson.set(json);
          });

      Method handleMethod =
          ServerConnection.class.getDeclaredMethod("handleRawResponse", String.class);
      handleMethod.setAccessible(true);
      String pushMessage = "{\"type\":\"PUSH_NEW_AUCTION\",\"payload\":\"new-auction-details\"}";
      handleMethod.invoke(conn, pushMessage);

      assertEquals(pushMessage, receivedJson.get());
    }
  }

  @Test
  public void testHandleRawResponse_InvalidJsonAndNoPendingRequest() throws Exception {
    try (var socketMock =
        mockConstruction(
            Socket.class,
            (mock, context) -> {
              when(mock.getOutputStream()).thenReturn(new ByteArrayOutputStream());
              when(mock.getInputStream()).thenReturn(getBlockingInputStream());
            })) {
      ServerConnection conn = ServerConnection.getInstance();
      Method handleMethod =
          ServerConnection.class.getDeclaredMethod("handleRawResponse", String.class);
      handleMethod.setAccessible(true);

      // 1. Invalid JSON should not throw exception out of handleRawResponse
      assertDoesNotThrow(() -> handleMethod.invoke(conn, "invalid json"));

      // 2. Request ID present but no pending request registered
      assertDoesNotThrow(
          () -> handleMethod.invoke(conn, "{\"requestId\":\"unknown-req-id\",\"success\":true}"));

      // 3. Unknown push type MessageType
      assertDoesNotThrow(() -> handleMethod.invoke(conn, "{\"type\":\"INVALID_TYPE_ENUM\"}"));
    }
  }

  @Test
  public void testStartListeningThread_EofAndException() throws Exception {
    // EOF simulation (readLine returns null)
    try (var socketMock =
        mockConstruction(
            Socket.class,
            (mock, context) -> {
              when(mock.getOutputStream()).thenReturn(new ByteArrayOutputStream());
              when(mock.getInputStream()).thenReturn(new ByteArrayInputStream("".getBytes()));
            })) {
      ServerConnection conn = ServerConnection.getInstance();
      assertTrue(conn.isConnected());
      // Wait for listener thread to process EOF
      Thread.sleep(100);
      assertFalse(conn.isConnected());
    }

    // Reset singleton
    Field instanceField = ServerConnection.class.getDeclaredField("instance");
    instanceField.setAccessible(true);
    instanceField.set(null, null);

    // IOException simulation in BufferedReader readLine
    try (var socketMock =
        mockConstruction(
            Socket.class,
            (mock, context) -> {
              when(mock.getOutputStream()).thenReturn(new ByteArrayOutputStream());
              java.io.InputStream badInput = mock(java.io.InputStream.class);
              when(badInput.read(any(byte[].class), anyInt(), anyInt()))
                  .thenThrow(new IOException("Socket read error"));
              when(mock.getInputStream()).thenReturn(badInput);
            })) {
      ServerConnection conn = ServerConnection.getInstance();
      assertTrue(conn.isConnected());
      Thread.sleep(100);
      assertFalse(conn.isConnected());
    }
  }
}
