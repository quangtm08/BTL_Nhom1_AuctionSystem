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
 * Order: users -> items -> auctions -> bids -> auto_bid_configs -> payment_transactions -> item_images.
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
            starting_price DECIMAL(19, 2) NOT NULL,
            current_highest_bid DECIMAL(19, 2) DEFAULT 0,
            highest_bidder_id VARCHAR(36),
            version BIGINT NOT NULL DEFAULT 0,
            created_at TIMESTAMP NOT NULL,
            updated_at TIMESTAMP NOT NULL,
            FOREIGN KEY (item_id) REFERENCES items(id),
            FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
        )
        """,
        "ALTER TABLE auctions ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0",
        "CREATE INDEX IF NOT EXISTS idx_auctions_item_id ON auctions(item_id)",
        "CREATE INDEX IF NOT EXISTS idx_auctions_status ON auctions(status)",
        "CREATE INDEX IF NOT EXISTS idx_auctions_highest_bidder_id ON auctions(highest_bidder_id)",
        "CREATE INDEX IF NOT EXISTS idx_auctions_status_end_time ON auctions(status, end_time)",
        """
        CREATE TABLE IF NOT EXISTS bids (
            id VARCHAR(36) PRIMARY KEY,
            auction_id VARCHAR(36) NOT NULL,
            bidder_id VARCHAR(36) NOT NULL,
            amount DECIMAL(19, 2) NOT NULL,
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
        """,
        "CREATE INDEX IF NOT EXISTS idx_auto_bid_configs_auction_id ON auto_bid_configs(auction_id)",
        "CREATE INDEX IF NOT EXISTS idx_auto_bid_configs_bidder_id ON auto_bid_configs(bidder_id)",
        """
        CREATE TABLE IF NOT EXISTS payment_transactions (
            id VARCHAR(36) PRIMARY KEY,
            auction_id VARCHAR(36) NOT NULL,
            payer_id VARCHAR(36) NOT NULL,
            payee_id VARCHAR(36) NOT NULL,
            amount DECIMAL(19, 2) NOT NULL,
            status VARCHAR(50) NOT NULL,
            created_at TIMESTAMP NOT NULL,
            updated_at TIMESTAMP NOT NULL,
            FOREIGN KEY (auction_id) REFERENCES auctions(id),
            FOREIGN KEY (payer_id) REFERENCES users(id),
            FOREIGN KEY (payee_id) REFERENCES users(id)
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_payment_transactions_auction_id ON payment_transactions(auction_id)",
        "CREATE INDEX IF NOT EXISTS idx_payment_transactions_payer_id ON payment_transactions(payer_id)",
        "CREATE INDEX IF NOT EXISTS idx_payment_transactions_payee_id ON payment_transactions(payee_id)",
        """
        CREATE TABLE IF NOT EXISTS item_images (
            id VARCHAR(36) PRIMARY KEY,
            item_id VARCHAR(36) NOT NULL,
            object_key VARCHAR(512) NOT NULL UNIQUE,
            public_url TEXT NOT NULL,
            is_primary BOOLEAN NOT NULL DEFAULT FALSE,
            sort_order INTEGER NOT NULL DEFAULT 0,
            created_at TIMESTAMP NOT NULL,
            updated_at TIMESTAMP NOT NULL,
            FOREIGN KEY (item_id) REFERENCES items(id) ON DELETE CASCADE
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_item_images_item_id ON item_images(item_id)",
        "CREATE INDEX IF NOT EXISTS idx_item_images_is_primary ON item_images(is_primary)",
        "CREATE INDEX IF NOT EXISTS idx_item_images_item_sort ON item_images(item_id, sort_order)",
        """
        CREATE TABLE IF NOT EXISTS wallets (
            user_id VARCHAR(36) PRIMARY KEY,
            balance DECIMAL(19, 2) NOT NULL DEFAULT 100000.00,
            created_at TIMESTAMP NOT NULL,
            updated_at TIMESTAMP NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users(id)
        )
        """,
        """
        CREATE TABLE IF NOT EXISTS wallet_transactions (
            id VARCHAR(36) PRIMARY KEY,
            user_id VARCHAR(36) NOT NULL,
            amount DECIMAL(19, 2) NOT NULL,
            transaction_type VARCHAR(50) NOT NULL,
            reference_id VARCHAR(36),
            description TEXT,
            created_at TIMESTAMP NOT NULL,
            FOREIGN KEY (user_id) REFERENCES users(id)
        )
        """,
        "CREATE INDEX IF NOT EXISTS idx_wallet_transactions_user_id ON wallet_transactions(user_id)",
        """
        INSERT INTO wallets (user_id, balance, created_at, updated_at)
        SELECT id, 100000.00, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
        FROM users
        WHERE id NOT IN (SELECT user_id FROM wallets)
        """
    };

    public static void init(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Kiểm tra xem database hiện tại có phải là SQLite hay không
            boolean isSqlite = conn.getMetaData().getDatabaseProductName().toLowerCase().contains("sqlite");
            for (String ddl : STATEMENTS) {
                String sql = ddl;
                // SQLite không hỗ trợ cú pháp "ADD COLUMN IF NOT EXISTS".
                // Do đó, nếu là SQLite, chúng ta chuyển đổi thành "ADD COLUMN".
                if (isSqlite && ddl.contains("ADD COLUMN IF NOT EXISTS")) {
                    sql = ddl.replace("ADD COLUMN IF NOT EXISTS", "ADD COLUMN");
                }
                try {
                    stmt.execute(sql);
                } catch (SQLException e) {
                    String msg = e.getMessage().toLowerCase();
                    // Khi dùng SQLite và cột đã tồn tại từ trước, câu lệnh ALTER TABLE ADD COLUMN
                    // sẽ ném ra ngoại lệ "duplicate column name" hoặc "already exists".
                    // Chúng ta bỏ qua lỗi này vì cột đã được khởi tạo thành công ở lần chạy trước.
                    if (msg.contains("duplicate column name") || msg.contains("already exists")) {
                        // Bỏ qua lỗi vì cột đã tồn tại
                    } else {
                        throw e;
                    }
                }
            }
            System.out.println("DatabaseInitializer: schema ready.");
        } catch (SQLException e) {
            throw new RuntimeException("DatabaseInitializer: schema bootstrap failed", e);
        }
    }
}
