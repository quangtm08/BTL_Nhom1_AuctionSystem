package com.nhom1.auction.server.bidding;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.nhom1.auction.common.dto.bidding.BidWithAuctionDto;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.enums.AuctionStatus;
import com.nhom1.auction.common.enums.BidType;

public class BidRepository {

	private final Connection connection;

	public BidRepository(Connection connection) {
		this.connection = connection;
	}

	public void save(BidTransaction bidTransaction) {
		String sql = """
			INSERT INTO bids(id, auction_id, bidder_id, amount, bid_type, created_at)
			VALUES (?, ?, ?, ?, ?, ?)
			""";

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, bidTransaction.getId().toString());
			ps.setString(2, bidTransaction.getAuctionId().toString());
			ps.setString(3, bidTransaction.getBidderId().toString());
			ps.setBigDecimal(4, bidTransaction.getAmount());
			ps.setString(5, bidTransaction.getBidType().name());
			ps.setTimestamp(6, java.sql.Timestamp.valueOf(bidTransaction.getCreatedAt()));
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("Failed to save bid", e);
		}
	}

	public List<BidTransaction> findByAuctionId(UUID auctionId) {
		String sql = """
			SELECT id, auction_id, bidder_id, amount, bid_type, created_at
			FROM bids
			WHERE auction_id = CAST(? AS VARCHAR)
			ORDER BY created_at ASC
			""";

		List<BidTransaction> bids = new ArrayList<>();

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, auctionId.toString());

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					bids.add(mapBidTransaction(rs));
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to find bids by auction id", e);
		}

		return bids;
	}

	public List<BidWithAuctionDto> findByBidderId(UUID bidderId) {
		String sql = """
			SELECT b.auction_id,
				   i.name AS item_name,
				   b.amount AS your_bid,
				   a.current_highest_bid,
				   a.status,
				   a.end_time,
				   a.highest_bidder_id
			FROM bids b
			JOIN auctions a ON CAST(a.id AS VARCHAR) = CAST(b.auction_id AS VARCHAR)
			JOIN items i ON CAST(i.id AS VARCHAR) = CAST(a.item_id AS VARCHAR)
			WHERE b.bidder_id = CAST(? AS VARCHAR)
			ORDER BY b.created_at DESC
			""";

		List<BidWithAuctionDto> result = new ArrayList<>();

		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, bidderId.toString());

			try (ResultSet rs = ps.executeQuery()) {
				while (rs.next()) {
					UUID highestBidderId = parseUuidNullable(rs.getString("highest_bidder_id"));
					boolean isWinning = bidderId.equals(highestBidderId);

					result.add(new BidWithAuctionDto(
							rs.getString("auction_id"),
							rs.getString("item_name"),
							rs.getBigDecimal("your_bid"),
							rs.getBigDecimal("current_highest_bid"),
							AuctionStatus.valueOf(rs.getString("status")),
							LocalDateTime.parse(rs.getString("end_time")),
							isWinning
					));
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to find bids by bidder id", e);
		}

		return result;
	}

	public Optional<LocalDateTime> findLastBidTime(UUID auctionId) {
		String sql = """
			SELECT MAX(created_at) AS last_bid_time
			FROM bids
			WHERE auction_id = CAST(? AS VARCHAR)
			""";
// PreparedStatement handles SQL injection and ensures proper resource management
		try (PreparedStatement ps = connection.prepareStatement(sql)) {
			ps.setString(1, auctionId.toString());

    		try (ResultSet rs = ps.executeQuery()) {
				if (rs.next()) {
					java.sql.Timestamp lastBidTs = rs.getTimestamp("last_bid_time");
					if (lastBidTs != null) {
						return Optional.of(lastBidTs.toLocalDateTime());
					}
				}
			}
		} catch (SQLException e) {
			throw new RuntimeException("Failed to find last bid time", e);
		}

		return Optional.empty();
	}

    public int deleteByBidderId(UUID bidderId) {
        String sql = "DELETE FROM bids WHERE bidder_id = CAST(? AS VARCHAR)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bidderId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete bids by bidder id", e);
        }
    }

    public int deleteByAuctionId(UUID auctionId) {
        String sql = "DELETE FROM bids WHERE auction_id = CAST(? AS VARCHAR)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, auctionId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete bids by auction id", e);
        }
    }

    private BidTransaction mapBidTransaction(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID auctionId = UUID.fromString(rs.getString("auction_id"));
        UUID bidderId = UUID.fromString(rs.getString("bidder_id"));
        BidType bidType = BidType.valueOf(rs.getString("bid_type"));
        java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : LocalDateTime.now();

        return new BidTransaction(
                id,
                auctionId,
                bidderId,
                rs.getBigDecimal("amount"),
                bidType,
                createdAt,
                createdAt
        );
    }

    private UUID parseUuidNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return UUID.fromString(value);
    }
}
