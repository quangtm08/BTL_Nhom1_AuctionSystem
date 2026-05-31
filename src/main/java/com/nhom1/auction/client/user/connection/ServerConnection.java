package com.nhom1.auction.client.user.connection;

import com.fasterxml.jackson.databind.JsonNode;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/*
- Bridge between the Client and the Server.
- It manages the persistent Socket connection and handles JSON deserialization
*/
public class ServerConnection {

    private static ServerConnection instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    private final Map<String, PendingRequest<?>> pendingRequests =
        new ConcurrentHashMap<>();
    private final Map<MessageType, Consumer<String>> pushHandlers =
        new ConcurrentHashMap<>();

    // Helper to store the box and the blueprint
    private static class PendingRequest<T> {

        final CompletableFuture<ResponseMessage<T>> future;
        final Class<T> responseClass;

        PendingRequest(
            CompletableFuture<ResponseMessage<T>> future,
            Class<T> responseClass
        ) {
            this.future = future;
            this.responseClass = responseClass;
        }
    }

    private ServerConnection() {
        connect();
    }

    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    private void connect() {
        String cloudHost = "kodama.proxy.rlwy.net";
        int cloudPort = 49734;
        String localHost = "localhost";
        int localPort = 12345;
        int timeoutMillis = 6000;

        try {
            System.out.println(
                "Server: Connecting to Cloud (" +
                    cloudHost +
                    ":" +
                    cloudPort +
                    ")..."
            );
            this.socket = new Socket();
            this.socket.connect(
                new InetSocketAddress(cloudHost, cloudPort),
                timeoutMillis
            );

            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            this.connected = true;
            System.out.println("Connected to Cloud Auction Server.");
            startListening();
        } catch (IOException e) {
            System.err.println("Cloud connection failed: " + e.getMessage());
            System.out.println(
                "Falling back to Local Server (" +
                    localHost +
                    ":" +
                    localPort +
                    ")..."
            );

            try {
                this.socket = new Socket();
                this.socket.connect(
                    new InetSocketAddress(localHost, localPort),
                    timeoutMillis
                );

                this.out = new PrintWriter(socket.getOutputStream(), true);
                this.in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream())
                );
                this.connected = true;
                System.out.println("Connected to Local Auction Server.");
                startListening();
            } catch (IOException ex) {
                System.err.println(
                    "Could not connect to any server: " + ex.getMessage()
                );
                this.connected = false;
            }
        }
    }

    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                String responseJson;
                while (connected && (responseJson = in.readLine()) != null) {
                    handleRawResponse(responseJson);
                }
                if (connected) {
                    System.out.println(
                        "DEBUG: Server closed connection (EOF)."
                    );
                    this.connected = false;
                }
            } catch (IOException e) {
                System.err.println("Server connection lost: " + e.getMessage());
                this.connected = false;
            }
        });
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    @SuppressWarnings("unchecked")
    public <T> CompletableFuture<ResponseMessage<T>> sendRequest(
        RequestMessage<?> request,
        Class<T> responseClass
    ) {
        if (!connected) {
            CompletableFuture<ResponseMessage<T>> failed =
                new CompletableFuture<>();
            failed.completeExceptionally(
                new IOException("Not connected to server")
            );
            return failed;
        }

        String initialRequestId = request.getRequestId();
        if (initialRequestId == null || initialRequestId.isEmpty()) {
            initialRequestId = UUID.randomUUID().toString();
            request.setRequestId(initialRequestId);
        }
        final String requestId = initialRequestId;

        CompletableFuture<ResponseMessage<T>> future =
            new CompletableFuture<>();

        future.orTimeout(20, TimeUnit.SECONDS).whenComplete((res, ex) -> {
            if (ex instanceof java.util.concurrent.TimeoutException) {
                System.err.println(
                    "DEBUG: Request timed out for ID: " + requestId
                );
                pendingRequests.remove(requestId);
            }
        });

        pendingRequests.put(
            requestId,
            new PendingRequest<>(future, responseClass)
        );

        try {
            String json = JsonUtil.toJson(request);
            System.out.println(
                ">>> SENDING [" +
                    socket.getInetAddress() +
                    ":" +
                    socket.getPort() +
                    "]: " +
                    json
            );
            out.println(json);
        } catch (Exception e) {
            System.err.println("!!! SEND FAILURE: " + e.getMessage());
            pendingRequests.remove(requestId);
            future.completeExceptionally(e);
        }

        return future;
    }

    private void handleRawResponse(String json) {
        System.out.println("<<< RECEIVED: " + json);
        PendingRequest<?> pending = null;
        try {
            JsonNode root = JsonUtil.readTree(json);
            String requestId = JsonUtil.fieldText(root, "requestId");

            if (requestId != null) {
                pending = pendingRequests.remove(requestId);
                if (pending != null) {
                    ResponseMessage<?> response = JsonUtil.fromResponseJson(
                        json,
                        pending.responseClass
                    );
                    ((CompletableFuture) pending.future).complete(response);
                } else {
                    System.out.println(
                        "DEBUG: No pending request found for ID: " + requestId
                    );
                }
            } else {
                String typeText = JsonUtil.fieldText(root, "type");
                if (
                    typeText != null &&
                    !typeText.isBlank() &&
                    !"null".equalsIgnoreCase(typeText)
                ) {
                    MessageType pushType = MessageType.valueOf(typeText);
                    Consumer<String> handler = pushHandlers.get(pushType);
                    if (handler != null) handler.accept(json);
                }
            }
        } catch (Exception e) {
            System.err.println(
                "DEBUG: Error processing server response: " + e.getMessage()
            );
            if (pending != null) pending.future.completeExceptionally(e);
        }
    }

    public void registerPushHandler(
        MessageType type,
        Consumer<String> handler
    ) {
        pushHandlers.put(type, handler);
    }

    public boolean isConnected() {
        return connected;
    }
}
