package com.nhom1.auction.server.auction;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.entity.Auction;
import com.nhom1.auction.common.enums.AuctionStatus;
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

public class AuctionRepositoryTest {

    private DataSource mockDataSource;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private AuctionRepository repo;

    @BeforeEach
    public void setUp() throws SQLException {
        mockDataSource = mock(DataSource.class);
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(
            mockPreparedStatement
        );
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        repo = new AuctionRepository(mockDataSource);
    }

    @Test
    public void testSave_Success_NullFields() throws SQLException {
        UUID auctionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Auction auction = new Auction(
            auctionId,
            itemId,
            sellerId,
            BigDecimal.TEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(2),
            null,
            null,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        repo.save(auction);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testSave_Success_NonNullFields() throws SQLException {
        UUID auctionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        Auction auction = new Auction(
            auctionId,
            itemId,
            sellerId,
            BigDecimal.TEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(2),
            bidderId,
            new BigDecimal("100.0"),
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        repo.save(auction);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testSave_ThrowsException() throws SQLException {
        UUID auctionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Auction auction = new Auction(
            auctionId,
            itemId,
            sellerId,
            BigDecimal.TEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(2),
            null,
            null,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(mockDataSource.getConnection()).thenThrow(
            new SQLException("Conn failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.save(auction)
        );
        assertTrue(thrown.getMessage().contains("Failed to save auction"));
    }

    @Test
    public void testSaveWithConnection_ThrowsException() throws SQLException {
        UUID auctionId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        Auction auction = new Auction(
            auctionId,
            itemId,
            sellerId,
            BigDecimal.TEN,
            LocalDateTime.now(),
            LocalDateTime.now().plusDays(2),
            null,
            null,
            AuctionStatus.OPEN,
            LocalDateTime.now(),
            LocalDateTime.now()
        );

        when(mockConnection.prepareStatement(anyString())).thenThrow(
            new SQLException("Prep failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.save(auction, mockConnection)
        );
        assertTrue(thrown.getMessage().contains("Failed to save auction"));
    }

    @Test
    public void testFindById_Success() throws SQLException {
        UUID id = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("id")).thenReturn(id.toString());
        when(mockResultSet.getString("item_id")).thenReturn(itemId.toString());
        when(mockResultSet.getString("seller_id")).thenReturn(
            sellerId.toString()
        );
        when(mockResultSet.getTimestamp("start_time")).thenReturn(
            Timestamp.valueOf(LocalDateTime.now())
        );
        when(mockResultSet.getTimestamp("end_time")).thenReturn(
            Timestamp.valueOf(LocalDateTime.now())
        );
        when(mockResultSet.getBigDecimal("starting_price")).thenReturn(
            BigDecimal.TEN
        );
        when(mockResultSet.getString("highest_bidder_id")).thenReturn(
            bidderId.toString()
        );
        when(mockResultSet.getBigDecimal("current_highest_bid")).thenReturn(
            new BigDecimal("15.0")
        );
        when(mockResultSet.getString("status")).thenReturn("OPEN");
        when(mockResultSet.getTimestamp("created_at")).thenReturn(
            Timestamp.valueOf(LocalDateTime.now())
        );
        when(mockResultSet.getTimestamp("updated_at")).thenReturn(
            Timestamp.valueOf(LocalDateTime.now())
        );

        Optional<Auction> opt = repo.findById(id);
        assertTrue(opt.isPresent());
        assertEquals(id, opt.get().getId());
        assertEquals(bidderId, opt.get().getHighestBidderId());
    }

    @Test
    public void testFindById_NotFound() throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockResultSet.next()).thenReturn(false);

        Optional<Auction> opt = repo.findById(id);
        assertFalse(opt.isPresent());
    }

    @Test
    public void testFindById_ThrowsException() throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockDataSource.getConnection()).thenThrow(
            new SQLException("Conn failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.findById(id)
        );
        assertTrue(
            thrown.getMessage().contains("Failed to find auction by id")
        );
    }

    @Test
    public void testFindByIdWithConnection_ThrowsException()
        throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockConnection.prepareStatement(anyString())).thenThrow(
            new SQLException("Prep failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.findById(id, mockConnection)
        );
        assertTrue(
            thrown.getMessage().contains("Failed to find auction by id")
        );
    }

    @Test
    public void testFindAll_Success() throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("id")).thenReturn(id.toString());
        when(mockResultSet.getString("item_id")).thenReturn(
            UUID.randomUUID().toString()
        );
        when(mockResultSet.getString("seller_id")).thenReturn(
            UUID.randomUUID().toString()
        );
        when(mockResultSet.getBigDecimal("starting_price")).thenReturn(
            BigDecimal.TEN
        );
        when(mockResultSet.getString("status")).thenReturn("OPEN");

        List<Auction> list = repo.findAll();
        assertEquals(1, list.size());
    }

    @Test
    public void testFindAll_ThrowsException() throws SQLException {
        when(mockDataSource.getConnection()).thenThrow(
            new SQLException("Conn failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.findAll()
        );
        assertTrue(thrown.getMessage().contains("Failed to find all auctions"));
    }

    @Test
    public void testFindBySellerId_Success() throws SQLException {
        UUID sellerId = UUID.randomUUID();
        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("id")).thenReturn(
            UUID.randomUUID().toString()
        );
        when(mockResultSet.getString("item_id")).thenReturn(
            UUID.randomUUID().toString()
        );
        when(mockResultSet.getString("seller_id")).thenReturn(
            sellerId.toString()
        );
        when(mockResultSet.getBigDecimal("starting_price")).thenReturn(
            BigDecimal.TEN
        );
        when(mockResultSet.getString("status")).thenReturn("OPEN");

        List<Auction> list = repo.findBySellerId(sellerId);
        assertEquals(1, list.size());
    }

    @Test
    public void testFindBySellerId_ThrowsException() throws SQLException {
        UUID sellerId = UUID.randomUUID();
        when(mockDataSource.getConnection()).thenThrow(
            new SQLException("Conn failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.findBySellerId(sellerId)
        );
        assertTrue(thrown.getMessage().contains("Failed to find by sellerId"));
    }

    @Test
    public void testFindBySellerIdWithConnection_ThrowsException()
        throws SQLException {
        UUID sellerId = UUID.randomUUID();
        when(mockConnection.prepareStatement(anyString())).thenThrow(
            new SQLException("Prep failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.findBySellerId(sellerId, mockConnection)
        );
        assertTrue(thrown.getMessage().contains("Failed to find by sellerId"));
    }

    @Test
    public void testFindByItemId_Success() throws SQLException {
        UUID itemId = UUID.randomUUID();
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getString("id")).thenReturn(
            UUID.randomUUID().toString()
        );
        when(mockResultSet.getString("item_id")).thenReturn(itemId.toString());
        when(mockResultSet.getString("seller_id")).thenReturn(
            UUID.randomUUID().toString()
        );
        when(mockResultSet.getBigDecimal("starting_price")).thenReturn(
            BigDecimal.TEN
        );
        when(mockResultSet.getString("status")).thenReturn("OPEN");

        Optional<Auction> opt = repo.findByItemId(itemId);
        assertTrue(opt.isPresent());
    }

    @Test
    public void testFindByItemId_ThrowsException() throws SQLException {
        UUID itemId = UUID.randomUUID();
        when(mockDataSource.getConnection()).thenThrow(
            new SQLException("Conn failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.findByItemId(itemId)
        );
        assertTrue(thrown.getMessage().contains("Failed to find by itemId"));
    }

    @Test
    public void testUpdateStatus_Success() throws SQLException {
        UUID id = UUID.randomUUID();
        repo.updateStatus(id, AuctionStatus.FINISHED);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testUpdateStatus_ThrowsException() throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockDataSource.getConnection()).thenThrow(
            new SQLException("Conn failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.updateStatus(id, AuctionStatus.FINISHED)
        );
        assertTrue(thrown.getMessage().contains("Failed to update status"));
    }

    @Test
    public void testUpdateStatusWithConnection_ThrowsException()
        throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockConnection.prepareStatement(anyString())).thenThrow(
            new SQLException("Prep failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.updateStatus(id, AuctionStatus.FINISHED, mockConnection)
        );
        assertTrue(thrown.getMessage().contains("Failed to update status"));
    }

    @Test
    public void testUpdateHighestBid_Success_NullBidder() throws SQLException {
        UUID id = UUID.randomUUID();
        repo.updateHighestBid(
            id,
            new BigDecimal("100.0"),
            null,
            0L,
            mockConnection
        );
        verify(mockPreparedStatement).setNull(2, java.sql.Types.VARCHAR);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testUpdateHighestBid_Success_NonNullBidder()
        throws SQLException {
        UUID id = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();
        repo.updateHighestBid(
            id,
            new BigDecimal("100.0"),
            bidderId,
            0L,
            mockConnection
        );
        verify(mockPreparedStatement).setString(2, bidderId.toString());
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testUpdateHighestBid_ThrowsException() throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockConnection.prepareStatement(anyString())).thenThrow(
            new SQLException("Prep failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.updateHighestBid(
                id,
                BigDecimal.TEN,
                UUID.randomUUID(),
                0L,
                mockConnection
            )
        );
        assertTrue(
            thrown.getMessage().contains("Failed to update highest bid")
        );
    }

    @Test
    public void testUpdateEndTime_Success() throws SQLException {
        UUID id = UUID.randomUUID();
        repo.updateEndTime(id, LocalDateTime.now().plusDays(1));
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testUpdateEndTime_ThrowsException() throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockDataSource.getConnection()).thenThrow(
            new SQLException("Conn failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.updateEndTime(id, LocalDateTime.now())
        );
        assertTrue(thrown.getMessage().contains("Failed to update end time"));
    }

    @Test
    public void testDeleteById_Success() throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockPreparedStatement.executeUpdate()).thenReturn(1);

        int result = repo.deleteById(id);
        assertEquals(1, result);
    }

    @Test
    public void testDeleteById_ThrowsException() throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockDataSource.getConnection()).thenThrow(
            new SQLException("Conn failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.deleteById(id)
        );
        assertTrue(thrown.getMessage().contains("Failed to delete auction"));
    }

    @Test
    public void testDeleteByIdWithConnection_ThrowsException()
        throws SQLException {
        UUID id = UUID.randomUUID();
        when(mockConnection.prepareStatement(anyString())).thenThrow(
            new SQLException("Prep failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.deleteById(id, mockConnection)
        );
        assertTrue(thrown.getMessage().contains("Failed to delete auction"));
    }

    @Test
    public void testClearHighestBidderByUserId_Success() throws SQLException {
        UUID bidderId = UUID.randomUUID();
        repo.clearHighestBidderByUserId(bidderId);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testClearHighestBidderByUserId_ThrowsException()
        throws SQLException {
        UUID bidderId = UUID.randomUUID();
        when(mockDataSource.getConnection()).thenThrow(
            new SQLException("Conn failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.clearHighestBidderByUserId(bidderId)
        );
        assertTrue(
            thrown
                .getMessage()
                .contains("Failed to clear highest bidder on auctions")
        );
    }

    @Test
    public void testClearHighestBidderByUserIdWithConnection_ThrowsException()
        throws SQLException {
        UUID bidderId = UUID.randomUUID();
        when(mockConnection.prepareStatement(anyString())).thenThrow(
            new SQLException("Prep failed")
        );

        RuntimeException thrown = assertThrows(RuntimeException.class, () ->
            repo.clearHighestBidderByUserId(bidderId, mockConnection)
        );
        assertTrue(
            thrown
                .getMessage()
                .contains("Failed to clear highest bidder on auctions")
        );
    }
}
