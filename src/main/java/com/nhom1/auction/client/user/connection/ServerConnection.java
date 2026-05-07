package com.nhom1.auction.client.user.connection;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;

/*
 - Bridge between the Client and the Server.
 - It manages the persistent Socket connection and handles JSON deserialization
 */
public class ServerConnection {

    private static ServerConnection instance;

    private final ObjectMapper mapper = new ObjectMapper();
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
        DateTimeFormatter flexibleDateTimeFormatter =
            new DateTimeFormatterBuilder()
                .appendPattern("yyyy-MM-dd")
                .optionalStart()
                .appendLiteral('T')
                .optionalEnd()
                .optionalStart()
                .appendLiteral(' ')
                .optionalEnd()
                .appendPattern("HH:mm:ss")
                .optionalStart()
                .appendFraction(ChronoField.NANO_OF_SECOND, 0, 9, true)
                .optionalEnd()
                .toFormatter();

        JavaTimeModule javaTimeModule = new JavaTimeModule();
        javaTimeModule.addDeserializer(
            LocalDateTime.class,
            new LocalDateTimeDeserializer(flexibleDateTimeFormatter)
        );
        mapper.registerModule(javaTimeModule);
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
            // --- CONNECTION SETTINGS ---
            // String host = "localhost"; int port = 12345; // LOCAL
            String host = "autorack.proxy.rlwy.net"; int port = 17896; // CLOUD (Railway)
            // ---------------------------

            System.out.println(
                "Server: Connecting to " + host + ":" + port + "..."
            );
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(
                new InputStreamReader(socket.getInputStream())
            );
            this.connected = true;
            System.out.println("Connected to Auction Server.");
            startListening();
        } catch (IOException e) {
            System.err.println(
                "Could not connect to server: " + e.getMessage()
            );
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

        String requestId = request.getRequestId();
        if (requestId == null || requestId.isEmpty()) {
            requestId = UUID.randomUUID().toString();
            request.setRequestId(requestId);
        }

        CompletableFuture<ResponseMessage<T>> future =
            new CompletableFuture<>();
        pendingRequests.put(
            requestId,
            new PendingRequest<>(future, responseClass)
        );

        try {
            out.println(JsonUtil.toJson(request));
        } catch (Exception e) {
            pendingRequests.remove(requestId);
            future.completeExceptionally(e);
        }

        return future;
    }

    private void handleRawResponse(String json) {
        try {
            JsonNode root = mapper.readTree(json);
            String requestId =
                root.has("requestId") && !root.get("requestId").isNull()
                    ? root.get("requestId").asText()
                    : null;

            if (requestId != null) {
                PendingRequest<?> pending = pendingRequests.remove(requestId);
                if (pending != null) {
                    JavaType type = mapper
                        .getTypeFactory()
                        .constructParametricType(
                            ResponseMessage.class,
                            pending.responseClass
                        );
                    ResponseMessage<?> response = mapper.readValue(json, type);
                    ((CompletableFuture) pending.future).complete(response);
                }
            } else if (root.has("type")) {
                // Push notification
                MessageType pushType = MessageType.valueOf(
                    root.get("type").asText()
                );
                Consumer<String> handler = pushHandlers.get(pushType);
                if (handler != null) {
                    handler.accept(json);
                }
            }
        } catch (Exception e) {
            System.err.println(
                "Error parsing server response: " + e.getMessage()
            );
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
