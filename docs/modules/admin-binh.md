# 🛡️ Module Quản Trị Hệ Thống (Admin) - Bình

Tài liệu này giải thích chi tiết kiến trúc và luồng hoạt động của tính năng Quản trị viên (Admin).

---

## 1. Các thành phần (Classes) tham gia

Tính năng được chia làm hai khu vực: Giao diện (Client) và Xử lý nghiệp vụ (Server).

### Phía Client (Giao diện Quản trị)
*   **`AdminOverviewController`**: Quản lý giao diện, chịu trách nhiệm liên kết dữ liệu thống kê và hiển thị lên màn hình `admin_overview.fxml`.
*   **`AdminClientService`**: Đóng gói các yêu cầu từ Controller thành cấu trúc `RequestMessage` và gửi qua kết nối mạng.
*   **`BaseClientService`**: Lớp cơ sở cung cấp các hàm tiện ích cho việc gửi tin và xử lý phản hồi bất đồng bộ, được kế thừa bởi `AdminClientService`.

### Phía Server (Hệ thống trung tâm)
*   **`AdminModule`**: Đảm nhiệm vai trò khởi tạo các thành phần của module Admin và đăng ký luồng (routes) vào `MessageRouter`.
*   **`AdminHandler`**: Tiếp nhận gói tin JSON từ Client, phân tích yêu cầu (dựa trên `MessageType`), và gọi tiếp đến `AdminService`.
*   **`AdminService`**: Xử lý logic nghiệp vụ cốt lõi. Chịu trách nhiệm xác thực quyền hạn (đảm bảo chỉ user có vai trò `ADMIN` mới được thực thi) và thực hiện thao tác xóa hoặc truy xuất dữ liệu.
*   **`UserRepository`**: Tương tác trực tiếp với cơ sở dữ liệu để truy xuất thông tin người dùng.
*   **`SqlAdminAuctionGateway`**: Lớp trung gian (Adapter) cho phép `AdminService` lấy danh sách thống kê phiên đấu giá mà không phụ thuộc trực tiếp vào module Auction.

---

## 2. Luồng hoạt động chi tiết (Flow Client ↔ Server)

Dưới đây là luồng xử lý tiêu chuẩn khi Admin truy cập bảng điều khiển (Dashboard):

### Bước 1: Gửi yêu cầu từ Client
1. Màn hình `AdminOverviewController` khởi tạo và yêu cầu lấy dữ liệu thống kê.
2. Controller gọi đồng thời hai hàm `adminClientService.listUsers()` và `adminClientService.listAllAuctions()`.
3. **`AdminClientService`** đóng gói các lệnh này vào đối tượng `RequestMessage` (ví dụ với loại `ADMIN_LIST_USERS`) và đẩy vào Socket. Quá trình này hoàn toàn bất đồng bộ thông qua `CompletableFuture`, giúp giao diện không bị gián đoạn.

### Bước 2: Xử lý tại Server
1. Khi gói tin đến Server, thành phần điều hướng mạng (`MessageRouter`) phân tích và chuyển hướng gói tin đến **`AdminHandler`**.
2. `AdminHandler` giải mã dữ liệu JSON, trích xuất ID người gọi (`callerId`) và chuyển cho **`AdminService`**.
3. **`AdminService`** thực hiện quy trình bảo mật: Xác nhận `callerId` thuộc về một Quản trị viên hợp lệ. Sau đó, nó gọi **`UserRepository`** để truy xuất dữ liệu.
4. Dữ liệu thô từ Database được ánh xạ (map) sang định dạng `UserSummaryDto`.
5. `AdminHandler` đóng gói tập dữ liệu này vào `ResponseMessage` và gửi trả lại qua luồng Socket.

### Bước 3: Nhận phản hồi và Cập nhật Client
1. Luồng lắng nghe mạng (Listener thread) của Client nhận được phản hồi.
2. Hệ thống trích xuất `Request ID` để xác định phản hồi này thuộc về yêu cầu nào trong danh sách chờ (`PendingRequests`).
3. Sau khi giải mã thành công, `CompletableFuture` của yêu cầu ban đầu được hoàn tất.
4. Khối lệnh callback (`.thenAccept(...)`) bên trong **`AdminOverviewController`** được kích hoạt. Việc cập nhật số liệu lên giao diện được thực thi thông qua `Platform.runLater()` để đảm bảo an toàn luồng (thread-safety) trong JavaFX.

---

## 3. Trạng thái tích hợp (Wiring Status)
Toàn bộ thành phần của module Admin đã được Điều phối viên kết nối thành công trong `ServerContext`. 
Luồng truy xuất dữ liệu chéo từ Admin sang Auction được xử lý triệt để thông qua **`SqlAdminAuctionGateway`**, đảm bảo tính độc lập và toàn vẹn mã nguồn của từng cá nhân.
