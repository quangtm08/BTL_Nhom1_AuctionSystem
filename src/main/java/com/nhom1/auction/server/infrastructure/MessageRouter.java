package com.nhom1.auction.server.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;

import java.util.EnumMap;
import java.util.Map;

// It identifies the message type and hands it to the registered Action.

public class MessageRouter {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Maps a MessageType to a specific piece of logic (Action).
    private final Map<MessageType, MessageRouteAction> routes = new EnumMap<>(MessageType.class);

    // Allows Handlers to sign up for specific message types.
    public void register(MessageType type, MessageRouteAction action) {
        routes.put(type, action);
        System.out.println("Router: Registered handler for " + type);
    }


     //Primary entry point. Takes raw JSON, returns raw JSON
    public String handleRequest(String json) {
        String requestId = null;
        try {
            // 1. Peek at the JSON to get the 'Envelope' info
            JsonNode rootNode = objectMapper.readTree(json);
            
            // Validate type
            if (!rootNode.has("type")) {
                return serializeError(null, "INVALID_FORMAT", "Missing message type");
            }

            MessageType type = MessageType.valueOf(rootNode.get("type").asText());
            requestId = rootNode.has("requestId") ? rootNode.get("requestId").asText() : null;

            // 2. Extract the inner 'payload' as a raw string (The Goods)
            // We use .toString() to keep it as JSON text for the Handler to unpack later.
            String payloadJson = rootNode.has("payload") ? rootNode.get("payload").toString() : null;

            // 3. Find the registered Action
            MessageRouteAction action = routes.get(type);
            if (action == null) {
                return serializeError(requestId, "UNKNOWN_TYPE", "No handler registered for: " + type);
            }

            // 4. Execute
            ResponseMessage<?> response = action.execute(requestId, payloadJson);

            // 5. Translate back: Turn the Object into a String to send over the socket
            return JsonUtil.toJson(response);

        } catch (IllegalArgumentException e) {
            return serializeError(requestId, "INVALID_TYPE", "Unknown message type");
        } catch (Exception e) {
            e.printStackTrace();
            return serializeError(requestId, "SERVER_ERROR", "Internal error: " + e.getMessage());
        }
    }

    // Helper to create a standardized error JSON string.
    private String serializeError(String requestId, String code, String message) {
        try {
            ResponseMessage<Object> error = new ResponseMessage<>(requestId, code, message);
            return JsonUtil.toJson(error);
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"Critical serialization error\"}";
        }
    }
}
