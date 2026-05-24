package com.nhom1.auction.server;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.server.infrastructure.database.DBConnection;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.ServerSocket;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ServerTest {

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

    DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
    when(mockConnection.getMetaData()).thenReturn(mockMetaData);
    when(mockMetaData.getDatabaseProductName()).thenReturn("SQLite");

    Field field = DBConnection.class.getDeclaredField("dataSource");
    field.setAccessible(true);
    field.set(null, mockDataSource);
  }

  @Test
  public void testServerMainPortBusy() throws IOException {
    int port = 41177;

    // Bind the port so Server main fails to bind and exits immediately
    try (ServerSocket busySocket = new ServerSocket(port)) {
      assertDoesNotThrow(() -> Server.main(new String[0]));
    }
  }
}
