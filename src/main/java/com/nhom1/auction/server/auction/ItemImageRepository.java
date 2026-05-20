package com.nhom1.auction.server.auction;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;

public class ItemImageRepository {
    private final DataSource dataSource;

    public ItemImageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void saveImageUrls(UUID itemId, List<String> imageUrls) {
        try (Connection conn = dataSource.getConnection()) {
            saveImageUrls(itemId, imageUrls, conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save item images", e);
        }
    }

    public void saveImageUrls(UUID itemId, List<String> imageUrls, Connection connection) {
        if (itemId == null || imageUrls == null || imageUrls.isEmpty()) {
            return;
        }
        String sql = """
            INSERT INTO item_images(id, item_id, object_key, public_url, is_primary, sort_order, created_at, updated_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            int order = 0;
            for (String imageUrl : imageUrls) {
                if (imageUrl == null || imageUrl.isBlank()) continue;
                String cleaned = imageUrl.trim();
                ps.setString(1, UUID.randomUUID().toString());
                ps.setString(2, itemId.toString());
                ps.setString(3, buildObjectKey(itemId, order, cleaned));
                ps.setString(4, cleaned);
                ps.setBoolean(5, order == 0);
                ps.setInt(6, order);
                ps.setTimestamp(7, new java.sql.Timestamp(now));
                ps.setTimestamp(8, new java.sql.Timestamp(now));
                ps.addBatch();
                order++;
            }
            ps.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException(
                    "Failed to save item images: " + e.getMessage() + " (SQLState=" + e.getSQLState() + ")",
                    e);
        }
    }

    public List<String> findImageUrlsByItemId(UUID itemId) {
        try (Connection conn = dataSource.getConnection()) {
            return findImageUrlsByItemId(itemId, conn);
        } catch (SQLException e) {
            if (isMissingItemImagesTable(e)) {
                return List.of();
            }
            throw new RuntimeException("Failed to fetch item images", e);
        }
    }

    public List<String> findImageUrlsByItemId(UUID itemId, Connection connection) {
        String sql = """
            SELECT public_url
            FROM item_images
            WHERE item_id = ?
            ORDER BY is_primary DESC, sort_order ASC, created_at ASC
            """;
        List<String> urls = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, itemId.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    urls.add(rs.getString("public_url"));
                }
            }
            return urls;
        } catch (SQLException e) {
            if (isMissingItemImagesTable(e)) {
                return List.of();
            }
            throw new RuntimeException("Failed to fetch item images", e);
        }
    }

    private boolean isMissingItemImagesTable(SQLException e) {
        String message = e.getMessage();
        String sqlState = e.getSQLState();
        return (message != null
                && (message.toLowerCase().contains("no such table")
                || message.toLowerCase().contains("item_images")
                || message.toLowerCase().contains("does not exist")))
                || "42P01".equals(sqlState);
    }

    private String buildObjectKey(UUID itemId, int sortOrder, String imageUrl) {
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        if (filename.isBlank()) {
            filename = "image-" + sortOrder;
        }
        return "items/" + itemId + "/" + sortOrder + "-" + filename;
    }
}
