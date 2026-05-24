package com.nhom1.auction.server.auction;

import com.nhom1.auction.common.entity.Art;
import com.nhom1.auction.common.entity.Electronics;
import com.nhom1.auction.common.entity.Item;
import com.nhom1.auction.common.entity.Vehicle;
import com.nhom1.auction.common.enums.ItemCategory;
import com.nhom1.auction.common.enums.ItemCondition;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;

public class ItemRepository {
  private final DataSource dataSource;

  public ItemRepository(DataSource dataSource) {
    this.dataSource = dataSource;
  }

  // Writes
  public void save(Item item, UUID sellerId, Connection conn) {
    String sql =
        """
        INSERT INTO items(id, seller_id, name, description, category, condition,
                          created_at, updated_at)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
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
    }
  }

  // Reads
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

  // Updates
  public int updateBasicInfo(
      UUID itemId,
      String name,
      String description,
      ItemCategory category,
      ItemCondition condition,
      Connection conn) {
    String sql =
        """
        UPDATE items
        SET name = ?, description = ?, category = ?, condition = ?, updated_at = ?
        WHERE id = ?
        """;
    try (PreparedStatement ps = conn.prepareStatement(sql)) {
      ps.setString(1, name);
      ps.setString(2, description);
      ps.setString(3, category.name());
      ps.setString(4, condition.name());
      ps.setTimestamp(5, java.sql.Timestamp.valueOf(LocalDateTime.now()));
      ps.setString(6, itemId.toString());
      return ps.executeUpdate();
    } catch (SQLException e) {
      throw new RuntimeException("Failed to update item", e);
    }
  }

  // Deletes
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

  // Row mapping
  private Item mapResultSetToItem(ResultSet rs) throws SQLException {
    UUID id = UUID.fromString(rs.getString("id"));
    String name = rs.getString("name");
    String description = rs.getString("description");
    ItemCategory category = ItemCategory.valueOf(rs.getString("category"));
    ItemCondition condition = ItemCondition.valueOf(rs.getString("condition"));
    java.sql.Timestamp createdTs = rs.getTimestamp("created_at");
    java.sql.Timestamp updatedTs = rs.getTimestamp("updated_at");
    LocalDateTime createdAt =
        (createdTs != null) ? createdTs.toLocalDateTime() : LocalDateTime.now();
    LocalDateTime updatedAt =
        (updatedTs != null) ? updatedTs.toLocalDateTime() : LocalDateTime.now();

    return switch (category) {
      case ELECTRONICS ->
          new Electronics(id, name, description, category, condition, createdAt, updatedAt);
      case ART -> new Art(id, name, description, category, condition, createdAt, updatedAt);
      case VEHICLE -> new Vehicle(id, name, description, category, condition, createdAt, updatedAt);
    };
  }
}
