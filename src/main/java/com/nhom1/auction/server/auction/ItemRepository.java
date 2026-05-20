package com.nhom1.auction.server.auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import com.nhom1.auction.common.entity.Art;
import com.nhom1.auction.common.entity.Electronics;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.entity.Vehicle;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;

/**
 * Repository for Item entity with polymorphic support.
 * Handles Art, Electronics, and Vehicle subtypes based on category.
 */
public class ItemRepository {
    private final Connection connection;

    public ItemRepository(Connection connection) {
        this.connection = connection;
        ensureTable();
    }

    private void ensureTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS items (
                id VARCHAR(36) PRIMARY KEY,
                seller_id VARCHAR(36) NOT NULL,
                name VARCHAR(255) NOT NULL,
                description TEXT,
                category VARCHAR(50) NOT NULL,
                condition VARCHAR(50) NOT NULL,
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                FOREIGN KEY (seller_id) REFERENCES users(id)
            );
            CREATE INDEX IF NOT EXISTS idx_items_seller_id ON items(seller_id);
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize items table", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Invalid data while initializing items table", e);
        }
    }

    /**
     * Save an Item to the database.
     * - Item is a pure domain object without sellerId
     * - Repository fills seller_id from the parameter
     */


    public void save(Item item, UUID sellerId) {
        String sql = """
            INSERT INTO items(id, seller_id, name, description, category, condition,
                              created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, item.getId().toString());
            ps.setString(2, sellerId.toString());
            ps.setString(3, item.getName());
            ps.setString(4, item.getDescription());
            ps.setString(5, item.getCategory().name());
            ps.setString(6, item.getCondition().name());

            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            ps.setTimestamp(7, now);
            ps.setTimestamp(8, now);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save item", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Invalid item data while saving item", e);
        }
    }

    /**
     * Find an Item by ID and reconstruct the correct subtype based on category column.
     */
    public Optional<Item> findById(UUID id) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find item by id", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to map item by id", e);
        }
        return Optional.empty();
    }

    /**
     * Map ResultSet to the appropriate Item subtype based on category.
     */
    private Item mapResultSetToItem(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        String name = rs.getString("name");
        String description = rs.getString("description");
        ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
        ItemCondition condition = ItemCondition.valueOf(rs.getString("condition"));
        java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
        java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
        LocalDateTime createdAt = (createdTs != null) ? createdTs.toLocalDateTime() : LocalDateTime.now();
        LocalDateTime updatedAt = (updatedTs != null) ? updatedTs.toLocalDateTime() : LocalDateTime.now();

        return switch (category) {
            case ELECTRONICS -> new Electronics(
                id, name, description, category, condition,
                createdAt, updatedAt
            );
            case ART -> new Art(
                id, name, description, category, condition,
                createdAt, updatedAt
            );
            case VEHICLE -> new Vehicle(
                id, name, description, category, condition,
                createdAt, updatedAt
            );
        };
    }

    public int deleteById(UUID itemId) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, itemId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete item", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Invalid item id while deleting item", e);
        }
    }

}
