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
        Constructor<DatabaseInitializer> constructor = DatabaseInitializer.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        DatabaseInitializer instance = constructor.newInstance();
        assertNotNull(instance);
    }

    @Test
    public void testInit_Success() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        Statement statement = mock(Statement.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);

        assertDoesNotThrow(() -> DatabaseInitializer.init(dataSource));

        verify(dataSource).getConnection();
        verify(connection).createStatement();
        verify(statement, atLeastOnce()).execute(anyString());
    }

    @Test
    public void testInit_ThrowsSQLException() throws SQLException {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new SQLException("Mock DB error"));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> 
            DatabaseInitializer.init(dataSource)
        );

        assertTrue(thrown.getMessage().contains("DatabaseInitializer: schema bootstrap failed"));
    }
}
