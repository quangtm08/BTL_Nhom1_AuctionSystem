package com.nhom1.auction.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.nhom1.auction.common.dto.notification.AuctionTimeExtendedEvent;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class JsonUtilTest {

  @Test
  void fromJson_acceptsIsoAndSpaceSeparatedLocalDateTime() throws Exception {
    AuctionTimeExtendedEvent isoEvent =
        JsonUtil.fromJson(
            "{\"auctionId\":\"a1\",\"newEndTime\":\"2026-05-24T10:15:30\"}",
            AuctionTimeExtendedEvent.class);
    AuctionTimeExtendedEvent spaceEvent =
        JsonUtil.fromJson(
            "{\"auctionId\":\"a1\",\"newEndTime\":\"2026-05-24 10:15:30\"}",
            AuctionTimeExtendedEvent.class);

    LocalDateTime expected = LocalDateTime.of(2026, 5, 24, 10, 15, 30);
    assertEquals(expected, isoEvent.getNewEndTime());
    assertEquals(expected, spaceEvent.getNewEndTime());
  }

  @Test
  void responsePayload_parsesGenericResponseWithFlexibleDateTime() throws Exception {
    String json =
        """
        {
          "requestId": "r1",
          "success": true,
          "payload": {
            "auctionId": "a1",
            "newEndTime": "2026-05-24 10:15:30",
            "triggeredAt": "2026-05-24T10:00:00"
          }
        }
        """;

    AuctionTimeExtendedEvent event = JsonUtil.responsePayload(json, AuctionTimeExtendedEvent.class);

    assertEquals("a1", event.getAuctionId());
    assertEquals(LocalDateTime.of(2026, 5, 24, 10, 15, 30), event.getNewEndTime());
    assertEquals(LocalDateTime.of(2026, 5, 24, 10, 0), event.getTriggeredAt());
  }

  @Test
  void fieldHelpers_returnNullForMissingOrNullFields() throws Exception {
    JsonNode root = JsonUtil.readTree("{\"requestId\":null,\"payload\":{\"ok\":true}}");

    assertNull(JsonUtil.fieldText(root, "requestId"));
    assertNull(JsonUtil.fieldJson(root, "missing"));
    assertEquals("{\"ok\":true}", JsonUtil.fieldJson(root, "payload"));
  }
}
