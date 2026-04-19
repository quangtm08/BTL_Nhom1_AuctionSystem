# Hướng dẫn Tái cấu trúc Hệ thống: Giao diện & Kiến trúc

## 🎯 Mục đích
Chuẩn hóa cách đặt tên, tách biệt rạch ròi giữa **Client** và **Server**, đồng thời bảo mật thông tin cơ sở dữ liệu (Database).

## 🏗️ Hệ thống "Shell" & Điều hướng (Client)
Hợp nhất logic điều hướng và khung giao diện (Shell) vào một lớp duy nhất.
- **Interface:** `com.nhom1.auction.common.classes.BaseShellController`
- **Lớp thực thi:** `com.nhom1.auction.client.ShellController`
- **FXML:** `views/main.fxml` (Khung chứa tất cả các màn hình khác)

## 📁 Tách biệt Client - Server 
Logic xử lý dữ liệu và kết nối DB đã được chuyển từ `common` về đúng vị trí **Server**.
- **Database:** `com.nhom1.auction.server.database.DBConnection`
- **Service:** `com.nhom1.auction.server.service.AuthService`
- **Lý do:** Client chỉ được phép gửi yêu cầu qua Socket. Việc để DB ở `common` là lỗ hổng bảo mật.

## 🏷️ Quy chuẩn Đặt tên UI
Tên gọi được đồng bộ hóa giữa file `.java`, `.fxml`, và `.css` theo hướng mô tả **Tính năng**.

### Phân hệ Người dùng (User)
- **Browse:** `AuctionBrowseController` ↔ `auction_browse.fxml`
- **My Bids:** `MyBidsController` ↔ `my_bids.fxml`
- **My Listings:** `MyListingsController` ↔ `my_listings.fxml`
- **Detail:** `AuctionDetailController` ↔ `auction_detail.fxml`
- **Sidebar:** `UserSidebarController` ↔ `user_sidebar.fxml`

### Phân hệ Quản trị (Admin)
- **Overview:** `AdminOverviewController` ↔ `admin_overview.fxml`
- **Users:** `UserManagementController` ↔ `user_management.fxml`
- **Auctions:** `AuctionManagementController` ↔ `auction_management.fxml`
- **Sidebar:** `AdminSidebarController` ↔ `admin_sidebar.fxml`

## 🧭 Cập nhật AppView
Enum `AppView` đã được đổi tên hằng số để khớp với logic mới:
- `EXPLORE` → `AUCTION_BROWSE`
- `BIDS` → `MY_BIDS`
- `LISTINGS` → `MY_LISTINGS`
- `MAIN_DASHBOARD` → `ADMIN_OVERVIEW`
- `LIVE_AUCTION_BID` → `AUCTION_DETAIL`

---
*Lưu ý: Logic nghiệp vụ không bị thay đổi.*
