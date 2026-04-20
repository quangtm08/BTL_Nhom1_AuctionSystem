package com.nhom1.auction.server;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nhom1.auction.common.dto.auth.LoginRequest;
import com.nhom1.auction.common.dto.auth.RegisterRequest;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.protocol.ErrorResponse;
import com.nhom1.auction.server.handler.AuthHandler;

/*
 - The central switchboard of the server.
 - It receives raw JSON strings from the network, parses them, and routes them to the appropriate handler.
 */

public class MessageRouter {
    private final ObjectMapper objectMapper;
    private final AuthHandler authHandler;

    public MessageRouter(AuthHandler authHandler) {
        this.authHandler = authHandler;
        this.objectMapper = new ObjectMapper();
        // Required to handle LocalDateTime in entities/DTOs
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Primary entry point for all incoming messages.
     * parameter json The raw JSON string from the client.
     * return a JSON string response to be sent back.
     */
    public String handleRequest(String json) {
        try {
            // 1. Look at the message type without full deserialization
            JsonNode rootNode = objectMapper.readTree(json);
            //Error when there is no type
            if (!rootNode.has("type")) {
                return serializeError("INVALID_FORMAT", "Missing message type", null);
            }

            String typeStr = rootNode.get("type").asText();
            MessageType type = MessageType.valueOf(typeStr);
            String requestId = rootNode.has("requestId") ? rootNode.get("requestId").asText() : null;

            // 2. Route based on type
            switch (type) {
                case LOGIN:
                    RequestMessage<LoginRequest> loginReq = objectMapper.readValue(
                        json, new TypeReference<RequestMessage<LoginRequest>>() {}
                    );
                    return objectMapper.writeValueAsString(authHandler.handleLogin(loginReq));

                case REGISTER:
                    RequestMessage<RegisterRequest> registerReq = objectMapper.readValue(
                        json, new TypeReference<RequestMessage<RegisterRequest>>() {}
                    );
                    return objectMapper.writeValueAsString(authHandler.handleRegister(registerReq));

                default:
                    return serializeError("UNKNOWN_TYPE", "Unsupported message type: " + type, requestId);
            }
        } catch (IllegalArgumentException e) {
            return serializeError("INVALID_TYPE", "Unknown MessageType in request", null);
        } catch (Exception e) {
            e.printStackTrace();
            return serializeError("SERVER_ERROR", "Internal processing error: " + e.getMessage(), null);
        }
    }

    //Helper to create a standardized JSON error string if parsing fails early.

    private String serializeError(String code, String message, String requestId) {
        try {
            ResponseMessage<Object> errorResponse = new ResponseMessage<>(requestId, code, message);
            return objectMapper.writeValueAsString(errorResponse);
        } catch (Exception e) {
            return "{\"success\": false, \"error\": {\"code\": \"CRITICAL_ERROR\", \"message\": \"Failed to serialize error\"}}";
        }
    }
}
