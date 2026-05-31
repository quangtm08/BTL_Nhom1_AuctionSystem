package com.nhom1.auction.common.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.BidType;
import com.nhom1.auction.common.exception.AuctionClosedException;
import com.nhom1.auction.common.exception.InvalidBidException;
import com.nhom1.auction.common.exception.UnauthorizedActionException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class AuctionTest {

  @Test
  public void testConstructor_NullItemId_Throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                null,
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)));
  }

  @Test
  public void testConstructor_NullSellerId_Throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                UUID.randomUUID(),
                null,
                new BigDecimal("100.00"),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)));
  }

  @Test
  public void testConstructor_NullStartingPrice_Throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                null,
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)));
  }

  @Test
  public void testConstructor_NegativeStartingPrice_Throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("-10.00"),
                LocalDateTime.now(),
                LocalDateTime.now().plusHours(1)));
  }

  @Test
  public void testConstructor_NullStartTime_Throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                null,
                LocalDateTime.now().plusHours(1)));
  }

  @Test
  public void testConstructor_NullEndTime_Throws() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                LocalDateTime.now(),
                null));
  }

  @Test
  public void testConstructor_EndTimeBeforeStartTime_Throws() {
    LocalDateTime now = LocalDateTime.now();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                now,
                now.minusHours(1)));
  }

  @Test
  public void testConstructor_EndTimeEqualStartTime_Throws() {
    LocalDateTime now = LocalDateTime.now();
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Auction(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("100.00"), now, now));
  }

  @Test
  public void testConstructor_ValidArgs_CreatesOpenAuction() {
    UUID itemId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    BigDecimal startingPrice = new BigDecimal("100.00");
    LocalDateTime startTime = LocalDateTime.now();
    LocalDateTime endTime = startTime.plusHours(1);

    Auction auction = new Auction(itemId, sellerId, startingPrice, startTime, endTime);

    assertEquals(itemId, auction.getItemId());
    assertEquals(sellerId, auction.getSellerId());
    assertEquals(startingPrice, auction.getStartingPrice());
    assertEquals(startTime, auction.getStartTime());
    assertEquals(endTime, auction.getEndTime());
    assertEquals(AuctionStatus.OPEN, auction.getStatus());
    assertNull(auction.getHighestBidderId());
    assertNull(auction.getCurrentHighestBid());
  }

  @Test
  public void testGetMinBidIncrement_ReturnsCorrectValue() {
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1));

    BigDecimal minIncrement = auction.getMinBidIncrement();

    assertEquals(new BigDecimal("5.00"), minIncrement);
  }

  @Test
  public void testExtendEndTime_UpdatesEndTime() {
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1));
    LocalDateTime newEndTime = LocalDateTime.now().plusHours(2);

    auction.extendEndTime(newEndTime);

    assertEquals(newEndTime, auction.getEndTime());
  }

  @Test
  public void testPlaceBid_Success() {
    Auction auction = createRunningAuction();
    UUID bidderId = UUID.randomUUID();
    LocalDateTime bidTime = auction.getStartTime().plusMinutes(10);

    BidTransaction bidTransaction =
        auction.placeBid(bidderId, new BigDecimal("100.00"), BidType.MANUAL, bidTime);

    assertNotNull(bidTransaction, "The created bid should not be null");
    assertEquals(bidderId, auction.getHighestBidderId(), "The highest bidder should be updated");
    assertEquals(
        new BigDecimal("100.00"),
        auction.getCurrentHighestBid(),
        "The highest bid should be updated");
  }

  @Test
  public void testPlaceBid_RejectWhenAuctionIsNotRunning() {
    LocalDateTime startTime = LocalDateTime.now().plusHours(1);
    Auction auction =
        new Auction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.00"),
            startTime,
            startTime.plusHours(2));

    assertThrows(
        AuctionClosedException.class,
        () ->
            auction.placeBid(
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                BidType.MANUAL,
                startTime.plusMinutes(5)),
        "Bids should be rejected when the auction is not RUNNING");
  }

  @Test
  public void testPlaceBid_RejectWhenAmountDoesNotExceedCurrentHighestBid() {
    Auction auction = createRunningAuction();
    LocalDateTime bidTime = auction.getStartTime().plusMinutes(10);

    auction.placeBid(UUID.randomUUID(), new BigDecimal("100.00"), BidType.MANUAL, bidTime);

    assertThrows(
        InvalidBidException.class,
        () ->
            auction.placeBid(
                UUID.randomUUID(),
                new BigDecimal("100.00"),
                BidType.MANUAL,
                bidTime.plusMinutes(1)),
        "A new bid must be strictly greater than the current highest bid");
  }

  @Test
  public void testPlaceBid_RejectWhenLessThanMinIncrement() {
    Auction auction = createRunningAuction();
    LocalDateTime bidTime = auction.getStartTime().plusMinutes(10);

    auction.placeBid(UUID.randomUUID(), new BigDecimal("100.00"), BidType.MANUAL, bidTime);

    assertThrows(
        InvalidBidException.class,
        () ->
            auction.placeBid(
                UUID.randomUUID(),
                new BigDecimal("104.99"),
                BidType.MANUAL,
                bidTime.plusMinutes(1)),
        "A new bid must be at least currentHighestBid + 5% of starting price");
  }

  @Test
  public void testPlaceBid_AcceptedWhenEqualOrGreaterThanMinIncrement() {
    Auction auction = createRunningAuction();
    LocalDateTime bidTime = auction.getStartTime().plusMinutes(10);

    auction.placeBid(UUID.randomUUID(), new BigDecimal("100.00"), BidType.MANUAL, bidTime);

    BidTransaction bidTransaction =
        auction.placeBid(
            UUID.randomUUID(), new BigDecimal("105.00"), BidType.MANUAL, bidTime.plusMinutes(1));

    assertNotNull(bidTransaction, "The created bid should not be null");
    assertEquals(
        new BigDecimal("105.00"),
        auction.getCurrentHighestBid(),
        "The highest bid should be updated");
  }

  @Test
  public void testPlaceBid_RejectWhenSellerBidsOnOwnAuction() {
    Auction auction = createRunningAuction();

    assertThrows(
        UnauthorizedActionException.class,
        () ->
            auction.placeBid(
                auction.getSellerId(),
                new BigDecimal("100.00"),
                BidType.MANUAL,
                auction.getStartTime().plusMinutes(5)),
        "The seller should not be allowed to bid on their own auction");
  }

  @Test
  public void testValidator_NullArgs_Throws() {
    Auction auction = createRunningAuction();
    assertThrows(
        InvalidBidException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction, null, BigDecimal.TEN, BidType.MANUAL, LocalDateTime.now()));
    assertThrows(
        InvalidBidException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction, UUID.randomUUID(), null, BidType.MANUAL, LocalDateTime.now()));
  }

  @Test
  public void testValidator_AmountZeroOrNegative_Throws() {
    Auction auction = createRunningAuction();
    assertThrows(
        InvalidBidException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction, UUID.randomUUID(), BigDecimal.ZERO, BidType.MANUAL, LocalDateTime.now()));
    assertThrows(
        InvalidBidException.class,
        () ->
            AuctionBidValidator.validatePlaceBid(
                auction,
                UUID.randomUUID(),
                new BigDecimal("-1"),
                BidType.MANUAL,
                LocalDateTime.now()));
  }

  private Auction createRunningAuction() {
    LocalDateTime startTime = LocalDateTime.now().minusHours(1);
    return new Auction(
        UUID.randomUUID(),
        UUID.randomUUID(),
        UUID.randomUUID(),
        new BigDecimal("100.00"),
        startTime,
        startTime.plusHours(2),
        null,
        null,
        AuctionStatus.RUNNING,
        LocalDateTime.now(),
        LocalDateTime.now(),
        null);
  }
}
