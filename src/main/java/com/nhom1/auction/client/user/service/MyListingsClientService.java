package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.auction.MyListingsResponse;
import com.nhom1.auction.common.dto.auth.AuthResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class MyListingsClientService extends BaseClientService {

  public CompletableFuture<MyListingsResponse> listMyListings() {
    AuthResponse user = AppContext.getCurrentUser();
    if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
      return validationError("No user session. Please sign in again.");
    }

    RequestMessage<Map<String, String>> request =
        new RequestMessage<>(MessageType.LIST_MY_LISTINGS, Map.of("sellerId", user.getUserID()));
    return send(request, MyListingsResponse.class);
  }

  public CompletableFuture<String> deleteListing(String auctionId) {
    if (auctionId == null || auctionId.isBlank()) {
      return validationError("Auction ID is required.");
    }

    AuthResponse user = AppContext.getCurrentUser();
    if (user == null || user.getUserID() == null || user.getUserID().isBlank()) {
      return validationError("No user session. Please sign in again.");
    }

    RequestMessage<Map<String, String>> request =
        new RequestMessage<>(
            MessageType.DELETE_AUCTION,
            Map.of("sellerId", user.getUserID(), "auctionId", auctionId));
    return send(request, String.class);
  }
}
