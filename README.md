# BTL_AuctionSystem - Nhóm 1

Hệ thống đấu giá trực tuyến xây dựng trên nền tảng JavaFX (Client) và Socket (Server). Dự án đã được tái cấu trúc để chuẩn hóa quy trình đặt tên và tăng cường bảo mật kiến trúc.

## 🏗️ Kiến trúc Hệ thống

Dự án tuân thủ mô hình Client-Server rạch ròi:
- **Client**: Giao diện người dùng JavaFX, giao tiếp với Server qua Socket.
- **Server**: Xử lý logic nghiệp vụ, quản lý phiên đấu giá và kết nối trực tiếp cơ sở dữ liệu (SQLite/MySQL).
- **Common**: Chứa các Entity (Item, Auction, User), Enum và các lớp tiện ích dùng chung cho cả hai phía.

## 📁 Cấu trúc Thư mục Quan trọng

```text
src/main/java/com/nhom1/auction/
├── client/                 # Logic phía người dùng
│   ├── controller/         # Các Controller xử lý UI (đã chuẩn hóa tên)
│   ├── connection/         # Quản lý kết nối Socket tới Server
│   ├── ShellController     # Khung giao diện chính (Unified Shell)
│   └── ClientApplication   # Điểm chạy ứng dụng Client
├── server/                 # Logic phía máy chủ
│   ├── service/            # Xử lý nghiệp vụ (AuthService, v.v.)
│   ├── database/           # Kết nối DB (DBConnection) - Chỉ Server có quyền truy cập
│   └── Server              # Điểm chạy ứng dụng Server
└── common/                 # Code dùng chung
    ├── entity/             # Các đối tượng dữ liệu (Art, Vehicle, Auction...)
    ├── enums/              # AuctionStatus, UserRole...
    └── BaseShellController # Interface khung giao diện
```

## 🏷️ Quy chuẩn Đặt tên UI mới
Các Controller và View hiện đã được đặt tên theo **Tính năng**:
- `AuctionBrowseController` (Duyệt đấu giá)
- `MyBidsController` (Đấu giá của tôi)
- `MyListingsController` (Tin đăng của tôi)
- `AuctionDetailController` (Chi tiết & Đặt giá)
- `AdminOverviewController` (Tổng quan quản trị)

## 🚀 Hướng dẫn Chạy ứng dụng

Dự án sử dụng Maven. Đảm bảo bạn đã cài đặt JDK 21 (hoặc mới hơn).

### 1. Chạy Server
Mở terminal và chạy lệnh:
```bash
mvn exec:java -Dexec.mainClass="com.nhom1.auction.server.Server"
```

```text
BTL_AuctionSystem/
├── .github/                # Cấu hình CI/CD, workflow GitHub Actions
├── .idea/                  # Cấu hình IDE IntelliJ (không quan trọng khi deploy)
├── .mvn/                   # Wrapper Maven
├── .vscode/                # Cấu hình VS Code
├── database/               # Script cơ sở dữ liệu (SQL, schema)
├── design/                 # File thiết kế giao diện (Figma, ảnh mockup)
├── docs/                   # Tài liệu đặc tả, yêu cầu bài toán
├── src/
│   ├── main/
│   │   ├── java/com/nhom1/auction/
│   │   │   ├── client/                 # Phía client (JavaFX - giao diện)
│   │   │   │   ├── controller/         # Điều khiển UI (MVC Controller)
│   │   │   │   ├── AppAssets.java      # Quản lý tài nguyên (ảnh, font,…)
│   │   │   │   ├── AppNavigator.java   # Điều hướng giữa các màn hình
│   │   │   │   └── ClientApplication.java # Entry point của ứng dụng client
│   │   │   │
│   │   │   ├── common/                 # Thành phần dùng chung (shared)
│   │   │   │   ├── entity/             # Thực thể (User, Auction, Item,...)
│   │   │   │   ├── enums/              # Kiểu liệt kê (Status, Role,...)
│   │   │   │   ├── exception/          # Exception tùy chỉnh
│   │   │   │   ├── factory/            # Factory Pattern tạo object
│   │   │   │   ├── observer/           # Observer Pattern (real-time update)
│   │   │   │   ├── value/              # Value Object (Money, TimeRange,...)
│   │   │   │   └── utils/              #  Các lớp tiện ích (helper functions)
│   │   │   │
│   │   │   └── server/                 # Backend xử lý nghiệp vụ
│   │   │       ├── controller/         # Xử lý request từ client
│   │   │       ├── service/            # Business logic
│   │   │       ├── repository/         # Truy cập dữ liệu (DB)
│   │   │       └── ServerApplication.java # Entry point server
│   │   │
│   │   └── resources/
│   │       ├── assets/                 # Ảnh, font
│   │       ├── css/                    # File style JavaFX
│   │       └── views/                  # File FXML giao diện
│   │
│   └── test/                           # Unit test (JUnit)
├── target/                             # File build (auto sinh)
├── .gitignore                          # Bỏ qua file không cần commit
├── mvnw / mvnw.cmd                     # Maven wrapper
├── nbactions.xml                       # Config NetBeans (nếu dùng)
├── pom.xml                             # Cấu hình Maven (dependency, build)
└── README.md                           # Mô tả project
```

## 📝 Tài liệu tham khảo
Chi tiết về các thay đổi kiến trúc và sơ đồ ánh xạ UI có thể xem tại:
- `docs/project-structure-refactor.md`
- `docs/assignment-requirement.md`

---
*Dự án đang trong quá trình phát triển (Tuần 7).*
