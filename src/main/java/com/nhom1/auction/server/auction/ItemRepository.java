package com.nhom1.auction.server.auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import javax.sql.DataSource;

import com.nhom1.auction.common.entity.Art;
import com.nhom1.auction.common.entity.Electronics;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.entity.Vehicle;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import com.nhom1.auction.common.enums.VehicleFuelType;

public class ItemRepository {
    private final DataSource dataSource;

    public ItemRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // ===================== SAVE =====================
    public void save(Item item, UUID sellerId) {
        try (Connection conn = dataSource.getConnection()) {
            save(item, sellerId, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save item", e);
        }
    }

    public void save(Item item, UUID sellerId, Connection conn) {
        String sql = """
            INSERT INTO items(id, seller_id, name, description, category, condition,
                              brand, warranty_months, artist, era, production_year, fuel_type,
                              created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getId().toString());
            ps.setString(2, sellerId.toString());
            ps.setString(3, item.getName());
            ps.setString(4, item.getDescription());
            ps.setString(5, item.getCategory().name());
            ps.setString(6, item.getCondition().name());

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
                for (int i = 7; i <= 12; i++) {
                    ps.setNull(i, Types.VARCHAR);
                }
            }

            java.sql.Timestamp now = new java.sql.Timestamp(System.currentTimeMillis());
            ps.setTimestamp(13, now);
            ps.setTimestamp(14, now);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save item", e);
        }
    }

    // ===================== FIND BY ID =====================
    public Optional<Item> findById(UUID id) {
        try (Connection conn = dataSource.getConnection()) {
            return findById(id, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find item by id", e);
        }
    }

    public Optional<Item> findById(UUID id, Connection conn) {
        String sql = "SELECT * FROM items WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToItem(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find item by id", e);
        }
        return Optional.empty();
    }

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

    // ===================== DELETE BY ID =====================
    public int deleteById(UUID itemId) {
        try (Connection conn = dataSource.getConnection()) {
            return deleteById(itemId, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete item", e);
        }
    }

    public int deleteById(UUID itemId, Connection conn) {
        String sql = "DELETE FROM items WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemId.toString());
            return ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete item", e);
        }
    }
}
