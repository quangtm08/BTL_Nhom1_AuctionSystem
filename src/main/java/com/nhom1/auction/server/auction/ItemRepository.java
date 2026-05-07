package com.nhom1.auction.server.auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.UUID;

import com.nhom1.auction.common.entity.Art;
import com.nhom1.auction.common.entity.Electronics;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.entity.Vehicle;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;

/**
 * Repository for Item entity with polymorphic support.
 * Handles Art, Electronics, and Vehicle subtypes based on category.
 */
public class ItemRepository {
    private static final DateTimeFormatter SQLITE_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

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
                brand VARCHAR(100),
                warranty_months INTEGER,
                artist VARCHAR(255),
                era VARCHAR(100),
                production_year INTEGER,
                fuel_type VARCHAR(50),
                created_at TIMESTAMP NOT NULL,
                updated_at TIMESTAMP NOT NULL,
                FOREIGN KEY (seller_id) REFERENCES users(id)
            )
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize items table", e);
        }
    }

    /**
     * Save an Item to the database.
     * - Item is a pure domain object without sellerId
     * - Repository fills seller_id from the parameter
     * - Based on category, fills additional columns (Art/Electronics/Vehicle)
     */

     
    public void save(Item item, UUID sellerId) {
        String sql = """
            INSERT INTO items(id, seller_id, name, description, category, condition, 
                              brand, warranty_months, artist, era, production_year, fuel_type,
                              created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, item.getId().toString());
            ps.setString(2, sellerId.toString());
            ps.setString(3, item.getName());
            ps.setString(4, item.getDescription());
            ps.setString(5, item.getCategory().name());
            ps.setString(6, item.getCondition().name());

            // Category-specific fields
            ItemCategory category = item.getCategory();
            if (category == ItemCategory.ELECTRONICS && item instanceof Electronics electronics) {
                ps.setString(7, electronics.getBrand());
                if (electronics.getWarrantyMonths() != null) {
                    ps.setInt(8, electronics.getWarrantyMonths());
                } else {
                    ps.setNull(8, Types.INTEGER);
                }
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
                ps.setNull(11, Types.INTEGER);
                ps.setNull(12, Types.VARCHAR);
            } else if (category == ItemCategory.ART && item instanceof Art art) {
                ps.setNull(7, Types.VARCHAR);
                ps.setNull(8, Types.INTEGER);
                ps.setString(9, art.getArtist());
                ps.setString(10, art.getEra());
                ps.setNull(11, Types.INTEGER);
                ps.setNull(12, Types.VARCHAR);
            } else if (category == ItemCategory.VEHICLE && item instanceof Vehicle vehicle) {
                ps.setString(7, vehicle.getBrand());
                ps.setNull(8, Types.INTEGER);
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.VARCHAR);
                if (vehicle.getProductionYear() != null) {
                    ps.setInt(11, vehicle.getProductionYear());
                } else {
                    ps.setNull(11, Types.INTEGER);
                }
                ps.setString(12, vehicle.getFuelType() != null ? vehicle.getFuelType().name() : null);
            } else {
                // Default: all null
                for (int i = 7; i <= 12; i++) {
                    ps.setNull(i, Types.VARCHAR);
                }
            }

            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            ps.setTimestamp(13, now);
            ps.setTimestamp(14, now);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save item", e);
        }
    }

    /**
     * Find an Item by ID and reconstruct the correct subtype based on category column.
     */
    public Optional<Item> findById(UUID id) {
        String sql = "SELECT * FROM items WHERE id = CAST(? AS VARCHAR)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, id.toString());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
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
                rs.getString("brand"),
                (Integer) rs.getObject("warranty_months"),
                createdAt, updatedAt
            );
            case ART -> new Art(
                id, name, description, category, condition,
                rs.getString("artist"),
                rs.getString("era"),
                createdAt, updatedAt
            );
            case VEHICLE -> new Vehicle(
                id, name, description, category, condition,
                rs.getString("brand"),
                (Integer) rs.getObject("production_year"),
                rs.getString("fuel_type") != null ? VehicleFuelType.valueOf(rs.getString("fuel_type")) : null,
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
        }
    }

    private LocalDateTime parseSqliteDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        if (value.contains("T")) {
            return LocalDateTime.parse(value);
        }
        return LocalDateTime.parse(value, SQLITE_DATE_TIME_FORMATTER);
    }
}
