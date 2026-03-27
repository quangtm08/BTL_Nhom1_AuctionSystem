1. Cấu hình môi trường

- Java SDK: Phiên bản 25.

- Maven: 3.9.14 (Sử dụng thông qua Maven Wrapper).

2. Cách khởi chạy dự án với Maven Wrapper

Để tránh xung đột, không cần cài đặt Maven thủ công. Sử dụng file thực thi mvnw có sẵn 
trong thư mục gốc:

- Trên Windows: Chạy lệnh mvnw.cmd [lệnh] (Ví dụ: mvnw.cmd clean install).

- Trên macOS/Linux: Chạy lệnh ./mvnw [lệnh] (Ví dụ: ./mvnw clean install).

**NOTE: Sau khi clone về, chạy clean install để cài hết dependencies**

3. Các thư viện sử dụng (Dependencies)

Hiện đang có:
- JavaFX 21
- JUnit 5.10.0: Để làm Unit Test

Toàn bộ dependencies ở ~/pom.xml

4. Cấu trúc cơ bản

Mã nguồn được tổ chức theo Client-Server và MVC:

src/main/java/com/nhom1/auction/
├── common/      # Chứa các lớp dùng chung (User, Item, Bid)
├── server/      # Logic phía Server
│   └── dao/     # Xử lý dữ liệu (Database/File)
└── client/      # Logic phía Client
└── controller/ # Điều khiển giao diện JavaFX

src/main/resources/
├── views/       # Chứa các file giao diện .fxml
└── css/         # Chứa file style.css để làm đẹp UI

5. Styling
Trước khi commit code, nhấn Ctrl + Alt + L để IDE tự format lại code thêm 1 lần.