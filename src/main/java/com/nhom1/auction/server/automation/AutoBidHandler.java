package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.common.utils.JsonUtil;
import com.nhom1.auction.server.infrastructure.MessageRouter;

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
                return new ResponseMessage<>(requestId, response);
            } catch (IllegalArgumentException e) {
                return new ResponseMessage<>(requestId, "VALIDATION_ERROR", e.getMessage());
            } catch (Exception e) {
                return new ResponseMessage<>(requestId, "AUTO_BID_CONFIG_FAILED", e.getMessage());
            }
        });
    }
}
