# 🚀 Hướng Dẫn Phát Triển Tính Năng Client-Server

Tài liệu này sẽ hướng dẫn chi tiết cách "làm chủ" một tính năng (ví dụ: luồng Đấu Giá - Bidding, Quản Trị - Admin) từ lúc nhập dữ liệu trên giao diện (UI) cho đến khi lưu vào Database trên Server.

---

## 1. Các File Cần Tạo Và Chỉnh Sửa (Nơi Bạn Code)

Để hoàn thành một tính năng, cần làm việc ở 3 khu vực: Thư mục `common` (để dùng chung), thư mục `client` (giao diện) và thư mục `server` (xử lý logic).

### Bước 1: Khai báo ở thư mục `common`
Đây là nơi định nghĩa "ngôn ngữ chung" cho tính năng.
*   **Tạo tệp DTO (Data Transfer Object):** 
- Nằm trong `common/dto/tên_tính_năng`. Ví dụ: `BidRequest.java`, `BidResponse.java`. Những file này chỉ chứa các trường dữ liệu (variables), hàm `get`, hàm `set` và constructor. Không có logic phức tạp.
- Nôm na thì DTO sẽ chính là JSON payload khi được convert ở client và server để xử lý.


*   **Chỉnh sửa `MessageType.java` (trong `common/protocol`):** Thêm từ khóa cho tính năng tương ứng. Ví dụ: `BID_PLACE`, `GET_ALL_AUCTIONS`.

### Bước 2: Viết giao diện ở thư mục `client`
*   **Tạo/Chỉnh sửa UI Controller:** Ví dụ `BidController.java`.
*   **Logic bên trong Controller:**
    1. Lấy dữ liệu người dùng nhập từ giao diện (Text box, Button click).
    2. Gom dữ liệu vào đối tượng DTO vừa tạo (`BidRequest`).
    3. Đóng gói DTO vào `RequestMessage` với nhãn `MessageType` tương ứng.
    4. Gọi `ServerConnection.getInstance().sendRequest(...)` và dùng `.thenAccept(response -> { ... })` để xử lý khi lấy được kết quả từ máy chủ, sau đó cập nhật lại màn hình.

### Bước 3: Xử lý nghiệp vụ ở thư mục `server`
Cần tạo một folder mới trong `server` (ví dụ: `server/bidding`) và tạo 4 file sau:
*   **`FeatureRepository.java` (Ví dụ: `BidRepository.java`):** Chứa các câu lệnh SQL (`INSERT`, `SELECT`). Nhận kết nối `Connection` Database qua constructor. Không dùng code xử lý nghiệp vụ ở đây.
*   **`FeatureService.java` (Ví dụ: `BidService.java`):** Xử lý quy tắc nghiệp vụ (Ví dụ: giá thầu phải cao hơn giá hiện tại). Gọi hàm của Repository để lấy/ghi dữ liệu. Nếu gặp lỗi quy tắc, dùng lệnh `throw new Exception(...)`. Không nhắc gì tới JSON hay socket ở đây.
*   **`FeatureHandler.java` (Ví dụ: `BidHandler.java`):** Lớp phiên dịch. Cung cấp hàm `register(MessageRouter router)`. Bên trong hàm này, báo cho Router biết việc nhận xử lý `MessageType` nào. Hàm này nhận JSON từ Router, biến nó thành DTO, gọi `BidService` xử lý, sau đó trả DTO kết quả lại cho Router.
*   **`FeatureModule.java` (Ví dụ: `BidModule.java`):** Lớp kết nối. Cung cấp hàm `init(Connection connection, MessageRouter router)`. Bên trong, lần lượt khởi tạo Repository, Service, sau đó khởi tạo Handler và kích hoạt nó.

### Bước 4: Đăng ký tính năng
Chỉ cần chỉnh sửa **MỘT FILE DUY NHẤT** ở kiến trúc tầng trên là `ServerContext.java`.
*   Bổ sung thêm một dòng vào trong constructor: `BidModule.init(this.connection, this.router);`

---

## 2. Những File Không Cần Đụng Vào (Và Cách Chúng Hoạt Động)

Đây là các tệp nền tảng (Infrastructure) của hệ thống. **KHÔNG ĐƯỢC PHÉP** chỉnh sửa logic của chúng. Nhiệm vụ chỉ là hiểu chúng hoạt động ra sao và tận dụng bộ máy cấu trúc này.

### `ServerConnection.java` (Nằm ở `client`)
*   **Nhiệm vụ:** Người đưa thư bên Client. Duy trì kết nối mạng với Server.
*   **Cần biết hàm nào:**
    *   `sendRequest(RequestMessage request, Class responseClass)`: Tự động gửi gói tin đi và trả về một `CompletableFuture` (lời hứa). Khi Server có phản hồi trả về, nó tự động xử lý chuỗi JSON thành Object (theo `responseClass` được truyền vào) rồi gọi hàm cập nhật màn hình.

### `ClientHandler.java` (Nằm ở `server`)
*   **Nhiệm vụ:** Một luồng (thread) phục vụ riêng biệt cho một Client. Có 10 Client kết nối sẽ có 10 `ClientHandler` chạy.
*   **Cần biết hàm nào:**
    *   `run()`: Hàm chạy liên tục. Nó lấy dữ liệu văn bản từ Socket bằng `BufferedReader`, thảy vào mồm con `MessageRouter`, nhận lại kết quả văn bản phản hồi và tống thẳng vào `PrintWriter` để trả lại cho Socket mạng.

### `MessageRouter.java` (Nằm ở `server`)
*   **Nhiệm vụ:** Cảnh sát giao thông phân luồng.
*   **Cần biết hàm nào:**
    *   `handleRequest(String json)`: Được gọi bởi `ClientHandler`. Nó chuyển nhanh chuỗi JSON thành bảng tra cứu, bóc cái nhãn `type` (VD: `BID_PLACE`) ra xem. Nó tra xem ở trong ruột nó có Handler nào đã nhận loại `type` này chưa. Có thì nó gọi Handler đó ra để làm việc thông qua phương thức `execute`. Cuối cùng, kết quả sẽ được đóng gói lại từ Java JSON bằng hàm `.toJson()` để trả ngược xuống.
    *   `register(MessageType type, MessageRouteAction action)`: Hàm để các Handler báo danh công việc.

### `Server.java` (Nằm ở `server`)
*   **Nhiệm vụ:** Sếp sòng. Chạy hàm `main` khởi động mọi thứ mở cổng `12345` bằng `ServerSocket` để chờ client tham gia vào hệ thống.
*   **Không cần gọi hàm nào ở đây.**

### `JsonUtil.java` (Nằm ở `common/utils`)
*   **Nhiệm vụ:** Lớp tiện ích đóng gói (wrapper) thư viện Jackson.
*   **Chi tiết:** Giúp chuyển đổi qua lại giữa chuỗi JSON và Object Java một cách dễ dàng, giấu đi sự phức tạp của thư viện dữ liệu Jackson. Cả Client và Server đều dựa vào nó để "đóng gói" và "mở gói" thông điệp mạng.

---

## 3. Lưu Ý Về Việc Dùng Chéo Dữ Liệu (Cross-module Dependency)

Nếu một tính năng (ví dụ: `AuctionService`) cần thông tin từ một bộ phận khác (ví dụ cần thông tin người dùng từ bảng `users`):
*   **KHÔNG** viết lại câu lệnh SQL truy vấn user trong `AuctionRepository`.
*   **Cách đúng:** Truyền đối tượng `UserRepository` (hoặc `AuthService`) vào cho `AuctionService` thông qua hàm khởi tạo (constructor). Từ đó, tái sử dụng các hàm đã được viết sẵn (như `findByIdentifier()`). Việc các Service hoặc Repository gọi chéo nhau để đọc dữ liệu là hoàn toàn hợp lệ và khuyến khích nhằm tránh trùng lặp code.

**LƯU Ý:** CẦN TRAO ĐỔI VỚI NGƯỜI OWN FLOW KIA ĐỂ VIẾT THÊM METHOD VÀ SQL QUERY NẾU CẦN


---

## 4. Checklist Hoàn Thành Một Tính Năng

Hãy đánh dấu kiểm các bước sau để chắc chắn tính năng đã trọn vẹn:

- [ ] (Common) Tạo được DTO đầu vào (Request).
- [ ] (Common) Tạo được DTO đầu ra (Response) (nếu cần).
- [ ] (Common) Thêm được kiểu tin nhắn mới vào `MessageType`.
- [ ] (Server) Khởi tạo Repository và viết đúng câu truy vấn SQL cần thiết.
- [ ] (Server) Viết logic đúng nguyên tắc trong Service (KHÔNG JSON, KHÔNG SQL).
- [ ] (Server) Lắp ráp Handler: nhận JSON -> Gọi Service xử lý -> Trả Result về.
- [ ] (Server) Gom Repository, Service và Handler vào hàm `init` của một Module mới.
- [ ] (Server) Gọi hàm `.init(connection, router)` của Module trên vào trong `ServerContext`.
- [ ] (Client) Chỉnh sửa Controller để gom thông tin thành `RequestMessage`.
- [ ] (Client) Gọi hàm `sendRequest` của `ServerConnection`, nhận kết quả và đổ lại lên màn hình giao diện.
