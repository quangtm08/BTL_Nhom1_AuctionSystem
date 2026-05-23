package com.nhom1.auction.server.payment;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.payment.PaymentHistoryEntryDto;
import com.nhom1.auction.common.dto.payment.PendingPaymentDto;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class PaymentRepositoryTest {

  private DataSource mockDataSource;
  private Connection mockConnection;
  private Statement mockStatement;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;

  @BeforeEach
  public void setUp() throws SQLException {
    mockDataSource = mock(DataSource.class);
    mockConnection = mock(Connection.class);
    mockStatement = mock(Statement.class);
    mockPreparedStatement = mock(PreparedStatement.class);
    mockResultSet = mock(ResultSet.class);

    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.createStatement()).thenReturn(mockStatement);
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
  }

  @Test
  public void testConstructor_Success() {
    assertDoesNotThrow(() -> new PaymentRepository(mockDataSource));
  }

  @Test
  public void testSaveCompletedPayment_Success() throws SQLException {
    PaymentRepository repo = new PaymentRepository(mockDataSource);

    UUID auctionId = UUID.randomUUID();
    UUID payerId = UUID.randomUUID();
    UUID payeeId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.0");
    LocalDateTime now = LocalDateTime.now();

    repo.saveCompletedPayment(auctionId, payerId, payeeId, amount, now);

    verify(mockPreparedStatement).executeUpdate();
  }

  @Test
  public void testSaveCompletedPayment_ThrowsSQLException() throws SQLException {
    PaymentRepository repo = new PaymentRepository(mockDataSource);
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep error"));

    UUID auctionId = UUID.randomUUID();
    UUID payerId = UUID.randomUUID();
    UUID payeeId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("100.0");
    LocalDateTime now = LocalDateTime.now();

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> repo.saveCompletedPayment(auctionId, payerId, payeeId, amount, now));
    assertTrue(thrown.getMessage().contains("Failed to save payment transaction"));
  }

  @Test
  public void testExistsCompletedPaymentForAuction() throws SQLException {
    PaymentRepository repo = new PaymentRepository(mockDataSource);
    UUID auctionId = UUID.randomUUID();

    // Test returns true
    when(mockResultSet.next()).thenReturn(true);
    assertTrue(repo.existsCompletedPaymentForAuction(auctionId));

    // Test returns false
    when(mockResultSet.next()).thenReturn(false);
    assertFalse(repo.existsCompletedPaymentForAuction(auctionId));
  }

  @Test
  public void testExistsCompletedPaymentForAuction_ThrowsSQLException() throws SQLException {
    PaymentRepository repo = new PaymentRepository(mockDataSource);
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Query error"));
    UUID auctionId = UUID.randomUUID();

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> repo.existsCompletedPaymentForAuction(auctionId));
    assertTrue(thrown.getMessage().contains("Failed to check payment transaction"));
  }

  @Test
  public void testFindPendingPaymentsByBidder() throws SQLException {
    PaymentRepository repo = new PaymentRepository(mockDataSource);
    UUID bidderId = UUID.randomUUID();

    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("auction_id")).thenReturn(UUID.randomUUID().toString());
    when(mockResultSet.getString("item_name")).thenReturn("Wood Table");
    when(mockResultSet.getString("item_category")).thenReturn("FURNITURE");
    when(mockResultSet.getBigDecimal("amount")).thenReturn(new BigDecimal("150.0"));
    when(mockResultSet.getTimestamp("end_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

    List<PendingPaymentDto> pending = repo.findPendingPaymentsByBidder(bidderId);
    assertEquals(1, pending.size());
    assertEquals("Wood Table", pending.get(0).getItemName());
  }

  @Test
  public void testFindPendingPaymentsByBidder_ThrowsSQLException() throws SQLException {
    PaymentRepository repo = new PaymentRepository(mockDataSource);
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Query error"));
    UUID bidderId = UUID.randomUUID();

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findPendingPaymentsByBidder(bidderId));
    assertTrue(thrown.getMessage().contains("Failed to load pending payments"));
  }

  @Test
  public void testFindPaymentHistoryForUser() throws SQLException {
    PaymentRepository repo = new PaymentRepository(mockDataSource);
    UUID userId = UUID.randomUUID();

    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("auction_id")).thenReturn(UUID.randomUUID().toString());
    when(mockResultSet.getString("item_name")).thenReturn("Painting");
    when(mockResultSet.getBigDecimal("amount")).thenReturn(new BigDecimal("500.0"));
    when(mockResultSet.getString("direction")).thenReturn("PAY");
    when(mockResultSet.getTimestamp("created_at"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now()));

    List<PaymentHistoryEntryDto> history = repo.findPaymentHistoryForUser(userId);
    assertEquals(1, history.size());
    assertEquals("Painting", history.get(0).getItemName());
    assertEquals("PAY", history.get(0).getDirection());
  }

  @Test
  public void testFindPaymentHistoryForUser_ThrowsSQLException() throws SQLException {
    PaymentRepository repo = new PaymentRepository(mockDataSource);
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Query error"));
    UUID userId = UUID.randomUUID();

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findPaymentHistoryForUser(userId));
    assertTrue(thrown.getMessage().contains("Failed to load payment history"));
  }
}
