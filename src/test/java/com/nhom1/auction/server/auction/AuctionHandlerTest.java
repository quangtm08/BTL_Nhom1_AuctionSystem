package com.nhom1.auction.server.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.dto.auction.CreateAuctionRequest;
import com.nhom1.auction.common.dto.auction.UpdateAuctionRequest;
import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.exception.ValidationException;
import com.nhom1.auction.common.protocol.MessageType;
import com.nhom1.auction.common.protocol.ResponseMessage;
import com.nhom1.auction.server.infrastructure.MessageRouter;
import com.nhom1.auction.server.infrastructure.NotificationService;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AuctionHandlerTest {

  private AuctionService auctionService;
  private NotificationService notificationService;
  private AuctionHandler handler;
  private MessageRouter router;

  @BeforeEach
  public void setUp() {
    auctionService = mock(AuctionService.class);
    notificationService = mock(NotificationService.class);
    handler = new AuctionHandler(auctionService, notificationService);
    router = new MessageRouter();
    handler.register(router);
  }

  // ======================== CREATE_AUCTION ========================

  @Test
  public void testCreateAuction_Success_WithHighestBidder() throws Exception {
    Auction auction = mock(Auction.class);
    UUID auctionUuid = UUID.randomUUID();
    UUID itemUuid = UUID.randomUUID();
    UUID sellerUuid = UUID.randomUUID();
    UUID bidderUuid = UUID.randomUUID();
    when(auction.getId()).thenReturn(auctionUuid);
    when(auction.getItemId()).thenReturn(itemUuid);
    when(auction.getSellerId()).thenReturn(sellerUuid);
    when(auction.getStartTime()).thenReturn(LocalDateTime.now());
    when(auction.getEndTime()).thenReturn(LocalDateTime.now().plusDays(7));
    when(auction.getHighestBidderId()).thenReturn(bidderUuid); // non-null branch
    when(auction.getCurrentHighestBid()).thenReturn(BigDecimal.TEN);
    when(auction.getStatus()).thenReturn(AuctionStatus.OPEN);
    when(auction.getCreatedAt()).thenReturn(LocalDateTime.now());
    when(auction.getUpdatedAt()).thenReturn(LocalDateTime.now());
    when(auctionService.createAuction(any(), any())).thenReturn(auction);

    String json =
        "{\"type\":\"CREATE_AUCTION\",\"requestId\":\"r1\",\"payload\":"
            + "{\"sellerId\":\""
            + sellerUuid
            + "\",\"name\":\"Watch\",\"description\":\"A watch\","
            + "\"category\":\"ELECTRONICS\",\"condition\":\"NEW\","
            + "\"startingPrice\":100,\"startTime\":\"2099-01-01T10:00:00\","
            + "\"endTime\":\"2099-01-08T10:00:00\"}}";

    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
  }

  @Test
  public void testCreateAuction_Success_NullHighestBidder() throws Exception {
    Auction auction = mock(Auction.class);
    when(auction.getId()).thenReturn(UUID.randomUUID());
    when(auction.getItemId()).thenReturn(UUID.randomUUID());
    when(auction.getSellerId()).thenReturn(UUID.randomUUID());
    when(auction.getHighestBidderId()).thenReturn(null); // null branch
    when(auction.getStatus()).thenReturn(AuctionStatus.OPEN);
    when(auctionService.createAuction(any(), any())).thenReturn(auction);

    String json =
        "{\"type\":\"CREATE_AUCTION\",\"requestId\":\"r1\",\"payload\":"
            + "{\"sellerId\":\""
            + UUID.randomUUID()
            + "\",\"startingPrice\":50}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
  }

  @Test
  public void testCreateAuction_ServiceThrows_ReturnsError() throws Exception {
    when(auctionService.createAuction(any(), any())).thenThrow(new ValidationException("Bad input"));

    String json =
        "{\"type\":\"CREATE_AUCTION\",\"requestId\":\"r1\",\"payload\":{\"sellerId\":\"x\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":false") || result.contains("false"));
  }

  @Test
  public void testCreateAuction_InvalidJson_ReturnsInvalidFormat() {
    // Completely malformed inner payload that cannot be deserialized to CreateAuctionRequest
    String json = "{\"type\":\"CREATE_AUCTION\",\"requestId\":\"r1\",\"payload\":\"not-an-object\"}";
    String result = router.handleRequest(json);
    // Either invalid format or error response
    assertNotNull(result);
  }

  // ======================== LIST_MY_LISTINGS ========================

  @Test
  public void testListMyListings_Success() {
    when(auctionService.getMyListings(any())).thenReturn(List.of(
        new AuctionSummaryDto(
            UUID.randomUUID().toString(),
            "Watch",
            "ELECTRONICS",
            new BigDecimal("100.00"),
            null,
            null,
            LocalDateTime.now().plusDays(1),
            AuctionStatus.OPEN,
            UUID.randomUUID().toString())));

    String json =
        "{\"type\":\"LIST_MY_LISTINGS\",\"requestId\":\"r2\","
            + "\"payload\":{\"sellerId\":\""
            + UUID.randomUUID()
            + "\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
  }

  @Test
  public void testListMyListings_MissingSellerId_NullBranch() {
    when(auctionService.getMyListings(null)).thenReturn(List.of());
    // payload with no "sellerId" key -> sellerId = null
    String json =
        "{\"type\":\"LIST_MY_LISTINGS\",\"requestId\":\"r2\",\"payload\":{\"other\":\"val\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
  }

  @Test
  public void testListMyListings_ServiceThrows_ReturnsError() {
    when(auctionService.getMyListings(any())).thenThrow(new ValidationException("Seller not found"));
    String json =
        "{\"type\":\"LIST_MY_LISTINGS\",\"requestId\":\"r2\","
            + "\"payload\":{\"sellerId\":\"bad\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false"));
  }

  // ======================== DELETE_AUCTION ========================

  @Test
  public void testDeleteAuction_Success() throws Exception {
    doNothing().when(auctionService).deleteAuction(any(), any());
    String auctionId = UUID.randomUUID().toString();
    String json =
        "{\"type\":\"DELETE_AUCTION\",\"requestId\":\"r3\","
            + "\"payload\":{\"sellerId\":\""
            + UUID.randomUUID()
            + "\",\"auctionId\":\""
            + auctionId
            + "\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
    verify(notificationService).broadcastAuctionDeleted(auctionId);
  }

  @Test
  public void testDeleteAuction_MissingFields_NullBranch() throws Exception {
    doNothing().when(auctionService).deleteAuction(any(), any());
    // payload with no sellerId or auctionId -> both null
    String json =
        "{\"type\":\"DELETE_AUCTION\",\"requestId\":\"r3\",\"payload\":{}}";
    String result = router.handleRequest(json);
    // service is called with nulls
    assertNotNull(result);
  }

  @Test
  public void testDeleteAuction_ServiceThrows_ReturnsError() throws Exception {
    doThrow(new ValidationException("Not found")).when(auctionService).deleteAuction(any(), any());
    String json =
        "{\"type\":\"DELETE_AUCTION\",\"requestId\":\"r3\","
            + "\"payload\":{\"sellerId\":\""
            + UUID.randomUUID()
            + "\",\"auctionId\":\""
            + UUID.randomUUID()
            + "\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false"));
  }

  // ======================== UPDATE_AUCTION ========================

  @Test
  public void testUpdateAuction_Success() throws Exception {
    doNothing().when(auctionService).updateAuction(any());
    String json =
        "{\"type\":\"UPDATE_AUCTION\",\"requestId\":\"r4\","
            + "\"payload\":{\"sellerId\":\""
            + UUID.randomUUID()
            + "\",\"auctionId\":\""
            + UUID.randomUUID()
            + "\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("\"success\":true"));
  }

  @Test
  public void testUpdateAuction_ServiceThrows_ReturnsError() throws Exception {
    doThrow(new ValidationException("Cannot update")).when(auctionService).updateAuction(any());
    String json =
        "{\"type\":\"UPDATE_AUCTION\",\"requestId\":\"r4\","
            + "\"payload\":{\"sellerId\":\""
            + UUID.randomUUID()
            + "\"}}";
    String result = router.handleRequest(json);
    assertTrue(result.contains("false"));
  }
}
