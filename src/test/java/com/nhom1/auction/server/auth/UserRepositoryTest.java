package com.nhom1.auction.server.auth;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.nhom1.auction.common.entity.User;
import com.nhom1.auction.common.enums.UserRole;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class UserRepositoryTest {

  private DataSource mockDataSource;
  private Connection mockConnection;
  private Statement mockStatement;
  private PreparedStatement mockPreparedStatement;
  private ResultSet mockResultSet;
  private UserRepository repo;

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

    repo = new UserRepository(mockDataSource);
  }

  @Test
  public void testFindAll_Success() throws SQLException {
    when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);
    when(mockResultSet.next()).thenReturn(true, false);
    when(mockResultSet.getString("id")).thenReturn(UUID.randomUUID().toString());
    when(mockResultSet.getString("username")).thenReturn("testuser");
    when(mockResultSet.getString("email")).thenReturn("test@mail.com");
    when(mockResultSet.getString("password")).thenReturn("pwd");
    when(mockResultSet.getString("role")).thenReturn("USER");
    when(mockResultSet.getTimestamp("created_at"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    when(mockResultSet.getTimestamp("updated_at"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now()));

    List<User> list = repo.findAll();
    assertEquals(1, list.size());
    assertEquals("testuser", list.get(0).getUsername());
  }

  @Test
  public void testFindAll_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> repo.findAll());
    assertTrue(thrown.getMessage().contains("Failed to find all users"));
  }

  @Test
  public void testFindAllWithConnection_ThrowsException() throws SQLException {
    when(mockConnection.createStatement()).thenThrow(new SQLException("Stmt failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findAll(mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to find all users"));
  }

  @Test
  public void testFindByIdentifier_Found() throws SQLException {
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("id")).thenReturn(UUID.randomUUID().toString());
    when(mockResultSet.getString("username")).thenReturn("testuser");
    when(mockResultSet.getString("email")).thenReturn("test@mail.com");
    when(mockResultSet.getString("password")).thenReturn("pwd");
    when(mockResultSet.getString("role")).thenReturn("USER");
    when(mockResultSet.getTimestamp("created_at"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now()));
    when(mockResultSet.getTimestamp("updated_at"))
        .thenReturn(Timestamp.valueOf(LocalDateTime.now()));

    Optional<User> userOpt = repo.findByIdentifier("testuser");
    assertTrue(userOpt.isPresent());
    assertEquals("testuser", userOpt.get().getUsername());
  }

  @Test
  public void testFindByIdentifier_NotFound() throws SQLException {
    when(mockResultSet.next()).thenReturn(false);
    Optional<User> userOpt = repo.findByIdentifier("notfound");
    assertFalse(userOpt.isPresent());
  }

  @Test
  public void testFindByIdentifier_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findByIdentifier("error"));
    assertTrue(thrown.getMessage().contains("Failed to find user by identifier"));
  }

  @Test
  public void testFindById_Found() throws SQLException {
    UUID id = UUID.randomUUID();
    when(mockResultSet.next()).thenReturn(true);
    when(mockResultSet.getString("id")).thenReturn(id.toString());
    when(mockResultSet.getString("username")).thenReturn("testuser");
    when(mockResultSet.getString("email")).thenReturn("test@mail.com");
    when(mockResultSet.getString("password")).thenReturn("pwd");
    when(mockResultSet.getString("role")).thenReturn("USER");

    Optional<User> userOpt = repo.findById(id);
    assertTrue(userOpt.isPresent());
    assertEquals(id, userOpt.get().getId());
  }

  @Test
  public void testFindById_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.findById(UUID.randomUUID()));
    assertTrue(thrown.getMessage().contains("Failed to find user by id"));
  }

  @Test
  public void testFindByIdWithConnection_ThrowsException() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep failed"));
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> repo.findById(UUID.randomUUID(), mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to find user by id"));
  }

  @Test
  public void testExistsByUsername() throws SQLException {
    when(mockResultSet.next()).thenReturn(true);
    assertTrue(repo.existsByUsername("test"));

    when(mockResultSet.next()).thenReturn(false);
    assertFalse(repo.existsByUsername("test"));
  }

  @Test
  public void testExistsByUsername_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.existsByUsername("error"));
    assertTrue(thrown.getMessage().contains("Failed to check username existence"));
  }

  @Test
  public void testExistsByEmail() throws SQLException {
    when(mockResultSet.next()).thenReturn(true);
    assertTrue(repo.existsByEmail("test@mail.com"));

    when(mockResultSet.next()).thenReturn(false);
    assertFalse(repo.existsByEmail("test@mail.com"));
  }

  @Test
  public void testExistsByEmail_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.existsByEmail("error"));
    assertTrue(thrown.getMessage().contains("Failed to check email existence"));
  }

  @Test
  public void testSave_Success() throws SQLException {
    User user =
        new User(
            UUID.randomUUID(),
            "username",
            "email@mail.com",
            "pwd",
            UserRole.USER,
            LocalDateTime.now(),
            LocalDateTime.now());
    repo.save(user);
    verify(mockPreparedStatement).executeUpdate();
  }

  @Test
  public void testSave_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    User user =
        new User(
            UUID.randomUUID(),
            "username",
            "email@mail.com",
            "pwd",
            UserRole.USER,
            LocalDateTime.now(),
            LocalDateTime.now());

    RuntimeException thrown = assertThrows(RuntimeException.class, () -> repo.save(user));
    assertTrue(thrown.getMessage().contains("Failed to save user"));
  }

  @Test
  public void testSaveWithConnection_ThrowsException() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep failed"));
    User user =
        new User(
            UUID.randomUUID(),
            "username",
            "email@mail.com",
            "pwd",
            UserRole.USER,
            LocalDateTime.now(),
            LocalDateTime.now());

    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.save(user, mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to save user"));
  }

  @Test
  public void testDeleteById_Success() throws SQLException {
    UUID id = UUID.randomUUID();
    when(mockPreparedStatement.executeUpdate()).thenReturn(1);
    assertTrue(repo.deleteById(id));

    when(mockPreparedStatement.executeUpdate()).thenReturn(0);
    assertFalse(repo.deleteById(id));
  }

  @Test
  public void testDeleteById_ThrowsException() throws SQLException {
    when(mockDataSource.getConnection()).thenThrow(new SQLException("Conn failed"));
    RuntimeException thrown =
        assertThrows(RuntimeException.class, () -> repo.deleteById(UUID.randomUUID()));
    assertTrue(thrown.getMessage().contains("Failed to delete user"));
  }

  @Test
  public void testDeleteByIdWithConnection_ThrowsException() throws SQLException {
    when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Prep failed"));
    RuntimeException thrown =
        assertThrows(
            RuntimeException.class, () -> repo.deleteById(UUID.randomUUID(), mockConnection));
    assertTrue(thrown.getMessage().contains("Failed to delete user"));
  }
}
