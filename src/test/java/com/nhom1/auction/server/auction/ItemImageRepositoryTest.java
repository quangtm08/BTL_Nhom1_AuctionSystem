package com.nhom1.auction.server.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ItemImageRepositoryTest {

  private DataSource mockDataSource;
  private Connection mockConnection;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;
  private ItemImageRepository repo;

  @BeforeEach
  public void setUp() throws SQLException {
    mockDataSource = mock(DataSource.class);
    mockConnection = mock(Connection.class);
    mockPreparedStatement = mock(PreparedStatement.class);
    mockResultSet = mock(ResultSet.class);

    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

    repo = new ItemImageRepository(mockDataSource);
  }

  @Test
  public void testSaveImageUrls_NullOrEmpty() throws SQLException {
    repo.saveImageUrls(UUID.randomUUID(), null);
    repo.saveImageUrls(null, List.of("http://test.com/img.png"));
    repo.saveImageUrls(UUID.randomUUID(), Collections.emptyList());

    verify(mockConnection, never()).prepareStatement(anyString());
  }

  @Test
  public void testSaveImageUrls_Success() throws SQLException {
    UUID itemId = UUID.randomUUID();
    repo.saveImageUrls(
        itemId,
        java.util.Arrays.asList(
            "http://test.com/img1.png", "  ", null, "http://test.com/img2.png"));
    verify(mockPreparedStatement, times(2)).addBatch();
    verify(mockPreparedStatement).executeBatch();
  }

  @Test
  public void testSaveImageUrls_ThrowsException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockPreparedStatement.executeBatch()).thenThrow(new SQLException("Batch error", "42000"));

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> repo.saveImageUrls(itemId, List.of("http://test.com/img.png")));
    assertTrue(thrown.getMessage().contains("Failed to save item images"));
  }

  @Test
  public void testSaveImageUrlsWithConnection_ThrowsException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> repo.saveImageUrls(itemId, List.of("http://test.com/img.png")));
    assertTrue(thrown.getMessage().contains("Failed to save item images"));
  }

  @Test
  public void testFindImageUrlsByItemId_Success() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockResultSet.next()).thenReturn(true, true, false);
    when(mockResultSet.getString("public_url")).thenReturn("url1", "url2");

    List<String> list = repo.findImageUrlsByItemId(itemId);
    assertEquals(2, list.size());
    assertEquals("url1", list.get(0));
    assertEquals("url2", list.get(1));
  }

  @Test
  public void testFindImageUrlsByItemId_MissingTableException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("no such table: item_images", "42P01"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findImageUrlsByItemId(itemId));
    assertTrue(thrown.getMessage().contains("Failed to fetch item images"));
  }

  @Test
  public void testFindImageUrlsByItemId_OtherException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockConnection.prepareStatement(anyString()))
        .thenThrow(new SQLException("Other DB error", "50000"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findImageUrlsByItemId(itemId));
    assertTrue(thrown.getMessage().contains("Failed to fetch item images"));
  }

  @Test
  public void testFindImageUrlsByItemIdWithConnection_MissingTableException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockDataSource.getConnection())
        .thenThrow(new SQLException("relation item_images does not exist", "42P01"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findImageUrlsByItemId(itemId));
    assertTrue(thrown.getMessage().contains("Failed to fetch item images"));
  }

  @Test
  public void testFindImageUrlsByItemIdWithConnection_OtherException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findImageUrlsByItemId(itemId));
    assertTrue(thrown.getMessage().contains("Failed to fetch item images"));
  }
}
