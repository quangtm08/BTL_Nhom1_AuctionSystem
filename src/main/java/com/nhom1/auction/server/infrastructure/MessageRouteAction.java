package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.common.protocol.ResponseMessage;

@FunctionalInterface
public interface MessageRouteAction {
    //Take raw JSON and Jackson mapper -> return Response object
    ResponseMessage<?> execute(String requestId, String jsonPayload) throws Exception;
}
