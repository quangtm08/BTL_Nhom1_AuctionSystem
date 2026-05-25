package com.nhom1.auction.server.admin;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import com.nhom1.auction.common.enums.AuctionStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

public class SqlAdminAuctionGateway implements AdminAuctionGateway {
  private final DataSource dataSource;

  public SqlAdminAuctionGateway(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public List<AuctionSummaryDto> findAllAuctionSummaries() {
    String sql =
        """
        SELECT a.id,
               i.name AS item_name,
               i.category AS category,
               a.starting_price,
               a.current_highest_bid,
               a.start_time,
               a.end_time,
               a.status,
               i.seller_id
        FROM auctions a
        JOIN items i ON i.id = a.item_id
        ORDER BY a.created_at DESC
        """;

    List<AuctionSummaryDto> result = new ArrayList<>();

    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql);
        ResultSet rs = ps.executeQuery()) {

      while (rs.next()) {
        java.sql.Timestamp sTs = rs.getTimestamp("start_time");
        java.sql.Timestamp eTs = rs.getTimestamp("end_time");
        java.time.LocalDateTime s = sTs != null ? sTs.toLocalDateTime() : null;
        java.time.LocalDateTime e = eTs != null ? eTs.toLocalDateTime() : null;
        result.add(
            new AuctionSummaryDto(
                rs.getString("id"),
                rs.getString("item_name"),
                rs.getString("category"),
                rs.getBigDecimal("starting_price"),
                rs.getBigDecimal("current_highest_bid"),
                s,
                e,
                AuctionStatus.valueOf(rs.getString("status")),
                rs.getString("seller_id")));
      }

    } catch (SQLException e) {
      throw new RuntimeException("Failed to list auction summaries", e);
    }

    return result;
  }

  @Override
  public boolean cancelAuctionById(String auctionId) {
    String sql =
        """
        UPDATE auctions
        SET status = 'CANCELED', updated_at = CURRENT_TIMESTAMP
        WHERE id = ? AND status IN ('PENDING', 'OPEN', 'RUNNING')
        """;
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, auctionId);
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to cancel auction by id", e);
    }
  }
}
