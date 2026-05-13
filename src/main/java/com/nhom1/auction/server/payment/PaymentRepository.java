package com.nhom1.auction.server.payment;

import com.nhom1.auction.common.dto.payment.PaymentItemDto;
import com.nhom1.auction.common.enums.AuctionStatus;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaymentRepository {
    private final Connection connection;

    public PaymentRepository(Connection connection) {
        this.connection = connection;
    }

    public List<PaymentItemDto> findPendingPayments(UUID bidderId) {
        return findPaymentsByStatus(bidderId, AuctionStatus.FINISHED, "Awaiting payment");
    }

    public List<PaymentItemDto> findPaymentHistory(UUID bidderId) {
        return findPaymentsByStatus(bidderId, AuctionStatus.PAID, "Paid");
    }

    private List<PaymentItemDto> findPaymentsByStatus(UUID bidderId, AuctionStatus status, String label) {
        String sql = """
                SELECT a.id AS auction_id,
                       i.name AS item_name,
                       i.category AS item_category,
                       COALESCE(a.current_highest_bid, a.starting_price) AS amount,
                       a.updated_at AS event_time
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                WHERE a.highest_bidder_id = ? AND a.status = ?
                ORDER BY a.updated_at DESC
                """;

        List<PaymentItemDto> payments = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bidderId.toString());
            ps.setString(2, status.name());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(new PaymentItemDto(
                            rs.getString("auction_id"),
                            rs.getString("item_name"),
                            rs.getString("item_category"),
                            rs.getBigDecimal("amount"),
                            rs.getTimestamp("event_time") != null
                                    ? rs.getTimestamp("event_time").toLocalDateTime()
                                    : null,
                            label));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load payments", e);
        }
        return payments;
    }
}
