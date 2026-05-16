package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.ResponseFactory;

public class AutoBidHandler {
    private final AutoBidService autoBidService;

    public AutoBidHandler(AutoBidService autoBidService) {
        this.autoBidService = autoBidService;
    }

    public void register(MessageRouter router) {
        router.register(MessageType.AUTO_BID_CONFIG, (requestId, payloadJson) -> {
            try {
                AutoBidConfigRequest dto = JsonUtil.fromJson(payloadJson, AutoBidConfigRequest.class);
                AutoBidConfigResponse response = autoBidService.saveConfig(dto);
                return ResponseFactory.success(requestId, response);
            } catch (Exception e) {
                return ResponseFactory.fromException(requestId, e);
            }
        });
    }
}
