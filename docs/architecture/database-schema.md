# Database Schema

Schema duoc bootstrap trong `DatabaseInitializer` bang cac lenh `CREATE TABLE IF NOT EXISTS`.
`DBConnection` dung HikariCP; local dung SQLite `database/auction-system.db`, cloud dung PostgreSQL neu co `PGHOST`.

## Bang va quan he

```dbml
Table users {
  id varchar(36) [pk]
  username varchar(255) [not null, unique]
  email varchar(255) [not null, unique]
  password varchar(255) [not null]
  role varchar(50) [not null, note: "USER | ADMIN"]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table items {
  id varchar(36) [pk]
  seller_id varchar(36) [not null, ref: > users.id]
  name varchar(255) [not null]
  description text
  category varchar(50) [not null, note: "ELECTRONICS | ART | VEHICLE"]
  condition varchar(50) [not null, note: "NEW | LIKE_NEW | USED | DAMAGED"]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table auctions {
  id varchar(36) [pk]
  item_id varchar(36) [not null, ref: > items.id]
  start_time timestamp
  end_time timestamp
  duration_days integer
  status varchar(50) [not null, note: "PENDING | OPEN | RUNNING | FINISHED | PAID | CANCELED"]
  starting_price decimal(19,2) [not null]
  current_highest_bid decimal(19,2) [default: 0]
  highest_bidder_id varchar(36) [ref: > users.id]
  version bigint [not null, default: 0, note: "Optimistic-lock token"]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table bids {
  id varchar(36) [pk]
  auction_id varchar(36) [not null, ref: > auctions.id]
  bidder_id varchar(36) [not null, ref: > users.id]
  amount decimal(19,2) [not null]
  bid_type varchar(50) [not null, note: "MANUAL | AUTO"]
  created_at timestamp [not null]
}

Table auto_bid_configs {
  auction_id varchar(36) [not null, ref: > auctions.id]
  bidder_id varchar(36) [not null, ref: > users.id]
  max_amount decimal(19,2) [not null]
  increment_amount decimal(19,2) [not null]
  created_at timestamp [not null]
  updated_at timestamp [not null]

  indexes {
    (auction_id, bidder_id) [pk]
  }
}

Table payment_transactions {
  id varchar(36) [pk]
  auction_id varchar(36) [not null, ref: > auctions.id]
  payer_id varchar(36) [not null, ref: > users.id]
  payee_id varchar(36) [not null, ref: > users.id]
  amount decimal(19,2) [not null]
  status varchar(50) [not null, note: "COMPLETED"]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table item_images {
  id varchar(36) [pk]
  item_id varchar(36) [not null, ref: > items.id]
  object_key varchar(512) [not null, unique]
  public_url text [not null]
  is_primary boolean [not null, default: false]
  sort_order integer [not null, default: 0]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table wallets {
  user_id varchar(36) [pk, ref: > users.id]
  balance decimal(19,2) [not null, default: 100000.00]
  created_at timestamp [not null]
  updated_at timestamp [not null]
}

Table wallet_transactions {
  id varchar(36) [pk]
  user_id varchar(36) [not null, ref: > users.id]
  amount decimal(19,2) [not null]
  transaction_type varchar(50) [not null, note: "DEPOSIT | WITHDRAW | PAYMENT | RECEIPT | REFUND"]
  reference_id varchar(36)
  description text
  created_at timestamp [not null]
}
```

## Indexes

- `idx_items_seller_id`
- `idx_auctions_item_id`, `idx_auctions_status`, `idx_auctions_highest_bidder_id`, `idx_auctions_status_end_time`
- `idx_bids_auction_id`, `idx_bids_bidder_id`
- `idx_auto_bid_configs_bidder_id`
- `idx_payment_transactions_auction_id`, `idx_payment_transactions_payer_id`, `idx_payment_transactions_payee_id`
- `idx_item_images_item_id`, `idx_item_images_is_primary`, `idx_item_images_item_sort`
- `idx_wallet_transactions_user_id`

## Ghi chu trien khai

- `auctions.version` duoc `BidService` dung cho optimistic concurrency khi cap nhat highest bid.
- `wallets` duoc seed tu dong cho user chua co wallet bang balance `100000.00` luc server startup.
- Xoa user/item/auction dua vao FK `ON DELETE CASCADE`, nhung cac service van chu dong xoa/cap nhat theo transaction de giu nghiep vu ro rang.
