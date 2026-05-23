package com.nhom1.auction.server.infrastructure;

import com.nhom1.auction.common.protocol.ResponseMessage;

@FunctionalInterface
public interface MessageRouteAction {
  // Contract: Take requestId and payload -> return Response object with the same Id and response
  // payload
  // Used by feature handler (E.g. AuthHanlder)
  ResponseMessage<?> execute(String requestId, String jsonPayload) throws Exception;
}
