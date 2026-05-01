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

---

## 2. Phía Client: Cơ chế nhận thông báo (Listen)

Lớp `ServerConnection` đã được nâng cấp để hoạt động như một trạm lắng nghe tín hiệu chạy ngầm. Khi nhận được một thông điệp từ server không chứa `requestId`, hệ thống tự động phân loại đó là tin nhắn Push và phân phối tới các giao diện (Controller) đã đăng ký.

### Hướng dẫn tích hợp cho Client Controller

Để một màn hình tự động cập nhật khi có sự kiện real-time, Controller quản lý màn hình đó cần đăng ký lắng nghe sự kiện.

1.  Sử dụng phương thức `ServerConnection.getInstance().registerPushHandler()`.
2.  Truyền vào loại sự kiện cần lắng nghe (`MessageType`) và đoạn mã xử lý dữ liệu JSON trả về.
3.  **Bắt buộc** gọi `Platform.runLater()` bao bọc toàn bộ code cập nhật giao diện (Label, Text, Table...). Do luồng nhận dữ liệu mạng chạy ngầm (background thread), việc thay đổi UI trực tiếp từ luồng này sẽ gây lỗi ứng dụng (JavaFX Thread Exception).

**Ví dụ tích hợp mẫu trong Controller:**
```java
import javafx.application.Platform;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;
import com.nhom1.auction.client.user.connection.ServerConnection;
import com.nhom1.auction.common.protocol.MessageType;

public class AuctionDetailController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đăng ký nhận thông báo khi có lượt đặt giá mới
        ServerConnection.getInstance().registerPushHandler(MessageType.PUSH_BID_UPDATE, json -> {
            
            // Bắt buộc đẩy logic cập nhật UI vào luồng chính
            Platform.runLater(() -> {
                System.out.println("Nhận dữ liệu real-time: " + json);
                // 1. Phân tích chuỗi JSON thành DTO sự kiện tương ứng
                // 2. Cập nhật trạng thái đối tượng Auction trong bộ nhớ
                // 3. Thay đổi các Label hiển thị trên màn hình
            });
        });
    }
}
```
