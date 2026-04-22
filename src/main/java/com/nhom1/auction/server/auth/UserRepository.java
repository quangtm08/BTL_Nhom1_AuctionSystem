package com.nhom1.auction.server.auth;

import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
- Execute raw SQL and return result for AuthService
- Interacts with user table
 */
public class UserRepository {
    private final Connection connection;

    public UserRepository(Connection connection) {
        this.connection = connection;
    }



    //Return all users
    public List<User> findAll() {
        String sql = "SELECT * FROM users";
        List<User> users = new ArrayList<>();
        try (Statement stmt = connection.createStatement()) {
            try (ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    users.add(new User(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password"),
                        UserRole.valueOf(rs.getString("role")),
                        LocalDateTime.parse(rs.getString("created_at")),
                        LocalDateTime.parse(rs.getString("updated_at"))
                    ));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }



    //Take username or email. Return user object if found
    public Optional<User> findByIdentifier(String identifier) {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, identifier);
            ps.setString(2, identifier);

            System.out.println("[DB] Searching for: " + identifier);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("[DB] Match found: " + rs.getString("username"));
                    
                    // Robust date parsing to prevent crashes on legacy/manual DB entries
                    LocalDateTime createdAt;
                    LocalDateTime updatedAt;
                    try {
                        createdAt = LocalDateTime.parse(rs.getString("created_at"));
                        updatedAt = LocalDateTime.parse(rs.getString("updated_at"));
                    } catch (Exception e) {
                        System.err.println("Warning: Could not parse dates for user " + rs.getString("username") + ". Using current time.");
                        createdAt = LocalDateTime.now();
                        updatedAt = LocalDateTime.now();
                    }

                    User user = new User(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password"),
                        UserRole.valueOf(rs.getString("role")),
                        createdAt,
                        updatedAt
                    );
                    return Optional.of(user);
                } else {
                    System.out.println("[DB] No match found for: " + identifier);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }



    //Take in username. Returns boolean if user exists
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

    //Take in email. Returns boolean if user exists
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


    //Take in user object. Convert it to SQL fields and save user to database
    public void save(User user) {
        String sql = "INSERT INTO users(id, username, email, password, role, created_at, updated_at) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, user.getId().toString());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getEmail());
            ps.setString(4, user.getPassword());
            ps.setString(5, user.getRole().name());
            ps.setString(6, user.getCreatedAt().toString());
            ps.setString(7, user.getUpdatedAt().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
