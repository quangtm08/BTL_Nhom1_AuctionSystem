# Tài liệu Kỹ thuật: Giải pháp Toàn diện cho Tính năng Autobid

## 1. Phân tích vấn đề hiện tại
Hệ thống đấu giá tự động (Autobid) hiện gặp phải các giới hạn nghiêm trọng về hiệu năng và an toàn dữ liệu do thiết kế đệ quy đồng bộ trên luồng chính.

### Các hạn chế chính:
*   **Chặn luồng (Blocking):** Luồng chính xử lý yêu cầu của người dùng bị giữ lại cho đến khi toàn bộ chuỗi autobid kết thúc.
*   **Lỗi bộ nhớ (Stack Overflow):** Việc gọi đệ quy sâu (tối đa 20 cấp) gây rủi ro tràn bộ nhớ stack.
*   **Xung đột luồng (Thread Safety):** Việc sử dụng một kết nối duy nhất (Singleton Connection) mà không có cơ chế đồng bộ hóa dẫn đến việc các luồng lệnh SQL bị xáo trộn hoặc chồng chéo trạng thái giao dịch (transaction state).
*   **Giữ khóa quá lâu:** Khóa dữ liệu được giữ xuyên suốt quá trình đệ quy, gây tắc nghẽn cho các yêu cầu đồng thời khác.

## 2. Giải pháp kỹ thuật toàn diện

### 2.1. Xử lý Bất đồng bộ bằng Luồng chạy ngầm (Daemon Thread)
*   **Cơ chế:** Khi người dùng đặt giá thành công, hệ thống gửi phản hồi ngay lập tức. Sau đó, một luồng chạy ngầm (Daemon Thread) hoặc ExecutorService được kích hoạt để xử lý logic autobid.
*   **Lợi ích:** Giải phóng luồng chính, tối ưu hóa thời gian phản hồi (Latency) cho người dùng.

### 2.2. Chuyển đổi sang logic Vòng lặp (Iterative Logic)
*   **Cơ chế:** Thay thế đệ quy bằng vòng lặp `while` với giới hạn số lần thực hiện (MAX_DEPTH = 20).
*   **Lợi ích:** Loại bỏ rủi ro Stack Overflow, quản lý bộ nhớ ổn định hơn.

### 2.3. Đảm bảo an toàn cho Singleton Connection
*   **Cơ chế:** Do sử dụng một kết nối duy nhất cho toàn bộ hệ thống, mọi thao tác truy cập cơ sở dữ liệu phải được bọc trong khối **`synchronized(connection)`**.
*   **Lợi ích:** Đảm bảo tính toàn vẹn của "ống dẫn" lệnh SQL, tránh việc nhiều luồng gửi lệnh đồng thời làm hỏng trạng thái của Connection.

### 2.4. Quản lý Giao dịch và Khóa dòng (Pessimistic Locking)
*   **Cơ chế:** 
    *   Sử dụng cú pháp **`SELECT ... FOR UPDATE`** (trong PostgreSQL) để khóa hàng dữ liệu của phiên đấu giá ngay khi bắt đầu một lượt bid.
    *   Tắt chế độ `autoCommit` và thực hiện `commit()` thủ công sau mỗi lượt bid thành công trong vòng lặp.
*   **Lợi ích:** Ngăn chặn việc nhiều luồng (Human và Bot) cập nhật cùng một giá trị giá cao nhất tại một thời điểm, đảm bảo tính nhất quán tuyệt đối của dữ liệu.

## 3. Quy trình thực hiện (Implementation Workflow)

1.  **Giai đoạn đặt bid thủ công (Main Thread):**
    *   Nhận yêu cầu -> `synchronized(connection)` -> Bắt đầu Transaction -> `SELECT FOR UPDATE` -> Ghi Bid -> `commit()` -> Trả kết quả thành công cho người dùng.

2.  **Giai đoạn Autobid (Daemon Thread):**
    *   Khởi chạy luồng ngầm.
    *   Chạy vòng lặp `while` (tối đa 20 lần):
        *   `synchronized(connection)` -> Bắt đầu Transaction mới.
        *   Tìm bot đủ điều kiện và thực hiện đặt giá (sử dụng `FOR UPDATE`).
        *   Nếu thành công -> `commit()`.
        *   Nếu thất bại hoặc không còn bot -> `rollback()` và kết thúc vòng lặp.
    *   Giải phóng Lock và kết thúc luồng ngầm.

## 4. Danh sách các thay đổi mã nguồn (Action Items)
*   **DBConnection.java:** Thêm `synchronized` cho phương thức `getConnection()`.
*   **BidHandler.java:** Bọc lời gọi `triggerAutoBids` trong `CompletableFuture.runAsync` hoặc `new Thread(...).start()`.
*   **AutoBidService.java:** Thay thế logic đệ quy trong `triggerAutoBidsInternal` bằng vòng lặp `while`.
*   **Repository Layers:** Đảm bảo mọi câu lệnh UPDATE đều nằm trong block `synchronized` và có cơ chế quản lý Transaction (`commit/rollback`) rõ ràng.
