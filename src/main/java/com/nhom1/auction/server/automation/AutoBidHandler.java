package com.nhom1.auction.server.automation;

import com.nhom1.auction.common.dto.autobid.AutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigDetailResponse;
import com.nhom1.auction.common.dto.autobid.AutoBidConfigResponse;
import com.nhom1.auction.common.dto.autobid.DeleteAutoBidConfigRequest;
import com.nhom1.auction.common.dto.autobid.GetAutoBidConfigRequest;
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

        router.register(MessageType.GET_AUTO_BID_CONFIG, (requestId, payloadJson) -> {
            try {
                GetAutoBidConfigRequest dto = JsonUtil.fromJson(payloadJson, GetAutoBidConfigRequest.class);
                AutoBidConfigDetailResponse response = autoBidService.getConfig(dto.getAuctionId(), dto.getBidderId());
                return ResponseFactory.success(requestId, response);
            } catch (Exception e) {
                return ResponseFactory.fromException(requestId, e);
            }
        });

        router.register(MessageType.DELETE_AUTO_BID_CONFIG, (requestId, payloadJson) -> {
            try {
                DeleteAutoBidConfigRequest dto = JsonUtil.fromJson(payloadJson, DeleteAutoBidConfigRequest.class);
                AutoBidConfigResponse response = autoBidService.deleteConfig(dto.getAuctionId(), dto.getBidderId());
                return ResponseFactory.success(requestId, response);
            } catch (Exception e) {
                return ResponseFactory.fromException(requestId, e);
            }
        });
    }
}
