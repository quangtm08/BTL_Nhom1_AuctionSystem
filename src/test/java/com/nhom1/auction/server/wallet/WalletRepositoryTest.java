package com.nhom1.auction.server.wallet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.entity.Wallet;
import com.nhom1.auction.common.entity.WalletTransaction;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WalletRepositoryTest {

  private DataSource mockDataSource;
  private Connection mockConnection;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;

  @BeforeEach
  public void setUp() throws SQLException {
    mockDataSource = mock(DataSource.class);
    mockConnection = mock(Connection.class);
    mockPreparedStatement = mock(PreparedStatement.class);
    mockResultSet = mock(ResultSet.class);

    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
  }

  @Test
  public void testConstructor_Success() {
    assertDoesNotThrow(() -> new WalletRepository(mockDataSource));
  }

  @Test
  public void testFindByUserId_WalletExists() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    BigDecimal balance = new BigDecimal("150.0");
    LocalDateTime now = LocalDateTime.now();

    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getBigDecimal("balance")).thenReturn(balance);
    when(mockResultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));
    when(mockResultSet.getTimestamp("updated_at")).thenReturn(Timestamp.valueOf(now));

    Optional<Wallet> walletOpt = repo.findByUserId(userId);
    assertTrue(walletOpt.isPresent());
    assertEquals(userId, walletOpt.get().getUserId());
    assertEquals(balance, walletOpt.get().getBalance());
  }

  @Test
  public void testFindByUserId_WalletDoesNotExist() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();

    when(mockResultSet.next()).thenReturn(false);

    Optional<Wallet> walletOpt = repo.findByUserId(userId);
    assertFalse(walletOpt.isPresent());
  }

  @Test
  public void testFindByUserId_ThrowsSQLException() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    when(mockDataSource.getConnection()).thenThrow(new SQLException("DB error"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> repo.findByUserId(userId));
    assertTrue(thrown.getMessage().contains("Failed to find wallet for user"));
  }

  @Test
  public void testFindByUserIdWithConnection_ThrowsSQLException() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep error"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findByUserId(userId, mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to find wallet for user"));
  }

  @Test
  public void testSave_UpdateExistingWallet() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    BigDecimal balance = new BigDecimal("100.0");
    Wallet wallet = new Wallet(userId, balance);

    PreparedStatement mockExistsPs = mock(PreparedStatement.class);
    PreparedStatement mockUpdatePs = mock(PreparedStatement.class);
    ResultSet mockExistsRs = mock(ResultSet.class);

    when(mockConnection.prepareStatement(contains("SELECT 1 FROM wallets")))
        .thenReturn(mockExistsPs);
    when(mockConnection.prepareStatement(contains("UPDATE wallets"))).thenReturn(mockUpdatePs);
    when(mockExistsPs.executeQuery()).thenReturn(mockExistsRs);
    when(mockExistsRs.next()).thenReturn(true);

    repo.save(wallet);

    verify(mockUpdatePs).setBigDecimal(eq(1), eq(balance));
    verify(mockUpdatePs).executeUpdate();
  }

  @Test
  public void testSave_InsertNewWallet() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    BigDecimal balance = new BigDecimal("100.0");
    Wallet wallet = new Wallet(userId, balance);

    PreparedStatement mockExistsPs = mock(PreparedStatement.class);
    PreparedStatement mockInsertPs = mock(PreparedStatement.class);
    ResultSet mockExistsRs = mock(ResultSet.class);

    when(mockConnection.prepareStatement(contains("SELECT 1 FROM wallets")))
        .thenReturn(mockExistsPs);
    when(mockConnection.prepareStatement(contains("INSERT INTO wallets"))).thenReturn(mockInsertPs);
    when(mockExistsPs.executeQuery()).thenReturn(mockExistsRs);
    when(mockExistsRs.next()).thenReturn(false);

    repo.save(wallet);

    verify(mockInsertPs).setString(eq(1), eq(userId.toString()));
    verify(mockInsertPs).setBigDecimal(eq(2), eq(balance));
    verify(mockInsertPs).executeUpdate();
  }

  @Test
  public void testSave_ThrowsSQLException() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(userId, BigDecimal.TEN);
    when(mockDataSource.getConnection()).thenThrow(new SQLException("DB error"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> repo.save(wallet));
    assertTrue(thrown.getMessage().contains("Failed to save wallet"));
  }

  @Test
  public void testSaveWithConnection_ThrowsSQLException() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    Wallet wallet = new Wallet(userId, BigDecimal.TEN);
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep error"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.save(wallet, mockConnection));
    assertTrue(
        thrown.getMessage().contains("Failed to check wallet existence")
            || thrown.getMessage().contains("Failed to insert wallet")
            || thrown.getMessage().contains("Failed to update wallet"));
  }

  @Test
  public void testFindTransactionsByUserId() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    UUID txId = UUID.randomUUID();
    BigDecimal amount = new BigDecimal("50.0");
    LocalDateTime now = LocalDateTime.now();

    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("id")).thenReturn(txId.toString());
    when(mockResultSet.getBigDecimal("amount")).thenReturn(amount);
    when(mockResultSet.getString("transaction_type")).thenReturn("DEPOSIT");
    when(mockResultSet.getString("reference_id")).thenReturn("ref-1");
    when(mockResultSet.getString("description")).thenReturn("desc");
    when(mockResultSet.getTimestamp("created_at")).thenReturn(Timestamp.valueOf(now));

    List<WalletTransaction> txs = repo.findTransactionsByUserId(userId);
    assertEquals(1, txs.size());
    WalletTransaction tx = txs.get(0);
    assertEquals(txId, tx.getId());
    assertEquals(userId, tx.getUserId());
    assertEquals(amount, tx.getAmount());
    assertEquals("DEPOSIT", tx.getTransactionType());
  }

  @Test
  public void testFindTransactionsByUserId_ThrowsSQLException() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    when(mockDataSource.getConnection()).thenThrow(new SQLException("DB error"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findTransactionsByUserId(userId));
    assertTrue(thrown.getMessage().contains("Failed to find transactions for user"));
  }

  @Test
  public void testFindTransactionsByUserIdWithConnection_ThrowsSQLException() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep error"));

    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> repo.findTransactionsByUserId(userId, mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to find transactions for user"));
  }

  @Test
  public void testSaveTransaction_Success() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    UUID userId = UUID.randomUUID();
    WalletTransaction tx =
        new WalletTransaction(userId, new BigDecimal("10.0"), "WITHDRAW", "ref-2", "withdraw test");

    repo.saveTransaction(tx);

    verify(mockPreparedStatement).setString(eq(1), eq(tx.getId().toString()));
    verify(mockPreparedStatement).setString(eq(2), eq(userId.toString()));
    verify(mockPreparedStatement).setBigDecimal(eq(3), eq(tx.getAmount()));
    verify(mockPreparedStatement).setString(eq(4), eq("WITHDRAW"));
    verify(mockPreparedStatement).setString(eq(5), eq("ref-2"));
    verify(mockPreparedStatement).setString(eq(6), eq("withdraw test"));
    verify(mockPreparedStatement).executeUpdate();
  }

  @Test
  public void testSaveTransaction_ThrowsSQLException() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    WalletTransaction tx =
        new WalletTransaction(UUID.randomUUID(), BigDecimal.ONE, "DEPOSIT", null, "test");
    when(mockDataSource.getConnection()).thenThrow(new SQLException("DB error"));

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> repo.saveTransaction(tx));
    assertTrue(thrown.getMessage().contains("Failed to save wallet transaction"));
  }

  @Test
  public void testSaveTransactionWithConnection_ThrowsSQLException() throws SQLException {
    WalletRepository repo = new WalletRepository(mockDataSource);
    WalletTransaction tx =
        new WalletTransaction(UUID.randomUUID(), BigDecimal.ONE, "DEPOSIT", null, "test");
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep error"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.saveTransaction(tx, mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to save wallet transaction"));
  }
}
