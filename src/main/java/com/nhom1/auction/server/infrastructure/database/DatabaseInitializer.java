package com.nhom1.auction.server.infrastructure.database;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.sql.DataSource;

/**
 * Centralized schema bootstrap. Runs idempotent CREATE TABLE IF NOT EXISTS statements
 * once at server startup. Tables are created in FK-dependency order so strict-FK
 * backends (PostgreSQL) do not reject forward references.
 *
 * Order: users -> items -> auctions -> bids -> auto_bid_configs.
 */
public final class DatabaseInitializer {

    private DatabaseInitializer() {}

    private static final String[] STATEMENTS = {
        """
        CREATE TABLE IF NOT EXISTS users (
            id VARCHAR(36) PRIMARY KEY,
            username VARCHAR(255) UNIQUE NOT NULL,
            email VARCHAR(255) UNIQUE NOT NULL,
            password VARCHAR(255) NOT NULL,
            role VARCHAR(50) NOT NULL,
            created_at TIMESTAMP NOT NULL,
            updated_at TIMESTAMP NOT NULL
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS items (
            id VARCHAR(36) PRIMARY KEY,
            seller_id VARCHAR(36) NOT NULL,
            name VARCHAR(255) NOT NULL,
            description TEXT,
            category VARCHAR(50) NOT NULL,
            condition VARCHAR(50) NOT NULL,
            created_at TIMESTAMP NOT NULL,
            updated_at TIMESTAMP NOT NULL,
            FOREIGN KEY (seller_id) REFERENCES users(id)
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_items_seller_id ON items(seller_id)",
        """
        CREATE TABLE IF NOT EXISTS auctions (
            id VARCHAR(36) PRIMARY KEY,
            item_id VARCHAR(36) NOT NULL,
            start_time TIMESTAMP NOT NULL,
            end_time TIMESTAMP NOT NULL,
            status VARCHAR(50) NOT NULL,
            starting_price NUMERIC(19, 2) NOT NULL,
            current_highest_bid NUMERIC(19, 2) DEFAULT 0,
            highest_bidder_id VARCHAR(36),
            created_at TIMESTAMP NOT NULL,
            updated_at TIMESTAMP NOT NULL,
            FOREIGN KEY (item_id) REFERENCES items(id),
            FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_auctions_item_id ON auctions(item_id)",
        "CREATE INDEX IF NOT EXISTS idx_auctions_status ON auctions(status)",
        "CREATE INDEX IF NOT EXISTS idx_auctions_highest_bidder_id ON auctions(highest_bidder_id)",
        """
        CREATE TABLE IF NOT EXISTS bids (
            id VARCHAR(36) PRIMARY KEY,
            auction_id VARCHAR(36) NOT NULL,
            bidder_id VARCHAR(36) NOT NULL,
            amount NUMERIC(19, 2) NOT NULL,
            bid_type VARCHAR(50) NOT NULL,
            created_at TIMESTAMP NOT NULL,
            FOREIGN KEY (auction_id) REFERENCES auctions(id),
            FOREIGN KEY (bidder_id) REFERENCES users(id)
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_bids_auction_id ON bids(auction_id)",
        "CREATE INDEX IF NOT EXISTS idx_bids_bidder_id ON bids(bidder_id)",
        """
        CREATE TABLE IF NOT EXISTS auto_bid_configs (
            auction_id VARCHAR(36) NOT NULL,
            bidder_id VARCHAR(36) NOT NULL,
            max_amount DECIMAL(19, 2) NOT NULL,
            increment_amount DECIMAL(19, 2) NOT NULL,
            created_at TIMESTAMP NOT NULL,
            updated_at TIMESTAMP NOT NULL,
            PRIMARY KEY (auction_id, bidder_id),
            FOREIGN KEY (auction_id) REFERENCES auctions(id),
            FOREIGN KEY (bidder_id) REFERENCES users(id)
        )
        """
    };

    public static void init(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String ddl : STATEMENTS) {
                stmt.execute(ddl);
            }
            System.out.println("DatabaseInitializer: schema ready.");
        } catch (SQLException e) {
            throw new RuntimeException("DatabaseInitializer: schema bootstrap failed", e);
        }
    }
}
