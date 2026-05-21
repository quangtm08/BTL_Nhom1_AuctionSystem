# BTL Auction System — Nhóm 1

Ứng dụng đấu giá: **JavaFX (client)** giao tiếp với **máy chủ TCP** bằng **JSON** (Jackson). Server gom nghiệp vụ, router tin nhắn và truy cập **SQLite** (file trong thư mục `database/`). Client không kết nối DB trực tiếp.

## Kiến trúc tóm tắt

| Thành phần | Vai trò |
|------------|---------|
| **Client** | Giao diện FXML/CSS, điều hướng (`AppNavigator`), kết nối socket qua `ServerConnection`, DTO dùng chung từ `common`. |
| **Server** | `ServerSocket` (mặc định cổng **12345**), mỗi client một luồng (`ClientHandler`), định tuyến `MessageType` qua `MessageRouter` và các module (auth, auction, bidding, admin, automation). |
| **Common** | Entity, enum, DTO, protocol (`RequestMessage` / `ResponseMessage`), exception, value object, observer. |

Giao thức: client và server trao đổi theo dòng JSON; một số sự kiện được đẩy từ server (notification / realtime tùy luồng).

## Công nghệ

- **Java 21** **JavaFX 21**
- **Maven** (`mvnw` / `mvnw.cmd`)
- **SQLite** — `jdbc:sqlite:database/auction-system.db` (WAL, busy timeout; xem `DBConnection`)
- **Jackson** + JSR-310 cho `LocalDateTime`
- **JUnit 5** cho unit test
- **MySQL connector** có trong `pom.xml` (phục vụ mở rộng); triển khai hiện tại dùng SQLite

## Tính năng chính (mức tổng quan)

- Đăng ký / đăng nhập, phân quyền người dùng
- Duyệt phiên đấu giá, chi tiết, đặt giá
- Tin đăng của tôi, lịch sử đấu giá
- Thanh toán (luồng payment trên client/server)
- **Auto-bid** và lịch tự động (`AuctionScheduler` / automation)
- **Admin**: tổng quan, quản lý phiên đấu giá, quản lý người dùng

## Cấu trúc mã nguồn (`src/main/java/com/nhom1/auction/`)

```text
client/
├── ClientApplication.java      # Entry JavaFX
├── ShellController.java        # Khung shell chung
├── AppNavigator.java, AppView.java, AppAssets.java, BaseShellController.java
├── user/
│   ├── Controller/             # Đăng nhập, duyệt, chi tiết, đấu giá, listing, thanh toán, ...
│   ├── Connection/             # ServerConnection (socket + JSON)
│   └── service/                # *ClientService gọi server
└── admin/
    ├── controller/             # Admin overview, auction management, user management, ...
    └── service/                # AdminClientService

server/
├── Server.java                 # main — lắng nghe cổng 12345
├── infrastructure/             # ClientHandler, MessageRouter, ServerContext, ClientRegistry, NotificationService, database/DBConnection
├── auth/                       # AuthModule, AuthHandler, AuthService, UserRepository
├── auction/                    # Phiên đấu giá, item
├── bidding/                    # Đặt giá, BidService
├── admin/                      # Thao tác quản trị
└── automation/                 # Auto-bid, scheduler, gateway đấu giá

common/
├── entity/, enums/, exception/, factory/, observer/, value/, utils/
├── dto/                        # auth, auction, bidding, admin, payment, autobid, notification
└── protocol/                   # MessageType, RequestMessage, ResponseMessage, ...
```

**Tài nguyên UI:** `src/main/resources/` — `views/` (FXML), `css/`, `assets/`.

**Cơ sở dữ liệu:** file SQLite trong `database/` (có thể có `.db`, `-wal`, `-shm` khi đang chạy). Chạy server từ **thư mục gốc repo** để đường dẫn `database/auction-system.db` khớp với JDBC.

## Chạy ứng dụng

Yêu cầu **JDK 21 hoặc mới hơn**. Thứ tự: **server trước**, **client sau** (client kết nối `localhost:12345`).

Dự án sử dụng Maven. Đảm bảo bạn đã cài đặt JDK 21 (hoặc mới hơn).

```bash
./mvnw -q exec:java -Dexec.mainClass="com.nhom1.auction.server.Server"
```

Windows (PowerShell):

```powershell
.\mvnw.cmd -q exec:java "-Dexec.mainClass=com.nhom1.auction.server.Server"
```

**Client (JavaFX)**

```bash
./mvnw -q javafx:run
```

Hoặc chạy class `com.nhom1.auction.client.ClientApplication` từ IDE.

**Chạy test**

```bash
./mvnw -q test
```

GitHub Actions (nhánh `main`): build và test bằng Maven wrapper (JDK 21 Temurin).

## Tài liệu trong repo

- Yêu cầu bài: `docs/requirements/assignment-requirement.md`
- Giao tiếp client–server: `docs/architecture/client-server-communication.md`
- Schema / DB: `docs/architecture/database-schema.md`
- Xử lý exception: `docs/architecture/exception-handling.md`
- UI: `docs/guidelines/ui-standards.md`
- Module theo thành viên / sprint: `docs/modules/`, `docs/sprints/`

---
Dự án phát triển trong khuôn khổ BTL; chi tiết thay đổi kiến trúc nên cập nhật song song trong thư mục `docs/`.
