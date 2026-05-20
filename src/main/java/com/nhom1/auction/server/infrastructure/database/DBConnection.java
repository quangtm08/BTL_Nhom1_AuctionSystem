package com.nhom1.auction.server.infrastructure.database;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;
import javax.sql.DataSource;

public class DBConnection {
    private static HikariDataSource dataSource = null;

    private DBConnection() {}

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            String pgHost = System.getenv("PGHOST");
            if (pgHost != null) {
                dataSource = buildPostgresPool();
            } else {
                dataSource = buildSQLitePool();
            }
        }
        return dataSource;
    }

    private static HikariDataSource buildPostgresPool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s",
            System.getenv("PGHOST"),
            System.getenv("PGPORT"),
            System.getenv("PGDATABASE")));
        config.setUsername(System.getenv("PGUSER"));
        config.setPassword(System.getenv("PGPASSWORD"));
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        System.out.println("Database: Connecting to PostgreSQL pool (Cloud)");
        return new HikariDataSource(config);
    }

    private static HikariDataSource buildSQLitePool() {
        new java.io.File("database").mkdirs();

        SQLiteConfig sqliteConfig = new SQLiteConfig();
        sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
        sqliteConfig.setBusyTimeout(5000);
        sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        sqliteConfig.enforceForeignKeys(true);

        SQLiteDataSource sqliteDS = new SQLiteDataSource(sqliteConfig);
        sqliteDS.setUrl("jdbc:sqlite:database/auction-system.db");

        HikariConfig config = new HikariConfig();
        config.setDataSource(sqliteDS);
        config.setMaximumPoolSize(10);
        config.setConnectionTimeout(30000);
        System.out.println("Database: Connecting to SQLite pool (Local)");
        return new HikariDataSource(config);
    }
}
