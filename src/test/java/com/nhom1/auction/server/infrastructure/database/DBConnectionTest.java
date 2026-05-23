package com.nhom1.auction.server.infrastructure.database;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.zaxxer.hikari.HikariDataSource;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

public class DBConnectionTest {

  @BeforeEach
  public void setUp() throws Exception {
    // Reset the static dataSource field before each test
    Field field = DBConnection.class.getDeclaredField("dataSource");
    field.setAccessible(true);
    field.set(null, null);

    // Clear environment variables we might set
    setEnv("PGHOST", null);
    setEnv("PGPORT", null);
    setEnv("PGDATABASE", null);
    setEnv("PGUSER", null);
    setEnv("PGPASSWORD", null);
  }

  @Test
  public void testPrivateConstructor() throws Exception {
    Constructor<DBConnection> constructor = DBConnection.class.getDeclaredConstructor();
    constructor.setAccessible(true);
    DBConnection instance = constructor.newInstance();
    assertNotNull(instance);
  }

  @Test
  public void testGetDataSource_SQLitePool() {
    DataSource ds = DBConnection.getDataSource();
    assertNotNull(ds);

    // Call again to verify cached singleton instance is returned
    DataSource ds2 = DBConnection.getDataSource();
    assertSame(ds, ds2);
  }

  @Test
  public void testGetDataSource_PostgresPool() {
    setEnv("PGHOST", "localhost");
    setEnv("PGPORT", "5432");
    setEnv("PGDATABASE", "testdb");
    setEnv("PGUSER", "user");
    setEnv("PGPASSWORD", "pass");

    try (MockedConstruction<HikariDataSource> mocked = mockConstruction(HikariDataSource.class)) {
      DataSource ds = DBConnection.getDataSource();
      assertNotNull(ds);
      assertEquals(1, mocked.constructed().size());
    }
  }

  @Test
  public void testBuildPostgresPoolReflection() {
    try {
      Method method = DBConnection.class.getDeclaredMethod("buildPostgresPool");
      method.setAccessible(true);
      try (MockedConstruction<HikariDataSource> mocked = mockConstruction(HikariDataSource.class)) {
        Object result = method.invoke(null);
        assertNotNull(result);
        assertEquals(1, mocked.constructed().size());
      }
    } catch (Exception e) {
      fail("Reflection setup failed: " + e.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  private void setEnv(String key, String value) {
    try {
      Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
      Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
      theEnvironmentField.setAccessible(true);
      Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
      if (value == null) {
        env.remove(key);
      } else {
        env.put(key, value);
      }

      Field theCaseInsensitiveEnvironmentField =
          processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
      theCaseInsensitiveEnvironmentField.setAccessible(true);
      Map<String, String> cienv =
          (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
      if (value == null) {
        cienv.remove(key);
      } else {
        cienv.put(key, value);
      }
    } catch (Exception e) {
      // If reflection fails (e.g. on different JVMs), we can try setting system properties or fall
      // back.
      // But on standard Windows Oracle/OpenJDK, this reflection works.
    }
  }
}
