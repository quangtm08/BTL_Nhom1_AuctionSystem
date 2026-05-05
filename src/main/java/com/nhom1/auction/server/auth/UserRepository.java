package com.nhom1.auction.server.auth;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;

/**
 * - Execute raw SQL and return result for AuthService
 * - Interacts with user table
 */
public class UserRepository {
    private final Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
    }

    // Return all users
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        try (Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    users.add(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    // Take username or email. Return user object if found
    public Optional<User> findByIdentifier(String identifier) {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
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
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // Shared dependency point for Binh's admin/payment features.
    // AuthModule already exports UserRepository via ServerContext, so other
    // modules can safely reuse this lookup without re-owning user SQL.
    public Optional<User> findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    // Take in username. Returns boolean if user exists
    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Take in email. Returns boolean if user exists
    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    // Take in user object. Convert it to SQL fields and save user to database
    public void save(User user) {
        String sql = "INSERT INTO users(id, username, email, password, role, created_at, updated_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getId().toString());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getRole().name());
            ps.setTimestamp(6, java.sql.Timestamp.valueOf(user.getCreatedAt()));
            ps.setTimestamp(7, java.sql.Timestamp.valueOf(user.getUpdatedAt()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Shared dependency point for Binh's admin feature.
    public boolean deleteById(UUID id) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            return ps.executeUpdate() > 0;
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        LocalDateTime createdAt;
        LocalDateTime updatedAt;
        try {
            java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
            java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
            createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : LocalDateTime.now();
            updatedAt = (updatedTs != null) ? updatedTs.toLocalDateTime() : LocalDateTime.now();
        } catch (Exception e) {
            System.err.println("Warning: Could not parse dates for user " + rs.getString("username")
                    + ". Using current time.");
            createdAt = LocalDateTime.now();
            updatedAt = LocalDateTime.now();
        }

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
