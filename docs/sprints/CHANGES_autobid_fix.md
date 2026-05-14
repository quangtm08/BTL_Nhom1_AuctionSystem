# Fix: AutoBid Async + Bid Timeout (Concurrent Bid Issue)

## Vấn đề gốc

Khi 3 client đồng thời bid vào cùng một auction, client nhận lỗi **"Server unreachable: null"** do `TimeoutException` sau 10 giây. Nguyên nhân gốc rễ có 3 tầng:

1. **`triggerAutoBids()` chạy đồng bộ** trong luồng xử lý request — mỗi auto-bid chain có thể gọi đệ quy tới 20 lần, mỗi lần tốn 5 DB ops → tổng cộng 3 clients × 100 ops = ~300 DB ops tuần tự trước khi trả response.
2. **`Auction.auctionLock` vô dụng** — lock là per-instance, mỗi `findById()` tạo object `Auction` mới → 3 thread đều vượt validation cùng lúc với dữ liệu cũ.
3. **Singleton `Connection` không được bảo vệ** — tất cả thread (bid handlers, AuctionScheduler, AutoBidWorker) tranh chấp cùng 1 connection mà không có mutual exclusion.

---

## Các file đã thay đổi

### 1. `DBConnection.java`
**Vị trí:** `src/main/java/com/nhom1/auction/server/infrastructure/database/DBConnection.java`

**Thay đổi:** Thêm `synchronized` vào `getConnection()`.

**Lý do:** Tránh double-initialization khi nhiều thread gọi đồng thời lúc server khởi động.

```diff
- public static Connection getConnection() {
+ public static synchronized Connection getConnection() {
```

---

### 2. `BidService.java`
**Vị trí:** `src/main/java/com/nhom1/auction/server/bidding/BidService.java`

**Thay đổi:**
- Thêm field `Connection connection` và cập nhật constructor nhận thêm tham số `Connection`.
- Phương thức `placeBid()` thêm tham số `BidType bidType` (thay vì hardcode `BidType.MANUAL`).
- Bọc toàn bộ chuỗi read→validate→write trong `synchronized(connection)` + JDBC transaction (commit/rollback).
- Thêm helper `safeRollback()` và `safeSetAutoCommit()`.

**Lý do:** Đảm bảo toàn bộ chuỗi findById → placeBid → save → updateHighestBid là atomic. `synchronized(connection)` dùng singleton connection làm mutex — chỉ 1 bid được xử lý tại một thời điểm, loại bỏ race condition.

```diff
+ private final Connection connection;

- public BidService(BidRepository, AuctionRepository, ItemRepository, UserRepository)
+ public BidService(BidRepository, AuctionRepository, ItemRepository, UserRepository, Connection connection)

- public BidTransaction placeBid(UUID bidderId, UUID auctionId, BigDecimal amount)
+ public BidTransaction placeBid(UUID bidderId, UUID auctionId, BigDecimal amount, BidType bidType)
+     synchronized (connection) {
+         connection.setAutoCommit(false);
+         // ... findById, placeBid, save, updateHighestBid ...
+         connection.commit();
+     }
```

---

### 3. `BidModule.java`
**Vị trí:** `src/main/java/com/nhom1/auction/server/bidding/BidModule.java`

**Thay đổi:** Truyền `connection` vào `BidService` constructor.

```diff
- BidService service = new BidService(repository, auctionRepository, itemRepository, userRepository);
+ BidService service = new BidService(repository, auctionRepository, itemRepository, userRepository, connection);
```

---

### 4. `BidGatewayImpl.java`
**Vị trí:** `src/main/java/com/nhom1/auction/server/bidding/BidGatewayImpl.java`

**Thay đổi:** Truyền `BidType.AUTO` khi gọi `bidService.placeBid()` từ auto-bid path.

**Lý do:** Trước đây auto-bid được lưu vào DB với type `MANUAL` do hardcode — sai về mặt dữ liệu.

```diff
+ import com.nhom1.auction.common.enums.BidType;

- return bidService.placeBid(bidderId, auctionId, amount);
+ return bidService.placeBid(bidderId, auctionId, amount, BidType.AUTO);
```

---

### 5. `BidHandler.java`
**Vị trí:** `src/main/java/com/nhom1/auction/server/bidding/BidHandler.java`

**Thay đổi:**
- Truyền `BidType.MANUAL` khi gọi `bidService.placeBid()` từ manual bid path.
- Đổi từ `triggerAutoBids()` (đồng bộ, block) sang `scheduleAutoBids()` (fire-and-forget, async).
- Response được trả về **ngay lập tức** trước khi auto-bid chain bắt đầu.

**Lý do:** Đây là fix chính cho timeout — client không còn phải chờ toàn bộ auto-bid chain hoàn thành mới nhận được response.

```diff
- BidTransaction bidTransaction = bidService.placeBid(bidderId, auctionId, request.getBidAmount());
+ BidTransaction bidTransaction = bidService.placeBid(bidderId, auctionId, request.getBidAmount(), BidType.MANUAL);

+ ResponseMessage<PlaceBidResponse> result = new ResponseMessage<>(requestId, response);
- autoBidService.triggerAutoBids(...);   // block tới khi xong
+ autoBidService.scheduleAutoBids(...);  // fire-and-forget
- return new ResponseMessage<>(requestId, response);
+ return result;  // trả về NGAY, không đợi auto-bids
```

---

### 6. `AutoBidService.java`
**Vị trí:** `src/main/java/com/nhom1/auction/server/automation/AutoBidService.java`

**Thay đổi:** Viết lại hoàn toàn logic trigger:

| Trước | Sau |
|-------|-----|
| Đệ quy (`triggerAutoBidsInternal` depth++) | Vòng lặp `for (depth < 20)` |
| Không có `NotificationService` | Thêm `NotificationService` field |
| Chạy đồng bộ trên request thread | Chạy trên daemon `ExecutorService` riêng |
| Broadcast sau mỗi auto-bid | Broadcast **1 lần duy nhất** sau khi cả chain kết thúc |
| Không có `scheduleAutoBids()` | Thêm `scheduleAutoBids()` public (fire-and-forget) |

**Chi tiết kỹ thuật:**
- `ExecutorService executor = Executors.newSingleThreadExecutor(daemon=true)` — các chains xếp hàng, chạy tuần tự, không chặn JVM shutdown.
- Dùng snapshot variables (`final BigDecimal snapshotBid`) trong lambda để tuân thủ quy tắc effectively-final của Java.
- `triggerAutoBids()` giữ nguyên chữ ký để `AuctionScheduler` không bị breaking change.

---

### 7. `AutoBidModule.java`
**Vị trí:** `src/main/java/com/nhom1/auction/server/automation/AutoBidModule.java`

**Thay đổi:** Thêm tham số `NotificationService` vào `init()` và truyền vào `AutoBidService` constructor.

```diff
- public static AutoBidService init(Connection connection, MessageRouter router, BidGateway bidGateway)
+ public static AutoBidService init(Connection connection, MessageRouter router,
+                                   BidGateway bidGateway, NotificationService notificationService)

- AutoBidService service = new AutoBidService(repository, bidGateway);
+ AutoBidService service = new AutoBidService(repository, bidGateway, notificationService);
```

---

### 8. `ServerContext.java`
**Vị trí:** `src/main/java/com/nhom1/auction/server/infrastructure/ServerContext.java`

**Thay đổi:** Truyền `this.notificationService` vào `AutoBidModule.init()`.

```diff
- AutoBidModule.init(this.connection, this.router, bidGateway);
+ AutoBidModule.init(this.connection, this.router, bidGateway, this.notificationService);
```

---

## Luồng hoạt động sau khi fix

```
[Manual Bid Request]
        │
        ▼
BidHandler.handlePlaceBid()
        │
        ├─► synchronized(connection) {
        │       findById()         ← đọc state mới nhất
        │       auction.placeBid() ← validate in-memory
        │       bidRepository.save()
        │       auctionRepository.updateHighestBid()
        │       connection.commit()
        │   }  ← nhả lock
        │
        ├─► broadcastBidUpdate()   [đồng bộ, nhanh]
        │
        ├─► scheduleAutoBids()     ← nộp task vào executor, RETURN NGAY
        │
        └─► return ResponseMessage ← client nhận < 500ms

[auto-bid-worker thread — chạy ngầm]
        │
        ▼
  for (depth 0..19):
        ├─► findEligibleBots()
        ├─► synchronized(connection) { ... commit }
        └─► [sau khi loop xong] broadcastBidUpdate() ← 1 lần duy nhất
```

---

## Commit message gợi ý

```
fix(bidding): resolve concurrent bid timeout and autobid blocking

When 3 clients bid simultaneously, the server returned "Server
unreachable: null" (TimeoutException after 10s). Root causes:

1. triggerAutoBids() ran synchronously on the request thread,
   blocking up to 20 recursive calls x 5 DB ops before returning
   a response.
2. Auction.auctionLock was per-instance (new object each findById),
   making it useless for cross-request mutual exclusion.
3. The singleton Connection had no synchronization, causing all
   threads to race on the same JDBC connection.

Changes:
- BidService: wrap placeBid() in synchronized(connection) + JDBC
  transaction (commit/rollback) to make read-validate-write atomic.
- BidHandler: switch from triggerAutoBids() (blocking) to
  scheduleAutoBids() (fire-and-forget); response is now returned
  immediately after the manual bid is committed.
- AutoBidService: replace recursion with iterative loop; add
  single-thread daemon ExecutorService so auto-bid chains queue up
  without touching the request thread; broadcast once after the
  full chain settles instead of after each individual auto-bid.
- BidGatewayImpl: pass BidType.AUTO so auto-bids are recorded
  correctly in the database (was incorrectly stored as MANUAL).
- DBConnection: add synchronized to getConnection() to prevent
  double-initialization under concurrent startup.
- Wire NotificationService through AutoBidModule to AutoBidService.
```
