package com.nhom1.auction.server.payment;

import com.nhom1.auction.common.dto.payment.PaymentHistoryEntryDto;
import com.nhom1.auction.common.dto.payment.PendingPaymentDto;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

public class PaymentRepository {
  private final DataSource dataSource;

  public PaymentRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  // Writes
  public void saveCompletedPayment(
      UUID auctionId, UUID payerId, UUID payeeId, BigDecimal amount, LocalDateTime now) {
    try (Connection conn = dataSource.getConnection()) {
      saveCompletedPayment(auctionId, payerId, payeeId, amount, now, conn);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save payment transaction", e);
    }
  }

  public void saveCompletedPayment(
      UUID auctionId,
      UUID payerId,
      UUID payeeId,
      BigDecimal amount,
      LocalDateTime now,
      Connection conn) {
    String sql =
        """
INSERT INTO payment_transactions(id, auction_id, payer_id, payee_id, amount, status, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, 'COMPLETED', ?, ?)
""";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, UUID.randomUUID().toString());
      ps.setString(2, auctionId.toString());
      ps.setString(3, payerId.toString());
      ps.setString(4, payeeId.toString());
      ps.setBigDecimal(5, amount);
      ps.setTimestamp(6, Timestamp.valueOf(now));
      ps.setTimestamp(7, Timestamp.valueOf(now));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save payment transaction", e);
    }
  }

  // Reads
  public boolean existsCompletedPaymentForAuction(UUID auctionId) {
    try (Connection conn = dataSource.getConnection()) {
      return existsCompletedPaymentForAuction(auctionId, conn);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check payment transaction", e);
    }
  }

  public boolean existsCompletedPaymentForAuction(UUID auctionId, Connection conn) {
    String sql =
        "SELECT 1 FROM payment_transactions WHERE auction_id = ? AND status = 'COMPLETED' LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auctionId.toString());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check payment transaction", e);
    }
  }

  public List<PendingPaymentDto> findPendingPaymentsByBidder(UUID bidderId) {
    String sql =
        """
        SELECT a.id AS auction_id,
               i.name AS item_name,
               i.category AS item_category,
               a.current_highest_bid AS amount,
               a.end_time
        FROM auctions a
        JOIN items i ON i.id = a.item_id
        WHERE a.highest_bidder_id = ?
          AND a.status = 'FINISHED'
          AND a.current_highest_bid IS NOT NULL
          AND NOT EXISTS (
              SELECT 1
              FROM payment_transactions pt
              WHERE pt.auction_id = a.id
                AND pt.status = 'COMPLETED'
          )
        ORDER BY a.end_time DESC
        """;
    List<PendingPaymentDto> payments = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, bidderId.toString());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          payments.add(
              new PendingPaymentDto(
                  rs.getString("auction_id"),
                  rs.getString("item_name"),
                  rs.getString("item_category"),
                  rs.getBigDecimal("amount"),
                  rs.getTimestamp("end_time").toLocalDateTime()));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load pending payments", e);
    }
    return payments;
  }

  public List<PaymentHistoryEntryDto> findPaymentHistoryForUser(UUID userId) {
    String sql =
        """
        SELECT pt.auction_id,
               i.name AS item_name,
               pt.amount,
               CASE
                   WHEN pt.payer_id = ? THEN 'PAY'
                   ELSE 'RECEIVE'
               END AS direction,
               pt.created_at
        FROM payment_transactions pt
        JOIN auctions a ON a.id = pt.auction_id
        JOIN items i ON i.id = a.item_id
        WHERE pt.payer_id = ? OR pt.payee_id = ?
        ORDER BY pt.created_at DESC
        """;
    List<PaymentHistoryEntryDto> entries = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId.toString());
      ps.setString(2, userId.toString());
      ps.setString(3, userId.toString());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          entries.add(
              new PaymentHistoryEntryDto(
                  rs.getString("auction_id"),
                  rs.getString("item_name"),
                  rs.getBigDecimal("amount"),
                  rs.getString("direction"),
                  rs.getTimestamp("created_at").toLocalDateTime()));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to load payment history", e);
    }
    return entries;
  }
}
