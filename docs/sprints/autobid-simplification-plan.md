# Auto-Bid Simplification Status

Tai lieu cu la ke hoach don gian hoa auto-bid. Code hien tai da ap dung phan chinh cua ke hoach: `AutoBidConfig` co `createdAt`, `AutoBidRepository` doc `created_at`, va `AutoBidService` dung `PriorityQueue`.

## Trang thai hien tai trong code

- `AutoBidRepository.findByAuctionId(...)` select `created_at` tu bang `auto_bid_configs`.
- `AutoBidConfig` co constructor nhan `LocalDateTime createdAt` va getter `getCreatedAt()`.
- `AutoBidService.runAutoBids(...)` tao `PriorityQueue<AutoBidConfig>` voi comparator:

```java
Comparator.comparing(AutoBidConfig::getCreatedAt)
    .thenComparing(AutoBidConfig::getBidderId)
```

- Bidder dang dan dau bi bo qua khi xep queue.
- Config chi duoc dua vao queue neu `maxAmount` du tra muc gia toi thieu tiep theo.
- Dat bid tu dong van di qua `BidGateway.placeAutoBid(...)`, tuc la van dung validation va transaction cua `BidService`.
- `MAX_TRIGGER_DEPTH` van giu vai tro chan loop vo han.

## Luong hien tai

```text
Co bid moi hoac config moi
-> Doc auction hien tai
-> Neu auction khong RUNNING thi xoa config cua auction do
-> Doc tat ca auto-bid config cua auction
-> Tim config cua leader hien tai neu co
-> Xep cac bidder khac du dieu kien vao PriorityQueue theo createdAt
-> Poll config uu tien nhat
-> Tinh next amount
-> Goi BidGateway.placeAutoBid(...)
-> Cap nhat currentHighestBid/currentHighestBidderId
-> Lap toi khi dung dieu kien
-> Broadcast PUSH_BID_UPDATE mot lan neu co auto-bid duoc dat
```

## Diem con phuc tap

Code van co logic escalation khi leader hien tai cung co auto-bid config:

- Neu selected bidder can canh tranh voi leader auto-bid, `nextAmt` co the nhay theo `leaderConfig.getMaxAmount() + selected.getIncrement()`.
- Neu selected bidder co `maxAmount <= leaderConfig.getMaxAmount()`, selected se bid toi max cua chinh minh de leader co co hoi vuot tiep o vong sau.

Phan nay giu tinh nang canh tranh auto-bid day du hon so voi phien ban don gian nhat. Neu muc tieu la de bao ve de tai, co the giai thich rang PriorityQueue quyet dinh thu tu ung vien, con escalation quyet dinh so tien hop le khi hai auto-bidder canh tranh.

## Khong can lam tiep

Cac viec trong ban ke hoach cu da xong:

- Lay `created_at` tu DB.
- Them `createdAt` vao config object.
- Dung `PriorityQueue`.
- Giu `BidGateway.placeAutoBid(...)`.
- Giu gioi han lap `MAX_TRIGGER_DEPTH`.

## Test nen xem

- `server/automation/AutoBidServiceTest.java`
- `server/automation/AutoBidRepositoryTest.java`
- `server/bidding/BidGatewayImplTest.java`
