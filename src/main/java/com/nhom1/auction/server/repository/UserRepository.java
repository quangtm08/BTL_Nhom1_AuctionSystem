package com.nhom1.auction.server.repository;

import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public class UserRepository {
    private final Connection connection;

    /*
     * The Connection is passed from the outside (Service/Handler).
     * This makes testing easier and ensures we use a single database connection.
     */
    public UserRepository(Connection connection) {
        this.connection = connection;
    }

    /*
     * Find a user by their username OR email.
     * This is used for Login where the user can provide either as an identifier.
     */
    public Optional<User> findByIdentifier(String identifier) {
        String sql = "SELECT * FROM users WHERE username = ? OR email = ?";

        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)) {
            // Fill the two '?' in the SQL string with the identifier
            preparedStatement.setString(1, identifier);
            preparedStatement.setString(2, identifier);

            try (ResultSet rs = preparedStatement.executeQuery()) { //SQL query get executed, return result in an ResultSet object
                if (rs.next()) { //rs.next(): returns true if the ResultSet table has a next row (user exist), false if no next row (user don't exist)
                    // Reconstruct the User object using data from the Database row
                    User user = new User(
                        UUID.fromString(rs.getString("id")),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("password"),
                        UserRole.valueOf(rs.getString("role")),
                        LocalDateTime.parse(rs.getString("created_at")),
                        LocalDateTime.parse(rs.getString("updated_at"))
                    );
                    return Optional.of(user);
                }
            }
        } catch (SQLException e) {
            // Log the error (can replace this with a logger later)
            System.err.println("Error finding user by identifier: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }


    public boolean existsByUsername(String username) {
        String sql = "SELECT 1 FROM users WHERE username = ? LIMIT 1"; //SELECT 1 because only need to check existance

        try(PreparedStatement preparedStatement = connection.prepareStatement(sql)){
            preparedStatement.setString(1, username);
            try (ResultSet rs = preparedStatement.executeQuery()){
                if (rs.next()){
                    return true;
                }
            }
        } catch (SQLException e){
            System.err.println("Error checking if user exists by username: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }



    public boolean existsByEmail(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";

        try(PreparedStatement preparedStatement = connection.prepareStatement(sql)){
            preparedStatement.setString(1, email);
            try (ResultSet rs = preparedStatement.executeQuery()){
                if (rs.next()){
                    return true;
                }
            }
        } catch (SQLException e){
            System.err.println("Error checking if user exists by email: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }



    public void save(User user) {
        String sql = "INSERT INTO users(id, username, email, password, role, created_at, updated_at) "
            + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement preparedStatement = connection.prepareStatement(sql)){
            preparedStatement.setString(1, user.getId().toString()); //SQLite does not have UUID
            preparedStatement.setString(2, user.getUsername());
            preparedStatement.setString(3, user.getEmail());
            preparedStatement.setString(4, user.getPassword());
            preparedStatement.setString(5, user.getRole().name());
            preparedStatement.setString(6, user.getCreatedAt().toString());
            preparedStatement.setString(7, user.getUpdatedAt().toString());

            preparedStatement.executeUpdate();
        } catch (SQLException e){
            System.out.println("Error saving user: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
