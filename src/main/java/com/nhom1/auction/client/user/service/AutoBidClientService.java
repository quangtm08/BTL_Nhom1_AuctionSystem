package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigDetailResponse;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.dto.autobid.DeleteAutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.GetAutoBidConfigRequest;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

public class AutoBidClientService extends BaseClientService {

  public CompletableFuture<AutoBidConfigResponse> saveConfig(
      String auctionId, BigDecimal maxAmount, BigDecimal increment) {
    if (auctionId == null || auctionId.isBlank()) {
      return validationError("Auction ID is required");
    }
    if (AppContext.getCurrentUser() == null) {
      return validationError("Please sign in before setting auto bid");
    }
    if (maxAmount == null || maxAmount.compareTo(BigDecimal.ZERO) <= 0) {
      return validationError("Max amount must be > 0");
    }
    if (increment == null || increment.compareTo(BigDecimal.ZERO) <= 0) {
      return validationError("Increment must be > 0");
    }
    if (maxAmount.compareTo(increment) < 0) {
      return validationError("Max amount must be >= increment");
    }

    AutoBidConfigRequest payload =
        new AutoBidConfigRequest(
            auctionId,
            AppContext.getCurrentUser().getUserID(),
            maxAmount.toPlainString(),
            increment.toPlainString());
    RequestMessage<AutoBidConfigRequest> request =
        new RequestMessage<>(MessageType.AUTO_BID_CONFIG, payload);

    return send(request, AutoBidConfigResponse.class);
  }

  public CompletableFuture<AutoBidConfigDetailResponse> getConfig(String auctionId) {
    if (auctionId == null || auctionId.isBlank()) {
      return validationError("Auction ID is required");
    }
    if (AppContext.getCurrentUser() == null) {
      return validationError("Please sign in before getting auto bid config");
    }

    GetAutoBidConfigRequest payload =
        new GetAutoBidConfigRequest(auctionId, AppContext.getCurrentUser().getUserID());
    RequestMessage<GetAutoBidConfigRequest> request =
        new RequestMessage<>(MessageType.GET_AUTO_BID_CONFIG, payload);
    return send(request, AutoBidConfigDetailResponse.class);
  }

  public CompletableFuture<AutoBidConfigResponse> deleteConfig(String auctionId) {
    if (auctionId == null || auctionId.isBlank()) {
      return validationError("Auction ID is required");
    }
    if (AppContext.getCurrentUser() == null) {
      return validationError("Please sign in before deleting auto bid config");
    }

    DeleteAutoBidConfigRequest payload =
        new DeleteAutoBidConfigRequest(auctionId, AppContext.getCurrentUser().getUserID());
    RequestMessage<DeleteAutoBidConfigRequest> request =
        new RequestMessage<>(MessageType.DELETE_AUTO_BID_CONFIG, payload);
    return send(request, AutoBidConfigResponse.class);
  }
}
