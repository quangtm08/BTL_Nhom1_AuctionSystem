# 🔐 Luồng Xác Thực (Authentication Flow) Trong Hệ Thống Client-Server

Tài liệu này giải thích tổng quan cách ứng dụng và máy chủ xử lý xác thực, làm cơ sở để phát triển các tính năng mới sau này.

---

## 1. Kiến Trúc Tổng Quan (The Big Picture)

> [!NOTE]
> **Quy trình hoạt động cơ bản**:
> Client tạo tin nhắn (JSON) -> Gửi qua mạng -> Server phân loại tin nhắn -> Xử lý nghiệp vụ -> Tương tác CSDL (SQL) -> Đóng gói phản hồi -> Gửi về Client.

### Vai trò của từng gói mã nguồn:
*   **`client`**: Nơi chứa giao diện người dùng (JavaFX). Chịu trách nhiệm lấy dữ liệu từ người dùng và chờ phản hồi.
*   **`server`**: Lắng nghe và xử lý logic. Không quan tâm UI trông như thế nào.
*   **`common`**: Nơi thoả thuận định dạng "ngôn ngữ chung" (DTO, MessageType) giữa Client và Server để hai bên hiểu nhau.

---

## 2. Luồng Hoạt Động Cụ Thể (Flow Dữ Liệu Lên Và Xuống)

### Bước 1: Từ Người Dùng Đến Kết Nối Mạng (Client)
Khi người dùng bấm "Login":
1.  **Giao diện (Controller)** như `SignInController` thu thập Username và Password.
2.  Nó tạo một đối tượng DTO (Data Transfer Object - đối tượng chuyên dùng để đóng gói và truyền tải dữ liệu) gọi là `LoginRequest` (nằm trong thư mục `common`).
3.  Nó đóng gói DTO này vào một "Phong bì" gọi là `RequestMessage`, dán nhãn là `MessageType.LOGIN`.
4.  Nó gọi **`ServerConnection`** (người đưa thư) để gửi phong bì đó đi. `ServerConnection` đính kèm một `requestId` độc nhất cho phong bì này và đưa máy trạm vào trạng thái "Chờ phản hồi".

### Bước 2: Tại Trạm Điều Phối Máy Chủ (Server Infrastructure)
Tin nhắn bay qua mạng (đã được tự động biến thành dạng văn bản JSON) và đến Server.
1.  **`ClientHandler`**: Nhân viên trực điện thoại của từng Client. Nó nhận chuỗi JSON và chuyển tiếp luôn, không cần đọc nội dung.
2.  **`MessageRouter`**: Trạm phân loại trung tâm. Nó đọc "nhãn" (`MessageType`) trên phong bì JSON.
    *   Nếu nhãn là `LOGIN` hoặc `REGISTER`, nó sẽ chuyển nguyên văn tin nhắn cho phòng ban **Authentication**.

### Bước 3: Xử Lý Nghiệp Vụ (Authentication Feature)
Cụm xử lý tính năng (*Feature Module*) bắt đầu hoạt động. Theo chuẩn dự án, mọi tính năng đều có 3 thành phần:
1.  **`AuthHandler` (Người phiên dịch)**: Xé lớp vỏ ngoài của JSON, biến nó trở lại thành đối tượng lập trình Java (`LoginRequest`). Sau đó, nó gọi `AuthService` và yêu cầu "Hãy xử lý ông này giúp tôi".
2.  **`AuthService` (Bộ não)**: Thực hiện logic nghiệp vụ thuần túy. Nó không biết gì về JSON hay Mạng máy tính. Nó kiểm tra độ dài mật khẩu, mã hóa, ... và gọi `UserRepository` để hỏi "Hãy tìm ID người dùng này".
3.  **`UserRepository` (Người quản lý kho)**: Thành phần duy nhất tương tác trực tiếp với Database. Nó thực thi lệnh SQL (`SELECT * FROM users WHERE...`) và trả kết quả về cho `AuthService`.

> [!TIP]
> **Dành cho thành viên phát triển tính năng mới**:
> Bạn chỉ cần xây dựng cụm 3 tệp `FeatureHandler`, `FeatureService`, `FeatureRepository` (cùng với các tệp DTO tương ứng đặt trong `common`). Phần "Trạm Điều Phối" (`MessageRouter`, `ClientHandler`) đã được tự động hoá!

### Bước 4: Trả Kết Quả Về Cho Client
1.  Nếu tài khoản đúng, `AuthService` báo cáo thành công. Quá trình quay ngược trở lại.
2.  `AuthHandler` tạo kết quả thành đối tượng `AuthResponse`.
3.  `MessageRouter` hỗ trợ biến đối tượng đó thành chuỗi JSON phản hồi, dán lại cái nhãn `requestId` ban đầu và đẩy về `ClientHandler` để gửi qua mạng.
4.  Tại Client, **`ServerConnection`** lắng nghe mạng, thấy chuỗi trả về chứa `requestId`. Nó biết được phản hồi này thuộc về tác vụ "đăng nhập" đang chờ dở và kích hoạt hàm cập nhật giao diện (chuyển sang màn hình Dashboard).

---

## 3. Lý Do Phân Tách Kiến Trúc

Việc phân rã kiến trúc thành nhiều lớp đóng vai trò quan trọng trong việc giải quyết các rủi ro dài hạn:
*   **Decoupling (Giảm phụ thuộc):** `AuthService` hoàn toàn độc lập với cách Server giao tiếp bằng Socket. Ngay cả khi dự án sau này đổi từ Java Socket sang HTTP REST API, phần Service và Repository vẫn giữ lại được nguyên vẹn 100%.
*   **Asynchronous (Khả năng xử lý song song):** Giải thích về `requestId`: Chìa khóa vàng định tuyến. Giả sử người dùng trên Client vừa bấm "Đấu giá" (Mất 2 giây xử lý ở DB) rồi bấm luôn "Xem điểm danh" (Mất 0.1 giây). Server sẽ gửi "Kết quả xem điểm" về trước. Nhờ hệ thống `requestId`, Client hoàn toàn biết nhận diện và gán kết quả cho từng hộp giao diện phù hợp mà không bị chặn, chờ lần lượt.
*   **Error Isolation:** Lỗi của tính năng Đấu Giá không làm sụp hệ thống Xác Thực. Nếu quá trình deserialize DTO bị lỗi trong `BidHandler`, `MessageRouter` đơn thuần tạo ra một `ErrorResponse` rồi trả về bình thường. Tuyến kết nối Client-Server không bao giờ bị đứng vì tính năng nội bộ chết. 

> [!IMPORTANT]
> **Các quy tắc cần nhớ khi làm tính năng:**
> 1. Không để mã UI (Controller bên Client) xử lý logic tính toán. Giao diện chỉ làm nhiệm vụ Thu (Input) và Gửi (Request).
> 2. Đừng quên khai báo DTO và `MessageType` mới trong thư mục `common` đồng nhất hai bên.
> 3. Cấm `Service` import các thư viện đọc/ghi JSON như Jackson hay Socket. Service là môi trường tinh khiết cho logic tính toán và kiểm duyệt.
