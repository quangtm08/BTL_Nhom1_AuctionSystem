package com.nhom1.auction.server.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ItemRepositoryTest {

  private DataSource mockDataSource;
  private Connection mockConnection;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;
  private ItemRepository repo;

  @BeforeEach
  public void setUp() throws SQLException {
    mockDataSource = mock(DataSource.class);
    mockConnection = mock(Connection.class);
    mockPreparedStatement = mock(PreparedStatement.class);
    mockResultSet = mock(ResultSet.class);

    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

    repo = new ItemRepository(mockDataSource);
  }

  @Test
  public void testSave_Success() throws SQLException {
    UUID itemId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    Item item =
        new Electronics(
            itemId,
            "Phone",
            "Nice",
            ItemCategory.ELECTRONICS,
            ItemCondition.NEW,
            LocalDateTime.now(),
            LocalDateTime.now());

    repo.save(item, sellerId, mockConnection);
    verify(mockPreparedStatement).executeUpdate();
  }

  @Test
  public void testSave_ThrowsException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    UUID sellerId = UUID.randomUUID();
    Item item =
        new Electronics(
            itemId,
            "Phone",
            "Nice",
            ItemCategory.ELECTRONICS,
            ItemCondition.NEW,
            LocalDateTime.now(),
            LocalDateTime.now());

    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep error"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.save(item, sellerId, mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to save item"));
  }

  @Test
  public void testFindById_Electronics() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("id")).thenReturn(itemId.toString());
    when(mockResultSet.getString("name")).thenReturn("Phone");
    when(mockResultSet.getString("description")).thenReturn("Nice");
    when(mockResultSet.getString("category")).thenReturn("ELECTRONICS");
    when(mockResultSet.getString("condition")).thenReturn("NEW");
    when(mockResultSet.getTimestamp("created_at"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    when(mockResultSet.getTimestamp("updated_at"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now()));

    Optional<Item> itemOpt = repo.findById(itemId);
    assertTrue(itemOpt.isPresent());
    assertTrue(itemOpt.get() instanceof Electronics);
    assertEquals("Phone", itemOpt.get().getName());
  }

  @Test
  public void testFindById_Art() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("id")).thenReturn(itemId.toString());
    when(mockResultSet.getString("name")).thenReturn("Painting");
    when(mockResultSet.getString("description")).thenReturn("Beautiful");
    when(mockResultSet.getString("category")).thenReturn("ART");
    when(mockResultSet.getString("condition")).thenReturn("NEW");

    Optional<Item> itemOpt = repo.findById(itemId);
    assertTrue(itemOpt.isPresent());
    assertTrue(itemOpt.get() instanceof Art);
  }

  @Test
  public void testFindById_Vehicle() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("id")).thenReturn(itemId.toString());
    when(mockResultSet.getString("name")).thenReturn("Car");
    when(mockResultSet.getString("description")).thenReturn("Fast");
    when(mockResultSet.getString("category")).thenReturn("VEHICLE");
    when(mockResultSet.getString("condition")).thenReturn("NEW");

    Optional<Item> itemOpt = repo.findById(itemId);
    assertTrue(itemOpt.isPresent());
    assertTrue(itemOpt.get() instanceof Vehicle);
  }

  @Test
  public void testFindById_NotFound() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockResultSet.next()).thenReturn(false);

    Optional<Item> itemOpt = repo.findById(itemId);
    assertFalse(itemOpt.isPresent());
  }

  @Test
  public void testFindById_ThrowsException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> repo.findById(itemId));
    assertTrue(thrown.getMessage().contains("Failed to find item by id"));
  }

  @Test
  public void testFindByIdWithConnection_ThrowsException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep failed"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findById(itemId, mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to find item by id"));
  }

  @Test
  public void testDeleteById_Success() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockPreparedStatement.executeUpdate()).thenReturn(1);

    int result = repo.deleteById(itemId);
    assertEquals(1, result);
  }

  @Test
  public void testDeleteById_ThrowsException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> repo.deleteById(itemId));
    assertTrue(thrown.getMessage().contains("Failed to delete item"));
  }

  @Test
  public void testDeleteByIdWithConnection_ThrowsException() throws SQLException {
    UUID itemId = UUID.randomUUID();
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep failed"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.deleteById(itemId, mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to delete item"));
  }
}
