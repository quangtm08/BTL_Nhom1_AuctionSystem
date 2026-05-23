package com.nhom1.auction.server.wallet;

import com.nhom1.auction.common.entity.Wallet;
import com.nhom1.auction.common.entity.WalletTransaction;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class WalletRepository {
  private final DataSource dataSource;

  public WalletRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  public Optional<Wallet> findByUserId(UUID userId) {
    try (Connection conn = dataSource.getConnection()) {
      return findByUserId(userId, conn);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find wallet for user: " + userId, e);
    }
  }

  public Optional<Wallet> findByUserId(UUID userId, Connection conn) {
    String sql = "SELECT * FROM wallets WHERE user_id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId.toString());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          BigDecimal balance = rs.getBigDecimal("balance");
          Timestamp createdTs = rs.getTimestamp("created_at");
          Timestamp updatedTs = rs.getTimestamp("updated_at");
          LocalDateTime createdAt =
              createdTs != null ? createdTs.toLocalDateTime() : LocalDateTime.now();
          LocalDateTime updatedAt =
              updatedTs != null ? updatedTs.toLocalDateTime() : LocalDateTime.now();
          return Optional.of(new Wallet(userId, balance, createdAt, updatedAt));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find wallet for user: " + userId, e);
    }
    return Optional.empty();
  }

  public void save(Wallet wallet) {
    try (Connection conn = dataSource.getConnection()) {
      save(wallet, conn);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save wallet: " + wallet.getUserId(), e);
    }
  }

  public void save(Wallet wallet, Connection conn) {
    boolean exists = existsWallet(wallet.getUserId(), conn);
    if (exists) {
      String sql = "UPDATE wallets SET balance = ?, updated_at = ? WHERE user_id = ?";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setBigDecimal(1, wallet.getBalance());
        ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
        ps.setString(3, wallet.getUserId().toString());
        ps.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException("Failed to update wallet: " + wallet.getUserId(), e);
      }
    } else {
      String sql =
          "INSERT INTO wallets (user_id, balance, created_at, updated_at) VALUES (?, ?, ?, ?)";
      try (PreparedStatement ps = conn.prepareStatement(sql)) {
        ps.setString(1, wallet.getUserId().toString());
        ps.setBigDecimal(2, wallet.getBalance());
        ps.setTimestamp(3, Timestamp.valueOf(wallet.getCreatedAt()));
        ps.setTimestamp(4, Timestamp.valueOf(wallet.getUpdatedAt()));
        ps.executeUpdate();
      } catch (SQLException e) {
        throw new RuntimeException("Failed to insert wallet: " + wallet.getUserId(), e);
      }
    }
  }

  private boolean existsWallet(UUID userId, Connection conn) {
    String sql = "SELECT 1 FROM wallets WHERE user_id = ? LIMIT 1";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId.toString());
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check wallet existence: " + userId, e);
    }
  }

  public void updateBalance(UUID userId, BigDecimal newBalance, Connection conn) {
    String sql = "UPDATE wallets SET balance = ?, updated_at = ? WHERE user_id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setBigDecimal(1, newBalance);
      ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
      ps.setString(3, userId.toString());
      int affected = ps.executeUpdate();
      if (affected == 0) {
        // If it doesn't exist, create it (shouldn't happen with migration, but safe fallback)
        Wallet wallet = new Wallet(userId, newBalance);
        save(wallet, conn);
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update wallet balance: " + userId, e);
    }
  }

  public List<WalletTransaction> findTransactionsByUserId(UUID userId) {
    try (Connection conn = dataSource.getConnection()) {
      return findTransactionsByUserId(userId, conn);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find transactions for user: " + userId, e);
    }
  }

  public List<WalletTransaction> findTransactionsByUserId(UUID userId, Connection conn) {
    String sql = "SELECT * FROM wallet_transactions WHERE user_id = ? ORDER BY created_at DESC";
    List<WalletTransaction> list = new ArrayList<>();
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, userId.toString());
      try (ResultSet rs = ps.executeQuery()) {
        while (rs.next()) {
          UUID id = UUID.fromString(rs.getString("id"));
          BigDecimal amount = rs.getBigDecimal("amount");
          String type = rs.getString("transaction_type");
          String refId = rs.getString("reference_id");
          String desc = rs.getString("description");
          Timestamp createdTs = rs.getTimestamp("created_at");
          LocalDateTime createdAt =
              createdTs != null ? createdTs.toLocalDateTime() : LocalDateTime.now();
          list.add(new WalletTransaction(id, userId, amount, type, refId, desc, createdAt));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find transactions for user: " + userId, e);
    }
    return list;
  }

  public void saveTransaction(WalletTransaction tx) {
    try (Connection conn = dataSource.getConnection()) {
      saveTransaction(tx, conn);
    } catch (SQLException e) {
      throw new RuntimeException(
          "Failed to save wallet transaction for user: " + tx.getUserId(), e);
    }
  }

  public void saveTransaction(WalletTransaction tx, Connection conn) {
    String sql =
        "INSERT INTO wallet_transactions (id, user_id, amount, transaction_type, reference_id,"
            + " description, created_at) VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, tx.getId().toString());
      ps.setString(2, tx.getUserId().toString());
      ps.setBigDecimal(3, tx.getAmount());
      ps.setString(4, tx.getTransactionType());
      ps.setString(5, tx.getReferenceId());
      ps.setString(6, tx.getDescription());
      ps.setTimestamp(7, Timestamp.valueOf(tx.getCreatedAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException(
          "Failed to save wallet transaction for user: " + tx.getUserId(), e);
    }
  }
}
