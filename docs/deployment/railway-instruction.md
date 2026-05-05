# Hướng dẫn Triển khai Auction Server lên Railway

Tài liệu này hướng dẫn cách cấu trúc và vận hành hệ thống Auction Server trên nền tảng Railway, hỗ trợ tự động chuyển đổi giữa môi trường phát triển (Local) và môi trường thực tế (Cloud).

## 1. Cơ chế Môi trường kép (Dual-Environment)

Hệ thống được thiết kế để tự nhận diện môi trường chạy dựa trên các biến môi trường (Environment Variables).

### Cơ sở dữ liệu (DBConnection.java)
- **Local:** Nếu không tìm thấy các biến cấu hình PostgreSQL, hệ thống tự động sử dụng **SQLite** (`database/auction_system.db`).
- **Cloud (Railway):** Khi triển khai, hệ thống sẽ ưu tiên sử dụng các biến môi trường `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` để kết nối tới **PostgreSQL**.

### Cổng kết nối (Server.java)
- **Local:** Mặc định sử dụng cổng `12345`.
- **Cloud:** Tự động đọc biến `PORT` do Railway cung cấp để thực hiện Dynamic Port Binding.

## 2. Cấu hình Build & Thực thi

### Fat JAR (pom.xml)
Dự án sử dụng `maven-shade-plugin` để đóng gói toàn bộ code và dependencies vào một file duy nhất gọi là **Fat JAR** tại `target/auction-app-1.0-SNAPSHOT.jar`. 
- Railway sẽ tự động chạy lệnh `./mvnw package` mỗi khi có thay đổi code trên GitHub.

### Procfile
File `Procfile` tại thư mục gốc khai báo lệnh khởi chạy cho Railway:
```text
web: java -jar target/auction-app-1.0-SNAPSHOT.jar
```

## 3. Cấu hình trên Railway Dashboard

Để server hoạt động, cần thực hiện các bước sau trên Dashboard:

1.  **Thêm PostgreSQL:** Tạo một service PostgreSQL trong project.
2.  **Biến môi trường:** Railway sẽ tự động liên kết các biến `PG...` vào Server service.
3.  **TCP Proxy (Quan trọng):**
    - Vào phần **Settings** -> **Networking**.
    - Tạo một **TCP Proxy**.
    - Sử dụng địa chỉ Proxy này (ví dụ: `hopper.proxy.rlwy.net:16743`) để cấu hình Client.

## 4. Kết nối từ Client (ServerConnection.java)

Trong code Client, class `ServerConnection` hỗ trợ chuyển đổi nhanh giữa Local và Cloud:

```java
// String host = "localhost"; int port = 12345; // CHẾ ĐỘ LOCAL
String host = "hopper.proxy.rlwy.net"; int port = 16743; // CHẾ ĐỘ CLOUD (Railway)
```

- Khi làm việc nhóm hoặc test local, hãy bỏ comment dòng **LOCAL**.
- Khi muốn kết nối tới server thực tế, hãy sử dụng dòng **CLOUD**.

## 5. Lưu ý về Java Version & DB Compatibility
- Dự án sử dụng **JDK 21** (LTS) để đảm bảo tương thích tốt nhất với môi trường xây dựng của Railway.
- **Quan trọng:** PostgreSQL khắt khe hơn SQLite về kiểu dữ liệu. Luôn sử dụng `ps.setTimestamp()` và `rs.getTimestamp()` thay vì xử lý chuỗi (String) cho các cột thời gian (`created_at`, `end_time`, v.v.). Điều này đảm bảo code chạy tốt trên cả SQLite (Local) và PostgreSQL (Railway).
- Tránh sử dụng cú pháp `INSERT OR REPLACE` của SQLite; hãy chuyển sang chuẩn SQL hoặc sử dụng `ON CONFLICT` cho PostgreSQL.

---
*Chúc may mắn với phiên bản Cloud đầu tiên của bạn!*
