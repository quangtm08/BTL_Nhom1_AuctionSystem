-- Auction System Schema (PostgreSQL Compatible)

-- 1. Table: users
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- 2. Table: items
CREATE TABLE IF NOT EXISTS items (
    id VARCHAR(36) PRIMARY KEY,
    seller_id VARCHAR(36) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    condition VARCHAR(50) NOT NULL,
    brand VARCHAR(100),
    warranty_months INTEGER,
    artist VARCHAR(255),
    era VARCHAR(100),
    production_year INTEGER,
    fuel_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (seller_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_items_seller_id ON items(seller_id);

-- 3. Table: auctions
CREATE TABLE IF NOT EXISTS auctions (
    id VARCHAR(36) PRIMARY KEY,
    item_id VARCHAR(36) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    starting_price DECIMAL(19, 2) NOT NULL,
    current_highest_bid DECIMAL(19, 2) DEFAULT 0,
    highest_bidder_id VARCHAR(36),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (highest_bidder_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_auctions_item_id ON auctions(item_id);
CREATE INDEX IF NOT EXISTS idx_auctions_status ON auctions(status);
CREATE INDEX IF NOT EXISTS idx_auctions_highest_bidder_id ON auctions(highest_bidder_id);

-- 4. Table: bids
CREATE TABLE IF NOT EXISTS bids (
    id VARCHAR(36) PRIMARY KEY,
    auction_id VARCHAR(36) NOT NULL,
    bidder_id VARCHAR(36) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL,
    bid_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    FOREIGN KEY (auction_id) REFERENCES auctions(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_bids_auction_id ON bids(auction_id);
CREATE INDEX IF NOT EXISTS idx_bids_bidder_id ON bids(bidder_id);

-- 5. Table: auto_bid_configs
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
);

-- 6. Table: item_images (image URLs for auction items)
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
);

CREATE INDEX IF NOT EXISTS idx_item_images_item_id ON item_images(item_id);
CREATE INDEX IF NOT EXISTS idx_item_images_is_primary ON item_images(is_primary);
CREATE INDEX IF NOT EXISTS idx_item_images_item_sort ON item_images(item_id, sort_order);
