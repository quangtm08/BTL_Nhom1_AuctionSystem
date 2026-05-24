# Hướng Dẫn Phát Triển Tính Năng Real-time (Push Notification)

Tài liệu này tập trung hướng dẫn cơ chế Push Notification (cập nhật thời gian thực), được bổ sung song song với luồng Request-Response hiện tại của hệ thống. 

*(Lưu ý: Luồng Request-Response cơ bản vẫn tuân theo tài liệu `client-server-guideline.md`)*

---

## 1. Phía Server: Cơ chế phát thông báo (Broadcast)

Để server có thể chủ động gửi dữ liệu về client (ví dụ: có lượt đặt giá mới), hệ thống sử dụng hai lớp hạ tầng cốt lõi:

*   **`ClientRegistry`**: Quản lý danh sách các kết nối socket đang hoạt động. Thực hiện ánh xạ giữa kết nối socket và User ID.
*   **`NotificationService`**: Dịch vụ trung tâm dùng để đóng gói và gửi thông báo. Lớp này được cung cấp sẵn các phương thức như `broadcastBidUpdate` và `broadcastAuctionEnded`.

### Hướng dẫn tích hợp cho Server Handler

Khi một sự kiện quan trọng xảy ra (như lưu thành công một lượt đặt giá mới), các module chức năng (như `BidHandler`) cần phát thông báo cho toàn hệ thống.

1.  Bảo đảm nhận tham chiếu `NotificationService` từ `ServerContext` thông qua tham số của phương thức `init()`.
2.  Gọi phương thức phát thông báo tương ứng sau khi xử lý thành công logic nghiệp vụ.

**Ví dụ minh họa:**
```java
// Bên trong BidHandler, sau khi cập nhật dữ liệu đặt giá thành công vào cơ sở dữ liệu
notificationService.broadcastBidUpdate(auctionId, amount, userId);
```

### Cơ chế Gửi Tin nhắn Không chặn (Non-blocking)

Từ phiên bản hiện tại, lớp `ClientRegistry` đã được nâng cấp để thực hiện gửi thông báo ở chế độ **bất đồng bộ (asynchronous)** sử dụng `CompletableFuture.runAsync()`.

- **Lợi ích**: Server sẽ không còn bị treo khi gặp một Client có kết nối mạng chậm. Luồng xử lý chính của Server chỉ việc "giao việc" cho các worker thread chạy ngầm và tiếp tục xử lý các yêu cầu khác ngay lập tức.
- **Tác động**: Một client lag sẽ không làm ảnh hưởng đến tốc độ nhận thông báo của các client khác. Tuân thủ nguyên tắc cách ly lỗi trong hệ thống phân tán.

---

## 2. Phía Client: Cơ chế nhận thông báo (Listen)

Lớp `ServerConnection` vẫn là nơi nhận dữ liệu thô từ socket. Khi nhận được một thông điệp từ server không chứa `requestId`, hệ thống tự động phân loại đó là tin nhắn Push.

Điểm mới: Controller không đăng ký trực tiếp với `ServerConnection` và không tự parse JSON nữa. Luồng hiện tại là:

```text
ServerConnection -> ClientPushService -> Typed Event DTO -> active screen Controller
```

Các DTO push đang dùng:

| MessageType | DTO |
| --- | --- |
| `PUSH_BID_UPDATE` | `BidUpdateEvent` |
| `PUSH_NEW_AUCTION` | `NewAuctionEvent` |
| `PUSH_AUCTION_ENDED` | `AuctionEndedEvent` |
| `PUSH_AUCTION_DELETED` | `AuctionDeletedEvent` |
| `PUSH_USER_DELETED` | `UserDeletedEvent` |
| `PUSH_USER_CREATED` | `UserCreatedEvent` |

### Hướng dẫn tích hợp cho Client Controller

Để một màn hình tự động cập nhật khi có sự kiện real-time, Controller quản lý màn hình đó cần đăng ký lắng nghe sự kiện thông qua `ClientPushService`.

1.  Lấy singleton `ClientPushService.getInstance()`.
2.  Gọi method subscribe tương ứng, ví dụ `onBidUpdate(...)`.
3.  Mỗi loại sự kiện chỉ có một handler đang hoạt động. Khi màn hình mới đăng ký handler cho cùng event, handler cũ được thay thế.
4.  Khi chuyển màn hình, `AppNavigator` xóa các handler push đang hoạt động trước khi controller mới được load.
5.  **Bắt buộc** gọi `Platform.runLater()` khi cập nhật giao diện (Label, Text, Table...). Callback push vẫn chạy từ background thread.

**Ví dụ tích hợp mẫu trong Controller:**
```java
import com.nhom1.auction.client.service.ClientPushService;
import com.nhom1.auction.common.dto.notification.BidUpdateEvent;
import javafx.application.Platform;

public class AuctionDetailController {
    private final ClientPushService pushService = ClientPushService.getInstance();

    public void initialize() {
        pushService.onBidUpdate(event -> {
            Platform.runLater(() -> updateBidUi(event));
        });
    }

    private void updateBidUi(BidUpdateEvent event) {
        // event.getAuctionId()
        // event.getNewHighestBid()
        // Update labels/cards here.
    }
}
```

### Vì sao chỉ có một handler cho mỗi event?

Ứng dụng hiện dùng mô hình một controller chính cho mỗi màn hình. Controller chính nhận push event rồi cập nhật các component con nếu cần, ví dụ cập nhật label bên trong auction card hoặc listing card. Vì vậy ta giữ thiết kế đơn giản: handler mới nhất là handler của màn hình hiện tại.

Khi điều hướng sang màn hình khác, `AppNavigator` gọi `ClientPushService.clearHandlersIfInitialized()` để tránh controller cũ tiếp tục nhận push event.

### Lưu ý khi thêm màn hình mới

Nếu một màn hình có nhiều controller con, chỉ controller cha nên đăng ký push event. Controller cha sau đó truyền dữ liệu xuống component con hoặc cập nhật UI con qua method public phù hợp. Nếu sau này cần nhiều phần UI độc lập cùng nghe một event, có thể nâng cấp `ClientPushService` sang mô hình multi-subscriber.
