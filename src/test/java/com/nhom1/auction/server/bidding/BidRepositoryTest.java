package com.nhom1.auction.server.bidding;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.dto.bidding.BidWithAuctionDto;
import com.nhom1.auction.common.entity.BidTransaction;
import com.nhom1.auction.common.enums.BidType;
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

public class BidRepositoryTest {

  private DataSource mockDataSource;
  private Connection mockConnection;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;
  private BidRepository repo;

  @BeforeEach
  public void setUp() throws SQLException {
    mockDataSource = mock(DataSource.class);
    mockConnection = mock(Connection.class);
    mockPreparedStatement = mock(PreparedStatement.class);
    mockResultSet = mock(ResultSet.class);

    when(mockDataSource.getConnection()).thenReturn(mockConnection);
    when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
    when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

    repo = new BidRepository(mockDataSource);
  }

  @Test
  public void testSave_Success() throws SQLException {
    BidTransaction tx =
        new BidTransaction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.0"),
            BidType.MANUAL,
            LocalDateTime.now(),
            LocalDateTime.now());
    repo.save(tx);
    verify(mockPreparedStatement).executeUpdate();
  }

  @Test
  public void testSave_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    BidTransaction tx =
        new BidTransaction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.0"),
            BidType.MANUAL,
            LocalDateTime.now(),
            LocalDateTime.now());

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> repo.save(tx));
    assertTrue(thrown.getMessage().contains("Failed to save bid"));
  }

  @Test
  public void testSaveWithConnection_ThrowsException() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep failed"));
    BidTransaction tx =
        new BidTransaction(
            UUID.randomUUID(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            new BigDecimal("100.0"),
            BidType.MANUAL,
            LocalDateTime.now(),
            LocalDateTime.now());

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.save(tx, mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to save bid"));
  }

  @Test
  public void testFindByAuctionId_Success() throws SQLException {
    UUID auctionId = UUID.randomUUID();
    UUID bidId = UUID.randomUUID();
    UUID bidderId = UUID.randomUUID();

    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("id")).thenReturn(bidId.toString());
    when(mockResultSet.getString("auction_id")).thenReturn(auctionId.toString());
    when(mockResultSet.getString("bidder_id")).thenReturn(bidderId.toString());
    when(mockResultSet.getBigDecimal("amount")).thenReturn(new BigDecimal("120.0"));
    when(mockResultSet.getString("bid_type")).thenReturn("MANUAL");
    when(mockResultSet.getTimestamp("created_at"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now()));

    List<BidTransaction> list = repo.findByAuctionId(auctionId);
    assertEquals(1, list.size());
    assertEquals(bidId, list.get(0).getId());
  }

  @Test
  public void testFindByAuctionId_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findByAuctionId(UUID.randomUUID()));
    assertTrue(thrown.getMessage().contains("Failed to find bids by auction id"));
  }

  @Test
  public void testFindByBidderId_Success() throws SQLException {
    UUID bidderId = UUID.randomUUID();
    UUID auctionId = UUID.randomUUID();

    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("auction_id")).thenReturn(auctionId.toString());
    when(mockResultSet.getString("item_name")).thenReturn("Item Name");
    when(mockResultSet.getBigDecimal("your_bid")).thenReturn(new BigDecimal("120.0"));
    when(mockResultSet.getBigDecimal("current_highest_bid")).thenReturn(new BigDecimal("120.0"));
    when(mockResultSet.getString("status")).thenReturn("OPEN");
    when(mockResultSet.getTimestamp("end_time")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    when(mockResultSet.getString("highest_bidder_id")).thenReturn(bidderId.toString());

    List<BidWithAuctionDto> list = repo.findByBidderId(bidderId);
    assertEquals(1, list.size());
    assertEquals(auctionId.toString(), list.get(0).getAuctionId());
    assertTrue(list.get(0).isWinning());
  }

  @Test
  public void testFindByBidderId_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findByBidderId(UUID.randomUUID()));
    assertTrue(thrown.getMessage().contains("Failed to find bids by bidder id"));
  }

  @Test
  public void testFindLastBidTime_Success() throws SQLException {
    UUID auctionId = UUID.randomUUID();
    LocalDateTime time = LocalDateTime.now();

    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getTimestamp("last_bid_time")).thenReturn(Timestamp.valueOf(time));

    Optional<LocalDateTime> lastTime = repo.findLastBidTime(auctionId);
    assertTrue(lastTime.isPresent());
    assertEquals(time, lastTime.get());
  }

  @Test
  public void testFindLastBidTime_Empty() throws SQLException {
    UUID auctionId = UUID.randomUUID();
    when(mockResultSet.next()).thenReturn(false);

    Optional<LocalDateTime> lastTime = repo.findLastBidTime(auctionId);
    assertFalse(lastTime.isPresent());
  }

  @Test
  public void testFindLastBidTime_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findLastBidTime(UUID.randomUUID()));
    assertTrue(thrown.getMessage().contains("Failed to find last bid time"));
  }

  @Test
  public void testDeleteByBidderId_Success() throws SQLException {
    UUID bidderId = UUID.randomUUID();
    when(mockPreparedStatement.executeUpdate()).thenReturn(5);

    int count = repo.deleteByBidderId(bidderId);
    assertEquals(5, count);
  }

  @Test
  public void testDeleteByBidderId_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.deleteByBidderId(UUID.randomUUID()));
    assertTrue(thrown.getMessage().contains("Failed to delete bids by bidder id"));
  }

  @Test
  public void testDeleteByBidderIdWithConnection_ThrowsException() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep failed"));
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> repo.deleteByBidderId(UUID.randomUUID(), mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to delete bids by bidder id"));
  }

  @Test
  public void testDeleteByAuctionId_Success() throws SQLException {
    UUID auctionId = UUID.randomUUID();
    when(mockPreparedStatement.executeUpdate()).thenReturn(3);

    int count = repo.deleteByAuctionId(auctionId);
    assertEquals(3, count);
  }

  @Test
  public void testDeleteByAuctionId_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.deleteByAuctionId(UUID.randomUUID()));
    assertTrue(thrown.getMessage().contains("Failed to delete bids by auction id"));
  }

  @Test
  public void testDeleteByAuctionIdWithConnection_ThrowsException() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep failed"));
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class,
            () -> repo.deleteByAuctionId(UUID.randomUUID(), mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to delete bids by auction id"));
  }
}
