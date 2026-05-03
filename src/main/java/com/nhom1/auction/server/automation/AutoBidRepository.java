package com.nhom1.auction.server.automation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AutoBidRepository {
    private final Connection connection;

    public AutoBidRepository(Connection connection) {
        this.connection = connection;
        ensureTable();
    }

    private void ensureTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS auto_bid_configs (
                auction_id TEXT NOT NULL,
                bidder_id TEXT NOT NULL,
                max_amount REAL NOT NULL,
                increment_amount REAL NOT NULL,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                PRIMARY KEY (auction_id, bidder_id)
            )
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize auto_bid_configs table", e);
        }
    }

    public void save(AutoBidConfig config) {
        String sql = """
            INSERT OR REPLACE INTO auto_bid_configs (
                auction_id, bidder_id, max_amount, increment_amount, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            """;
        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, config.getAuctionId().toString());
            ps.setString(2, config.getBidderId().toString());
            ps.setDouble(3, config.getMaxAmount().doubleValue());
            ps.setDouble(4, config.getIncrement().doubleValue());
            ps.setString(5, now.toString());
            ps.setString(6, now.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save auto bid config", e);
        }
    }

    public List<AutoBidConfig> findByAuctionId(UUID auctionId) {
        String sql = """
            SELECT auction_id, bidder_id, max_amount, increment_amount
            FROM auto_bid_configs
            WHERE auction_id = ?
            """;
        List<AutoBidConfig> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, auctionId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new AutoBidConfig(
                        UUID.fromString(rs.getString("auction_id")),
                        UUID.fromString(rs.getString("bidder_id")),
                        BigDecimal.valueOf(rs.getDouble("max_amount")),
                        BigDecimal.valueOf(rs.getDouble("increment_amount"))
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read auto bid configs", e);
        }
        return result;
    }
}
