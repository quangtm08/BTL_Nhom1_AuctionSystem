Table users {
  id text [pk, note: "UUID"]
  username text [not null, unique]
  email text [not null, unique]
  password text [not null]
  role text [not null, default: "USER", note: "USER | ADMIN"]
  created_at text [not null]
  updated_at text [not null]
}

Table items {
id text [pk, note: "UUID"]
seller_id text [not null, ref: > users.id]
name text [not null]
description text
category text [not null, note: "ELECTRONICS | ART | VEHICLE"]
condition text [not null, note: "NEW | LIKE_NEW | USED | DAMAGED"]

// Electronics only
brand text
warranty_months integer

// Art only
artist text
era text

// Vehicle only
production_year integer
fuel_type text [note: "GASOLINE | DIESEL | ELECTRIC | HYBRID"]

created_at text [not null]
updated_at text [not null]
}

Table auctions {
id text [pk, note: "UUID"]
item_id text [not null, ref: - items.id]
start_time text [not null]
end_time text [not null]
status text [not null, default: "OPEN", note: "OPEN | RUNNING | FINISHED | PAID | CANCELED"]
starting_price real [not null]
current_highest_bid real [not null, default: 0.0, note: "Cached — update atomically with each bid"]
highest_bidder_id text [ref: > users.id]
created_at text [not null]
updated_at text [not null]
}

Table bids {
id text [pk, note: "UUID"]
auction_id text [not null, ref: > auctions.id]
bidder_id text [not null, ref: > users.id]
amount real [not null]
bid_type text [not null, note: "MANUAL | AUTO"]
created_at text [not null]
}

Table auto_bids {
auction_id text [not null, ref: > auctions.id]
bidder_id text [not null, ref: > users.id]
max_amount real [not null]
increment real [not null]
created_at text [not null]

indexes {
(auction_id, bidder_id) [pk]
}
}