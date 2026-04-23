# Kế hoạch Phân bổ Nhiệm vụ

> **Đọc trước:** `feature-development-guideline.md` — nắm vững mô hình Handler → Service → Repository trước khi bắt đầu.

---

## Nhắc lại kiến trúc

Mỗi tính năng server gồm 4 file, dùng `AuthModule` làm tham chiếu:

```
server/<tên_tính_năng>/
  ├── FeatureRepository.java   — chỉ SQL, không logic
  ├── FeatureService.java      — chỉ logic, không JSON, không SQL
  ├── FeatureHandler.java      — nhận JSON → gọi Service → trả JSON
  └── FeatureModule.java       — init() static, kết nối 3 file trên, gọi từ ServerContext
```

Phía client: dùng `SignInController` làm tham chiếu — build DTO từ form, gửi qua `ServerConnection`, xử lý response trong `Platform.runLater`.

---

## File dùng chung — Quy tắc sở hữu

**Chỉ Thành viên 4 commit vào các file sau.** Thành viên khác nêu thay đổi cần thiết trong chat, TV4 thực hiện.

| File | Lý do |
|---|---|
| `common/protocol/MessageType.java` | Cả 4 thành viên cần thêm mục |
| `server/infrastructure/ServerContext.java` | Mọi module đều đăng ký tại đây |
| `client/AppView.java` | Màn hình mới cần thêm mục |

---

## Thành viên 1 — Luồng Người bán

**Phạm vi:** Seller tạo phiên đấu giá → hệ thống lưu và hiển thị phiên đó.

---

### DTOs cần tạo (`common/dto/auction/`)

**`CreateAuctionRequest`** — thông tin tạo phiên đấu giá: tên, mô tả, danh mục, tình trạng, giá khởi điểm, thời gian bắt đầu/kết thúc, và các trường phụ theo danh mục (Art: artist/era; Electronics: brand/warrantyMonths; Vehicle: brand/productionYear/fuelType). Thêm `sellerId` (String) — client sẽ điền từ `AppContext.getCurrentUser().getUserID()`.

**`CreateAuctionResponse`** — trả về auctionId và status sau khi tạo thành công.

**`AuctionSummaryDto`** — **thống nhất với TV2 trước khi viết.** Xem phần Chữ ký Contract bên dưới.

**`MyListingsResponse`** — danh sách `AuctionSummaryDto` của seller.

Yêu cầu TV4 thêm `CREATE_AUCTION` vào `MessageType`.

---

### Server: `server/auction/`

**`ItemRepository`**
- `save(Item item, UUID sellerId)` — Item là đối tượng domain thuần túy, không mang sellerId. Repository tự điền cột `seller_id` trong DB từ tham số truyền vào. Dựa vào category để điền các cột phụ (Art/Electronics/Vehicle).
- `findById(UUID id)` — SELECT và tái tạo đúng subtype dựa vào cột category.

**`AuctionRepository`** — **công bố cho TV2 & TV3 vào ngày đầu tiên.** Xem phần Chữ ký Contract bên dưới.

> Bảng `auctions` **không có** cột `seller_id`. Thông tin seller nằm ở bảng `items`. Mọi truy vấn cần seller phải JOIN với bảng `items`.

**`AuctionService`**
- `createAuction(sellerId, dto)` — validate thời gian và giá, dùng `ItemFactory` để tạo Item (không gọi `new Art/Electronics/Vehicle` trực tiếp), lưu item và auction, trả về auction.
- `getMyListings(sellerId)` — lấy danh sách phiên đấu giá theo seller, map sang `AuctionSummaryDto`.

**`AuctionHandler`** — đăng ký 2 route: `CREATE_AUCTION` và `LIST_MY_LISTINGS`.

**`AuctionModule`** — khởi tạo Repository/Service/Handler, **trả về `AuctionRepository`** để TV4 dùng trong `ServerContext`.

---

### Client

**`MyListingsController`** — khi load màn hình, gửi request lấy danh sách phiên đấu giá của người dùng hiện tại và render ra UI.

**`CreateAuctionController`** (mới) + FXML — đọc dữ liệu từ form, gửi `CreateAuctionRequest`, điều hướng về `MY_LISTINGS` nếu thành công.

---

### Sản phẩm bàn giao
- 5 file server trong `server/auction/`
- 4 DTO trong `common/dto/auction/`
- 2 màn hình client hoàn thiện

### Thảo luận với

| Chủ đề | Với | Hạn chót |
|---|---|---|
| Danh sách trường `AuctionSummaryDto` | TV2 | Trước khi viết DTO |
| Chữ ký `AuctionRepository` | TV2 & TV3 | Ngày đầu tiên |
| `AuctionModule.init()` trả về `AuctionRepository` | TV4 | TV4 kết nối `ServerContext` |
| Thêm `CREATE_AUCTION` vào `MessageType` | TV4 | Trước khi viết Handler |

---

## Thành viên 2 — Luồng Đấu giá chính

**Phạm vi:** Người mua duyệt danh sách, xem chi tiết phiên đấu giá, đặt bid, xem lịch sử bid của mình.

---

### DTOs cần tạo (`common/dto/bidding/`)

**`PlaceBidRequest`** — auctionId, amount (double), bidderId (từ `AppContext.getCurrentUser().getUserID()`).

**`PlaceBidResponse`** — bidId, newHighestBid, newHighestBidderId.

**`AuctionDetailDto`** — đầy đủ thông tin phiên: auctionId, itemId, itemName, itemDescription, itemCategory, itemCondition, sellerId, currentHighestBid, highestBidderId, status, startTime, endTime, bidHistory (danh sách `BidSummaryDto`).

**`BidSummaryDto`** — bidId, bidderId, amount, bidType (MANUAL/AUTO), createdAt.

**`BidWithAuctionDto`** — dùng trong màn hình My Bids: auctionId, itemName, yourBid, currentHighestBid, status, endTime, isWinning.

**`MyBidsResponse`** — danh sách `BidWithAuctionDto`.

**`ListAuctionsResponse`** — danh sách `AuctionSummaryDto` (do TV1 sở hữu).

**`GetAuctionDetailRequest`** — auctionId.

Xác nhận với TV4 các `MessageType` sau không bị đổi tên: `PLACE_BID`, `LIST_AUCTIONS`, `GET_AUCTION_DETAIL`, `LIST_MY_BIDS`.

---

### Server: `server/bidding/`

**`BidRepository`**
- `save(BidTransaction)` — INSERT vào bảng bids. **Lưu ý: bảng bids không có cột `updated_at`**, chỉ chèn `created_at`.
- `findByAuctionId(UUID)` — lấy lịch sử bid của một phiên, sắp xếp theo thời gian tăng dần.
- `findByBidderId(UUID)` — JOIN với bảng auctions và items để lấy đủ thông tin cho `BidWithAuctionDto`.
- `findLastBidTime(UUID auctionId)` — trả về `Optional<LocalDateTime>`, dùng cho anti-sniping trong scheduler của TV3.

**`BidService`** — **công bố chữ ký `placeBid()` cho TV3 vào ngày đầu tiên.** Xem phần Chữ ký Contract bên dưới.
- `placeBid(bidderId, auctionId, amount)` — tải Auction từ DB, gọi `auction.placeBid()` (method này tự quản lý lock — **không lock thêm lần nữa**), lưu bid và cập nhật bid cao nhất.
- `getAuctionDetail(auctionId)` — tải auction, item, lịch sử bid, lắp ráp `AuctionDetailDto`.
- `listAllAuctions()` — lấy tất cả phiên, map sang `AuctionSummaryDto`.
- `getMyBids(bidderId)` — lấy lịch sử bid của người dùng.

> `BidService` cần cả `AuctionRepository` (TV1) lẫn `ItemRepository` (TV1). `BidModule.init()` nhận cả hai từ `ServerContext`.

**`BidHandler`** — đăng ký 4 route: `PLACE_BID`, `LIST_AUCTIONS`, `GET_AUCTION_DETAIL`, `LIST_MY_BIDS`. Sau khi `placeBid` thành công, gọi `notificationService.broadcastBidUpdate(...)` để push thông báo real-time.

**`BidModule`** — nhận `AuctionRepository`, `ItemRepository`, `NotificationService` từ `ServerContext` qua tham số `init()`.

---

### Client

**`AuctionBrowseController`** — load danh sách phiên khi khởi tạo. Khi click vào một phiên, lưu auctionId vào `AppContext.setSelectedAuctionId()` rồi điều hướng sang màn hình chi tiết.

**`AuctionDetailController`** — load chi tiết phiên theo `AppContext.getSelectedAuctionId()`. Xử lý nút "Place Bid". Dự phòng chỗ cho push handler real-time (TV4 kết nối sau).

**`MyBidsController`** — load và hiển thị lịch sử bid của người dùng hiện tại.

---

### Sản phẩm bàn giao
- 4 file server trong `server/bidding/`
- 8 DTO trong `common/dto/bidding/`
- 3 màn hình client hoàn thiện

### Thảo luận với

| Chủ đề | Với | Hạn chót |
|---|---|---|
| Danh sách trường `AuctionSummaryDto` | TV1 | Trước khi viết DTO |
| Chữ ký `AuctionRepository` & `ItemRepository` | TV1 | Ngày đầu tiên |
| Tham số `BidModule.init()` | TV4 | TV4 kết nối `ServerContext` |
| Chữ ký `NotificationService.broadcastBidUpdate()` | TV4 | Trước khi viết `BidHandler` |
| `AutoBidService.triggerAutoBids()` gọi ở đâu? | TV3 | Thống nhất: gọi từ `BidHandler` sau khi bid thủ công thành công, **không** gọi từ bên trong `BidService` |
| Các trường `PUSH_BID_UPDATE` DTO | TV4 | Trước khi viết `BidHandler` |

---

## Thành viên 3 — Tự động hóa & Thanh toán

**Phạm vi:** Vòng đời phiên đấu giá theo thời gian (scheduler), auto-bid bot, thanh toán, và dashboard admin.

---

### DTOs cần tạo

`common/dto/admin/`: `AdminUserListResponse` (chứa `List<UserSummaryDto>`), `UserSummaryDto` (id, username, email, role), `AdminDeleteUserRequest` (targetUserId, callerId), `AdminAuctionListResponse` (chứa `List<AuctionSummaryDto>`).

`common/dto/autobid/`: `AutoBidConfigRequest` (auctionId, bidderId, maxAmount, increment), `AutoBidConfigResponse` (status).

`common/dto/payment/`: `ProcessPaymentRequest` (auctionId, bidderId), `ProcessPaymentResponse` (status).

Yêu cầu TV4 thêm vào `MessageType`: `ADMIN_LIST_USERS`, `ADMIN_DELETE_USER`, `ADMIN_LIST_AUCTIONS`, `PROCESS_PAYMENT`. Xác nhận `AUTO_BID_CONFIG` không đổi tên.

---

### Server

**Module `server/admin/`**

`AdminService` — nhận `UserRepository` (từ `AuthModule`, TV4 truyền vào) và `AuctionRepository` (từ TV1, TV4 truyền vào). Cung cấp: `getAllUsers()`, `deleteUser(targetId, callerId)` (kiểm tra quyền ADMIN), `getAllAuctions()`.

`AdminHandler` — đăng ký 3 route: `ADMIN_LIST_USERS`, `ADMIN_DELETE_USER`, `ADMIN_LIST_AUCTIONS`.

`AdminModule` — nhận `UserRepository` và `AuctionRepository` qua tham số `init()`.

> `AuthModule.init()` đã trả về `UserRepository` — TV4 đã có sẵn, chỉ cần truyền vào.

---

**Module `server/automation/`**

`AutoBidRepository` — lưu và đọc cấu hình auto-bid theo phiên. Dùng `INSERT OR REPLACE` (SQLite upsert) theo PK phức hợp (auction_id, bidder_id).

`AutoBidConfig` — value class đơn giản: auctionId, bidderId, maxAmount, increment.

`AutoBidService` — **công bố chữ ký `triggerAutoBids()` cho TV2 vào ngày đầu tiên.**
- `saveConfig(dto)` — lưu cấu hình auto-bid.
- `triggerAutoBids(auctionId, newHighestBid, currentHighestBidderId)` — tìm auto-bidder đủ điều kiện (chưa đang dẫn đầu, maxAmount còn đủ), tính bid tiếp theo và gọi `BidService.placeBid()`. **Thêm bộ chặn đệ quy** để tránh vòng lặp vô hạn.

`AutoBidModule` — đăng ký route `AUTO_BID_CONFIG`, nhận `BidService` từ `ServerContext`.

---

`AuctionScheduler` — **không theo mô hình Handler/Service/Repository.** Chạy nền mỗi giây bằng `ScheduledExecutorService`.

Mỗi "tick":
1. Lấy toàn bộ phiên đang RUNNING.
2. Nếu phiên đã hết giờ: kiểm tra anti-sniping (truy vấn `BidRepository.findLastBidTime()` của TV2 — nếu bid gần nhất nằm trong X giây cuối thì gia hạn, ngược lại chuyển sang FINISHED và broadcast kết thúc).
3. Với phiên OPEN có startTime đã qua: chuyển sang RUNNING.

---

**Module `server/payment/`**

`PaymentService` — nhận `AuctionRepository` và `UserRepository`. Xử lý thanh toán: kiểm tra phiên FINISHED, xác nhận người gọi là người thắng, cập nhật trạng thái sang PAID.

`PaymentHandler` — đăng ký route `PROCESS_PAYMENT`.

`PaymentModule` — nhận `AuctionRepository` và `UserRepository` qua tham số `init()`.

---

### Client

**`AdminOverviewController`** — khi load, gửi 2 request song song: lấy danh sách user và danh sách phiên đấu giá để hiển thị trong bảng.

**`PaymentController`** — nút "Pay Now" gửi request thanh toán với auctionId hiện tại và userId hiện tại.

**Popup Auto-bid** (FXML mới nhỏ) — form nhập maxAmount và increment, gửi `AutoBidConfigRequest`.

---

### Sản phẩm bàn giao
- 9 file server trong `server/admin/`, `server/automation/`, `server/payment/`
- Tất cả DTO trong 3 thư mục DTO
- 3 màn hình/popup client hoàn thiện

### Thảo luận với

| Chủ đề | Với | Hạn chót |
|---|---|---|
| Chữ ký `AuctionRepository` (updateStatus, updateEndTime) | TV1 | Ngày đầu tiên |
| `triggerAutoBids()` gọi từ `BidHandler`, không phải `BidService` | TV2 | Trước khi TV2 viết `BidHandler` |
| `BidRepository.findLastBidTime()` cho anti-sniping | TV2 | Trước khi viết `tick()` |
| Chữ ký `NotificationService.broadcastAuctionEnded()` | TV4 | Trước khi viết `tick()` |
| `BidService` truyền vào `AutoBidModule` | TV4 | TV4 kết nối `ServerContext` |

---

## Thành viên 4 — Điều phối viên

**Phạm vi:** Hệ thống push thông báo real-time, quản lý file dùng chung, kết nối `ServerContext`, hỗ trợ các thành viên ngay từ ngày đầu.

---

### Sản phẩm bàn giao **Ngày 1** (phải xong trước khi người khác bắt đầu)

1. **`AppContext`** — thêm trường `selectedAuctionId` với getter/setter tương ứng.

2. **`MessageType`** — thêm tất cả mục mới từ TV1, TV2, TV3 trong một lần commit: `CREATE_AUCTION`, `ADMIN_LIST_USERS`, `ADMIN_DELETE_USER`, `ADMIN_LIST_AUCTIONS`, `PROCESS_PAYMENT`, `PUSH_BID_UPDATE`, `PUSH_AUCTION_ENDED`.

3. **`NotificationService` stub** (thân phương thức rỗng) — để TV2 & TV3 có thể compile: `broadcastBidUpdate(auctionId, newBid, newHighestBidderId)` và `broadcastAuctionEnded(auctionId, winnerId)`.

4. **`registerPushHandler` stub trên `ServerConnection`** — để TV2 có thể compile code real-time phía client.

---

### Server: Mở rộng hạ tầng

**`ClientRegistry`** — quản lý map từ clientId/userId sang `ClientHandler`. Cần các phương thức: đăng ký, hủy đăng ký, broadcast tới tất cả, gửi tới một user cụ thể.

**Mở rộng `ClientHandler`** — nâng `PrintWriter out` từ biến cục bộ thành field của class. Thêm phương thức `push(String json)` để `ClientRegistry` có thể gửi tin. Lưu `clientId` (UUID) và nhận `ClientRegistry` qua constructor.

**Mở rộng `Server`** — trong vòng lặp accept, đăng ký mỗi `ClientHandler` mới vào `ClientRegistry`.

**`NotificationService`** (hoàn thiện) — serialize sự kiện thành JSON và broadcast tới tất cả client qua `ClientRegistry`.

> **Lưu ý giao thức push:** `ResponseMessage` hiện không có trường nào để phân biệt tin nhắn push. Cần thêm `String pushType` vào `ResponseMessage` (hoặc tạo wrapper riêng), để client có thể nhận biết và điều hướng tới đúng handler. **Thống nhất với cả nhóm trước khi ai bắt đầu viết code liên quan đến push.**

**Mở rộng `ServerConnection`** (phía client) — thêm `Map<String, Consumer<String>> pushHandlers`. Trong vòng đọc response, nếu nhận được tin nhắn không có `requestId` thì điều hướng tới push handler tương ứng theo `pushType`.

---

### `ServerContext` — kết nối toàn bộ hệ thống

Thứ tự khởi tạo:
1. `ClientRegistry` và `NotificationService`
2. `AuthModule.init()` → lấy `UserRepository` ✅ (đã xong)
3. `AuctionModule.init()` → lấy `AuctionRepository` (TV1 cần trả về)
4. `BidModule.init()` với `AuctionRepository`, `ItemRepository`, `NotificationService`
5. `AutoBidModule.init()` với `BidService`
6. `AdminModule.init()` với `UserRepository`, `AuctionRepository`
7. `PaymentModule.init()` với `AuctionRepository`, `UserRepository`
8. Khởi động `AuctionScheduler`

> Có rủi ro phụ thuộc vòng giữa `BidService` và `AutoBidService`. Giải pháp: `BidHandler` nhận cả hai service và gọi `triggerAutoBids()` sau khi bid thủ công thành công — `BidService` không biết về `AutoBidService`.

---

### Client

**`AuctionDetailController`** — đăng ký push handler để cập nhật giao diện khi nhận `PUSH_BID_UPDATE`.

---

### Sản phẩm bàn giao
- `ClientRegistry.java`, `NotificationService.java` (hoàn thiện)
- Bản mở rộng `ClientHandler.java` và `ServerConnection.java`
- `ServerContext.java` hoàn chỉnh
- `MessageType.java` và `AppView.java` đã cập nhật
- `AppContext.java` với `selectedAuctionId`

### Thảo luận với

| Chủ đề | Với | Hạn chót |
|---|---|---|
| Giao thức push (`pushType` trong `ResponseMessage`) | **Cả nhóm** | Ngày 1 — ảnh hưởng tất cả |
| Kiểu trả về `AuctionModule.init()` | TV1 | Trước khi viết `ServerContext` |
| Tham số `BidModule.init()` | TV2 | Trước khi viết `ServerContext` |
| Phụ thuộc vòng `BidService` ↔ `AutoBidService` | TV2 & TV3 | Ngày 1 |

---

## Bản đồ rủi ro xung đột

| Rủi ro | Giải pháp |
|---|---|
| Nhiều người chỉnh sửa `MessageType.java` | TV4 sở hữu — nhắn cho TV4 mục cần thêm |
| Nhiều người chỉnh sửa `ServerContext.java` | TV4 sở hữu — gửi chữ ký module, TV4 kết nối |
| `AuctionRepository` khởi tạo hai lần | Khởi tạo một lần trong `ServerContext`, truyền tham số cho mọi module |
| `BidHandler` cần `NotificationService` trước khi TV4 xong | TV4 giao stub vào ngày 1 |
| `AutoBidService` ↔ `BidService` phụ thuộc vòng | Inject `AutoBidService` vào `BidHandler`, không vào `BidService` |
| ~~`UserRepository` bị nhốt trong `AuthModule`~~ | ✅ Đã giải quyết |

---

## Chữ ký Contract — Các thành viên PHẢI thống nhất trước khi code

Đây là các chữ ký phương thức mà một thành viên **công bố** và thành viên khác **phụ thuộc vào**. Không được thay đổi sau khi đã thống nhất.

### TV1 công bố → TV2 & TV3 phụ thuộc

```java
// ── AuctionRepository ──────────────────────────────────────────────────────

void save(Auction auction)
// Lưu một phiên đấu giá mới vào DB (chỉ dùng khi tạo mới, không phải update).
// Các cột cần INSERT: id, item_id, start_time, end_time, status,
//   current_highest_bid (mặc định 0.0), highest_bidder_id (mặc định null), created_at, updated_at.
// KHÔNG có cột seller_id trên bảng auctions — không cần INSERT seller_id ở đây.
// Thông tin seller nằm ở bảng items. Mọi truy vấn cần seller phải JOIN.

Optional<Auction> findById(UUID id)
// Tải một phiên đấu giá từ DB theo ID.
// Phải JOIN với bảng items (ON auctions.item_id = items.id) để lấy seller_id.
// Tái tạo đối tượng Auction qua constructor DB (10 tham số) — không dùng constructor tạo mới (4 tham số).
// Trả về Optional.empty() nếu không tìm thấy.

List<Auction> findAll()
// Tải toàn bộ phiên đấu giá trong DB.
// Phải JOIN với items để lấy seller_id cho từng phiên.
// Được dùng bởi AuctionScheduler (TV3) mỗi giây để kiểm tra trạng thái,
// và bởi BidService (TV2) để cung cấp danh sách cho màn hình Browse.

List<Auction> findBySellerId(UUID sellerId)
// Lấy tất cả phiên đấu giá của một seller cụ thể.
// Vì seller_id nằm ở bảng items, phải JOIN:
//   SELECT a.* FROM auctions a JOIN items i ON a.item_id = i.id WHERE i.seller_id = ?
// Dùng cho màn hình "Danh sách của tôi" (TV1).

void updateStatus(UUID auctionId, AuctionStatus status)
// Cập nhật trạng thái phiên (ví dụ: OPEN → RUNNING → FINISHED → PAID / CANCELED).
// Chỉ UPDATE cột status và updated_at — không đụng đến các cột khác.
// Tách riêng khỏi updateHighestBid để tránh vô tình ghi đè dữ liệu bid khi chỉ đổi trạng thái.

void updateHighestBid(UUID auctionId, BigDecimal amount, UUID bidderId)
// Cập nhật bid cao nhất sau khi có bid mới hợp lệ được chấp nhận.
// UPDATE cột current_highest_bid, highest_bidder_id, và updated_at.
// Tách riêng khỏi updateStatus để không vô tình ghi đè trạng thái khi chỉ cập nhật bid.

void updateEndTime(UUID auctionId, LocalDateTime newEndTime)
// Gia hạn thời điểm kết thúc của phiên — dùng cho cơ chế anti-sniping.
// Khi người dùng đặt bid trong vài giây cuối, AuctionScheduler gọi phương thức này
// để kéo dài thêm thời gian, tránh việc snipe (đặt bid có chủ đích ngay trước khi hết giờ).
// Sau khi UPDATE DB, cần gọi thêm auction.extendEndTime(newEndTime) để cập nhật đối tượng in-memory.
```

```java
// ── ItemRepository ─────────────────────────────────────────────────────────

void save(Item item, UUID sellerId)
// Lưu một món hàng mới vào DB.
// Item là pure domain object — không mang sellerId để giữ entity sạch.
// sellerId được truyền riêng, Repository tự điền vào cột items.seller_id.
// Dựa vào item.getCategory() để điền thêm cột phụ tương ứng:
//   ART         → artist, era
//   ELECTRONICS → brand, warranty_months
//   VEHICLE     → brand, production_year, fuel_type

Optional<Item> findById(UUID id)
// Tải món hàng từ DB theo ID.
// Đọc cột category trước, sau đó dựa vào giá trị đó để tái tạo đúng subtype
// (Art, Electronics, hoặc Vehicle) với đầy đủ các trường phụ tương ứng.
```

### TV2 công bố → TV3 phụ thuộc

```java
// ── BidService ─────────────────────────────────────────────────────────────

BidTransaction placeBid(UUID bidderId, UUID auctionId, BigDecimal amount)
    throws InvalidBidException, AuctionClosedException
// Điểm vào duy nhất để đặt bid — dùng cho cả bid thủ công lẫn auto-bid.
// Logic: tải Auction từ DB → gọi auction.placeBid() để validate và ghi vào lịch sử bid
// (method này tự quản lý lock bên trong — KHÔNG được lock thêm ở đây) →
// lưu BidTransaction vào bảng bids → cập nhật current_highest_bid trên bảng auctions.
// Ném InvalidBidException nếu bid không hợp lệ (bid thấp hơn hiện tại, tự bid phiên mình...).
// Ném AuctionClosedException nếu phiên không ở trạng thái RUNNING.
// Nếu cần phân biệt bid thủ công và auto-bid, overload thêm tham số BidType.

// ── BidRepository ──────────────────────────────────────────────────────────

Optional<LocalDateTime> findLastBidTime(UUID auctionId)
// Trả về thời điểm của bid gần nhất trong một phiên đấu giá.
// Câu SQL: SELECT MAX(created_at) FROM bids WHERE auction_id = ?
// Dùng bởi AuctionScheduler (TV3) trong cơ chế anti-sniping:
// nếu bid cuối nằm trong X giây trước khi hết giờ, scheduler gia hạn phiên thay vì kết thúc.
// Trả về Optional.empty() nếu phiên chưa có bid nào.
```

### TV4 công bố → TV2 & TV3 phụ thuộc

```java
// ── NotificationService ────────────────────────────────────────────────────

void broadcastBidUpdate(UUID auctionId, BigDecimal newBid, UUID newHighestBidderId)
// Gửi push notification tới tất cả client đang mở phiên đấu giá này.
// Được gọi bởi BidHandler ngay sau khi placeBid() thành công.
// Nội dung gửi đi là BidUpdateEvent (auctionId, newHighestBid, newHighestBidderId, timestamp).
// Client nhận được sẽ cập nhật giá hiển thị mà không cần reload trang.

void broadcastAuctionEnded(UUID auctionId, UUID winnerId)
// Gửi push notification báo hiệu phiên đã kết thúc.
// Được gọi bởi AuctionScheduler khi chuyển trạng thái sang FINISHED.
// winnerId là người thắng cuộc (highest bidder) — truyền null nếu không có bid nào.

// ── ServerConnection (phía client) ────────────────────────────────────────

void registerPushHandler(MessageType type, Consumer<String> handler)
// Đăng ký một hàm xử lý cho một loại push message từ server.
// Khi server gửi xuống tin nhắn push (không có requestId), client tự động tìm
// và gọi handler tương ứng theo pushType trong tin nhắn.
// Dùng Consumer<String> thay vì typed callback để lớp ServerConnection
// không phụ thuộc vào bất kỳ DTO push cụ thể nào — controller tự parse JSON.
```

### TV1 & TV2 cùng thống nhất — DTO dùng chung

```java
// AuctionSummaryDto — dùng bởi TV1 (MyListings), TV2 (Browse), TV3 (Admin)
// Chứa đủ thông tin để hiển thị trong một card hoặc một dòng trong danh sách.
// Không chứa itemDescription hay bidHistory — những trường đó thuộc về AuctionDetailDto.
// Tất cả ID đều là String (UUID.toString()) để tránh lỗi deserialize với Jackson.

String auctionId
String itemId
String itemName
String itemCategory     // ItemCategory.name() — "ELECTRONICS" | "ART" | "VEHICLE"
String status           // AuctionStatus.name() — "OPEN" | "RUNNING" | "FINISHED" | "PAID" | "CANCELED"
double currentHighestBid  // 0.0 nếu chưa có bid nào
String highestBidderId  // null nếu chưa có bid nào
String endTime          // LocalDateTime.toString()
String sellerId         // cần để MyListingsController xác định phiên nào thuộc về mình
```

### TV4 định nghĩa — Push event DTOs

```java
// common/dto/notification/BidUpdateEvent
// Server gửi xuống sau mỗi bid thành công. Client dùng để cập nhật giá real-time.
String auctionId            // client dùng để biết cập nhật phiên nào (chỉ update nếu đang xem đúng phiên)
String newHighestBidderId
double newHighestBid
String timestamp            // thời điểm bid xảy ra, LocalDateTime.now().toString()

// common/dto/notification/AuctionEndedEvent
// Server gửi xuống khi scheduler kết thúc phiên. Client dùng để hiển thị thông báo kết quả.
String auctionId
String winnerId             // null nếu không có bid nào trong suốt phiên
double finalPrice           // 0.0 nếu không có bid nào
```

---

## Bảng Tổng kết

| Thành viên      | Server                                                            | Client                      | Thử thách chính                                                   |
|-----------------|-------------------------------------------------------------------|-----------------------------|-------------------------------------------------------------------|
| 1 (Người bán)   | `server/auction/` — 5 file                                        | Tạo đấu giá, Danh sách của tôi | ItemFactory; JOIN với items cho seller_id                         |
| 2 (Người bid)   | `server/bidding/` — 4 file                                        | Duyệt, Chi tiết, Bid của tôi   | `Auction.placeBid()` đã có lock — không lock thêm                  |
| 3 (Hệ thống)    | `server/automation/`, `server/admin/`, `server/payment/` — 9 file | Admin, Thanh toán, Popup auto-bid | Phụ thuộc vòng với BidService; chặn đệ quy trong triggerAutoBids |
| 4 (Điều phối)   | Mở rộng `server/infrastructure/` — 3 file                          | Push handler trong AuctionDetail | Giao thức push; kết nối ServerContext                             |
