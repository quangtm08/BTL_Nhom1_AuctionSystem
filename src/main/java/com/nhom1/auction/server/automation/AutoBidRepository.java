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
                auction_id VARCHAR(36) NOT NULL,
                bidder_id VARCHAR(36) NOT NULL,
                max_amount DECIMAL(19, 2) NOT NULL,
                increment_amount DECIMAL(19, 2) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                PRIMARY KEY (auction_id, bidder_id),
                FOREIGN KEY (auction_id) REFERENCES auctions(id),
                FOREIGN KEY (bidder_id) REFERENCES users(id)
            )
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize auto_bid_configs table", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Invalid data while initializing auto_bid_configs table", e);
        }
    }

    public void save(AutoBidConfig config) {
        // PostgreSQL and SQLite 3.24+ compatible UPSERT
        String sql = """
            INSERT INTO auto_bid_configs (
                auction_id, bidder_id, max_amount, increment_amount, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT (auction_id, bidder_id) DO UPDATE SET
                max_amount = EXCLUDED.max_amount,
                increment_amount = EXCLUDED.increment_amount,
                updated_at = EXCLUDED.updated_at
            """;
        java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, config.getAuctionId().toString());
            ps.setString(2, config.getBidderId().toString());
            ps.setBigDecimal(3, config.getMaxAmount());
            ps.setBigDecimal(4, config.getIncrement());
            ps.setTimestamp(5, now);
            ps.setTimestamp(6, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save auto bid config", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Invalid auto bid config data while saving", e);
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
                        rs.getBigDecimal("max_amount"),
                        rs.getBigDecimal("increment_amount")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read auto bid configs", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to map auto bid configs", e);
        }
        return result;
    }
}
