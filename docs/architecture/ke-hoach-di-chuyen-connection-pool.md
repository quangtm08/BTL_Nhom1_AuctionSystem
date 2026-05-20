# Kế hoạch Di chuyển: Từ Singleton Connection sang Connection Pool (HikariCP)

## 0. Tại sao phải chuyển đổi? (Bối cảnh & Lý do)
Trong giai đoạn đầu của dự án, chúng ta sử dụng một kết nối đơn lẻ (`Singleton Connection`) để đơn giản hóa việc phát triển. Tuy nhiên, thiết kế này gặp hai giới hạn lớn:
1.  **An toàn JDBC (JDBC Thread-safety):** Đối tượng `Connection` của Java không an toàn khi dùng chung giữa các luồng. Nếu hai luồng cùng gửi lệnh SQL qua một "ống dẫn" duy nhất, giao thức sẽ bị lỗi và làm sập kết nối.
2.  **Hiệu suất:** Do phải dùng `synchronized` để bảo vệ "ống dẫn" này, hệ thống chỉ có thể xử lý từng truy vấn một. Khi có 10 người dùng cùng truy cập, người thứ 10 phải đợi cả 9 người trước đó xong việc, tạo ra độ trễ không đáng có.

**Mục tiêu:** Chuyển sang **Connection Pool** để mỗi yêu cầu có một "ống dẫn" riêng, cho phép PostgreSQL xử lý song song và tận dụng tối đa sức mạnh phần cứng.

---

## 1. Phân tích hiện trạng
- **Khởi tạo**: Lớp `DBConnection.java` quản lý một đối tượng `java.sql.Connection` tĩnh duy nhất.
- **Kết nối (Wiring)**: Đối tượng duy nhất này được truyền từ `ServerContext` xuống tất cả các Module, Service và Repository.
- **Đồng bộ hóa (Concurrency)**: Để tránh việc nhiều luồng làm hỏng trạng thái của kết nối, các Service đang sử dụng khối `synchronized(connection)`.
- **Giao dịch (Transactions)**: Các giao dịch được quản lý thủ công bằng cách gọi `setAutoCommit(false)` trên kết nối dùng chung này.

## 2. Các vấn đề của thiết kế hiện tại
1.  **Nghẽn cổ chai (Concurrency Bottleneck)**: Vì chỉ có một luồng có thể sử dụng cơ sở dữ liệu tại một thời điểm (do khóa `synchronized`), hiệu suất của máy chủ bị giới hạn. Việc thêm luồng vào `ExecutorService` không giúp ích vì tất cả đều phải đợi cùng một khóa DB.
2.  **Giao dịch thiếu ổn định**: Nếu một service quên đặt lại `autoCommit` hoặc không rollback đúng cách, kết nối sẽ rơi vào trạng thái "bẩn", gây ảnh hưởng đến tất cả các yêu cầu tiếp theo từ những người dùng khác.
3.  **Hết hạn kết nối (Timeouts)**: Nếu máy chủ DB (như PostgreSQL trên Railway) ngắt kết nối tạm thời, toàn bộ máy chủ sẽ lỗi cho đến khi khởi động lại, vì hiện tại không có cơ chế tự động kết nối lại hoặc kiểm tra sức khỏe kết nối.

## 3. Giải pháp: HikariCP Connection Pool
Chúng ta sẽ triển khai **HikariCP**, thư viện quản lý bể kết nối hiệu suất cao nhất hiện nay cho Java.

### Lợi ích
- **Song song hóa thực sự**: Nhiều luồng có thể thực thi các câu lệnh SQL cùng một lúc.
- **Tự động phục hồi**: Bể kết nối tự động phát hiện và thay thế các kết nối "chết".
- **Quản lý tài nguyên**: Các kết nối được mượn, sử dụng và trả lại bể, tránh rò rỉ tài nguyên.

### Cấu hình Dependency (pom.xml)
```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

---

## 4. Các bước triển khai chi tiết

### Giai đoạn A: Cập nhật hạ tầng (Infrastructure)
1.  **Chỉnh sửa `DBConnection.java`**:
    - Thay thế trường `Connection` tĩnh bằng `HikariDataSource`.
    - Cấu hình các tham số quan trọng: `maximumPoolSize` (gợi ý: 10), `idleTimeout`, `connectionTimeout`.
    - Thêm phương thức `getDataSource()`.
2.  **Cập nhật `ServerContext.java`**:
    - Thay vì giữ `Connection`, lớp này sẽ giữ `DataSource`.
    - Truyền `DataSource` vào các phương thức `init` của module thay vì Connection.

### Giai đoạn B: Tái cấu trúc Repository
Các Repository sẽ không còn "sở hữu" một kết nối cố định.
1.  **Thay đổi Constructor**: Nhận `DataSource` thay vì `Connection`.
2.  **Mô hình phương thức**: Mỗi phương thức phải tự lấy kết nối riêng bằng "try-with-resources" để đảm bảo kết nối được trả lại pool:
    ```java
    try (Connection conn = dataSource.getConnection();
         PreparedStatement ps = conn.prepareStatement(sql)) {
        // Thực thi logic SQL
    } catch (SQLException e) {
        throw new RuntimeException(e);
    }
    ```

### Giai đoạn C: Quản lý Giao dịch (Service Layer)
Vì các giao dịch yêu cầu nhiều lời gọi Repository phải dùng chung **cùng một** kết nối, chúng ta cần thay đổi cách xử lý ở tầng Service.

**Chiến lược: Truyền kết nối tường minh (Explicit Connection Passing)**
1.  Trong các phương thức Service cần giao dịch:
    - Mượn một `Connection` từ `DataSource`.
    - Gọi `setAutoCommit(false)`.
    - Truyền đối tượng `Connection` cụ thể này vào các phương thức của repository (ví dụ: `repo.save(data, connection)`).
2.  **Cập nhật Repository**: Bổ sung tham số `Connection` cho các phương thức được sử dụng trong giao dịch.
3.  `commit()` và đóng kết nối trong khối `finally`.

---

## 5. Danh sách các file bị ảnh hưởng

| Đường dẫn File | Loại thay đổi | Lý do |
| :--- | :--- | :--- |
| `DBConnection.java` | Cấu trúc | Thay Singleton `Connection` bằng `HikariDataSource`. |
| `ServerContext.java` | Kết nối | Cập nhật logic khởi tạo để truyền `DataSource`. |
| `*Module.java` | Interface | Sửa chữ ký hàm `init(...)` để nhận `DataSource`. |
| `*Repository.java` | Logic | Cập nhật constructor và bọc các lệnh SQL trong try-with-resources. |
| `AuctionService.java` | Giao dịch | Loại bỏ `synchronized`, triển khai quản lý giao dịch qua `DataSource`. |
| `DatabaseInitializer.java` | Mới | Tách logic tạo bảng ra khỏi Repository để chạy một lần duy nhất. |

---

## 6. Lưu ý quan trọng & Biện pháp an toàn (Phòng tránh lỗi)

### 1. Tránh "Rò rỉ kết nối" (Connection Leaks)
**Quy tắc**: Không bao giờ gọi `dataSource.getConnection()` mà không có khối `try-with-resources` hoặc khối `finally` để đóng kết nối. Nếu rò rỉ, pool sẽ cạn kiệt và máy chủ sẽ bị treo sau vài phút.

### 2. Thay thế `synchronized` bằng logic SQL (Atomic Updates)
Khi loại bỏ `synchronized`, bạn **phải** đảm bảo SQL đủ mạnh để tránh Race Condition:
- **Hành động**: Sử dụng điều kiện `WHERE` để kiểm tra trạng thái ngay tại DB:
  `UPDATE auctions SET current_highest_bid = ? WHERE id = ? AND current_highest_bid < ?`
- Nếu `executeUpdate()` trả về 0, nghĩa là đã có người khác đặt giá cao hơn -> Ném `ConflictException`.

### 3. Thứ tự khởi tạo
Các phương thức `ensureTable()` nên được chuyển sang lớp `DatabaseInitializer` riêng biệt, chạy một lần duy nhất trong `ServerContext` để tránh xung đột khi nhiều luồng cùng khởi tạo.

### 4. Kiểm thử (Testing)
- **Unit Tests**: Cần cập nhật Mockito để mock `DataSource` và `Connection`.
- **Parallel Testing**: Chạy ít nhất 2 Client cùng lúc, cùng nhấn "Đặt giá" để xác nhận hệ thống xử lý song song chính xác.
