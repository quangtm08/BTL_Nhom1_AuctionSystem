package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.common.dto.notification.AuctionEndedEvent;
import com.nhom1.auction.common.dto.notification.AuctionDeletedEvent;
import com.nhom1.auction.common.dto.notification.BidUpdateEvent;
import com.nhom1.auction.common.dto.notification.NewAuctionEvent;
import com.nhom1.auction.common.dto.notification.UserCreatedEvent;
import com.nhom1.auction.common.dto.notification.UserDeletedEvent;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class NotificationService {

    private final ClientRegistry clientRegistry;

    public NotificationService(ClientRegistry clientRegistry) {
        this.clientRegistry = clientRegistry;
    }

    //Broadcast to all users when a new bid appears
    public void broadcastBidUpdate(
        UUID auctionId,
        BigDecimal newHighestBid,
        UUID newHighestBidderId
    ) {
        BidUpdateEvent event = new BidUpdateEvent(
            auctionId.toString(),
            newHighestBid,
            newHighestBidderId != null ? newHighestBidderId.toString() : null,
            LocalDateTime.now().toString()
        );
        sendPush(MessageType.PUSH_BID_UPDATE, event);
    }

    public void broadcastAuctionEnded(
        UUID auctionId,
        UUID winnerId,
        BigDecimal finalPrice
    ) {
        AuctionEndedEvent event = new AuctionEndedEvent(
            auctionId.toString(),
            winnerId != null ? winnerId.toString() : null,
            finalPrice
        );
        sendPush(MessageType.PUSH_AUCTION_ENDED, event);
    }

    public void broadcastNewAuction(
        String auctionId,
        String itemName,
        BigDecimal startingPrice
    ) {
        NewAuctionEvent event = new NewAuctionEvent(
            auctionId,
            itemName,
            startingPrice
        );
        sendPush(MessageType.PUSH_NEW_AUCTION, event);
    }

    public void broadcastAuctionDeleted(String auctionId) {
        AuctionDeletedEvent payload = new AuctionDeletedEvent(auctionId);
        sendPush(MessageType.PUSH_AUCTION_DELETED, payload);
    }

    public void broadcastUserDeleted(String userId) {
        UserDeletedEvent payload = new UserDeletedEvent(userId);
        sendPush(MessageType.PUSH_USER_DELETED, payload);
    }

    public void broadcastUserCreated(String userId, String username, String email) {
        UserCreatedEvent payload = new UserCreatedEvent(userId, username, email);
        sendPush(MessageType.PUSH_USER_CREATED, payload);
    }

    private void sendPush(MessageType type, Object payload) {
        try {
            ResponseMessage<Object> responseMessage = new ResponseMessage<>();
            responseMessage.setSuccess(true);
            responseMessage.setType(type);
            responseMessage.setPayload(payload);

            String json = JsonUtil.toJson(responseMessage);
            clientRegistry.broadcast(json);
        } catch (Exception e) {
            System.err.println(
                "[NotificationService] Error sending push notification: " +
                    e.getMessage()
            );
            e.printStackTrace();
        }
    }
}
