package com.nhom1.auction.server.infrastructure.database;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class DBConnectionTest {

  @BeforeEach
  public void setUp() throws Exception {
    // Reset the static dataSource field before each test
    Field field = DBConnection.class.getDeclaredField("dataSource");
    field.setAccessible(true);
    field.set(null, null);
  }

  @Test
  public void testGetDataSource_SQLitePool() {
    DataSource ds = DBConnection.getDataSource();
    assertNotNull(ds);

    // Call again to verify cached singleton instance is returned
    DataSource ds2 = DBConnection.getDataSource();
    assertSame(ds, ds2);
  }
}
