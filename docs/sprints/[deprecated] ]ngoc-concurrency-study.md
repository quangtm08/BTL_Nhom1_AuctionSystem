# Ôn tập: Luồng Đấu Giá & Xử lý Concurrency
> **Phạm vi:** Ngọc – Place Bid flow, Thread Safety, Validation  
> **Mục tiêu:** Nắm vững trong 1 tiếng

---

## MỤC LỤC

1. [Kiến trúc tổng quan & Vòng đời Request](#1-kiến-trúc-tổng-quan--vòng-đời-request)
2. [Luồng Place Bid – End-to-End](#2-luồng-place-bid--end-to-end)
3. [Concurrency – Cơ chế Khóa](#3-concurrency--cơ-chế-khóa)
4. [Validation – AuctionBidValidator](#4-validation--auctionbidvalidator)
5. [Real-time Update – Client nhận thay đổi](#5-real-time-update--client-nhận-thay-đổi)
6. [Trả lời 3 câu hỏi ví dụ gốc](#6-trả-lời-3-câu-hỏi-ví-dụ-gốc)
7. [15 Câu hỏi bổ sung + Đáp án](#7-15-câu-hỏi-bổ-sung--đáp-án)

---

## 1. Kiến trúc tổng quan & Vòng đời Request

### Sơ đồ tổng quan

```
[JavaFX Client]
      │  User nhấn "Đặt giá"
      ▼
[Client Service]
      │  Validate input + đóng gói PlaceBidRequest (DTO)
      ▼
[ClientConnection]
      │  Serialize DTO → JSON, gắn MessageType = PLACE_BID, gửi qua TCP Socket
      ▼
[Server – ClientHandler]
      │  Đọc JSON từ Socket (chạy trên thread riêng)
      ▼
[MessageRouter]
      │  Đọc trường "type", tra EnumMap → dispatch đến BidHandler
      ▼
[BidHandler.handlePlaceBid()]
      │  JsonUtil.fromJson(payload, PlaceBidRequest.class) → gọi BidService
      ▼
[BidService.placeBid()]
      │  Gọi auction.placeBid() (có khóa) → lưu DB → broadcast
      ▼
[NotificationService.broadcastBidUpdate()]
      │  Đẩy BidUpdateEvent tới ALL clients qua ClientRegistry
      ▼
[JavaFX Client (tất cả)]
      │  Nhận PUSH_BID_UPDATE → Platform.runLater() → cập nhật UI
```

### Vai trò từng lớp

| Lớp | Vai trò chính |
|-----|--------------|
| `PlaceBidRequest` | DTO – container thuần dữ liệu, không có logic |
| `BidHandler` | Parse JSON, gọi service, map exception → error code |
| `BidService` | Orchestrate nghiệp vụ: load auction, đặt giá, lưu DB, notify |
| `Auction.placeBid()` | Thực thi logic core, giữ tính toàn vẹn dữ liệu với lock |
| `AuctionBidValidator` | Kiểm tra tất cả điều kiện hợp lệ trước khi accept bid |
| `NotificationService` | Broadcast sự kiện real-time tới mọi client |
| `ClientRegistry` | Quản lý danh sách kết nối đang hoạt động |

---

## 2. Luồng Place Bid – End-to-End

### Bước 1 – Client gửi request

Client đóng gói thành `PlaceBidRequest` DTO và serialize sang JSON:
```json
{
  "type": "PLACE_BID",
  "requestId": "abc-123",
  "payload": {
    "auctionId": "uuid-auction",
    "bidderId": "uuid-bidder",
    "amount": 1500000
  }
}
```

### Bước 2 – BidHandler nhận và parse

```java
// BidHandler.java – register() callback
PlaceBidRequest request = JsonUtil.fromJson(payloadJson, PlaceBidRequest.class);
return handlePlaceBid(requestId, request);
```

`JsonUtil.fromJson()` dùng thư viện (Gson/Jackson) ánh xạ JSON fields → Java fields theo tên field.  
`PlaceBidRequest` dùng constructor mặc định + setters (pattern POJO) để deserialization hoạt động.

### Bước 3 – BidService orchestrate

```java
// BidService.java
public BidTransaction placeBid(UUID bidderId, UUID auctionId, BigDecimal amount) {
    Auction auction = auctionRepository.findById(auctionId)
        .orElseThrow(() -> new ValidationException("Auction not found"));

    BidTransaction bidTransaction = auction.placeBid(bidderId, amount, BidType.MANUAL, LocalDateTime.now());
    // ↑ Toàn bộ logic concurrency nằm trong đây

    bidRepository.save(bidTransaction);
    auctionRepository.updateHighestBid(auctionId, bidTransaction.getAmount(), bidTransaction.getBidderId());
    return bidTransaction;
}
```

### Bước 4 – Auction.placeBid() với lock

```java
// Auction.java
public BidTransaction placeBid(UUID bidderId, BigDecimal amount, BidType bidType, LocalDateTime bidTime) {
    auctionLock.lock();          // ← ACQUIRE LOCK
    try {
        AuctionBidValidator.validatePlaceBid(this, bidderId, amount, bidType, bidTime);  // validate
        BidTransaction tx = new BidTransaction(getId(), bidderId, amount, bidType);
        synchronized (bidHistoryMonitor) {   // ← LOCK 2: bảo vệ List
            bidHistory.add(tx);
        }
        highestBidderId = bidderId;          // volatile write
        currentHighestBid = amount;          // volatile write
        touchUpdatedAt();
        return tx;
    } finally {
        auctionLock.unlock();    // ← RELEASE LOCK (luôn chạy dù có exception)
    }
}
```

### Bước 5 – Broadcast real-time

```java
// BidHandler.java sau khi placeBid() thành công
notificationService.broadcastBidUpdate(auctionId, bidTransaction.getAmount(), bidTransaction.getBidderId());
if (autoBidService != null) {
    autoBidService.triggerAutoBids(auctionId, bidTransaction.getAmount(), bidTransaction.getBidderId());
}
```

---

## 3. Concurrency – Cơ chế Khóa

### 3.1 ReentrantLock – `auctionLock`

```java
private final ReentrantLock auctionLock = new ReentrantLock(true); // fair = true
```

**ReentrantLock là gì?**  
Là khóa tương tự `synchronized` nhưng linh hoạt hơn: có thể thử lock không chặn (`tryLock()`), hỗ trợ timeout, và quan trọng nhất – hỗ trợ **fair mode**.

**Tại sao `fair: true`?**  
- Với `fair = false` (mặc định): thread nào được CPU schedule trước thì lấy lock → có thể có thread bị **starvation** (chờ mãi không được).  
- Với `fair = true`: lock được cấp theo thứ tự FIFO (thread nào chờ lâu nhất → được lock trước).  
- Trong đấu giá: đảm bảo các bid được xử lý **theo đúng thứ tự thời gian đến**, không có bid nào bị bỏ qua oan.

**Tại sao dùng ReentrantLock thay vì `synchronized`?**  
- Cùng một thread có thể `lock()` nhiều lần mà không deadlock (Reentrant = có thể vào lại).  
- Cú pháp `try-finally` đảm bảo `unlock()` **luôn chạy** dù có exception.

**Những gì `auctionLock` bảo vệ:**
- `status` – trạng thái phiên (OPEN/RUNNING/FINISHED...)
- `currentHighestBid` – giá cao nhất hiện tại
- `highestBidderId` – người đang dẫn đầu
- `endTime` – thời điểm kết thúc (cho anti-sniping)
- Validation + update xảy ra trong cùng một critical section, không thể bị chen ngang

### 3.2 Race Condition – và cách `auctionLock` ngăn chặn

**Race Condition là gì?**  
Khi hai hoặc nhiều thread cùng đọc-rồi-ghi một biến dùng chung, kết quả phụ thuộc vào thứ tự thực thi không đoán được.

**Ví dụ trong đấu giá (không có lock):**
```
currentHighestBid = 1,000,000
Thread A (Bidder A đặt 1,200,000): đọc currentHighestBid = 1,000,000 → hợp lệ!
Thread B (Bidder B đặt 1,100,000): đọc currentHighestBid = 1,000,000 → hợp lệ!
Thread A: ghi currentHighestBid = 1,200,000
Thread B: ghi currentHighestBid = 1,100,000  ← GHI ĐÈ! Giá bị giảm xuống!
```

**Với `auctionLock`:**
```
currentHighestBid = 1,000,000
Thread A: lock.lock() ← thành công, vào critical section
Thread B: lock.lock() ← BỊ CHẶN, phải chờ
Thread A: validate, ghi currentHighestBid = 1,200,000, lock.unlock()
Thread B: lock.lock() ← THÀNH CÔNG, vào critical section
Thread B: validate: 1,100,000 <= 1,200,000 → NÉM InvalidBidException ✓
```

### 3.3 Synchronized `bidHistoryMonitor` – Khóa thứ hai

```java
private final Object bidHistoryMonitor = new Object();

// Trong placeBid():
synchronized (bidHistoryMonitor) {
    bidHistory.add(bidTransaction);
}

// Trong getBidHistory():
synchronized (bidHistoryMonitor) {
    return List.copyOf(bidHistory);
}
```

**Tại sao cần khóa thứ hai riêng biệt?**

| Vấn đề | Giải thích |
|--------|-----------|
| `ArrayList` không thread-safe | Nếu một thread đang `add()` và thread khác đang `iterator()` → `ConcurrentModificationException` |
| Không thể dùng `auctionLock` | `getBidHistory()` là read-only và công khai – nếu dùng `auctionLock`, mỗi lần read lịch sử sẽ block toàn bộ đặt giá |
| Tách biệt concern | `auctionLock` bảo vệ *trạng thái phiên*; `bidHistoryMonitor` bảo vệ *danh sách ghi chép*. Hai concern khác nhau → hai khóa khác nhau |
| Hiệu năng | Giảm tranh chấp lock: read history và place bid không cần block nhau |

**`List.copyOf(bidHistory)`** – Tại sao trả về bản sao?  
Để caller không thể `add/remove` vào internal list → bảo vệ tính bất biến của lịch sử bid.

### 3.4 Biến `volatile`

```java
private volatile UUID highestBidderId;
private volatile BigDecimal currentHighestBid;
private volatile AuctionStatus status;
```

**`volatile` đảm bảo gì?**  
- **Visibility:** Mọi write vào biến volatile sẽ *ngay lập tức* được nhìn thấy bởi tất cả thread khác.  
- **Không cache:** CPU không được cache biến này vào register/L1 cache riêng – luôn phải đọc từ main memory.

**Tại sao cần `volatile` nếu đã có lock?**  
- `auctionLock` bảo vệ *section ghi*. Nhưng code khác (scheduler, notification service) có thể **đọc** `currentHighestBid` mà không acquire lock.
- Nếu không có `volatile`, thread đọc có thể thấy giá trị cũ do CPU cache → hiển thị sai giá trên UI.
- `volatile` đảm bảo: ngay khi `auctionLock` được release và giá trị được ghi, tất cả thread đọc đều thấy giá trị mới.

**`volatile` có đủ thay lock không?**  
Không. `volatile` chỉ đảm bảo visibility, không đảm bảo **atomicity** của các thao tác compound (check-then-act).  
Ví dụ: `if (amount > current) { current = amount; }` là 2 bước → cần lock để atomic.

### 3.5 Tính Atomic – Đảm bảo hai bid không xung đột

**"Atomic"** = không thể bị chia cắt – một thao tác hoặc thực hiện hoàn toàn, hoặc không thực hiện gì cả.

Trong `placeBid()`, toàn bộ sequence sau là một atomic unit nhờ `auctionLock`:
```
validate() → create BidTransaction → add to history → update highestBid
```

Không có thread nào có thể chen vào giữa các bước này. → Hai bid đồng thời không bao giờ ghi đè lẫn nhau.

---

## 4. Validation – AuctionBidValidator

`AuctionBidValidator` là lớp `final`, package-private, constructor private → chỉ có thể dùng từ `Auction.java` (cùng package). Đây là **utility class** thuần túy, không có state.

### Tất cả điều kiện kiểm tra

```
validatePlaceBid(auction, bidderId, amount, bidType, bidTime)
│
├─ Null checks: bidderId, amount, bidType, bidTime
├─ amount > 0
├─ bidderId ≠ auction.sellerId         → UnauthorizedActionException
├─ auction.status == RUNNING           → AuctionClosedException nếu không phải
├─ bidTime >= auction.startTime        → InvalidBidException
├─ bidTime <= auction.endTime          → AuctionClosedException
│
└─ Kiểm tra giá:
   ├─ Nếu chưa có bid (currentHighestBid == null):
   │    amount >= startingPrice         → InvalidBidException nếu không đủ
   ├─ Nếu đã có bid:
   │    amount > currentHighestBid      → InvalidBidException nếu <=
   │    amount >= currentHighestBid + minBidIncrement  → InvalidBidException nếu <
```

### Công thức minBidIncrement

```java
// Auction.java
public BigDecimal getMinBidIncrement() {
    return startingPrice.multiply(new BigDecimal("0.05"))
        .setScale(2, RoundingMode.HALF_UP);
}
```

**Ý nghĩa:** Bước giá tối thiểu = 5% của giá khởi điểm, làm tròn đến 2 chữ số thập phân.

**Ví dụ:**
- `startingPrice = 1,000,000` → `minBidIncrement = 50,000`
- `currentHighestBid = 1,200,000` → bid tiếp theo phải >= `1,250,000`

### Tại sao `minBidIncrement` tính từ `startingPrice` chứ không phải `currentHighestBid`?

Để bước giá **ổn định và dự đoán được**. Nếu tính từ `currentHighestBid`, bước giá tăng theo cấp số nhân → người dùng khó biết cần đặt bao nhiêu. Dùng `startingPrice` cố định, bước giá không đổi suốt phiên.

---

## 5. Real-time Update – Client nhận thay đổi

### 5.1 Flow sau khi bid thành công

```
BidHandler.handlePlaceBid()
  ↓ placeBid() thành công
NotificationService.broadcastBidUpdate(auctionId, newPrice, newBidderId)
  ↓
NotificationService.sendPush(PUSH_BID_UPDATE, BidUpdateEvent)
  ↓ serialize → JSON
ClientRegistry.broadcast(json)
  ↓ forEach
ClientHandler.push(json)   ← gọi cho TỪNG client đang kết nối
  ↓ synchronized (thread-safe write to socket)
Client nhận PUSH_BID_UPDATE
  ↓ Platform.runLater()
UI cập nhật giá mới
```

### 5.2 BidUpdateEvent – payload được push

```java
// NotificationService.broadcastBidUpdate()
BidUpdateEvent event = new BidUpdateEvent(
    auctionId.toString(),
    newHighestBid,           // BigDecimal
    newHighestBidderId,      // String UUID
    LocalDateTime.now()      // timestamp
);
```

### 5.3 ClientRegistry – quản lý kết nối

```java
// ClientRegistry.java
private final Map<UUID, ClientHandler> activeClients = new ConcurrentHashMap<>();
private final Map<UUID, UUID> userToClientMap = new ConcurrentHashMap<>();
```

**Tại sao `ConcurrentHashMap`?**  
- `broadcast()` và `register()`/`unregister()` có thể chạy đồng thời trên nhiều thread.
- `HashMap` không thread-safe → có thể corrupt khi concurrent modify.
- `ConcurrentHashMap` dùng segment-level locking, cho phép đọc đồng thời và write an toàn.

### 5.4 Platform.runLater() – JavaFX threading

JavaFX có quy tắc: **chỉ JavaFX Application Thread được phép cập nhật UI**.

Client nhận push từ `ClientHandler` – đang chạy trên **socket thread** (không phải JavaFX thread).  
Nếu cập nhật UI trực tiếp → `IllegalStateException: Not on FX application thread`.

`Platform.runLater(Runnable)` đưa Runnable vào **JavaFX event queue** để chạy trên đúng thread.

```java
// Trong client handler khi nhận PUSH_BID_UPDATE:
Platform.runLater(() -> {
    bidPriceLabel.setText(String.valueOf(newHighestBid));
});
```

---

## 6. Trả lời 3 câu hỏi ví dụ gốc

### Câu 1: "JSON từ client được chuyển đổi thành DTO `PlaceBidRequest` trên server như thế nào?"

**Đáp án đầy đủ:**

1. **Client** tạo `PlaceBidRequest(auctionId, bidderId, amount)` và gọi `JsonUtil.toJson()` → chuỗi JSON.
2. Chuỗi JSON được đặt vào trường `payload` của `RequestMessage` cùng với `type = "PLACE_BID"` và `requestId`.
3. `ClientConnection` gửi toàn bộ `RequestMessage` qua TCP socket.
4. **Server** – `ClientHandler` đọc dòng từ socket, gọi `MessageRouter.route(json)`.
5. `MessageRouter` parse trường `type` → tìm handler `PLACE_BID` trong EnumMap → gọi callback với `(requestId, payloadJson)`.
6. **BidHandler** nhận `payloadJson` (chỉ là phần payload) → gọi `JsonUtil.fromJson(payloadJson, PlaceBidRequest.class)`.
7. `JsonUtil.fromJson()` (Gson/Jackson) dùng **reflection**: tạo instance `PlaceBidRequest` bằng constructor mặc định, rồi gọi setter/set field cho từng key trong JSON:
   - `"auctionId"` → `setAuctionId()`
   - `"bidderId"` → `setBidderId()`
   - `"amount"` → `setBidAmount()` (field name trong JSON là `amount`, getter là `getBidAmount()` – Gson match theo field name, không phải getter name)

**Lưu ý quan trọng:** `PlaceBidRequest` phải có **constructor không tham số** (no-arg constructor) để serialization framework có thể khởi tạo object trước khi inject giá trị.

---

### Câu 2: "Race Condition là gì, và `auctionLock` ngăn chặn nó như thế nào trong một cuộc chiến đấu giá?"

**Đáp án đầy đủ:**

**Race Condition** xảy ra khi kết quả của một tính toán phụ thuộc vào thứ tự/timing của các thread, và thứ tự đó không được kiểm soát.

**Kịch bản Race Condition nếu không có lock:**

```
currentHighestBid = 1,000,000 (bước giá 50,000)

T=0: Thread A (đặt 1,100,000): đọc currentHighestBid=1M → validate OK
T=0: Thread B (đặt 1,080,000): đọc currentHighestBid=1M → validate OK (chưa biết A đang đặt)
T=1: Thread A: ghi currentHighestBid = 1,100,000
T=1: Thread B: ghi currentHighestBid = 1,080,000  ← Race! Giá GIẢM xuống 1,080,000
T=2: Thread A thắng nhưng hệ thống ghi nhận B là người thắng!
```

**Cách `auctionLock` ngăn chặn:**

`ReentrantLock` biến toàn bộ sequence `[validate → create → update]` thành **critical section** không thể bị chen ngang:

```
Thread A: auctionLock.lock() → THÀNH CÔNG
Thread B: auctionLock.lock() → BỊ CHẶN (block)
Thread A: validate 1,100,000 > 1,000,000 ✓, ghi currentHighestBid=1,100,000
Thread A: auctionLock.unlock()
Thread B: được unblock, auctionLock.lock() → THÀNH CÔNG
Thread B: validate 1,080,000 <= 1,100,000 → NÉM InvalidBidException
```

Kết quả: Chỉ Thread A thành công. Không có race condition. `fair=true` đảm bảo nếu Thread C đến sau cũng sẽ thấy giá 1,100,000 của A.

---

### Câu 3: "Làm thế nào client biết giá đã thay đổi mà không cần tải lại trang?"

**Đáp án đầy đủ – Mô hình Push (Server-push / Observer pattern):**

Hệ thống dùng **persistent TCP connection** + **server-push mechanism**:

1. **Connection duy trì:** Client không đóng kết nối sau mỗi request. `ClientHandler` chạy trong vòng lặp liên tục đọc từ socket.

2. **Server chủ động push:** Sau khi bid thành công, `BidHandler` gọi:
   ```java
   notificationService.broadcastBidUpdate(auctionId, newPrice, newBidderId);
   ```

3. **Broadcast tới tất cả:** `ClientRegistry.broadcast()` duyệt qua tất cả `activeClients` và gọi `client.push(json)` cho mỗi người.

4. **Client nhận message loại mới:** Client đang chờ trong vòng lặp read, nhận được message có `type = "PUSH_BID_UPDATE"` (không phải response cho request nào cả).

5. **UI thread-safe update:** Client dispatch sang JavaFX Application Thread qua `Platform.runLater()` để cập nhật label giá.

**Đây là Observer Pattern:** Server = Subject/Publisher, mỗi Client = Observer/Subscriber. Khi có sự kiện (bid mới), server notify tất cả observer.

---

## 7. 15 Câu hỏi bổ sung + Đáp án

> Câu hỏi từ Q1–Q5: Concurrency sâu | Q6–Q10: Code/Architecture | Q11–Q15: Tình huống & Edge case

---

### Q1. Điều gì xảy ra nếu `auctionLock.unlock()` không được gọi trong `placeBid()`?

**Đáp án:**  
Deadlock. Nếu một thread giữ lock mà không bao giờ release, tất cả thread khác cố gọi `auctionLock.lock()` sẽ **block vĩnh viễn**. Không có bid nào có thể được đặt nữa trong phiên đó.

**Cách code phòng ngừa:**  
Cấu trúc `try-finally` đảm bảo `unlock()` luôn chạy dù `validatePlaceBid()` hay bất kỳ bước nào ném exception:
```java
auctionLock.lock();
try {
    // ... có thể throw
} finally {
    auctionLock.unlock();  // LUÔN LUÔN chạy
}
```

---

### Q2. Tại sao `bidHistory` dùng `synchronized(bidHistoryMonitor)` thay vì `Collections.synchronizedList()`?

**Đáp án:**  
`Collections.synchronizedList()` chỉ sync từng method riêng lẻ. Nhưng để `getBidHistory()` trả về snapshot an toàn, cần lock **trong suốt** thao tác `List.copyOf()`:

```java
// Nếu dùng synchronizedList:
synchronized(bidHistory) {           // vẫn phải lock thủ công khi iterate
    return List.copyOf(bidHistory);
}
```

Dùng `bidHistoryMonitor` tường minh hơn và tách bạch rõ ràng: monitor này chỉ dành riêng cho `bidHistory`, không liên quan đến `auctionLock`. Hai khóa serve hai purpose khác nhau.

---

### Q3. Tại sao `BidService.placeBid()` gọi `auctionRepository.findById()` mỗi lần thay vì cache `Auction` trong memory?

**Đáp án:**  
`AuctionRepository.findById()` trả về một `Auction` object được load từ DB. Object này là **in-memory representation** và giữ lock riêng của nó.

Nếu cache Auction object lâu dài trong memory, các field như `currentHighestBid` trong DB và trong object sẽ **drift** (khác nhau). Khi scheduler cập nhật DB trực tiếp hoặc server restart, cache sẽ stale.

Với SQLite (single-file DB), load fresh mỗi request là chấp nhận được về hiệu năng.

---

### Q4. `AutoBidService.triggerAutoBids()` có thể gây ra vấn đề gì nếu hai manual bid đến đồng thời?

**Đáp án:**  
`triggerAutoBids()` được gọi từ `BidHandler` **sau khi** `placeBid()` thành công và lock đã được release. Nếu hai manual bid đến đồng thời:

1. Thread A: `placeBid()` thành công → gọi `triggerAutoBids(amount_A)`
2. Thread B: `placeBid()` thành công (sau A) → gọi `triggerAutoBids(amount_B)`
3. Hai `triggerAutoBids()` chạy đồng thời

Tuy nhiên, `triggerAutoBids()` gọi `bidGateway.placeAutoBid()` → cuối cùng gọi `BidService.placeBid()` → `auction.placeBid()` với `auctionLock`. Các auto-bid vẫn được serialize bởi lock. Auto-bid nào thua sẽ nhận `InvalidBidException` và bị **silently ignored** (code trong `AutoBidService` bắt exception và bỏ qua).

---

### Q5. Giải thích tại sao `currentHighestBid` cần vừa `volatile` vừa được bảo vệ bởi `auctionLock`.

**Đáp án:**  
Hai cơ chế phục vụ hai mục đích khác nhau, không thay thế nhau:

| Cơ chế | Đảm bảo gì | Thiếu gì nếu chỉ dùng một |
|--------|-----------|--------------------------|
| `auctionLock` | **Atomicity** – chuỗi validate+write không bị chen ngang | Không đảm bảo reader thread thấy giá trị mới ngay lập tức |
| `volatile` | **Visibility** – write được flush ngay vào main memory, mọi reader thấy ngay | Không atomic – check-then-act vẫn có race condition |

**Kết hợp:** `auctionLock` đảm bảo không ai ghi đồng thời; `volatile` đảm bảo sau khi writer unlock, mọi reader (không giữ lock) đọc được giá trị mới ngay lập tức.

---

### Q6. Trong `BidHandler`, tại sao `AutoBidService` được inject qua setter (`setAutoBidService()`) thay vì constructor?

**Đáp án:**  
Đây là giải pháp cho **circular dependency**:

```
BidHandler → AutoBidService
AutoBidService → BidGateway → BidService
BidService → (không cần BidHandler)
BidHandler → AutoBidService  (circular nếu cả hai qua constructor)
```

Trong `ServerContext`, các module được khởi tạo tuần tự:
1. Tạo `BidService` (không cần AutoBidService)
2. Tạo `BidHandler(bidService, notificationService)`
3. Tạo `AutoBidService`
4. `bidHandler.setAutoBidService(autoBidService)` ← inject sau

Setter injection cho phép "wire" sau khi tất cả objects đã được tạo.

---

### Q7. `PlaceBidRequest` có field `amount` nhưng getter là `getBidAmount()`. Điều này ảnh hưởng gì đến serialization?

**Đáp án:**  
Đây là điểm tinh tế của Java serialization framework:

- **Gson** (nếu dùng): serialize/deserialize dựa trên **field name**, không dựa trên getter/setter. Field tên `amount` → JSON key là `"amount"`. Getter name không quan trọng với Gson.
- **Jackson** (nếu dùng): dựa trên getter/setter. `getBidAmount()` → Jackson expect JSON key là `"bidAmount"`, không phải `"amount"` → có thể gây mismatch.

Client phải gửi JSON với đúng key mà server expect. Sự không nhất quán này (`field = amount`, `getter = getBidAmount`) là **potential bug** nếu framework dùng getter-based mapping.

---

### Q8. Làm thế nào `MessageRouter` biết điều hướng `PLACE_BID` đến `BidHandler`?

**Đáp án:**  
`BidHandler.register(router)` được gọi trong `ServerContext` khi khởi tạo. Bên trong `register()`:

```java
router.register(MessageType.PLACE_BID, (requestId, payloadJson) -> {
    PlaceBidRequest request = JsonUtil.fromJson(payloadJson, PlaceBidRequest.class);
    return handlePlaceBid(requestId, request);
});
```

`MessageRouter` lưu mapping `MessageType → Handler` trong `EnumMap`. Khi nhận message, extract `type` → tra map → gọi lambda callback tương ứng.

`EnumMap` được dùng thay vì `HashMap` vì key là enum → hiệu suất tốt hơn (array-based lookup).

---

### Q9. `AuctionBidValidator` là `final class` với `private constructor`. Ý nghĩa thiết kế là gì?

**Đáp án:**

| Đặc điểm | Ý nghĩa |
|----------|---------|
| `final class` | Không thể kế thừa, tránh override validation logic |
| `private constructor` | Không thể instantiate, buộc dùng static method |
| `package-private` (không có `public`) | Chỉ `Auction.java` (cùng package) mới gọi được, ẩn implementation detail |
| `static method` | Utility function thuần túy, không có state |

Đây là pattern **Utility Class** – tất cả method là static, class không thể được tạo instance. Validation logic được tách ra khỏi `Auction.java` để giữ Single Responsibility Principle: `Auction` quản lý state, `AuctionBidValidator` kiểm tra điều kiện.

---

### Q10. `ClientRegistry` dùng hai `ConcurrentHashMap` – một theo `clientId` và một theo `userId`. Tại sao cần cả hai?

**Đáp án:**

```java
Map<UUID, ClientHandler> activeClients;  // clientId → handler
Map<UUID, UUID> userToClientMap;         // userId → clientId
```

| Map | Dùng khi |
|-----|---------|
| `activeClients` | Broadcast tới tất cả (`broadcast()`), manage kết nối (register/unregister) |
| `userToClientMap` | Gửi message tới **một user cụ thể** (`sendToUser(userId)`) |

Một user có thể disconnect rồi reconnect → `clientId` (UUID random mỗi lần) thay đổi, nhưng `userId` không đổi. Sau login, `linkUser(userId, clientId)` cập nhật mapping.

`broadcast()` chỉ cần `activeClients`. `sendToUser()` cần `userToClientMap` để tìm `clientId` → rồi tra `activeClients`. Two-level lookup.

---

### Q11. Kịch bản: Bidder đặt giá đúng lúc phiên hết giờ (`bidTime == endTime`). Hệ thống xử lý thế nào?

**Đáp án:**  
Trong `AuctionBidValidator`:

```java
if (bidTime.isAfter(auction.getEndTime())) {
    throw new AuctionClosedException("auction has already ended");
}
```

Điều kiện là **strictly after** (`isAfter`), không phải `>=`. Nên `bidTime == endTime` → `isAfter` = false → **BID ĐƯỢC CHẤP NHẬN**.

Tuy nhiên, `AuctionScheduler` có thể đã gọi `endAuction()` và set `status = FINISHED` trước đó (race với scheduler). Nếu status là FINISHED:

```java
if (auction.getStatus() != AuctionStatus.RUNNING) {
    throw new AuctionClosedException("auction is not accepting bids");
}
```

→ Bid bị từ chối vì status không còn là RUNNING.

**Kết luận:** Kết quả phụ thuộc vào thứ tự: bid validation chạy trước hay scheduler chạy trước. `auctionLock` trong `endAuction()` và `placeBid()` đảm bảo một trong hai sẽ thắng, không có trạng thái corrupt.

---

### Q12. Kịch bản: Server có 100 client đang xem một phiên đấu giá. Điều gì xảy ra bên trong `broadcast()` khi một bid được đặt?

**Đáp án:**  
`ClientRegistry.broadcast()` chạy trên **thread của BidHandler** (là thread của client đặt bid):

```java
public void broadcast(String json) {
    activeClients.values().forEach(client -> client.push(json));  // sequential forEach
}
```

- `forEach` là **sequential** (không parallel) → lần lượt push từng client.
- Mỗi `client.push(json)` là `synchronized` → không bị overlap khi nhiều broadcast đồng thời.
- Với 100 clients, thread phải push lần lượt 100 lần → latency tăng tuyến tính.

**Bottleneck tiềm năng:** Nếu một client bị chậm (slow network), `push()` có thể block và làm chậm toàn bộ broadcast.

**Cải tiến có thể:** Dùng `parallelStream()` hoặc submit mỗi push vào thread pool riêng.

---

### Q13. Tại sao `BidTransaction` là immutable (không có setter)?

**Đáp án:**  
`BidTransaction` đại diện cho một **lịch sử giao dịch đã xảy ra** – một sự kiện quá khứ không thể thay đổi. Immutability đảm bảo:

1. **Thread-safety tự nhiên:** Object immutable có thể chia sẻ giữa nhiều thread mà không cần lock.
2. **Audit trail integrity:** Lịch sử bid không thể bị sửa sau khi tạo – quan trọng với tính toàn vẹn tài chính.
3. **Predictability:** Caller không thể accidently mutate một transaction đang được xử lý ở thread khác.

Khi load từ DB, dùng constructor thứ hai với đầy đủ tham số (bao gồm cả `id`, `createdAt`).

---

### Q14. Kịch bản: `BidService.placeBid()` thành công nhưng `bidRepository.save()` ném exception (DB lỗi). Trạng thái hệ thống sẽ thế nào?

**Đáp án – Đây là một bug/limitation trong thiết kế hiện tại:**

```java
// BidService.java
BidTransaction tx = auction.placeBid(...);  // ← in-memory state đã được update
bidRepository.save(tx);                      // ← NÉM EXCEPTION
// → currentHighestBid trong Auction object đã bị đổi
// → DB không có record mới
// → Auction object trong memory và DB KHÔNG ĐỒNG BỘ
```

**Hậu quả:**
- `auction.currentHighestBid` trong memory đã tăng lên.
- DB vẫn ghi nhận giá cũ.
- `AuctionRepository.updateHighestBid()` (dòng tiếp theo) cũng không chạy.
- Broadcast không xảy ra (exception propagate up).
- Sau khi exception được bắt ở BidHandler → client nhận error.
- **Nhưng** nếu server load lại Auction từ DB, `currentHighestBid` sẽ là giá cũ → inconsistency trong session hiện tại.

**Giải pháp đúng:** Cần transaction DB bao toàn bộ: nếu bất kỳ bước nào fail, rollback `auction.placeBid()` (hoặc toàn bộ flow).

---

### Q15. Giải thích Anti-Sniping và cách `AuctionScheduler` triển khai nó liên quan đến `Auction.extendEndTime()`.

**Đáp án:**

**Anti-Sniping** là kỹ thuật ngăn "bid sniper" – những người chờ đến giây cuối cùng để đặt giá, không cho người khác có cơ hội phản ứng.

**Logic trong `AuctionScheduler`:**

```
Mỗi tick (mỗi giây):
  Với mỗi phiên RUNNING có endTime sắp đến:
    Lấy lastBidTime = bidGateway.findLastBidTime(auctionId)
    Nếu lastBidTime >= (endTime - 15 giây):   // bid trong 15 giây cuối
        auction.extendEndTime(endTime + 30 giây)  // gia hạn thêm 30 giây
        auctionRepository.updateEndTime(...)
    Ngược lại:
        auction.endAuction()                       // kết thúc bình thường
        notificationService.broadcastAuctionEnded(...)
```

**`extendEndTime()` trong Auction.java:**
```java
public void extendEndTime(LocalDateTime newEndTime) {
    auctionLock.lock();     // lock để đảm bảo không race với placeBid()
    try {
        this.endTime = newEndTime;
        touchUpdatedAt();
    } finally {
        auctionLock.unlock();
    }
}
```

**Tại sao cần lock khi extend?**  
`placeBid()` đọc `endTime` trong validation (`bidTime.isAfter(auction.getEndTime())`). Nếu scheduler extend `endTime` đồng thời với `placeBid()` đọc nó, có thể có race. Lock đảm bảo extend và validate không xảy ra đồng thời.

**Kết quả:** Bidder có 30 giây để phản ứng sau mỗi last-minute bid → sân chơi công bằng hơn.

---

## Tổng kết – Bảng Quick Reference

| Khái niệm | Class/Field | Mô tả ngắn |
|-----------|------------|------------|
| Fair ReentrantLock | `Auction.auctionLock` | Serialize tất cả thay đổi trạng thái phiên, FIFO ordering |
| Volatile visibility | `currentHighestBid`, `status`, `highestBidderId` | Flush ngay lên main memory, mọi thread đọc thấy giá trị mới |
| Synchronized monitor | `bidHistoryMonitor` | Bảo vệ ArrayList `bidHistory` khỏi concurrent modification |
| Critical section | `Auction.placeBid()` body | validate + create + update là atomic, không thể bị chen |
| Utility validator | `AuctionBidValidator` | final, package-private, static method, không có state |
| Min bid increment | `startingPrice * 5%` | Cố định theo giá khởi điểm, làm tròn 2 chữ số |
| DTO | `PlaceBidRequest` | POJO với no-arg constructor cho JSON deserialization |
| Observer pattern | `NotificationService` + `ClientRegistry` | Server push BidUpdateEvent tới tất cả clients |
| JavaFX threading | `Platform.runLater()` | Chạy UI update trên JavaFX Application Thread |
| Anti-sniping | `AuctionScheduler` + `extendEndTime()` | Gia hạn 30s nếu bid trong 15s cuối |
| Circular dep fix | `BidHandler.setAutoBidService()` | Setter injection để tránh circular constructor dependency |
| ConcurrentHashMap | `ClientRegistry` | Thread-safe registry, broadcast không corrupt |

---

*Chúc ôn tập tốt! Nắm vững 5 điểm cốt lõi: (1) ReentrantLock fair=true, (2) cơ chế 2 khóa, (3) volatile visibility, (4) validation pipeline, (5) server-push observer pattern.*
