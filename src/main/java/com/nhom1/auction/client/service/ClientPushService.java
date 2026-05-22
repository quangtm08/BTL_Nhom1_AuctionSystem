package com.nhom1.auction.client.service;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.dto.notification.AuctionDeletedEvent;
import com.nhom1.auction.common.dto.notification.AuctionEndedEvent;
import com.nhom1.auction.common.dto.notification.AuctionTimeExtendedEvent;
import com.nhom1.auction.common.dto.notification.BidUpdateEvent;
import com.nhom1.auction.common.dto.notification.NewAuctionEvent;
import com.nhom1.auction.common.dto.notification.UserCreatedEvent;
import com.nhom1.auction.common.dto.notification.UserDeletedEvent;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import java.util.function.Consumer;

public class ClientPushService {
    private static ClientPushService instance;

    private final ObjectMapper mapper = new ObjectMapper();
    private volatile Consumer<BidUpdateEvent> bidUpdateHandler;
    private volatile Consumer<NewAuctionEvent> newAuctionHandler;
    private volatile Consumer<AuctionDeletedEvent> auctionDeletedHandler;
    private volatile Consumer<AuctionEndedEvent> auctionEndedHandler;
    private volatile Consumer<AuctionTimeExtendedEvent> auctionTimeExtendedHandler;
    private volatile Consumer<UserDeletedEvent> userDeletedHandler;
    private volatile Consumer<UserCreatedEvent> userCreatedHandler;

    private ClientPushService() {
        mapper.registerModule(new JavaTimeModule());
        ServerConnection connection = ServerConnection.getInstance();
        connection.registerPushHandler(MessageType.PUSH_BID_UPDATE,
                json -> dispatch(json, BidUpdateEvent.class, bidUpdateHandler));
        connection.registerPushHandler(MessageType.PUSH_NEW_AUCTION,
                json -> dispatch(json, NewAuctionEvent.class, newAuctionHandler));
        connection.registerPushHandler(MessageType.PUSH_AUCTION_DELETED,
                json -> dispatch(json, AuctionDeletedEvent.class, auctionDeletedHandler));
        connection.registerPushHandler(MessageType.PUSH_AUCTION_ENDED,
                json -> dispatch(json, AuctionEndedEvent.class, auctionEndedHandler));
        connection.registerPushHandler(MessageType.PUSH_AUCTION_TIME_EXTENDED,
                json -> dispatch(json, AuctionTimeExtendedEvent.class, auctionTimeExtendedHandler));
        connection.registerPushHandler(MessageType.PUSH_USER_DELETED,
                json -> dispatch(json, UserDeletedEvent.class, userDeletedHandler));
        connection.registerPushHandler(MessageType.PUSH_USER_CREATED,
                json -> dispatch(json, UserCreatedEvent.class, userCreatedHandler));
    }

    public static synchronized ClientPushService getInstance() {
        if (instance == null) {
            instance = new ClientPushService();
        }
        return instance;
    }

    public static synchronized void clearHandlersIfInitialized() {
        if (instance != null) {
            instance.clearHandlers();
        }
    }

    public void onBidUpdate(Consumer<BidUpdateEvent> handler) {
        this.bidUpdateHandler = handler;
    }

    public void onNewAuction(Consumer<NewAuctionEvent> handler) {
        this.newAuctionHandler = handler;
    }

    public void onAuctionDeleted(Consumer<AuctionDeletedEvent> handler) {
        this.auctionDeletedHandler = handler;
    }

    public void onAuctionEnded(Consumer<AuctionEndedEvent> handler) {
        this.auctionEndedHandler = handler;
    }

    public void onAuctionTimeExtended(Consumer<AuctionTimeExtendedEvent> handler) {
        this.auctionTimeExtendedHandler = handler;
    }

    public void onUserDeleted(Consumer<UserDeletedEvent> handler) {
        this.userDeletedHandler = handler;
    }

    public void onUserCreated(Consumer<UserCreatedEvent> handler) {
        this.userCreatedHandler = handler;
    }

    private void clearHandlers() {
        bidUpdateHandler = null;
        newAuctionHandler = null;
        auctionDeletedHandler = null;
        auctionEndedHandler = null;
        auctionTimeExtendedHandler = null;
        userDeletedHandler = null;
        userCreatedHandler = null;
    }

    private <T> void dispatch(String json, Class<T> payloadClass, Consumer<T> handler) {
        if (handler == null) {
            return;
        }
        try {
            T event = readPayload(json, payloadClass);
            if (event == null) {
                return;
            }
            handler.accept(event);
        } catch (Exception e) {
            System.err.println("Error parsing push " + payloadClass.getSimpleName() + ": " + e.getMessage());
        }
    }

    private <T> T readPayload(String json, Class<T> payloadClass) throws Exception {
        JavaType responseType = mapper.getTypeFactory()
                .constructParametricType(ResponseMessage.class, payloadClass);
        ResponseMessage<T> response = mapper.readValue(json, responseType);
        return response.getPayload();
    }
}
