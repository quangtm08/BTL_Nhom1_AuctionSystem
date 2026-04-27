package com.nhom1.auction.client.user.service;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.RequestMessage;
import com.nhom1.auction.common.utils.AppContext;
import java.util.concurrent.CompletableFuture;

public class AutoBidClientService extends BaseClientService {

    public CompletableFuture<AutoBidConfigResponse> saveConfig(
            String auctionId,
            double maxAmount,
            double increment
    ) {
        if (auctionId == null || auctionId.isBlank()) {
            return validationError("Auction ID is required");
        }
        if (AppContext.getCurrentUser() == null) {
            return validationError("Please sign in before setting auto bid");
        }
        if (maxAmount <= 0) {
            return validationError("Max amount must be > 0");
        }
        if (increment <= 0) {
            return validationError("Increment must be > 0");
        }
        if (maxAmount < increment) {
            return validationError("Max amount must be >= increment");
        }

        AutoBidConfigRequest payload = new AutoBidConfigRequest(
            auctionId,
            AppContext.getCurrentUser().getUserID(),
            maxAmount,
            increment
        );
        RequestMessage<AutoBidConfigRequest> request =
            new RequestMessage<>(MessageType.AUTO_BID_CONFIG, payload);

        return send(request, AutoBidConfigResponse.class);
    }
}
