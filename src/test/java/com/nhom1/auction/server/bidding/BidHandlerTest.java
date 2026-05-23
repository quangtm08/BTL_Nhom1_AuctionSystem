package com.nhom1.auction.server.bidding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.bidding.AuctionDetailDto;
import com.nhom1.auction.common.dto.bidding.ListAuctionsResponse;
import com.nhom1.auction.common.dto.bidding.MyBidsResponse;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.server.automation.AutoBidService;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class BidHandlerTest {

  private BidService bidService;
  private NotificationService notificationService;
  private AutoBidService autoBidService;
  private BidHandler handler;
  private MessageRouter router;

  @BeforeEach
  public void setUp() {
    bidService = mock(BidService.class);
    notificationService = mock(NotificationService.class);
    autoBidService = mock(AutoBidService.class);
    handler = new BidHandler(bidService, notificationService);
    router = new MessageRouter();
    handler.register(router);
  }

  // ======================== PLACE_BID ========================

  @Test
  public void testPlaceBid_Success_WithAutoBidService() throws Exception {
    UUID auctionId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    BidTransaction tx =
        new BidTransaction(
            UUID.randomUUID(), auctionId, bidderId, BigDecimal.TEN, BidType.MANUAL,
            LocalDateTime.now(), LocalDateTime.now());
    when(bidService.placeBid(any(), any(), any(), any())).thenReturn(tx);

    // Wire autoBidService so the "if (autoBidService != null)" branch is taken
    handler.setAutoBidService(autoBidService);

    String json =
        "{\"type\":\"PLACE_BID\",\"requestId\":\"r1\","
            + "\"payload\":{\"auctionId\":\""
            + auctionId
            + "\",\"bidderId\":\""
            + bidderId
            + "\",\"bidAmount\":50.0}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
    verify(autoBidService).scheduleAutoBids(any(), any(), any());
  }

  @Test
  public void testPlaceBid_Success_WithoutAutoBidService() throws Exception {
    UUID auctionId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    BidTransaction tx =
        new BidTransaction(
            UUID.randomUUID(), auctionId, bidderId, BigDecimal.TEN, BidType.MANUAL,
            LocalDateTime.now(), LocalDateTime.now());
    when(bidService.placeBid(any(), any(), any(), any())).thenReturn(tx);
    // autoBidService is null (not set) -> the null branch

    String json =
        "{\"type\":\"PLACE_BID\",\"requestId\":\"r1\","
            + "\"payload\":{\"auctionId\":\""
            + auctionId
            + "\",\"bidderId\":\""
            + bidderId
            + "\",\"bidAmount\":50.0}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
    verify(autoBidService, never()).scheduleAutoBids(any(), any(), any());
  }

  @Test
  public void testPlaceBid_InvalidUUID_ReturnsInvalidFormat() {
    // bidderId is not a valid UUID -> IllegalArgumentException branch
    String json =
        "{\"type\":\"PLACE_BID\",\"requestId\":\"r1\","
            + "\"payload\":{\"auctionId\":\"not-a-uuid\",\"bidderId\":\"also-not\","
            + "\"bidAmount\":50.0}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false"));
    assertTrue(result.contains("Invalid UUID") || result.contains("INVALID"));
  }

  @Test
  public void testPlaceBid_ServiceThrows_ReturnsError() throws Exception {
    when(bidService.placeBid(any(), any(), any(), any()))
        .thenThrow(new ValidationException("Bid too low"));
    UUID auctionId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();
    String json =
        "{\"type\":\"PLACE_BID\",\"requestId\":\"r1\","
            + "\"payload\":{\"auctionId\":\""
            + auctionId
            + "\",\"bidderId\":\""
            + bidderId
            + "\",\"bidAmount\":1.0}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false"));
  }

  @Test
  public void testPlaceBid_BadJson_ReturnsInvalidFormat() {
    String json = "{\"type\":\"PLACE_BID\",\"requestId\":\"r1\",\"payload\":\"bad-json\"}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false"));
  }

  // ======================== LIST_AUCTIONS ========================

  @Test
  public void testListAuctions_Success() throws Exception {
    ListAuctionsResponse response = new ListAuctionsResponse();
    response.setAuctions(List.of());
    when(bidService.listAllAuctions()).thenReturn(response);
    String json = "{\"type\":\"LIST_AUCTIONS\",\"requestId\":\"r2\",\"payload\":{}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
  }

  @Test
  public void testListAuctions_ServiceThrows_ReturnsError() throws Exception {
    when(bidService.listAllAuctions()).thenThrow(new RuntimeException("DB down"));
    String json = "{\"type\":\"LIST_AUCTIONS\",\"requestId\":\"r2\",\"payload\":{}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false"));
  }

  // ======================== GET_AUCTION_DETAIL ========================

  @Test
  public void testGetAuctionDetail_Success() throws Exception {
    UUID auctionId = UUID.randomUUID();
    when(bidService.getAuctionDetail(auctionId)).thenReturn(mock(AuctionDetailDto.class));
    String json =
        "{\"type\":\"GET_AUCTION_DETAIL\",\"requestId\":\"r3\","
            + "\"payload\":{\"auctionId\":\""
            + auctionId
            + "\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
  }

  @Test
  public void testGetAuctionDetail_InvalidUUID_ReturnsInvalidFormat() {
    String json =
        "{\"type\":\"GET_AUCTION_DETAIL\",\"requestId\":\"r3\","
            + "\"payload\":{\"auctionId\":\"bad-uuid\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false") || result.contains("INVALID"));
  }

  @Test
  public void testGetAuctionDetail_ServiceThrows_ReturnsError() throws Exception {
    UUID auctionId = UUID.randomUUID();
    when(bidService.getAuctionDetail(auctionId)).thenThrow(new RuntimeException("Not found"));
    String json =
        "{\"type\":\"GET_AUCTION_DETAIL\",\"requestId\":\"r3\","
            + "\"payload\":{\"auctionId\":\""
            + auctionId
            + "\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false"));
  }

  @Test
  public void testGetAuctionDetail_BadJson_ReturnsInvalidFormat() {
    String json =
        "{\"type\":\"GET_AUCTION_DETAIL\",\"requestId\":\"r3\",\"payload\":\"bad\"}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false"));
  }

  // ======================== LIST_MY_BIDS ========================

  @Test
  public void testListMyBids_Success() throws Exception {
    UUID bidderId = UUID.randomUUID();
    when(bidService.getMyBids(bidderId)).thenReturn(new MyBidsResponse(List.of()));
    String json =
        "{\"type\":\"LIST_MY_BIDS\",\"requestId\":\"r4\","
            + "\"payload\":{\"bidderId\":\""
            + bidderId
            + "\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
  }

  @Test
  public void testListMyBids_InvalidUUID_ReturnsInvalidFormat() {
    String json =
        "{\"type\":\"LIST_MY_BIDS\",\"requestId\":\"r4\","
            + "\"payload\":{\"bidderId\":\"not-uuid\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false") || result.contains("INVALID"));
  }

  @Test
  public void testListMyBids_ServiceThrows_ReturnsError() throws Exception {
    UUID bidderId = UUID.randomUUID();
    when(bidService.getMyBids(bidderId)).thenThrow(new RuntimeException("DB error"));
    String json =
        "{\"type\":\"LIST_MY_BIDS\",\"requestId\":\"r4\","
            + "\"payload\":{\"bidderId\":\""
            + bidderId
            + "\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false"));
  }
}
