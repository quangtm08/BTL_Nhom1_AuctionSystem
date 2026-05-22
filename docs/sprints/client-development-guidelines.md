# Quy Ước Làm Việc Với Client

Tài liệu này dùng khi sửa hoặc thêm màn hình client JavaFX. Mục tiêu: controller chỉ lo giao diện, service lo dữ liệu/server, các helper dùng chung lo định dạng và xử lý lặp lại.

## 1. Controller Chỉ Lo UI

Controller được phép làm:

- đọc dữ liệu từ `@FXML` fields;
- gắn action cho button/input;
- gọi client service;
- render dữ liệu ra label, card, grid, table;
- điều hướng bằng `AppNavigator`;
- cập nhật trạng thái loading/empty/error của màn hình;
- cập nhật UI khi nhận typed push event.

Controller không được làm:

- import hoặc gọi trực tiếp `ServerConnection`;
- tự tạo `RequestMessage`;
- tự chọn `MessageType`;
- tự parse JSON bằng `ObjectMapper`, `JsonNode`, `readTree`;
- viết logic lọc/kết hợp dữ liệu phức tạp nếu logic đó không thuần UI;
- format tiền, ngày giờ, trạng thái bằng method riêng nếu đã có helper dùng chung.

Ví dụ đúng:

```java
biddingService.listBrowseAuctions()
    .thenAccept(auctions -> Platform.runLater(() -> renderAuctionCards(auctions)));
```

Ví dụ không đúng:

```java
RequestMessage<?> request = new RequestMessage<>(MessageType.LIST_AUCTIONS, null);
ServerConnection.getInstance().sendRequest(request, ListAuctionsResponse.class);
```

## 2. Client Service Lo Server Và Dữ Liệu

Các class trong `client/user/service` và `client/admin/service` chịu trách nhiệm:

- validate input trước khi gửi request;
- lấy user hiện tại từ `AppContext` nếu request cần user id;
- tạo DTO request;
- tạo `RequestMessage`;
- chọn `MessageType`;
- gọi `send(...)` từ `BaseClientService`;
- unwrap response và trả về DTO/list đã sẵn sàng cho controller dùng;
- gom logic kết hợp dữ liệu từ nhiều request nếu đó là logic nghiệp vụ/view-model.

Ví dụ:

- `BiddingClientService.listBrowseAuctions()` trả về danh sách auction đã lọc cho màn Explore.
- `MyListingsClientService.listMyListings()` tải listing của user hiện tại.
- `MyListingsClientService.deleteListing(auctionId)` xóa listing theo user hiện tại.

Nếu một controller cần dữ liệu mới từ server, thêm method vào service phù hợp trước. Không gửi request trực tiếp trong controller.

## 3. Real-time Push Dùng `ClientPushService`

Controller không đăng ký push trực tiếp qua `ServerConnection`.

Luồng đúng:

```text
ServerConnection -> ClientPushService -> Typed Event DTO -> Controller
```

Controller dùng:

```java
private final ClientPushService pushService = ClientPushService.getInstance();

public void initialize() {
    pushService.onBidUpdate(event ->
        Platform.runLater(() -> updateBidUi(event))
    );
}
```

Quy tắc khi dùng push:

- dùng các method `onBidUpdate`, `onNewAuction`, `onAuctionDeleted`, `onAuctionEnded`, `onUserDeleted`, `onUserCreated`;
- callback nhận DTO đã parse sẵn, ví dụ `BidUpdateEvent`;
- luôn dùng `Platform.runLater(...)` khi sửa UI;
- controller cha đăng ký push, component/card con không tự đăng ký;
- không parse JSON push trong controller;
- không gọi `registerPushHandler(...)` trong controller.

Mỗi loại push hiện chỉ có một handler active. Khi chuyển màn hình, `AppNavigator` sẽ clear handler cũ trước khi load màn mới.

## 4. Định Dạng Hiển Thị Dùng `DisplayFormatters`

Dùng `com.nhom1.auction.client.util.DisplayFormatters` cho các định dạng lặp lại:

- `DisplayFormatters.money(amount)`;
- `DisplayFormatters.moneyOrDash(amount)`;
- `DisplayFormatters.timeLeft(endTime)`;
- `DisplayFormatters.shortDate(dateTime)`;
- `DisplayFormatters.dateTime(dateTime)`;
- `DisplayFormatters.bidTime(dateTime)`;
- `DisplayFormatters.auctionStatusLabel(status)`;
- `DisplayFormatters.isEnded(status)`;
- `DisplayFormatters.adminAuctionStatusStyle(status)`.

Không tạo lại các method kiểu:

```java
private String formatMoney(...)
private String formatTimeLeft(...)
private String formatDate(...)
private String formatStatus(...)
```

Nếu cần format mới dùng ở nhiều màn, thêm vào `DisplayFormatters`. Nếu format chỉ phục vụ đúng một màn và không lặp lại, có thể để local.

## 5. Async Và Lỗi

Khi xử lý lỗi từ `CompletableFuture`, dùng:

```java
Throwable cause = BaseClientService.extractFailure(ex);
```

Không dùng:

```java
ex.getCause().getMessage()
```

Lý do: `getCause()` có thể null và làm crash UI error handler.

Khi service trả lỗi validation/server, controller chỉ quyết định hiển thị lỗi ở đâu. Không tự map protocol error trong controller.

## 6. Session Và Điều Hướng

Khi logout:

```java
AppContext.clearSession();
AppNavigator.navigateTo(AppView.SIGN_IN);
```

Không chỉ navigate về sign-in mà không clear session.

Khi cần lưu auction đang chọn để mở detail:

```java
AppContext.setSelectedAuctionId(auctionId);
AppNavigator.navigateTo(AppView.AUCTION_DETAIL);
```

Đây là UI/navigation state, controller được giữ logic này.

## 7. Null Safety Khi Render Row/Card

DTO từ server có thể thiếu field. Khi render action button:

- kiểm tra id null/blank trước;
- disable button nếu thiếu id;
- không gọi `.equals(...)` trực tiếp từ value có thể null.

Đúng:

```java
String auctionId = payment.getAuctionId();
boolean missingAuctionId = auctionId == null || auctionId.isBlank();
payNow.setDisable(missingAuctionId || auctionId.equals(processingAuctionId));
```

Không đúng:

```java
payNow.setDisable(payment.getAuctionId().equals(processingAuctionId));
```

## 8. Khi Thêm Màn Hình Mới

Checklist nhanh:

- Controller chỉ render và gọi service.
- Request mới phải nằm trong service.
- Push realtime phải qua `ClientPushService`.
- DTO push mới phải nằm trong `common/dto/notification`.
- Format chung phải dùng `DisplayFormatters`.
- Lỗi async phải dùng `BaseClientService.extractFailure(...)`.
- Logout phải clear session.
- Không để controller import protocol/socket/Jackson.

Nếu thấy controller bắt đầu dài vì build nhiều row/card thủ công, cân nhắc tách thành component FXML/controller hoặc helper riêng.
