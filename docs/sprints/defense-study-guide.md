# Kế hoạch Ôn tập


## 🌍 Kiến thức chung: Vòng đời Request/Response
**Yêu cầu đối với TẤT CẢ thành viên**
Cần có khả năng truy vết một hành động cụ thể (ví dụ: "Đặt giá" hoặc "Tạo đấu giá") qua các lớp sau:

1.  **Client UI:** Người dùng nhấn nút.
2.  **Client Service:** Dữ liệu được kiểm tra (validate) và đóng gói vào **DTO**.
3.  **Server Connection:** DTO được chuyển đổi sang **JSON** và gửi qua socket kèm theo `MessageType`.
4.  **Server Message Router:** Server nhận JSON, xác định `MessageType` và điều hướng đến **Handler** phù hợp.
5.  **Feature Handler (Server):** Chuyển đổi JSON ngược lại thành DTO và gọi **Service**.
6.  **Server Service:** Thực thi logic nghiệp vụ (ví dụ: kiểm tra giá, cập nhật cơ sở dữ liệu).
7.  **Luồng phản hồi (Response):** Quy trình diễn ra ngược lại (Service -> Handler -> JSON -> Client -> UI).

Note: Cố gắng nắm rõ logic từng method ở những class riêng trong flow mình làm (controller, service (server), feature handler (server), repository). Những class chung còn lại (server connection, server, client handler, message router,...) thì nắm được vai trò chính là được.
---

## Quang: Infrastructure & Navigation
**Trọng tâm:** "Bộ khung" của ứng dụng và luồng giao tiếp.

### 📚 Nội dung nghiên cứu:
1.  **Kiến trúc Client-Server:**
    *   **Quản lý Socket:** Cách `Server` chấp nhận kết nối và `ClientHandler` quản lý vòng đời của một client.
    *   **Hệ thống điều hướng tin nhắn:** Cơ chế bên trong của `ServerMessageRouter` và `ServerContext`.
    *   **Tiêm phụ thuộc (Dependency Injection):** Cách các service được khởi tạo và chia sẻ trong hệ thống.
2.  **Hệ thống Push thời gian thực:**
    *   Cơ chế `ClientRegistry` và `NotificationService`.
    *   Cách server xác định client để đẩy cập nhật (Mô hình Observer).
3.  **Điều hướng UI & Đa luồng:**
    *   **AppNavigator:** Cách chuyển đổi giữa các màn hình và duy trì trạng thái.
    *   **Platform.runLater:** Lý do cần thiết để cập nhật UI JavaFX từ các luồng socket.
    *   **Luồng Xác thực (Auth):** Mã nguồn tham chiếu chuẩn cho tất cả các module khác.
4. **Flow authentication**

### ❓ Câu hỏi ví dụ:
*   "Làm thế nào server xử lý nhiều client đồng thời mà không bị tắc nghẽn (blocking)?"
*   "Điều gì xảy ra với socket nếu người dùng đóng ứng dụng đột ngột?"
*   "Tại sao không thể cập nhật trực tiếp một Label từ luồng ClientConnection?"

---

## Duy: Luồng Seller & Mô hình Dữ liệu
**Trọng tâm:** Mọi thứ liên quan đến việc tạo và quản lý danh sách đấu giá.

### 📚 Nội dung nghiên cứu:
1.  **Trục tính năng:** Truy vết luồng **Tạo đấu giá (Create Auction)** từ UI đến Cơ sở dữ liệu (xem phần Vòng đời ở trên).
2.  **Mô hình dữ liệu:**
    *   **Item Factory:** Cách hệ thống quyết định tạo `Art`, `Electronics`, hoặc `Vehicle` dựa trên đầu vào.
    *   **Kế thừa dữ liệu:** Lớp cơ sở `Item` và các lớp con chuyên biệt.
3.  **Quản lý danh sách:**
    *   **Màn hình My Listings:** Logic lấy và hiển thị các phiên đấu giá được lọc theo người bán hiện tại.
    *   **Truy vấn SQL (Joins):** Hiểu rằng bảng `auctions` cần được JOIN với bảng `items` để lấy thông tin người bán và danh mục.

### ❓ Câu hỏi ví dụ:
*   "Truy vết một request create auction: Điều gì xảy ra ở tầng Handler so với tầng Service?"
*   "Làm thế nào để lưu trữ các thuộc tính riêng biệt của các loại items khác nhau trong một cấu trúc cơ sở dữ liệu duy nhất?"
*   "Nếu thêm một danh mục mới là 'Furniture', những lớp (class) nào cần được chỉnh sửa?"

---

## Ngọc: Luồng Đấu giá & xử lý concurrency
**Trọng tâm:** Logic đặt giá và đảm bảo tính toàn vẹn dữ liệu.

### 📚 Nội dung nghiên cứu:
1.  **Trục tính năng:** Truy vết luồng **Đặt giá (Place Bid)** từ nút "Đặt giá" đến khi cập nhật thời gian thực trên màn hình của các client khác.
2.  **Xử lý đồng thời (Concurrency) & An toàn luồng:**
    *   **Khóa trong Auction.java:** Hiểu sâu về `ReentrantLock` và lý do sử dụng `fair: true`.
    *   **Cơ chế Hai Khóa (Double Locking):** Đặc biệt lưu ý phương thức `placeBid()`. Phương thức này sử dụng cả `auctionLock` (để bảo vệ trạng thái phiên) và `synchronized(bidHistoryMonitor)` (để bảo vệ danh sách lịch sử bid). Cần giải thích được tại sao cần tách biệt hai loại khóa này.
    *   **Biến Volatile:** Tại sao `currentHighestBid` cần đảm bảo tính hiển thị (visibility) giữa các luồng.
    *   **Cập nhật nguyên tử (Atomic):** Cách đảm bảo hai lượt đặt giá không bị xung đột và ghi đè lẫn nhau.
3.  **Kiểm tra tính hợp lệ (Validation):** Logic trong `AuctionBidValidator` (ví dụ: giá đặt phải lớn hơn giá hiện tại + bước giá).

### ❓ Câu hỏi ví dụ:
*   "JSON từ client được chuyển đổi thành DTO `PlaceBidRequest` trên server như thế nào?"
*   "Race Condition là gì, và `auctionLock` ngăn chặn nó như thế nào trong một cuộc chiến đấu giá?"
*   "Làm thế nào client biết giá đã thay đổi mà không cần tải lại trang?"

---

## Bình: Testing, Scheduler, Exception
**Trọng tâm:** Đảm bảo ứng dụng hoạt động ổn định, được kiểm thử và xử lý lỗi tốt.

### 📚 Nội dung nghiên cứu:
1.  **Testing:**
    *   Cách sử dụng Mockito để giả lập (mock) repository trong các bài kiểm tra service.
    *   Sự khác biệt giữa Unit test và Integration test trong dự án.
2.  **Scheduler:**
    *   **AuctionScheduler:** Cách `ScheduledExecutorService` quản lý các bước chuyển trạng thái (OPEN -> RUNNING -> FINISHED).
    *   **Anti-Sniping:** Cách hệ thống phát hiện lượt đặt giá giây cuối và gia hạn thời gian.
3.  **Exception Handling:**
    *   Hệ thống phân cấp `AuctionException`.
    *   Cách `BaseClientService` ánh xạ mã lỗi từ server sang các ngoại lệ Java cụ thể.
    *   Xử lý lỗi tập trung (UI Custom Alert).
    * Nói chung là mình đang xử lý exception xuyên suốt app thế nào

### ❓ Câu hỏi ví dụ:
*   "Anti-Sniping là gì và làm thế nào để triển khai nó mà không cần một công cụ lập lịch (Cron job) riêng biệt?"
*   "Làm thế nào để kiểm thử logic Server mà không cần mở một kết nối mạng (Network socket) thật?"
*   "Nếu kết nối cơ sở dữ liệu thất bại, người dùng sẽ được thông báo như thế nào?"

---

## 🚫 Ngoài phạm vi
*   **Module Admin:** Đang trong quá trình hoàn thiện, không nằm trong phần trình diễn chính.
*   **Auto-Bid:** Logic đã có nhưng chưa được kiểm chứng đầy đủ cho giai đoạn này.
