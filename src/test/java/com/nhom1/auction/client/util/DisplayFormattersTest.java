package com.nhom1.auction.client.util;

import static org.junit.jupiter.api.Assertions.*;

import com.nhom1.auction.common.enums.AuctionStatus;
import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

public class DisplayFormattersTest {

  @Test
  public void testPrivateConstructor() throws Exception {
    Constructor<DisplayFormatters> constructor = DisplayFormatters.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    DisplayFormatters instance = constructor.newInstance();
    assertNotNull(instance);
  }

  @Test
  public void testMoney() {
    assertEquals("$0", DisplayFormatters.money(null));
    assertEquals("$1,000", DisplayFormatters.money(new BigDecimal("1000")));
    assertEquals("$50.5", DisplayFormatters.money(new BigDecimal("50.5")));
  }

  @Test
  public void testMoneyOrDash() {
    assertEquals("-", DisplayFormatters.moneyOrDash(null));
    assertEquals("$500", DisplayFormatters.moneyOrDash(new BigDecimal("500")));
  }

  @Test
  public void testTimeLeft() {
    assertEquals("N/A", DisplayFormatters.timeLeft(null));

    // Past time
    LocalDateTime past = LocalDateTime.now().minusMinutes(5);
    assertEquals("Ended", DisplayFormatters.timeLeft(past));

    // More than 1 day left
    LocalDateTime daysLeft = LocalDateTime.now().plusDays(2).plusHours(1);
    assertTrue(DisplayFormatters.timeLeft(daysLeft).contains("days left"));

    // Hours left (less than 24 hours, more than 1 hour)
    LocalDateTime hoursLeft = LocalDateTime.now().plusHours(5);
    assertTrue(DisplayFormatters.timeLeft(hoursLeft).contains("hours left"));

    // Minutes left (less than 1 hour)
    LocalDateTime minutesLeft = LocalDateTime.now().plusMinutes(20);
    assertTrue(DisplayFormatters.timeLeft(minutesLeft).contains("min left"));
  }

  @Test
  public void testShortDate() {
    assertEquals("-", DisplayFormatters.shortDate(null));
    LocalDateTime date = LocalDateTime.of(2026, 5, 23, 10, 30);
    assertEquals("May 23, 2026", DisplayFormatters.shortDate(date));
  }

  @Test
  public void testDateTime() {
    assertEquals("-", DisplayFormatters.dateTime(null));
    LocalDateTime date = LocalDateTime.of(2026, 5, 23, 10, 30);
    assertEquals("23/05/2026 10:30", DisplayFormatters.dateTime(date));
  }

  @Test
  public void testBidTime() {
    assertEquals("", DisplayFormatters.bidTime(null));
    LocalDateTime date = LocalDateTime.of(2026, 5, 23, 10, 30);
    assertEquals("10:30 23/05", DisplayFormatters.bidTime(date));
  }

  @Test
  public void testAuctionStatusLabel() {
    assertEquals("Unknown", DisplayFormatters.auctionStatusLabel(null));
    assertEquals("Pending", DisplayFormatters.auctionStatusLabel(AuctionStatus.PENDING));
    assertEquals("Open", DisplayFormatters.auctionStatusLabel(AuctionStatus.OPEN));
    assertEquals("Running", DisplayFormatters.auctionStatusLabel(AuctionStatus.RUNNING));
    assertEquals("Ended", DisplayFormatters.auctionStatusLabel(AuctionStatus.FINISHED));
    assertEquals("Ended", DisplayFormatters.auctionStatusLabel(AuctionStatus.CANCELED));
    assertEquals("Ended", DisplayFormatters.auctionStatusLabel(AuctionStatus.PAID));
  }

  @Test
  public void testIsEnded() {
    assertFalse(DisplayFormatters.isEnded(AuctionStatus.PENDING));
    assertFalse(DisplayFormatters.isEnded(AuctionStatus.OPEN));
    assertFalse(DisplayFormatters.isEnded(AuctionStatus.RUNNING));
    assertTrue(DisplayFormatters.isEnded(AuctionStatus.FINISHED));
    assertTrue(DisplayFormatters.isEnded(AuctionStatus.CANCELED));
    assertTrue(DisplayFormatters.isEnded(AuctionStatus.PAID));
  }

  @Test
  public void testAdminAuctionStatusStyle() {
    assertEquals(
        "status-running", DisplayFormatters.adminAuctionStatusStyle(AuctionStatus.RUNNING));
    assertEquals(
        "status-pill-active", DisplayFormatters.adminAuctionStatusStyle(AuctionStatus.PAID));
    assertEquals(
        "status-pill-banned", DisplayFormatters.adminAuctionStatusStyle(AuctionStatus.CANCELED));
    assertEquals("status-open", DisplayFormatters.adminAuctionStatusStyle(AuctionStatus.PENDING));
    assertEquals("status-open", DisplayFormatters.adminAuctionStatusStyle(AuctionStatus.OPEN));
    assertEquals(
        "table-text-sub", DisplayFormatters.adminAuctionStatusStyle(AuctionStatus.FINISHED));
  }
}
