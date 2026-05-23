package com.nhom1.auction.server.infrastructure.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;

public class DatabaseInitializerTest {

  @Test
  public void testPrivateConstructor() throws Exception {
    Constructor<DatabaseInitializer> constructor =
        DatabaseInitializer.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    DatabaseInitializer instance = constructor.newInstance();
    assertNotNull(instance);
  }

  @Test
  public void testInit_Success() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    java.sql.DatabaseMetaData metaData = mock(java.sql.DatabaseMetaData.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

    assertDoesNotThrow(() -> DatabaseInitializer.init(dataSource));

    verify(dataSource).getConnection();
    verify(connection).createStatement();
    verify(statement, atLeastOnce()).execute(anyString());
  }

  @Test
  public void testInit_ThrowsSQLException() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    when(dataSource.getConnection()).thenThrow(new SQLException("Mock DB error"));

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> DatabaseInitializer.init(dataSource));

    assertTrue(thrown.getMessage().contains("DatabaseInitializer: schema bootstrap failed"));
  }

  @Test
  public void testInit_SQLitePath_DuplicateColumnIgnored() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    java.sql.DatabaseMetaData metaData = mock(java.sql.DatabaseMetaData.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("SQLite"); // isSqlite = true

    // Simulate "duplicate column name" error on ADD COLUMN statements -> should be ignored
    when(statement.execute(anyString()))
        .thenAnswer(
            inv -> {
              String sql = inv.getArgument(0, String.class);
              if (sql.contains("ADD COLUMN")) {
                throw new SQLException("duplicate column name: duration_days");
              }
              return false;
            });

    assertDoesNotThrow(() -> DatabaseInitializer.init(dataSource));
  }

  @Test
  public void testInit_SQLitePath_AlreadyExistsIgnored() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    java.sql.DatabaseMetaData metaData = mock(java.sql.DatabaseMetaData.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("SQLite");

    // Simulate "already exists" error -> should be ignored
    when(statement.execute(anyString()))
        .thenAnswer(
            inv -> {
              String sql = inv.getArgument(0, String.class);
              if (sql.contains("ADD COLUMN")) {
                throw new SQLException("table already exists");
              }
              return false;
            });

    assertDoesNotThrow(() -> DatabaseInitializer.init(dataSource));
  }

  @Test
  public void testInit_NonIgnorableError_Propagates() throws SQLException {
    DataSource dataSource = mock(DataSource.class);
    Connection connection = mock(Connection.class);
    Statement statement = mock(Statement.class);
    java.sql.DatabaseMetaData metaData = mock(java.sql.DatabaseMetaData.class);

    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.createStatement()).thenReturn(statement);
    when(connection.getMetaData()).thenReturn(metaData);
    when(metaData.getDatabaseProductName()).thenReturn("PostgreSQL");

    // A fatal SQL error that is NOT "duplicate column" or "already exists"
    when(statement.execute(anyString())).thenThrow(new SQLException("permission denied"));

    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> DatabaseInitializer.init(dataSource));
    assertEquals("DatabaseInitializer: schema bootstrap failed", ex.getMessage());
  }
}

