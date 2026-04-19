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

Dự án sử dụng Maven. Đảm bảo bạn đã cài đặt JDK 25.

### 1. Chạy Server
Mở terminal và chạy lệnh:
```bash
mvn exec:java -Dexec.mainClass="com.nhom1.auction.server.Server"
```

### 2. Chạy Client
Mở terminal mới và chạy lệnh:
```bash
mvn javafx:run
```

## 📝 Tài liệu tham khảo
Chi tiết về các thay đổi kiến trúc và sơ đồ ánh xạ UI có thể xem tại:
- `docs/project-structure-refactor.md`
- `docs/assignment-requirement.md`

---
*Dự án đang trong quá trình phát triển (Tuần 7).*
