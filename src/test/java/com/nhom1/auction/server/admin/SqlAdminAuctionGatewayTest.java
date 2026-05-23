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
}
