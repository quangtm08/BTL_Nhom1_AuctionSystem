# BTL Auction System — Nhóm 1

## 1. Mô tả bài toán & phạm vi

Hệ thống đấu giá trực tuyến nhiều người dùng đồng thời, giao tiếp client-server bằng **TCP socket** và **JSON** (Jackson). Client **JavaFX** chỉ gửi/nhận message qua socket; server xử lý toàn bộ nghiệp vụ (xác thực, đấu giá, đặt giá, thanh toán, auto-bid, lịch tự động, quản trị) và truy cập CSDL. Mặc định khi deploy trên Railway, server dùng **PostgreSQL**; **SQLite** trong thư mục `database/`  là fallback khi chạy local hoặc không kết nối được Postgres Server.

Phạm vi: đăng ký / đăng nhập, duyệt phiên đấu giá, đặt giá thủ công + tự động (auto-bid), thanh toán mock, quản trị hệ thống, thông báo realtime qua socket.

## 2. Công nghệ & yêu cầu cài đặt

| Hạng mục | Giá trị |
|---------|---------|
| Ngôn ngữ | Java 21 (LTS) |
| UI | JavaFX 21 (FXML + CSS) |
| Build | Maven (kèm `mvnw` / `mvnw.cmd`) |
| Giao thức | TCP socket, JSON (Jackson + JSR-310) |
| CSDL | PostgreSQL trên Railway; SQLite fallback local (`jdbc:sqlite:database/auction-system.db`, WAL) |
| Connection pool | HikariCP |
| Test | JUnit 5, Mockito |
| Coverage | JaCoCo |
| Định dạng | Spotless + Google Java Format |
| Packaging | Maven Shade Plugin tạo 2 fat JAR server/client |

**Yêu cầu môi trường:**
- **JDK 21** trở lên (đặt `JAVA_HOME` đúng).
- Kết nối Internet **nếu chạy theo Kịch bản A** (cloud server Railway + PostgreSQL).
- Không cần cài thêm JavaFX riêng — JAR client đã đóng gói sẵn native libs cho **Windows / macOS / Linux**.
- Nếu tự chạy server local với PostgreSQL, cần cấu hình `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`. Nếu thiếu `PGHOST`, server tự dùng SQLite fallback.

## 3. Cấu trúc module chính (`src/main/java/com/nhom1/auction/`)

```text
client/
├── ClientApplication.java      # Entry JavaFX
├── ClientLauncher.java         # Main wrapper cho fat JAR
├── ShellController.java, AppNavigator.java, AppView.java, AppAssets.java
├── service/                    # ClientPushService nhận realtime push
├── util/                       # Formatter, countdown, feedback, skeleton UI
├── user/
│   ├── controller/             # Đăng nhập, duyệt, chi tiết, đấu giá, listing, thanh toán
│   ├── connection/             # ServerConnection (socket + JSON, tự fallback local)
│   └── service/                # *ClientService gọi server
└── admin/
    ├── controller/             # Admin overview, auction & user management
    └── service/                # AdminClientService

server/
├── Server.java                 # main — mở ServerSocket, dùng PORT nếu có, fallback 12345
├── infrastructure/             # ClientHandler, ClientRegistry, MessageRouter, ServerContext
├── infrastructure/database/    # DBConnection, DatabaseInitializer
├── auth/                       # AuthModule, AuthHandler, AuthService, UserRepository
├── auction/                    # Phiên đấu giá, item
├── bidding/                    # Đặt giá, BidService
├── payment/                    # PaymentHandler, PaymentService, PaymentRepository
├── wallet/                     # Ví và lịch sử giao dịch
├── admin/                      # Thao tác quản trị
└── automation/                 # Auto-bid, scheduler, gateway

common/
├── entity/, enums/, exception/, factory/, utils/
├── dto/                        # auth, auction, bidding, admin, payment, autobid, notification, wallet
└── protocol/                   # MessageType, RequestMessage, ResponseMessage
```

Tài nguyên UI: `src/main/resources/views/` (FXML), `css/`, `assets/`.
CSDL local fallback: `database/auction-system.db` (cùng các file `-wal`, `-shm` khi đang chạy). Chạy server từ thư mục gốc repo để JDBC path khớp.

## 4. Vị trí file JAR

Sau khi build (`./mvnw -q -DskipTests package`):

| File | Vai trò | Main class |
|------|---------|------------|
| `target/auction-server.jar` | Server fat JAR (chạy local) | `com.nhom1.auction.server.Server` |
| `target/auction-client.jar` | Client fat JAR (đa nền tảng, kèm native JavaFX win/mac/linux) | `com.nhom1.auction.client.ClientLauncher` |

Lệnh build:

```bash
./mvnw -q -DskipTests package          # Linux / macOS
.\mvnw.cmd -q -DskipTests package      # Windows PowerShell
```

## 5. Hướng dẫn chạy Server / Client

### Kịch bản A — Cloud server (mặc định, khuyến nghị grader)

Server đã deploy sẵn trên Railway (`kodama.proxy.rlwy.net:49734`). **Chỉ cần chạy client**:

```bash
java -jar target/auction-client.jar
```

Client tự kết nối cloud. Nếu cloud không truy cập được, client tự fallback sang `localhost:12345`.

### Kịch bản B — Chạy local server + client

Thứ tự: **server trước, client sau**.

Terminal 1 (server):
```bash
java -jar target/auction-server.jar
```

Terminal 2 (client):
```bash
java -jar target/auction-client.jar
```

### Chế độ dev (tùy chọn)

```bash
./mvnw -q exec:java -Dexec.mainClass="com.nhom1.auction.server.Server"   # server
./mvnw -q javafx:run                                                      # client
./mvnw -q test                                                            # test
```

## 6. Danh sách chức năng đã hoàn thành

- **Auth & phân quyền**: đăng ký, đăng nhập, phân vai trò user / admin.
- **Đấu giá**: duyệt danh sách phiên, xem chi tiết, đặt giá thủ công với optimistic locking chống race condition.
- **Listing**: tạo / quản lý tin đăng đấu giá của người dùng.
- **Auto-bid**: đặt giá tự động theo ngưỡng + hybrid escalation, lịch chạy tự động qua `AuctionScheduler`.
- **Thanh toán**: luồng payment mock với cập nhật ví realtime hai chiều client–server.
- **Admin**: tổng quan hệ thống, quản lý phiên đấu giá, quản lý người dùng.
- **Realtime notification**: server đẩy sự kiện qua socket tới client liên quan.

## 7. Báo cáo PDF & video demo

Google Drive (chứa cả PDF báo cáo + video demo):

<https://drive.google.com/drive/folders/1RezNDeNXlE1QBGB51JY2uxZq178qgrW0?usp=sharing>

## 8. Tài liệu kỹ thuật trong repo

- Yêu cầu bài: `docs/requirements/assignment-requirement.md`
- Giao tiếp client–server: `docs/architecture/client-server-communication.md`
- Schema DB: `docs/architecture/database-schema.md`
- Deploy Railway: `docs/deployment/railway-instruction.md`
- Xử lý exception: `docs/architecture/exception-handling.md`
- Connection pool: `docs/architecture/thuc-thi-connection-pool.md`
- UI standard: `docs/guidelines/ui-standards.md`
- Module theo thành viên / sprint: `docs/modules/`, `docs/sprints/`
