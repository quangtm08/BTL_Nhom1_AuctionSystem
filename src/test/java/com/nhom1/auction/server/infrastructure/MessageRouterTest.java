package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class MessageRouterTest {

    private MessageRouter messageRouter;

    @Mock
    private MessageRouteAction mockAction;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        messageRouter = new MessageRouter();
    }

    @Test
    public void testHandleRequest_ValidJsonWithRegisteredType_RoutesToHandlerReturnsResponse() throws Exception {
        MessageType type = MessageType.LOGIN;
        messageRouter.register(type, mockAction);
        String json = "{\"type\":\"LOGIN\",\"requestId\":\"123\",\"payload\":{\"username\":\"user\"}}";
        ResponseMessage<String> response = new ResponseMessage<>("123", "Logged in");
        when(mockAction.execute("123", "{\"username\":\"user\"}")).thenReturn((ResponseMessage) response);

        String result = messageRouter.handleRequest(json);

        assertTrue(result.contains("\"success\":true") || result.contains("\"success\": true") || result.contains("true"));
        verify(mockAction).execute("123", "{\"username\":\"user\"}");
    }

    @Test
    public void testHandleRequest_JsonWithUnknownType_ReturnsErrorResponse() {
        String json = "{\"type\":\"UNKNOWN\",\"requestId\":\"123\"}";

        String result = messageRouter.handleRequest(json);

        assertTrue(result.contains("INVALID_TYPE"));
    }

    @Test
    public void testHandleRequest_JsonMissingTypeField_ReturnsMissingMessageTypeError() {
        String json = "{\"requestId\":\"123\"}";

        String result = messageRouter.handleRequest(json);

        assertTrue(result.contains("Missing message type"));
    }

    @Test
    public void testHandleRequest_MalformedJson_ReturnsErrorResponse() {
        String json = "{invalid json}";

        String result = messageRouter.handleRequest(json);

        assertTrue(result.contains("INVALID_FORMAT"));
    }
}
