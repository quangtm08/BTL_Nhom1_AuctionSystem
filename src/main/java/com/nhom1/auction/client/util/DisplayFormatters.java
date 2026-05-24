package com.nhom1.auction.client.util;

import com.nhom1.auction.common.enums.AuctionStatus;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class DisplayFormatters {
  private static final DateTimeFormatter SHORT_DATE = DateTimeFormatter.ofPattern("MMM d, yyyy");
  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
  private static final DateTimeFormatter BID_TIME = DateTimeFormatter.ofPattern("HH:mm dd/MM");

  private DisplayFormatters() {}

  public static String money(BigDecimal amount) {
    return amount == null ? "$0" : "$" + NumberFormat.getNumberInstance(Locale.US).format(amount);
  }

  public static String moneyOrDash(BigDecimal amount) {
    return amount == null ? "-" : money(amount);
  }

  public static String timeLeft(LocalDateTime endTime) {
    if (endTime == null) {
      return "N/A";
    }

    Duration duration = Duration.between(LocalDateTime.now(), endTime);
    if (duration.isNegative() || duration.isZero()) {
      return "Ended";
    }

    long days = duration.toDays();
    if (days > 0) {
      return days + " days left";
    }

    long hours = duration.toHours();
    if (hours > 0) {
      return hours + " hours left";
    }

    return Math.max(1, duration.toMinutes()) + " min left";
  }

  public static String shortDate(LocalDateTime dateTime) {
    return dateTime == null ? "-" : dateTime.format(SHORT_DATE);
  }

  public static String dateTime(LocalDateTime dateTime) {
    return dateTime == null ? "-" : dateTime.format(DATE_TIME);
  }

  public static String bidTime(LocalDateTime dateTime) {
    return dateTime == null ? "" : dateTime.format(BID_TIME);
  }

  public static String auctionStatusLabel(AuctionStatus status) {
    if (status == null) {
      return "Unknown";
    }
    return switch (status) {
      case PENDING -> "Pending";
      case OPEN -> "Open";
      case RUNNING -> "Running";
      case FINISHED, CANCELED, PAID -> "Ended";
    };
  }

  public static boolean isEnded(AuctionStatus status) {
    return status == AuctionStatus.FINISHED
        || status == AuctionStatus.CANCELED
        || status == AuctionStatus.PAID;
  }

  public static String adminAuctionStatusStyle(AuctionStatus status) {
    if (status == AuctionStatus.OPEN || status == AuctionStatus.PENDING) {
      return "status-open";
    }
    if (status == AuctionStatus.RUNNING) {
      return "status-running";
    }
    if (status == AuctionStatus.PAID) {
      return "status-pill-active";
    }
    if (status == AuctionStatus.CANCELED) {
      return "status-pill-banned";
    }
    return "table-text-sub";
  }
}
