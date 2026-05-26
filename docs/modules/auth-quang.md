# 🔐 Luồng Xác Thực (Authentication Flow) Trong Hệ Thống Client-Server

Tài liệu này giải thích chi tiết cách ứng dụng xử lý luồng xác thực, bao gồm cả kiến trúc **Client Service Layer** mới được tích hợp để tách biệt logic giao diện và logic mạng.

---

## 1. Kiến Trúc Tổng Quan (The Big Picture)

Hệ thống được chia thành các lớp chuyên biệt để đảm bảo tính dễ bảo trì và mở rộng:

### Phía Client (JavaFX):
*   **Controllers** (`SignInController`, `RegisterController`): Chỉ quản lý giao diện, bắt sự kiện người dùng và hiển thị kết quả.
*   **Client Services** (`AuthClientService`): Chứa logic nghiệp vụ phía client, quản lý Session (trạng thái đăng nhập).
*   **Infrastructure** (`BaseClientService`, `ServerConnection`): Xử lý việc đóng gói tin nhắn JSON và truyền tải qua Socket.

### Phía Server (Java):
*   **Handlers** (`AuthHandler`): "Người phiên dịch" giữa JSON và Java.
*   **Services** (`AuthService`): "Bộ não" xử lý logic nghiệp vụ (kiểm tra mật khẩu, tạo tài khoản).
*   **Repositories** (`UserRepository`): "Người quản lý kho" tương tác trực tiếp với Database.

---

## 2. Luồng Hoạt Động Cụ Thể (Data Flow)

### Bước 1: Từ Giao Diện Đến Tầng Dịch Vụ (Client)
Khi người dùng nhập thông tin và bấm "Login":
1.  **`SignInController`** thu thập dữ liệu và gọi phương thức `authService.login(user, pass)`.
2.  **`AuthClientService`** thực hiện validate nhanh (ví dụ: không để trống trường).
3.  **`AuthClientService`** tạo DTO `LoginRequest`, đóng gói vào `RequestMessage` và chuyển cho lớp cha **`BaseClientService`**.
4.  **`BaseClientService`** yêu cầu **`ServerConnection`** gửi tin nhắn đi và chờ đợi phản hồi (`CompletableFuture`).

### Bước 2: Tại Trạm Điều Phối Máy Chủ (Server)
Tin nhắn JSON đến Server:
1.  **`MessageRouter`** đọc nhãn `MessageType.LOGIN` và chuyển tiếp cho **`AuthHandler`**.
2.  **`AuthHandler`** giải mã JSON thành `LoginRequest` và gọi **`AuthService`**.
3.  **`AuthService`** gọi **`UserRepository`** để truy vấn thông tin từ database qua `DataSource` hiện tại (SQLite local hoặc PostgreSQL nếu có cấu hình cloud).
4.  Nếu thông tin khớp, **`AuthService`** trả về đối tượng `User`.

### Bước 3: Phản Hồi Và Cập Nhật Trạng Thái (Client)
1.  Server đóng gói kết quả vào `AuthResponse` và gửi về qua Socket.
2.  **`ServerConnection`** nhận JSON, map vào `AuthResponse` và hoàn thành `CompletableFuture`.
3.  **`BaseClientService`** bóc tách phong bì `ResponseMessage`, nếu có lỗi thì ném `AuctionException`. Nếu thành công, trả kết quả cho `AuthClientService`.
4.  **`AuthClientService`** lưu thông tin người dùng vào **`AppContext`** (Session) và trả kết quả về cho Controller.
5.  **`SignInController`** nhận dữ liệu thành công và thực hiện chuyển màn hình (`AppNavigator.navigateTo(...)`).

---

## 3. Lợi Ích Của Việc Phân Lớp

Việc thêm tầng **Client Service** mang lại các lợi ích quan trọng:
*   **Controllers sạch hơn**: Không còn code liên quan đến JSON, MessageType hay ServerConnection.
*   **Quản lý Session tập trung**: Việc lưu `currentUser` vào `AppContext` được thực hiện tự động trong Service, Controller không cần lo lắng về việc này.
*   **Tái sử dụng logic**: Các controller khác nhau (ví dụ: màn hình Login và màn hình Profile) có thể dùng chung một `AuthClientService`.
*   **Xử lý lỗi thống nhất**: Mọi lỗi mạng hoặc lỗi logic server đều được `BaseClientService` chuẩn hoá về `AuctionException`.

---

## 4. Checklist Cho Lập Trình Viên (Dành cho Team)

Khi bạn xây dựng một tính năng mới (ví dụ: Bidding):
1.  **Common**: Tạo DTO (`BidRequest`, `BidResponse`) và thêm `MessageType`.
2.  **Server**: Viết bộ 3 `Repository` -> `Service` -> `Handler`.
3.  **Client Service**: Tạo `BiddingClientService` kế thừa `BaseClientService` để đóng gói logic gửi nhận.
4.  **Client Controller**: Gọi `BiddingClientService` để lấy dữ liệu, chỉ tập trung vào việc hiển thị UI.

> [!IMPORTANT]
> **Quy tắc vàng**: Controller KHÔNG ĐƯỢC phép gọi trực tiếp `ServerConnection`. Mọi yêu cầu phải đi qua một `ClientService` tương ứng.
