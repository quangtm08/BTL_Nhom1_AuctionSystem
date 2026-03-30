1. Cấu hình môi trường

- Java SDK: Phiên bản 25.

- Maven: 3.9.14 (Sử dụng thông qua Maven Wrapper).

2. Cách khởi chạy dự án với Maven Wrapper

Để tránh xung đột, không cần cài đặt Maven thủ công. Sử dụng file thực thi mvnw có sẵn 
trong thư mục gốc:

- Trên Windows: Chạy lệnh mvnw.cmd [lệnh] (Ví dụ: .\mvnw.cmd clean install).

- Trên macOS/Linux: Chạy lệnh ./mvnw [lệnh] (Ví dụ: ./mvnw clean install).

**NOTE: Sau khi clone về, chạy clean install để cài hết dependencies**

3. Các thư viện sử dụng (Dependencies)

Hiện đang có:
- JavaFX 21
- JUnit 5.10.0: Để làm Unit Test

Toàn bộ dependencies ở ~/pom.xml

4. Cấu trúc cơ bản

Mã nguồn được tổ chức theo Client-Server và MVC:

src/main/java/com/nhom1/auction/common: Chứa các lớp thực thể (User, Item) dùng chung cho cả Client và Server.

src/main/java/com/nhom1/auction/server: Chứa logic xử lý của Server và package dao để quản lý dữ liệu.

src/main/java/com/nhom1/auction/client: Chứa logic phía người dùng và package controller để điều khiển giao diện.

src/main/resources: Chứa các file giao diện (.fxml) và file định dạng (.css).

5. Styling

Trước khi commit code, nhấn Ctrl + Alt + L để IDE tự format lại code thêm 1 lần.