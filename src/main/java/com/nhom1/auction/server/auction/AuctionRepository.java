package com.nhom1.auction.server.auction;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;

public class AuctionRepository {
    private final DataSource dataSource;

    public AuctionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ===================== SAVE =====================
    public void save(Auction auction) {
        try (Connection conn = dataSource.getConnection()) {
            save(auction, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save auction", e);
        }
    }

    public void save(Auction auction, Connection conn) {
        String sql = """
                    INSERT INTO auctions(
                        id, item_id, start_time, end_time, status, starting_price,
                        current_highest_bid, highest_bidder_id, duration_days, version,
                        created_at, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        Savepoint savepoint = createSavepoint(conn);
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getId().toString());
            ps.setString(2, auction.getItemId().toString());
            if (auction.getStartTime() != null) {
                ps.setTimestamp(3, java.sql.Timestamp.valueOf(auction.getStartTime()));
            } else {
                ps.setNull(3, java.sql.Types.TIMESTAMP);
            }
            if (auction.getEndTime() != null) {
                ps.setTimestamp(4, java.sql.Timestamp.valueOf(auction.getEndTime()));
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
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

            if (auction.getDurationDays() != null) {
                ps.setInt(9, auction.getDurationDays());
            } else {
                ps.setNull(9, Types.INTEGER);
            }

            ps.setLong(10, auction.getVersion());

            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            ps.setTimestamp(11, now);
            ps.setTimestamp(12, now);

            ps.executeUpdate();
        } catch (SQLException e) {
            if (isMissingDurationDaysOrVersionColumn(e)) {
                rollbackToSavepoint(conn, savepoint);
                saveWithoutDurationDays(auction, conn);
                return;
            }
            throw new RuntimeException("Failed to save auction", e);
        }
    }

    private void saveWithoutDurationDays(Auction auction, Connection conn) {
        String sql = """
                    INSERT INTO auctions(
                        id, item_id, start_time, end_time, status, starting_price,
                        current_highest_bid, highest_bidder_id, version, created_at, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getId().toString());
            ps.setString(2, auction.getItemId().toString());
            if (auction.getStartTime() != null) {
                ps.setTimestamp(3, java.sql.Timestamp.valueOf(auction.getStartTime()));
            } else {
                ps.setNull(3, java.sql.Types.TIMESTAMP);
            }
            if (auction.getEndTime() != null) {
                ps.setTimestamp(4, java.sql.Timestamp.valueOf(auction.getEndTime()));
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            ps.setString(5, auction.getStatus().name());
            ps.setBigDecimal(6, auction.getStartingPrice());
            ps.setBigDecimal(7, auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO);
            if (auction.getHighestBidderId() != null) {
                ps.setString(8, auction.getHighestBidderId().toString());
            } else {
                ps.setNull(8, Types.VARCHAR);
            }
            ps.setLong(9, auction.getVersion());

            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            ps.setTimestamp(10, now);
            ps.setTimestamp(11, now);

            ps.executeUpdate();
        } catch (SQLException e) {
            if (isMissingVersionColumn(e)) {
                saveLegacyWithoutDurationAndVersion(auction, conn);
                return;
            }
            throw new RuntimeException("Failed to save auction", e);
        }
    }

    private void saveLegacyWithoutDurationAndVersion(Auction auction, Connection conn) {
        String sql = """
                    INSERT INTO auctions(
                        id, item_id, start_time, end_time, status, starting_price,
                        current_highest_bid, highest_bidder_id, created_at, updated_at
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auction.getId().toString());
            ps.setString(2, auction.getItemId().toString());
            if (auction.getStartTime() != null) {
                ps.setTimestamp(3, java.sql.Timestamp.valueOf(auction.getStartTime()));
            } else {
                ps.setNull(3, java.sql.Types.TIMESTAMP);
            }
            if (auction.getEndTime() != null) {
                ps.setTimestamp(4, java.sql.Timestamp.valueOf(auction.getEndTime()));
            } else {
                ps.setNull(4, java.sql.Types.TIMESTAMP);
            }
            ps.setString(5, auction.getStatus().name());
            ps.setBigDecimal(6, auction.getStartingPrice());
            ps.setBigDecimal(7, auction.getCurrentHighestBid() != null ? auction.getCurrentHighestBid() : BigDecimal.ZERO);
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
        try (Connection conn = dataSource.getConnection()) {
            return findById(id, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find auction by id", e);
        }
    }

    public Optional<Auction> findById(UUID id, Connection conn) {
        String sql = """
                    SELECT a.*, i.seller_id
                    FROM auctions a
                    JOIN items i ON a.item_id = i.id
                    WHERE a.id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
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
        try (Connection conn = dataSource.getConnection()) {
            return findBySellerId(sellerId, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find by sellerId", e);
        }
    }

    public List<Auction> findBySellerId(UUID sellerId, Connection conn) {
        String sql = """
                    SELECT a.*, i.seller_id
                    FROM auctions a
                    JOIN items i ON a.item_id = i.id
                    WHERE i.seller_id = ?
                """;

        List<Auction> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemId.toString());
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
        try (Connection conn = dataSource.getConnection()) {
            updateStatus(auctionId, status, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update status", e);
        }
    }

    public void updateStatus(UUID auctionId, AuctionStatus status, Connection conn) {
        String sql = """
                    UPDATE auctions
                    SET status = ?, version = version + 1, updated_at = ?
                    WHERE id = ?
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, auctionId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update status", e);
        }
    }

    // ===================== UPDATE BID =====================
    /**
     * Atomic optimistic update for the auction row.
     *
     * How version-based locking works here:
     * 1. BidService reads the auction row and remembers its current version.
     * 2. The service validates and creates the bid based on that snapshot.
     * 3. This UPDATE only succeeds if the row still has the same version.
     * 4. On success, we increment version by 1 so later concurrent writers must
     *    observe the new state before updating again.
     *
     * This prevents "lost update" races where two concurrent bids both validate
     * against the same old snapshot and then try to overwrite each other.
     *
     * Returns:
     * - 1 row updated: caller won the race and its bid became the new highest bid.
     * - 0 rows updated: caller lost the race because another write changed the row
     *   first, or because the row is no longer RUNNING, or because the current
     *   highest bid is already equal/higher.
     */
    public int updateHighestBid(UUID auctionId, BigDecimal amount, UUID bidderId, long expectedVersion, Connection conn) {
        String sql = """
                    UPDATE auctions
                    SET current_highest_bid = ?, highest_bidder_id = ?, version = version + 1, updated_at = ?
                    WHERE id = ?
                      AND status = 'RUNNING'
                      AND version = ?
                      AND (current_highest_bid IS NULL OR current_highest_bid < ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, amount);

            if (bidderId != null) {
                ps.setString(2, bidderId.toString());
            } else {
                ps.setNull(2, java.sql.Types.VARCHAR);
            }

            ps.setTimestamp(3, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(4, auctionId.toString());
            ps.setLong(5, expectedVersion);
            ps.setBigDecimal(6, amount);

            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update highest bid", e);
        }
    }

    // ===================== UPDATE END TIME =====================
    public void updateEndTime(UUID auctionId, LocalDateTime newEndTime) {
        String sql = """
                    UPDATE auctions
                    SET end_time = ?, version = version + 1, updated_at = ?
                    WHERE id = ?
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(newEndTime));
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, auctionId.toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update end time", e);
        }
    }

    public int updateEndTime(UUID auctionId, LocalDateTime newEndTime, Connection conn) {
        String sql = """
                    UPDATE auctions
                    SET end_time = ?, updated_at = ?
                    WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, java.sql.Timestamp.valueOf(newEndTime));
            ps.setTimestamp(2, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(3, auctionId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update end time", e);
        }
    }

    public int updateStartingPriceAndEndTime(UUID auctionId, BigDecimal startingPrice, LocalDateTime newEndTime, Connection conn) {
        String sql = """
                    UPDATE auctions
                    SET starting_price = ?, current_highest_bid = ?, end_time = ?, updated_at = ?
                    WHERE id = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, startingPrice);
            ps.setBigDecimal(2, startingPrice);
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(newEndTime));
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, auctionId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update auction pricing/time", e);
        }
    }

    // ===================== DELETE BY ID =====================
    public int deleteById(UUID auctionId) {
        try (Connection conn = dataSource.getConnection()) {
            return deleteById(auctionId, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete auction", e);
        }
    }

    public int deleteById(UUID auctionId, Connection conn) {
        String sql = "DELETE FROM auctions WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, auctionId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete auction", e);
        }
    }

    // ===================== CLEAR HIGHEST BIDDER =====================
    public int clearHighestBidderByUserId(UUID bidderId) {
        try (Connection conn = dataSource.getConnection()) {
            return clearHighestBidderByUserId(bidderId, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear highest bidder on auctions", e);
        }
    }

    public int clearHighestBidderByUserId(UUID bidderId, Connection conn) {
        String sql = "UPDATE auctions SET highest_bidder_id = NULL WHERE highest_bidder_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bidderId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear highest bidder on auctions", e);
        }
    }

    private Savepoint createSavepoint(Connection conn) {
        try {
            return conn.getAutoCommit() ? null : conn.setSavepoint();
        } catch (SQLException e) {
            return null;
        }
    }

    private void rollbackToSavepoint(Connection conn, Savepoint savepoint) {
        if (savepoint == null) {
            return;
        }
        try {
            conn.rollback(savepoint);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to recover auction save fallback", e);
        }
    }

    private boolean isMissingDurationDaysOrVersionColumn(SQLException e) {
        String message = e.getMessage();
        return "42703".equals(e.getSQLState())
            || (message != null && (
                message.toLowerCase().contains("duration_days")
                || message.toLowerCase().contains("version")
            ));
    }

    private boolean isMissingVersionColumn(SQLException e) {
        String message = e.getMessage();
        return "42703".equals(e.getSQLState())
            || (message != null && message.toLowerCase().contains("version"));
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

        Integer durationDays = null;
        try {
            int dur = rs.getInt("duration_days");
            if (!rs.wasNull()) durationDays = dur;
        } catch (SQLException ignored) {
            // Backward compatibility: older DB schema may not have duration_days.
            durationDays = null;
        }

        java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
        java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime updatedAt = (updatedTs != null) ? updatedTs.toLocalDateTime() : LocalDateTime.now();
        // version is the optimistic-lock token for this auction row.
        // Any successful state-changing UPDATE increments it by 1.
        Integer version = (int) rs.getLong("version");

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
                updatedAt,
                durationDays,
                version);
    }

    // Update start, end and status atomically (used by admin approve flow)
    public boolean updateStartEndAndStatus(UUID auctionId, LocalDateTime startTime, LocalDateTime endTime, AuctionStatus status, Connection conn) {
        String sql = """
                    UPDATE auctions
                    SET start_time = ?, end_time = ?, status = ?, updated_at = ?
                    WHERE id = ? AND status = 'OPEN'
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startTime != null ? java.sql.Timestamp.valueOf(startTime) : null);
            ps.setTimestamp(2, endTime != null ? java.sql.Timestamp.valueOf(endTime) : null);
            ps.setString(3, status.name());
            ps.setTimestamp(4, java.sql.Timestamp.valueOf(LocalDateTime.now()));
            ps.setString(5, auctionId.toString());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to update auction start/end/status", e);
        }
    }
}
