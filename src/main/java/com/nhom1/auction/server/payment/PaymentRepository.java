package com.nhom1.auction.server.payment;

import com.nhom1.auction.common.dto.payment.PaymentHistoryEntryDto;
import com.nhom1.auction.common.dto.payment.PendingPaymentDto;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PaymentRepository {
    private final Connection connection;

    public PaymentRepository(Connection connection) {
        this.connection = connection;
        ensureTable();
    }

    private void ensureTable() {
        String sql = """
                CREATE TABLE IF NOT EXISTS payment_transactions (
                    id VARCHAR(36) PRIMARY KEY,
                    auction_id VARCHAR(36) NOT NULL,
                    payer_id VARCHAR(36) NOT NULL,
                    payee_id VARCHAR(36) NOT NULL,
                    amount NUMERIC(19, 2) NOT NULL,
                    status VARCHAR(50) NOT NULL,
                    created_at TIMESTAMP NOT NULL,
                    updated_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (auction_id) REFERENCES auctions(id),
                    FOREIGN KEY (payer_id) REFERENCES users(id),
                    FOREIGN KEY (payee_id) REFERENCES users(id)
                );
                CREATE INDEX IF NOT EXISTS idx_payment_transactions_auction_id ON payment_transactions(auction_id);
                CREATE INDEX IF NOT EXISTS idx_payment_transactions_payer_id ON payment_transactions(payer_id);
                CREATE INDEX IF NOT EXISTS idx_payment_transactions_payee_id ON payment_transactions(payee_id);
                """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize payment_transactions table", e);
        }
    }

    public void saveCompletedPayment(UUID auctionId, UUID payerId, UUID payeeId, BigDecimal amount, LocalDateTime now) {
        String sql = """
                INSERT INTO payment_transactions(id, auction_id, payer_id, payee_id, amount, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'COMPLETED', ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, auctionId.toString());
            ps.setString(3, payerId.toString());
            ps.setString(4, payeeId.toString());
            ps.setBigDecimal(5, amount);
            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setTimestamp(7, Timestamp.valueOf(now));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save payment transaction", e);
        }
    }

    public boolean existsCompletedPaymentForAuction(UUID auctionId) {
        String sql = "SELECT 1 FROM payment_transactions WHERE auction_id = ? AND status = 'COMPLETED' LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, auctionId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to check payment transaction", e);
        }
    }

    public List<PendingPaymentDto> findPendingPaymentsByBidder(UUID bidderId) {
        String sql = """
                SELECT a.id AS auction_id,
                       i.name AS item_name,
                       i.category AS item_category,
                       a.current_highest_bid AS amount,
                       a.end_time
                FROM auctions a
                JOIN items i ON i.id = a.item_id
                WHERE a.highest_bidder_id = ?
                  AND a.status = 'FINISHED'
                  AND a.current_highest_bid IS NOT NULL
                  AND NOT EXISTS (
                      SELECT 1
                      FROM payment_transactions pt
                      WHERE pt.auction_id = a.id
                        AND pt.status = 'COMPLETED'
                  )
                ORDER BY a.end_time DESC
                """;
        List<PendingPaymentDto> payments = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, bidderId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    payments.add(new PendingPaymentDto(
                            rs.getString("auction_id"),
                            rs.getString("item_name"),
                            rs.getString("item_category"),
                            rs.getBigDecimal("amount"),
                            rs.getTimestamp("end_time").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load pending payments", e);
        }
        return payments;
    }

    public List<PaymentHistoryEntryDto> findPaymentHistoryForUser(UUID userId) {
        String sql = """
                SELECT pt.auction_id,
                       i.name AS item_name,
                       pt.amount,
                       CASE
                           WHEN pt.payer_id = ? THEN 'PAY'
                           ELSE 'RECEIVE'
                       END AS direction,
                       pt.created_at
                FROM payment_transactions pt
                JOIN auctions a ON a.id = pt.auction_id
                JOIN items i ON i.id = a.item_id
                WHERE pt.payer_id = ? OR pt.payee_id = ?
                ORDER BY pt.created_at DESC
                """;
        List<PaymentHistoryEntryDto> entries = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, userId.toString());
            ps.setString(2, userId.toString());
            ps.setString(3, userId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    entries.add(new PaymentHistoryEntryDto(
                            rs.getString("auction_id"),
                            rs.getString("item_name"),
                            rs.getBigDecimal("amount"),
                            rs.getString("direction"),
                            rs.getTimestamp("created_at").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to load payment history", e);
        }
        return entries;
    }
}
