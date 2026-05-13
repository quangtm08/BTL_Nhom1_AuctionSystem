package com.nhom1.auction.server.infrastructure.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:sqlite:database/auction-system.db";
    private static Connection connection = null;

    private DBConnection() {
    }

    public static synchronized Connection getConnection() {
        if (connection == null) {
            try {
                String pgHost = System.getenv("PGHOST");
                if (pgHost != null) {
                    //PostgreSQL Connection (Cloud/Railway)
                    String pgPort = System.getenv("PGPORT");
                    String pgDb = System.getenv("PGDATABASE");
                    String pgUser = System.getenv("PGUSER");
                    String pgPass = System.getenv("PGPASSWORD");

                    String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", pgHost, pgPort, pgDb);

                    Class.forName("org.postgresql.Driver");
                    connection = DriverManager.getConnection(jdbcUrl, pgUser, pgPass);
                    System.out.println("Database: Connected to PostgreSQL (Cloud)");
                } else {
                    // SQLite Connection (Local Fallback)
                    java.io.File dbFile = new java.io.File("database/auction-system.db");
                    java.io.File dbDir = dbFile.getParentFile();
                    if (dbDir != null && !dbDir.exists()) {
                        dbDir.mkdirs();
                    }

                    Class.forName("org.sqlite.JDBC");
                    connection = DriverManager.getConnection(URL);
                    try (Statement st = connection.createStatement()) {
                        st.execute("PRAGMA journal_mode=WAL;");
                        st.execute("PRAGMA busy_timeout=5000;");
                        st.execute("PRAGMA synchronous=NORMAL;");
                        st.execute("PRAGMA foreign_keys=ON;");
                    }
                    System.out.println("Database: Connected to SQLite (Local)");
                }
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("Database connection error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return connection;
    }
}
