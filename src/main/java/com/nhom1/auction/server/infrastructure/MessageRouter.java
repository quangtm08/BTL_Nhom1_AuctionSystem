package com.nhom1.auction.server.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nhom1.auction.common.protocol.ErrorCode;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;

import java.util.EnumMap;
import java.util.Map;

//Identify MessageType from the JSON request -> route the message to the right feature handler
public class MessageRouter {
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    // Maps a MessageType to a specific piece of logic (Action) executed by the feature handler.
    private final Map<MessageType, MessageRouteAction> routes = new EnumMap<>(MessageType.class);

    // Allows feature handlers to sign up for specific message types (stored in the map).
    public void register(MessageType type, MessageRouteAction action) {
        routes.put(type, action);
        System.out.println("Router: Registered handler for " + type);
    }


    //ClientHandler calls this method
    //Take in the request JSON -> call the right feature handler -> get response JSON -> send back to ClientHandler
    public String handleRequest(String json) {
        String requestId = null;
        try {
            // 1. Peek at the JSON to get the 'Envelope' info
            JsonNode rootNode = objectMapper.readTree(json);
            
            if (!rootNode.has("type")) {
                return serializeError(null, "INVALID_FORMAT", "Missing message type");
            }

            //Get the type and requestId from the JSON request
            MessageType type = MessageType.valueOf(rootNode.get("type").asText());
            requestId = rootNode.has("requestId") ? rootNode.get("requestId").asText() : null;

            // 2. Extract the inner payload as a raw JSON string
            String payloadJson = rootNode.has("payload") ? rootNode.get("payload").toString() : null;

            // 3. Find the registered Action
            MessageRouteAction action = routes.get(type);
            if (action == null) {
                return serializeError(requestId, "UNKNOWN_TYPE", "No handler registered for: " + type);
            }
            // 4. If exists, execute
            ResponseMessage<?> response = action.execute(requestId, payloadJson);
            if (response != null && response.getType() == null) {
                response.setType(type);
            }

            // 5. Translate back: Turn the Object into a String to send over the socket.
            // Use common/utils/JsonUtil (to simplify and hide Jackson library)
            return JsonUtil.toJson(response);

        } catch (JsonProcessingException e) {
            return serializeError(requestId, ErrorCode.INVALID_FORMAT, "Malformed JSON request");
        } catch (IllegalArgumentException e) {
            return serializeError(requestId, "INVALID_TYPE", "Unknown message type");
        } catch (Exception e) {
            return serializeError(requestId, ErrorCode.SERVER_ERROR, "Internal error: " + e.getMessage());
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
