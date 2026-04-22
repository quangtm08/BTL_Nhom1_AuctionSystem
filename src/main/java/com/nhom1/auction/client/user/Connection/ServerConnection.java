package com.nhom1.auction.client.user.connection;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ServerConnection: The "Bridge" between the Client and the Server.
 * 
 * It manages the persistent Socket connection and handles JSON deserialization
 * using specific blueprints (JavaType) to avoid the LinkedHashMap issue.
 */
public class ServerConnection {
    private static ServerConnection instance;

    private final ObjectMapper mapper = new ObjectMapper();
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;

    // Helper to store the box and the blueprint
    private static class PendingRequest<T> {
        final CompletableFuture<ResponseMessage<T>> future;
        final Class<T> responseClass;

        PendingRequest(CompletableFuture<ResponseMessage<T>> future, Class<T> responseClass) {
            this.future = future;
            this.responseClass = responseClass;
        }
    }

    private final Map<String, PendingRequest<?>> pendingRequests = new ConcurrentHashMap<>();

    private ServerConnection() {
        // Ensure Jackson understands Java 8 dates (LocalDateTime)
        mapper.registerModule(new JavaTimeModule());
        connect();
    }

    public static synchronized ServerConnection getInstance() {
        if (instance == null) {
            instance = new ServerConnection();
        }
        return instance;
    }

    private void connect() {
        try {
            this.socket = new Socket("localhost", 12345);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            this.connected = true;
            System.out.println("Connected to Auction Server.");
            startListening();
        } catch (IOException e) {
            System.err.println("Could not connect to server: " + e.getMessage());
            this.connected = false;
        }
    }

    private void startListening() {
        Thread listenerThread = new Thread(() -> {
            try {
                String responseJson;
                while (connected && (responseJson = in.readLine()) != null) {
                    handleRawResponse(responseJson);
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
    public <T> CompletableFuture<ResponseMessage<T>> sendRequest(RequestMessage<?> request, Class<T> responseClass) {
        if (!connected) {
            CompletableFuture<ResponseMessage<T>> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IOException("Not connected to server"));
            return failed;
        }

        String requestId = request.getRequestId();
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
            request.setRequestId(requestId);
        }

        CompletableFuture<ResponseMessage<T>> future = new CompletableFuture<>();
        pendingRequests.put(requestId, new PendingRequest<>(future, responseClass));

        try {
            out.println(JsonUtil.toJson(request));
        } catch (Exception e) {
            pendingRequests.remove(requestId);
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * THE CLEAN FIX: Tell Jackson exactly what type T is BEFORE it reads the JSON.
     */
    private void handleRawResponse(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String requestId = root.has("requestId") ? root.get("requestId").asText() : null;

            if (requestId == null) return;

            PendingRequest<?> pending = pendingRequests.remove(requestId);
            if (pending != null) {
                // Construct the full type: ResponseMessage<pending.responseClass>
                JavaType type = mapper.getTypeFactory()
                                      .constructParametricType(ResponseMessage.class, pending.responseClass);
                
                // Jackson builds the AuthResponse (or other class) directly!
                ResponseMessage<?> response = mapper.readValue(json, type);

                // Use raw cast to resolve the promise safely
                ((CompletableFuture) pending.future).complete(response);
            }
        } catch (Exception e) {
            System.err.println("Error parsing server response: " + e.getMessage());
        }
    }

    public boolean isConnected() {
        return connected;
    }
}
