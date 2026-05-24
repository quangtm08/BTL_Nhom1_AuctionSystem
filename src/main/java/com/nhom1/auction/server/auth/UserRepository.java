package com.nhom1.auction.server.auth;

import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class UserRepository {
  private final DataSource dataSource;

  public UserRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  // Writes
  public void save(User user) {
    try (Connection conn = dataSource.getConnection()) {
      save(user, conn);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save user", e);
    }
  }

  public void save(User user, Connection conn) {
    String sql =
        "INSERT INTO users(id, username, email, password, role, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, user.getId().toString());
      ps.setString(2, user.getUsername());
      ps.setString(3, user.getEmail());
      ps.setString(4, user.getPassword());
      ps.setString(5, user.getRole().name());
      ps.setTimestamp(6, java.sql.Timestamp.valueOf(user.getCreatedAt()));
      ps.setTimestamp(7, java.sql.Timestamp.valueOf(user.getUpdatedAt()));
      ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to save user", e);
    }
  }

  // Reads
  public List<User> findAll() {
    try (Connection conn = dataSource.getConnection()) {
      return findAll(conn);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find all users", e);
    }
  }

  public List<User> findAll(Connection conn) {
    String sql = "SELECT * FROM users";
    List<User> users = new ArrayList<>();
    try (Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery(sql)) {
      while (rs.next()) {
        users.add(mapUser(rs));
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find all users", e);
    }
    return users;
  }

  public Optional<User> findByIdentifier(String identifier) {
    String sql = "SELECT * FROM users WHERE username = ? OR email = ?";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, identifier);
      ps.setString(2, identifier);

      System.out.println("[DB] Searching for: " + identifier);

      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          System.out.println("[DB] Match found: " + rs.getString("username"));
          return Optional.of(mapUser(rs));
        } else {
          System.out.println("[DB] No match found for: " + identifier);
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find user by identifier", e);
    }
    return Optional.empty();
  }

  public Optional<User> findById(UUID id) {
    try (Connection conn = dataSource.getConnection()) {
      return findById(id, conn);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find user by id", e);
    }
  }

  public Optional<User> findById(UUID id, Connection conn) {
    String sql = "SELECT * FROM users WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id.toString());
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          return Optional.of(mapUser(rs));
        }
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to find user by id", e);
    }
    return Optional.empty();
  }

  public boolean existsByUsername(String username) {
    String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, username);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check username existence", e);
    }
  }

  public boolean existsByEmail(String email) {
    String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
    try (Connection conn = dataSource.getConnection();
        PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, email);
      try (ResultSet rs = ps.executeQuery()) {
        return rs.next();
      }
    } catch (SQLException e) {
      throw new RuntimeException("Failed to check email existence", e);
    }
  }

  // Deletes
  public boolean deleteById(UUID id) {
    try (Connection conn = dataSource.getConnection()) {
      return deleteById(id, conn);
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete user", e);
    }
  }

  public boolean deleteById(UUID id, Connection conn) {
    String sql = "DELETE FROM users WHERE id = ?";
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, id.toString());
      return ps.executeUpdate() > 0;
    } catch (SQLException e) {
      throw new RuntimeException("Failed to delete user", e);
    }
  }

  // Row mapping
  private User mapUser(ResultSet rs) throws SQLException {
    java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
    java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
    java.time.LocalDateTime now = java.time.LocalDateTime.now();
    java.time.LocalDateTime createdAt = createdTs != null ? createdTs.toLocalDateTime() : now;
    java.time.LocalDateTime updatedAt = updatedTs != null ? updatedTs.toLocalDateTime() : now;

    return new User(
        UUID.fromString(rs.getString("id")),
        rs.getString("username"),
        rs.getString("email"),
        rs.getString("password"),
        UserRole.valueOf(rs.getString("role")),
        createdAt,
        updatedAt);
  }
}
