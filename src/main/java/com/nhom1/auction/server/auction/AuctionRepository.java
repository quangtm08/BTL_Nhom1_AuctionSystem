package com.nhom1.auction.server.auction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;

public class AuctionRepository {

    private final Connection connection;

    public AuctionRepository(Connection connection) {
        this.connection = connection;
    }

    // ===================== SAVE =====================
    public void save(Auction auction) {
        String sql = """
            INSERT INTO auctions(
                id, item_id, start_time, end_time, status,
                current_highest_bid, highest_bidder_id,
                created_at, updated_at
            )
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setObject(1, auction.getId());
            ps.setObject(2, auction.getItemId());
            ps.setObject(3, auction.getStartTime());
            ps.setObject(4, auction.getEndTime());
            ps.setString(5, auction.getStatus().name());

            // nullable
            if (auction.getCurrentHighestBid() != null) {
                ps.setBigDecimal(6, auction.getCurrentHighestBid());
            } else {
                ps.setNull(6, Types.DECIMAL);
            }

            if (auction.getHighestBidderId() != null) {
                ps.setObject(7, auction.getHighestBidderId());
            } else {
                ps.setNull(7, Types.OTHER);
            }

            LocalDateTime now = LocalDateTime.now();
            ps.setObject(8, now);
            ps.setObject(9, now);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to save auction", e);
        }
    }

    // ===================== FIND BY ID =====================
    public Optional<Auction> findById(UUID id) {
        String sql = """
            SELECT a.*, i.seller_id
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE a.id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find auction by id", e);
        }

        return Optional.empty();
    }

    // ===================== FIND ALL =====================
    public List<Auction> findAll() {
        String sql = """
            SELECT a.*, i.seller_id
            FROM auctions a
            JOIN items i ON a.item_id = i.id
        """;

        List<Auction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(map(rs));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find all auctions", e);
        }

        return list;
    }

    // ===================== FIND BY SELLER =====================
    public List<Auction> findBySellerId(UUID sellerId) {
        String sql = """
            SELECT a.*, i.seller_id
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE i.seller_id = ?
        """;

        List<Auction> list = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, sellerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find by sellerId", e);
        }

        return list;
    }

    // ===================== FIND BY ITEM =====================
    public Optional<Auction> findByItemId(UUID itemId) {
        String sql = """
            SELECT a.*, i.seller_id
            FROM auctions a
            JOIN items i ON a.item_id = i.id
            WHERE a.item_id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setObject(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Failed to find by itemId", e);
        }

        return Optional.empty();
    }

    // ===================== UPDATE STATUS =====================
    public void updateStatus(UUID auctionId, AuctionStatus status) {
        String sql = """
            UPDATE auctions
            SET status = ?, updated_at = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setObject(2, LocalDateTime.now());
            ps.setObject(3, auctionId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update status", e);
        }
    }

    // ===================== UPDATE BID =====================
    public void updateHighestBid(UUID auctionId, BigDecimal amount, UUID bidderId) {
        String sql = """
            UPDATE auctions
            SET current_highest_bid = ?, highest_bidder_id = ?, updated_at = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setBigDecimal(1, amount);

            if (bidderId != null) {
                ps.setObject(2, bidderId);
            } else {
                ps.setNull(2, Types.OTHER);
            }

            ps.setObject(3, LocalDateTime.now());
            ps.setObject(4, auctionId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update highest bid", e);
        }
    }

    // ===================== UPDATE END TIME =====================
    public void updateEndTime(UUID auctionId, LocalDateTime newEndTime) {
        String sql = """
            UPDATE auctions
            SET end_time = ?, updated_at = ?
            WHERE id = ?
        """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setObject(1, newEndTime);
            ps.setObject(2, LocalDateTime.now());
            ps.setObject(3, auctionId);

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Failed to update end time", e);
        }
    }

    // ===================== MAPPER =====================
    private Auction map(ResultSet rs) throws SQLException {

        UUID id = rs.getObject("id", UUID.class);
        UUID itemId = rs.getObject("item_id", UUID.class);
        UUID sellerId = rs.getObject("seller_id", UUID.class);

        LocalDateTime startTime = rs.getObject("start_time", LocalDateTime.class);
        LocalDateTime endTime = rs.getObject("end_time", LocalDateTime.class);

        UUID highestBidderId = rs.getObject("highest_bidder_id", UUID.class);
        BigDecimal currentHighestBid = rs.getBigDecimal("current_highest_bid");

        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));

        LocalDateTime createdAt = rs.getObject("created_at", LocalDateTime.class);
        LocalDateTime updatedAt = rs.getObject("updated_at", LocalDateTime.class);

        return new Auction(
                id,
                itemId,
                sellerId,
                startTime,
                endTime,
                highestBidderId,
                currentHighestBid,
                status,
                createdAt,
                updatedAt
        );
    }
}