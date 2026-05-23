package com.nhom1.auction.server.admin;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SqlAdminAuctionGatewayTest {

  @Mock private DataSource dataSource;

  @Mock private Connection connection;

  @Mock private PreparedStatement preparedStatement;

  private SqlAdminAuctionGateway gateway;

  @BeforeEach
  public void setUp() throws SQLException {
    MockitoAnnotations.openMocks(this);
    when(dataSource.getConnection()).thenReturn(connection);
    gateway = new SqlAdminAuctionGateway(dataSource);
  }

  @Test
  public void testCancelAuctionById_NoRowsUpdated_ReturnsFalse() throws SQLException {
    String auctionId = "auction-1";
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeUpdate()).thenReturn(0);

    boolean result = gateway.cancelAuctionById(auctionId);

    assertFalse(result);
    verify(preparedStatement).setString(1, auctionId);
  }

  @Test
  public void testCancelAuctionById_SqlFailure_PropagatesRuntimeException() throws SQLException {
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException("database down"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> gateway.cancelAuctionById("auction-1"));

    assertEquals("Failed to cancel auction by id", thrown.getMessage());
    assertInstanceOf(SQLException.class, thrown.getCause());
  }

  @Test
  public void testFindAllAuctionSummaries_Success() throws SQLException {
    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("id")).thenReturn("auc-1");
    when(rs.getString("item_name")).thenReturn("Item Name");
    when(rs.getString("category")).thenReturn("ART");
    when(rs.getBigDecimal("starting_price")).thenReturn(java.math.BigDecimal.TEN);
    when(rs.getBigDecimal("current_highest_bid")).thenReturn(java.math.BigDecimal.valueOf(15));
    when(rs.getTimestamp("start_time"))
        .thenReturn(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));
    when(rs.getTimestamp("end_time"))
        .thenReturn(java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().plusDays(1)));
    when(rs.getString("status")).thenReturn("OPEN");
    when(rs.getString("seller_id")).thenReturn("seller-123");

    var list = gateway.findAllAuctionSummaries();
    assertEquals(1, list.size());
    assertEquals("auc-1", list.get(0).getId());
    assertEquals("Item Name", list.get(0).getItemName());
  }

  @Test
  public void testFindAllAuctionSummaries_Exception() throws SQLException {
    when(connection.prepareStatement(anyString())).thenThrow(new SQLException("Query failed"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> gateway.findAllAuctionSummaries());
    assertTrue(thrown.getMessage().contains("Failed to list auction summaries"));
  }

  @Test
  public void testFindAllAuctionSummaries_NullTimestamps_BranchCovered() throws SQLException {
    // Tests null start_time and null end_time branches (ternary → null)
    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(true, false);
    when(rs.getString("id")).thenReturn("auc-2");
    when(rs.getString("item_name")).thenReturn("Painting");
    when(rs.getString("category")).thenReturn("ART");
    when(rs.getBigDecimal("starting_price")).thenReturn(java.math.BigDecimal.valueOf(200));
    when(rs.getBigDecimal("current_highest_bid")).thenReturn(null);
    when(rs.getTimestamp("start_time")).thenReturn(null); // null → s = null
    when(rs.getTimestamp("end_time")).thenReturn(null);   // null → e = null
    when(rs.getString("status")).thenReturn("PENDING");
    when(rs.getString("seller_id")).thenReturn("seller-456");

    var list = gateway.findAllAuctionSummaries();
    assertEquals(1, list.size());
    assertNull(list.get(0).getStartTime());
    assertNull(list.get(0).getEndTime());
  }

  @Test
  public void testFindAllAuctionSummaries_EmptyResultSet() throws SQLException {
    // Tests the while loop not entered path
    java.sql.ResultSet rs = mock(java.sql.ResultSet.class);
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeQuery()).thenReturn(rs);
    when(rs.next()).thenReturn(false); // empty

    var list = gateway.findAllAuctionSummaries();
    assertTrue(list.isEmpty());
  }

  @Test
  public void testCancelAuctionById_OneRowUpdated_ReturnsTrue() throws SQLException {
    when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    when(preparedStatement.executeUpdate()).thenReturn(1);

    boolean result = gateway.cancelAuctionById("auc-x");
    assertTrue(result);
  }
}

