# AuctionSystem - Hệ thống Đấu giá Trực tuyến

Dự án môn Lập trình nâng cao (Advanced Programming) - Nhóm 1.

## 1. Cấu hình môi trường

*   **Java SDK:** Phiên bản 25.
*   **Maven:** 3.9.14 (Sử dụng thông qua Maven Wrapper đính kèm).
*   **Hệ quản trị CSDL:** MySQL (Connector version 8.3.0).

## 2. Cách khởi chạy dự án

Sử dụng file thực thi `mvnw` (Maven Wrapper) có sẵn trong thư mục gốc để đảm bảo tính nhất quán:

*   **Cài đặt dependencies:**
    *   Windows: `.\mvnw.cmd clean install`
    *   macOS/Linux: `./mvnw clean install`
*   **Chạy ứng dụng (Client):**
    *   Windows: `.\mvnw.cmd javafx:run`
    *   macOS/Linux: `./mvnw javafx:run`

## 3. Các thư viện sử dụng (Dependencies)

*   **JavaFX 25:** Thư viện xây dựng giao diện người dùng.
*   **JUnit 5.10.0:** Công cụ thực hiện Unit Test.
*   **MySQL Connector/J 8.3.0:** Driver kết nối cơ sở dữ liệu MySQL.

Chi tiết cấu hình xem tại `pom.xml`.

## 4. Cấu trúc dự án (Current Project Structure)

Mã nguồn được tổ chức theo mô hình Client-Server và áp dụng các mẫu thiết kế (Design Patterns):

```text
BTL_AuctionSystem/
├── design/                 # Chứa các file thiết kế giao diện (UI/UX)
├── docs/                   # Tài liệu yêu cầu bài tập lớn và hướng dẫn
├── src/
│   ├── main/
│   │   ├── java/com/nhom1/auction/
│   │   │   ├── client/     # Logic phía người dùng (JavaFX)
│   │   │   │   ├── Controller/    # Các lớp điều khiển giao diện (MVC)
│   │   │   │   ├── AppAssets.java # Quản lý tài nguyên (Fonts, Images)
│   │   │   │   ├── AppNavigator.java # Điều hướng giữa các View
│   │   │   │   └── ClientApplication.java # Entry point của ứng dụng
│   │   │   └── common/     # Các thành phần dùng chung (Shared logic)
│   │   │       ├── entity/    # Các thực thể domain (Item, Auction, User,...)
│   │   │       ├── enums/     # Các kiểu liệt kê (AuctionStatus, BidType,...)
│   │   │       ├── exception/ # Các lớp ngoại lệ tùy chỉnh
│   │   │       ├── factory/   # Áp dụng Factory Pattern (ví dụ: ItemFactory)
│   │   │       └── value/     # Các đối tượng giá trị (Money, TimeRange)
│   │   └── resources/
│   │       ├── assets/     # Fonts và hình ảnh tĩnh
│   │       ├── css/        # Các file định dạng giao diện
│   │       └── views/      # Các file giao diện FXML
│   └── test/               # Unit tests (JUnit 5)
```

## 5. Trạng thái hiện tại (Current State)

*   **Domain Model:** Đã hoàn thiện cấu trúc cơ bản cho các thực thể (Art, Electronics, Vehicle, Auction, BidTransaction,...) và logic tính toán (Money, TimeRange).
*   **UI/UX:** Đã thiết kế xong các màn hình chính (Sign In, Register, Dashboard,...).
*   **Client Logic:** Đã triển khai khung điều hướng (Navigation) và tích hợp FXML/CSS.
*   **Server:** Đang trong quá trình phát triển (Pending).
*   **Database:** Đã tích hợp MySQL Driver, cấu hình schema đang được thực hiện.