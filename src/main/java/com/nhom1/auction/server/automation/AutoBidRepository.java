package com.nhom1.auction.server.automation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class AutoBidRepository {
  private final DataSource dataSource;

  public AutoBidRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public void save(AutoBidConfig config) {
    String sql =
        """
        INSERT INTO auto_bid_configs (
            auction_id, bidder_id, max_amount, increment_amount, created_at, updated_at
        ) VALUES (?, ?, ?, ?, ?, ?)
        ON CONFLICT (auction_id, bidder_id) DO UPDATE SET
            max_amount = EXCLUDED.max_amount,
            increment_amount = EXCLUDED.increment_amount,
            updated_at = EXCLUDED.updated_at
        """;
    java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, config.getAuctionId().toString());
      ps.setString(2, config.getBidderId().toString());
      ps.setBigDecimal(3, config.getMaxAmount());
      ps.setBigDecimal(4, config.getIncrement());
      ps.setTimestamp(5, now);
      ps.setTimestamp(6, now);
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save auto bid config", e);
    }
  }

  public List<AutoBidConfig> findByAuctionId(UUID auctionId) {
    String sql =
        """
        SELECT auction_id, bidder_id, max_amount, increment_amount, created_at
        FROM auto_bid_configs
        WHERE auction_id = ?
        """;
    List<AutoBidConfig> result = new ArrayList<>();
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auctionId.toString());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          java.sql.Timestamp ts = rs.getTimestamp("created_at");
          java.time.LocalDateTime createdAt =
              ts != null ? ts.toLocalDateTime() : java.time.LocalDateTime.now();
          result.add(
              new AutoBidConfig(
                  UUID.fromString(rs.getString("auction_id")),
                  UUID.fromString(rs.getString("bidder_id")),
                  rs.getBigDecimal("max_amount"),
                  rs.getBigDecimal("increment_amount"),
                  createdAt));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to read auto bid configs", e);
    }
    return result;
  }

  public Optional<AutoBidConfig> findByAuctionAndBidder(UUID auctionId, UUID bidderId) {
    String sql =
        """
        SELECT auction_id, bidder_id, max_amount, increment_amount, created_at
        FROM auto_bid_configs
        WHERE auction_id = ? AND bidder_id = ?
        """;
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auctionId.toString());
      ps.setString(2, bidderId.toString());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          java.sql.Timestamp ts = rs.getTimestamp("created_at");
          java.time.LocalDateTime createdAt =
              ts != null ? ts.toLocalDateTime() : java.time.LocalDateTime.now();
          return Optional.of(
              new AutoBidConfig(
                  UUID.fromString(rs.getString("auction_id")),
                  UUID.fromString(rs.getString("bidder_id")),
                  rs.getBigDecimal("max_amount"),
                  rs.getBigDecimal("increment_amount"),
                  createdAt));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to read auto bid config", e);
    }
    return Optional.empty();
  }

  public int deleteByAuctionAndBidder(UUID auctionId, UUID bidderId) {
    String sql = "DELETE FROM auto_bid_configs WHERE auction_id = ? AND bidder_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auctionId.toString());
      ps.setString(2, bidderId.toString());
      return ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete auto bid config", e);
    }
  }

  public int deleteByAuctionId(UUID auctionId) {
    String sql = "DELETE FROM auto_bid_configs WHERE auction_id = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auctionId.toString());
      return ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to cleanup auto bid configs by auction", e);
    }
  }
}
