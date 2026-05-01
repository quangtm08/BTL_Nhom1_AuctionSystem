# Giải Thích Hoạt Động Của 5 File Auction

Tài liệu này mô tả toàn bộ cách 5 file sau phối hợp với nhau:

- `AuctionModule.java`
- `AuctionHandler.java`
- `AuctionService.java`
- `AuctionRepository.java`
- `ItemRepository.java`

## 1) Bức tranh tổng quan

Module auction đi theo mô hình nhiều tầng:

1. `AuctionModule`: khởi tạo dependency và đăng ký handler vào `MessageRouter`.
2. `AuctionHandler`: nhận message theo `MessageType`, parse JSON, gọi service, trả `ResponseMessage`.
3. `AuctionService`: xử lý nghiệp vụ (validate, tạo item, tạo auction, list auction, delete có transaction).
4. `AuctionRepository` và `ItemRepository`: thao tác DB bằng JDBC.

Luồng chung: `MessageRouter -> AuctionHandler -> AuctionService -> Repository -> DB`.

## 2) Vai trò từng file

## `AuctionModule.java`

- Là entry point để bật tính năng auction.
- Trong `init(Connection connection, MessageRouter router)`:
  - Tạo `ItemRepository`.
  - Tạo `AuctionRepository`.
  - Tạo `AuctionService` (inject 2 repository + connection).
  - Tạo `AuctionHandler` và gọi `register(router)`.
- In log khởi tạo thành công và trả về `AuctionRepository`.

Ý nghĩa: module wiring tập trung ở một chỗ, giúp code startup rõ ràng.

## `AuctionHandler.java`

### Nhiệm vụ

- Đăng ký 3 message type:
  - `CREATE_AUCTION`
  - `LIST_MY_LISTINGS`
  - `DELETE_AUCTION`
- Parse payload JSON và map sang DTO/field cần thiết.
- Gọi method tương ứng trong `AuctionService`.
- Chuẩn hóa response lỗi (`INVALID_FORMAT`, `*_FAILED`).

### Chi tiết các handler

1. `CREATE_AUCTION`
   - Parse `CreateAuctionRequest`.
   - Gọi `handleCreateAuction`.
   - Nếu parse JSON lỗi -> trả `INVALID_FORMAT`.

2. `LIST_MY_LISTINGS`
   - Parse JSON dạng `JsonNode`.
   - Lấy `sellerId`.
   - Gọi `handleListMyListings`.

3. `DELETE_AUCTION`
   - Parse JSON dạng `JsonNode`.
   - Lấy `sellerId`, `auctionId`.
   - Gọi `handleDeleteAuction`.

### Mapping response

- Create: map `Auction` -> `CreateAuctionResponse`.
- List: bọc list trong `MyListingsResponse`.
- Delete: trả message `"Deleted"` khi thành công.

## `AuctionService.java`

Đây là tầng nghiệp vụ chính.

### 3.1 `createAuction(String sellerId, CreateAuctionRequest dto)`

Trình tự:

1. `validateCreateAuctionRequest(...)`
   - check sellerId hợp lệ UUID
   - check DTO không null
   - `startingPrice > 0`
   - `startTime`, `endTime` không null
   - `endTime` phải sau `startTime`
   - nếu `dto.sellerId` có giá trị thì phải trùng với param `sellerId`
2. Parse sellerId sang UUID.
3. Tạo `Item` bằng `createItem(dto)`:
   - `ART` -> `ItemFactory.createArt(...)`
   - `ELECTRONICS` -> bắt buộc `warrantyMonths`, rồi `createElectronics(...)`
   - `VEHICLE` -> bắt buộc `productionYear`, rồi `createVehicle(...)`
4. Lưu item qua `itemRepository.save(item, parsedSellerId)`.
5. Tạo object `Auction(itemId, sellerId, startTime, endTime)`.
6. Lưu auction qua `auctionRepository.save(auction)`.
7. Gọi `auctionRepository.updateHighestBid(auctionId, startingPrice, null)` để set giá khởi điểm vào trạng thái highest bid ban đầu.
8. Trả về `Auction`.

Ý nghĩa business quan trọng:

- Giá khởi điểm được đưa luôn vào `current_highest_bid`, phục vụ hiển thị danh sách và validate phiên bid đầu tiên.

### 3.2 `getMyListings(String sellerId)`

Trình tự:

1. Validate + parse sellerId.
2. Lấy danh sách auction theo seller: `auctionRepository.findBySellerId(...)`.
3. Với mỗi auction:
   - tìm item bằng `itemRepository.findById(itemId)`.
   - nếu không có item -> ném `IllegalStateException`.
   - map sang `AuctionSummaryDto`.

Lưu ý:

- `startingPrice` trong DTO đang lấy từ `auction.getCurrentHighestBid()`.
- Do `createAuction` đã set highest bid = starting price, giá trị này thể hiện được opening price ban đầu.

### 3.3 `deleteAuction(String sellerId, String auctionId)`

Trình tự:

1. Validate sellerId.
2. Validate auctionId (không blank, phải là UUID hợp lệ).
3. Tìm auction theo id.
4. Check quyền: seller đang gọi phải là chủ auction.
5. Thực thi transaction thủ công bằng cùng `connection`:
   - `setAutoCommit(false)`
   - delete auction trước (`auctionRepository.deleteById`)
   - delete item sau (`itemRepository.deleteById`)
   - nếu 1 trong 2 câu lệnh không xóa được record nào -> throw
   - `commit`
   - lỗi thì `rollback`
   - khôi phục lại `autoCommit` ban đầu trong `finally`
6. Bọc lỗi transaction thành `RuntimeException("Delete transaction failed", ex)`.

Ý nghĩa kỹ thuật:

- Đảm bảo tính nguyên tử: không xảy ra trạng thái xóa auction nhưng còn item, hoặc ngược lại.

## `AuctionRepository.java`

Repository JDBC cho bảng `auctions`, đồng thời join `items` khi cần `seller_id`.

## 4.1 Lưu dữ liệu

- `save(Auction auction)` insert:
  - id, item_id, start_time, end_time, status, current_highest_bid, highest_bidder_id, created_at, updated_at
- `current_highest_bid` null thì ghi `0`.
- `created_at`, `updated_at` set bằng `LocalDateTime.now()` và format `"yyyy-MM-dd HH:mm:ss"`.

## 4.2 Truy vấn

- `findById(UUID id)`:
  - join `auctions a` + `items i`
  - lấy `i.seller_id` để map vào entity `Auction`.
- `findAll()`: join và map toàn bộ.
- `findBySellerId(UUID sellerId)`: filter theo `i.seller_id`.
- `findByItemId(UUID itemId)`: tìm auction theo item.

## 4.3 Cập nhật

- `updateStatus(UUID auctionId, AuctionStatus status)`
- `updateHighestBid(UUID auctionId, BigDecimal amount, UUID bidderId)`
- `updateEndTime(UUID auctionId, LocalDateTime newEndTime)`

Các update đều cập nhật `updated_at = now`.

## 4.4 Xóa

- `deleteById(UUID auctionId)` trả về số record đã xóa.

## 4.5 Mapping / date-time

- `map(ResultSet rs)` map đầy đủ trường DB -> `Auction`.
- Hỗ trợ parse 2 format datetime:
  - ISO có `T` (`LocalDateTime.parse(value)`)
  - SQLite text format `"yyyy-MM-dd HH:mm:ss"`.

Điểm này giúp chịu lỗi format không đồng nhất từ DB.

## `ItemRepository.java`

Repository JDBC cho bảng `items`, có hỗ trợ polymorphic entity.

## 5.1 Save item theo subtype

- `save(Item item, UUID sellerId)` insert vào bảng `items`.
- Các cột chung: id, seller_id, name, description, category, condition, created_at, updated_at.
- Các cột đặc thù:
  - `ELECTRONICS`: `brand`, `warranty_months`
  - `ART`: `artist`, `era`
  - `VEHICLE`: `brand`, `production_year`, `fuel_type`
- Các cột không thuộc subtype hiện tại sẽ set `NULL`.

## 5.2 Find item và dựng lại subtype

- `findById(UUID id)` query theo id.
- `mapResultSetToItem(ResultSet rs)`:
  - đọc `category`
  - `switch` để tạo đúng class:
    - `Electronics`
    - `Art`
    - `Vehicle`

## 5.3 Delete

- `deleteById(UUID itemId)` trả số record đã xóa.

## 5.4 Date-time parse

- Tương tự AuctionRepository: hỗ trợ parse format ISO có `T` hoặc format SQLite `"yyyy-MM-dd HH:mm:ss"`.

## 6) Luồng end-to-end theo từng chức năng

## A. Tạo auction (`CREATE_AUCTION`)

1. Client gửi message + JSON payload.
2. `AuctionHandler` parse payload thành `CreateAuctionRequest`.
3. `AuctionService.createAuction(...)` validate business.
4. Tạo `Item` đúng subtype bằng factory.
5. `ItemRepository.save(...)`.
6. Tạo + lưu `Auction`.
7. Cập nhật `current_highest_bid = startingPrice`.
8. Trả `CreateAuctionResponse`.

## B. Xem listing của seller (`LIST_MY_LISTINGS`)

1. Handler parse `sellerId`.
2. Service gọi `AuctionRepository.findBySellerId`.
3. Với mỗi auction, gọi `ItemRepository.findById` để lấy tên item.
4. Map sang `AuctionSummaryDto`.
5. Trả `MyListingsResponse`.

## C. Xóa auction (`DELETE_AUCTION`)

1. Handler parse `sellerId`, `auctionId`.
2. Service validate input + quyền sở hữu.
3. Bật transaction trên connection.
4. Xóa auction rồi xóa item.
5. Commit hoặc rollback nếu lỗi.
6. Trả response thành công/thất bại.

## 7) Các điểm đáng chú ý kỹ thuật

1. Có kiểm tra quyền xóa auction theo seller sở hữu.
2. Có transaction thủ công cho delete để đảm bảo toàn vẹn dữ liệu.
3. Dùng repository tách biệt giúp code business không chứa SQL trực tiếp.
4. Có hỗ trợ polymorphism cho `Item` (Art/Electronics/Vehicle) ở tầng repository.
5. Có xử lý parse datetime linh hoạt do định dạng lưu có thể không đồng nhất.
6. Error handling ở handler chuẩn hóa mã lỗi cho protocol layer.

## 8) Kết luận

5 file này tạo thành một feature auction hoàn chỉnh theo hướng module hóa rõ ràng:

- `Module` lo wiring.
- `Handler` lo protocol/JSON.
- `Service` lo business rule + transaction orchestration.
- `Repository` lo persistence và mapping entity.

Nhờ vậy luồng tạo/xem/xóa auction tách tầng rõ, dễ mở rộng và dễ kiểm soát lỗi.
