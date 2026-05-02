package com.nhom1.auction.server.admin;

import com.nhom1.auction.common.dto.auction.AuctionSummaryDto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class SqlAdminAuctionGateway implements AdminAuctionGateway {
    private final Connection connection;

    public SqlAdminAuctionGateway(Connection connection) {
        this.connection = connection;
    }

    @Override
    public List<AuctionSummaryDto> findAllAuctionSummaries() {
        String sql = """
                SELECT a.id AS auction_id,
                       a.item_id,
                       i.name AS item_name,
                       i.category AS item_category,
                       a.status,
                       a.current_highest_bid,
                       a.highest_bidder_id,
                       a.end_time,
                       i.seller_id
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                ORDER BY a.created_at DESC
                """;

        List<AuctionSummaryDto> result = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new AuctionSummaryDto(
                        rs.getString("auction_id"),
                        rs.getString("item_id"),
                        rs.getString("item_name"),
                        rs.getString("item_category"),
                        rs.getString("status"),
                        rs.getDouble("current_highest_bid"),
                        rs.getString("highest_bidder_id"),
                        rs.getString("end_time"),
                        rs.getString("seller_id")
                ));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    @Override
    public boolean cancelAuctionById(String auctionId) {
        String sql = "UPDATE auctions SET status='CANCELED', updated_at=datetime('now') WHERE id=? AND status IN ('OPEN','RUNNING')";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, auctionId);
            return ps.executeUpdate() > 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
