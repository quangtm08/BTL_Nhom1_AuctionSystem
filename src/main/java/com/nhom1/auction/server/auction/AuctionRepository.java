package com.nhom1.auction.server.auction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;

public class AuctionRepository {
    private static final DateTimeFormatter SQLITE_DATE_TIME_FORMATTER = DateTimeFormatter
            .ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Connection connection;

    public AuctionRepository(Connection connection) {
        this.connection = connection;
    }

    // ===================== SAVE =====================
    public void save(Auction auction) {
        String sql = """
                    INSERT INTO auctions(
                        id, item_id, start_time, end_time, status, starting_price,
                        current_highest_bid, highest_bidder_id,
                        created_at, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {

            ps.setString(1, auction.getId().toString());
            ps.setString(2, auction.getItemId().toString());
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(auction.getStartTime()));
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(auction.getEndTime()));
            ps.setString(5, auction.getStatus().name());
            ps.setBigDecimal(6, auction.getStartingPrice());

            if (auction.getCurrentHighestBid() != null) {
                ps.setBigDecimal(7, auction.getCurrentHighestBid());
            } else {
                ps.setBigDecimal(7, BigDecimal.ZERO);
            }

            if (auction.getHighestBidderId() != null) {
                ps.setString(8, auction.getHighestBidderId().toString());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }

            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            ps.setTimestamp(9, now);
            ps.setTimestamp(10, now);

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
            ps.setString(1, sellerId.toString());

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

        UUID id = UUID.fromString(rs.getString("id"));
        UUID itemId = UUID.fromString(rs.getString("item_id"));
        UUID sellerId = UUID.fromString(rs.getString("seller_id"));

        java.sql.Timestamp startTs = rs.getTimestamp("start_time");
        java.sql.Timestamp endTs = rs.getTimestamp("end_time");
        LocalDateTime startTime = (startTs != null) ? startTs.toLocalDateTime() : null;
        LocalDateTime endTime = (endTs != null) ? endTs.toLocalDateTime() : null;

        BigDecimal startingPrice = rs.getBigDecimal("starting_price");
        String highestBidderIdRaw = rs.getString("highest_bidder_id");
        UUID highestBidderId = highestBidderIdRaw == null ? null : UUID.fromString(highestBidderIdRaw);
        BigDecimal currentHighestBid = rs.getBigDecimal("current_highest_bid");

        AuctionStatus status = AuctionStatus.valueOf(rs.getString("status"));

        java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
        java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime updatedAt = (updatedTs != null) ? updatedTs.toLocalDateTime() : LocalDateTime.now();

        return new Auction(
                id,
                itemId,
                sellerId,
                startingPrice,
                startTime,
                endTime,
                highestBidderId,
                currentHighestBid,
                status,
                createdAt,
                updatedAt);
    }

    public int deleteById(UUID auctionId) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, auctionId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete auction", e);
        }
    }

    public int clearHighestBidderByUserId(UUID bidderId) {
        String sql = "UPDATE auctions SET highest_bidder_id = NULL WHERE highest_bidder_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bidderId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear highest bidder on auctions", e);
        }
    }

    private LocalDateTime parseSqliteDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }
        return LocalDateTime.parse(value, SQLITE_DATE_TIME_FORMATTER);
    }

    private String formatSqliteDateTime(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.format(SQLITE_DATE_TIME_FORMATTER);
    }
}
