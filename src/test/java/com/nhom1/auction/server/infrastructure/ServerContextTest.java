package com.nhom1.auction.server.infrastructure;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import javax.sql.DataSource;
import com.zaxxer.hikari.HikariDataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nhom1.auction.server.infrastructure.database.DBConnection;

public class ServerContextTest {

    private HikariDataSource mockDataSource;
    private Connection mockConnection;
    private Statement mockStatement;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    public void setUp() throws Exception {
        mockDataSource = mock(HikariDataSource.class);
        mockConnection = mock(Connection.class);
        mockStatement = mock(Statement.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);

        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.createStatement()).thenReturn(mockStatement);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
        when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

        // Mock DatabaseMetaData to prevent NPE in any db checks
        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
        when(mockConnection.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getDatabaseProductName()).thenReturn("SQLite");

        // Inject the mockDataSource into DBConnection using reflection
        Field field = DBConnection.class.getDeclaredField("dataSource");
        field.setAccessible(true);
        field.set(null, mockDataSource);
    }

    @Test
    public void testServerContextInitialization() {
        assertDoesNotThrow(() -> {
            ServerContext context = new ServerContext();
            assertNotNull(context.getRouter());
            assertNotNull(context.getClientRegistry());
            assertNotNull(context.getNotificationService());
        });
    }
}
