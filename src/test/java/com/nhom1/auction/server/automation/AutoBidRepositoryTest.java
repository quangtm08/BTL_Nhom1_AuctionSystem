package com.nhom1.auction.server.automation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AutoBidRepositoryTest {

    private DataSource mockDataSource;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;
    private AutoBidRepository repo;

    @BeforeEach
    public void setUp() throws SQLException {
        mockDataSource = mock(DataSource.class);
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        repo = new AutoBidRepository(mockDataSource);
    }

    @Test
    public void testSave_Success() throws SQLException {
        AutoBidConfig config = new AutoBidConfig(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("1000.0"), new BigDecimal("50.0"));
        repo.save(config);
        verify(mockPreparedStatement).executeUpdate();
    }

    @Test
    public void testSave_ThrowsException() throws SQLException {
        when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
        AutoBidConfig config = new AutoBidConfig(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("1000.0"), new BigDecimal("50.0"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> repo.save(config));
        assertTrue(thrown.getMessage().contains("Failed to save auto bid config"));
    }

    @Test
    public void testFindByAuctionId_Success() throws SQLException {
        UUID auctionId = UUID.randomUUID();
        UUID bidderId = UUID.randomUUID();

        when(mockResultSet.next()).thenReturn(true, false);
        when(mockResultSet.getString("auction_id")).thenReturn(auctionId.toString());
        when(mockResultSet.getString("bidder_id")).thenReturn(bidderId.toString());
        when(mockResultSet.getBigDecimal("max_amount")).thenReturn(new BigDecimal("1000.0"));
        when(mockResultSet.getBigDecimal("increment_amount")).thenReturn(new BigDecimal("50.0"));

        List<AutoBidConfig> configs = repo.findByAuctionId(auctionId);
        assertEquals(1, configs.size());
        assertEquals(auctionId, configs.get(0).getAuctionId());
        assertEquals(bidderId, configs.get(0).getBidderId());
    }

    @Test
    public void testFindByAuctionId_ThrowsException() throws SQLException {
        UUID auctionId = UUID.randomUUID();
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep failed"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> repo.findByAuctionId(auctionId));
        assertTrue(thrown.getMessage().contains("Failed to read auto bid configs"));
    }
}
