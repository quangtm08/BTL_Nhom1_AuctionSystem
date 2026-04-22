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

## 5. Trạng thái hiện tại (Current State)

*   **Domain Model:** Đã hoàn thiện cấu trúc cơ bản cho các thực thể (Art, Electronics, Vehicle, Auction, BidTransaction,...) và logic tính toán (Money, TimeRange).
*   **UI/UX:** Đã thiết kế xong các màn hình chính (Sign In, Register, Dashboard,...).
*   **Client Logic:** Đã triển khai khung điều hướng (Navigation) và tích hợp FXML/CSS.
*   **Server:** Đang trong quá trình phát triển (Pending).
*   **Database:** Đã tích hợp MySQL Driver, cấu hình schema đang được thực hiện.
