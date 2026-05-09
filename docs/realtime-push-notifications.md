# Thay đổi tính năng Push Notification thời gian thực

## Tổng quan

Trong phiên làm việc này, hai tính năng được bổ sung để UI phía client tự động cập nhật theo thời gian thực mà không cần tải lại trang:

1. **Cập nhật giá đấu thầu trực tiếp** trên màn hình Browse (Khám phá) và MyListings mỗi khi có client đặt giá thầu mới.
2. **Tự động xóa card phiên đấu giá** trên màn hình Browse khi người bán xóa một phiên đấu giá của họ.

Trước các thay đổi này, server đã phát `PUSH_BID_UPDATE` nhưng các màn hình client chưa đăng ký lắng nghe. `PUSH_AUCTION_DELETED` là loại push hoàn toàn mới, được thêm từ server đến client.

---

## Tính năng 1: Cập nhật giá thầu thời gian thực

### Vấn đề
Khi một client đặt giá thầu, server đã gửi `PUSH_BID_UPDATE` đến tất cả các client đang kết nối. `AuctionDetailController` đã xử lý push này. Tuy nhiên, `AuctionBrowseController` và `MyListingsController` bỏ qua nó, khiến nhãn "Current bid" hiển thị giá cũ cho đến khi người dùng điều hướng đi và quay lại.

### Cách hoạt động
Mỗi card trong lưới giữ một nhãn (`Label`) hiển thị giá hiện tại. Khi màn hình render các card, nó lưu mỗi nhãn vào `Map<String, Label> priceLabels` với key là `auctionId`. Khi `PUSH_BID_UPDATE` đến, controller tra cứu nhãn tương ứng trong map và chỉ cập nhật nhãn đó — không reload toàn bộ, không bị nháy màn hình.

### Các file thay đổi

#### `AuctionBrowseController.java`
- Thêm import: `ArrayList`, `HashMap`, `Map`, `JsonNode`, `ObjectMapper`
- Thêm các field:
  ```java
  private final ObjectMapper mapper = new ObjectMapper();
  private final Map<String, Label> priceLabels = new HashMap<>();
  ```
- Trong `renderAuctionCards()`: thêm `priceLabels.clear()` trước khi xóa lưới.
- Trong `createAuctionCard()`: lưu nhãn giá vào map:
  ```java
  if (dto.getId() != null) priceLabels.put(dto.getId(), priceValue);
  ```
- Trong `initialize()`: đăng ký push handler:
  ```java
  ServerConnection.getInstance().registerPushHandler(
      MessageType.PUSH_BID_UPDATE,
      json -> handleBidUpdatePush(json)
  );
  ```
- Thêm phương thức mới `handleBidUpdatePush(String json)`:
  ```java
  private void handleBidUpdatePush(String json) {
      JsonNode root = mapper.readTree(json);
      JsonNode node = root.has("payload") ? root.get("payload") : root;
      String auctionId = node.get("auctionId").asText();
      BigDecimal newBid = new BigDecimal(node.get("newHighestBid").asText());
      Platform.runLater(() -> {
          Label label = priceLabels.get(auctionId);
          if (label != null) label.setText(formatMoney(newBid));
      });
  }
  ```

#### `MyListingsController.java`
Các thay đổi giống hệt `AuctionBrowseController`, áp dụng cho lưới danh sách phiên đấu giá của người bán:
- Thêm import: `HashMap`, `JsonNode`, `ObjectMapper` (`Map` đã có sẵn)
- Thêm field: `mapper` và `priceLabels`
- Trong `renderListings()`: thêm `priceLabels.clear()` trước khi xóa lưới.
- Trong `createListingCard()`: lưu nhãn giá vào map theo `dto.getId()`.
- Trong `initialize()`: đăng ký handler `PUSH_BID_UPDATE` sau `loadMyListings()`.
- Thêm phương thức `handleBidUpdatePush(String json)` (logic giống hệt trên).

---

## Tính năng 2: Tự động xóa card phiên đấu giá trên màn hình Browse

### Vấn đề
Khi người bán xóa một phiên đấu giá (chỉ được phép nếu chưa có ai đặt giá — đã được server kiểm tra), các client khác đang ở màn hình Browse vẫn thấy card đó. Trước đây không có push notification cho sự kiện xóa, và không có handler phía client để xóa card.

### Cách hoạt động
Sau khi xóa thành công, server phát `PUSH_AUCTION_DELETED` kèm `auctionId` đã bị xóa. Màn hình Browse lưu một bản sao local của danh sách phiên đấu giá đang hiển thị (`currentAuctions`). Khi nhận được push, nó xóa mục tương ứng khỏi danh sách và render lại lưới ngay trên client — không cần gọi lại server.

### Các file thay đổi

#### `MessageType.java`
Thêm một giá trị enum mới trong phần push notifications:
```java
PUSH_AUCTION_DELETED,
```

#### `NotificationService.java`
Thêm phương thức broadcast mới:
```java
public void broadcastAuctionDeleted(String auctionId) {
    java.util.Map<String, Object> payload = java.util.Map.of("auctionId", auctionId);
    sendPush(MessageType.PUSH_AUCTION_DELETED, payload);
}
```
Dữ liệu gửi đến client: `{ "auctionId": "<uuid>" }`

#### `AuctionHandler.java`
Trong `handleDeleteAuction()`, gọi `broadcastAuctionDeleted()` sau khi service xóa thành công:
```java
auctionService.deleteAuction(sellerId, auctionId);
notificationService.broadcastAuctionDeleted(auctionId);   // <- thêm mới
return new ResponseMessage<>(requestId, "Deleted");
```

#### `AuctionBrowseController.java`
- Thêm field lưu danh sách phiên đấu giá đang hiển thị:
  ```java
  private List<AuctionSummaryDto> currentAuctions = new ArrayList<>();
  ```
- Trong `handleFilteredAuctions()`: lưu danh sách vào field:
  ```java
  currentAuctions = new ArrayList<>(auctions);
  ```
- Trong `initialize()`: đăng ký push handler mới:
  ```java
  ServerConnection.getInstance().registerPushHandler(
      MessageType.PUSH_AUCTION_DELETED,
      json -> handleAuctionDeletedPush(json)
  );
  ```
- Thêm phương thức `handleAuctionDeletedPush(String json)`:
  ```java
  private void handleAuctionDeletedPush(String json) {
      JsonNode root = mapper.readTree(json);
      JsonNode node = root.has("payload") ? root.get("payload") : root;
      String auctionId = node.get("auctionId").asText();
      Platform.runLater(() -> {
          currentAuctions.removeIf(a -> auctionId.equals(a.getId()));
          renderAuctionCards(currentAuctions);
      });
  }
  ```

---

## Tổng hợp các file đã thay đổi

| File | Phía | Thay đổi |
|---|---|---|
| `common/protocol/MessageType.java` | Dùng chung | Thêm `PUSH_AUCTION_DELETED` |
| `server/infrastructure/NotificationService.java` | Server | Thêm `broadcastAuctionDeleted()` |
| `server/auction/AuctionHandler.java` | Server | Gọi `broadcastAuctionDeleted()` sau khi xóa |
| `client/.../AuctionBrowseController.java` | Client | Xử lý `PUSH_BID_UPDATE` và `PUSH_AUCTION_DELETED` |
| `client/.../MyListingsController.java` | Client | Xử lý `PUSH_BID_UPDATE` |

## Không thay đổi
- File FXML
- File CSS
- Schema cơ sở dữ liệu
- Các server handler khác
- `AuctionDetailController` (đã có sẵn xử lý `PUSH_BID_UPDATE` từ trước phiên làm việc này)
