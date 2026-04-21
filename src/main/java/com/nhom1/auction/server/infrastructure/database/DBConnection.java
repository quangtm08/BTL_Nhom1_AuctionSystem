package com.nhom1.auction.server.infrastructure.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {
    private static final String URL = "jdbc:sqlite:database/auction.db";
    private static Connection connection = null;

    private DBConnection() {}

    public static Connection getConnection() {
        if (connection == null) {
            try {
                Class.forName("org.sqlite.JDBC");
                connection = DriverManager.getConnection(URL);
                System.out.println("Database connection established.");
            } catch (ClassNotFoundException | SQLException e) {
                System.err.println("Database connection error: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return connection;
    }
}
