# 🌐 Socket Client-Server Module (Java)

## 1. Giới thiệu

Module này xây dựng hệ thống giao tiếp giữa **Client và Server sử dụng Socket (TCP)**.

Hệ thống gồm:
- Server: lắng nghe và xử lý nhiều client
- Client: gửi dữ liệu và nhận phản hồi

---

## 2. Kiến trúc tổng thể

Client kết nối tới Server thông qua TCP Socket.

Luồng hoạt động:

Client → Server → Xử lý → Response → Client

---

## 3. Cấu trúc project

com/nhom1/auction/
- server/
  - Server.java
  - ClientHandler.java
- client/
  - SimpleClient.java

---

## 4. Giải thích chi tiết từng class

---

### 4.1 Server.java – Khởi tạo Server

#### Mục đích
- Tạo server socket
- Lắng nghe kết nối từ client
- Tạo thread xử lý từng client

#### Cách hoạt động

Server mở cổng 12345:

ServerSocket serverSocket = new ServerSocket(port);

Sau đó chạy vòng lặp vô hạn để chờ client:

while (true) {
    Socket socket = serverSocket.accept();
}

Mỗi khi có client kết nối:
- Server nhận socket
- Tạo một luồng xử lý riêng

#### Xử lý đa luồng

Server dùng thread pool:

ExecutorService pool = Executors.newFixedThreadPool(10);

Sau đó:

pool.execute(new ClientHandler(socket));

→ Mỗi client sẽ được xử lý độc lập

#### Biến ClientConnected

Dùng để:
- Đếm số client đã kết nối
- In log ra màn hình

#### Vấn đề trong code

Hiện tại bạn đang tạo thread pool trong vòng lặp:

→ Điều này sai vì mỗi client lại tạo pool mới (rất tốn tài nguyên)

Cách đúng:
- Tạo pool 1 lần trước while

---

### 4.2 ClientHandler.java – Xử lý từng client

#### Mục đích
- Nhận dữ liệu từ client
- Xử lý dữ liệu
- Gửi phản hồi lại

#### Cách hoạt động

Khi thread chạy:

run() được gọi

Tạo 2 luồng:

- BufferedReader → đọc dữ liệu từ client
- PrintWriter → gửi dữ liệu về client

#### Vòng lặp chính

while ((message = in.readLine()) != null)

→ Server sẽ:
1. Nhận message từ client
2. In ra console
3. Tạo phản hồi
4. Gửi lại

#### Ví dụ

Client gửi:
hello

Server xử lý:
Server receive: hello

#### Khi client ngắt kết nối

IOException xảy ra → in:

Client disconnect

---

### 4.3 SimpleClient.java – Client test

#### Mục đích
- Test server
- Gửi dữ liệu thủ công

#### Cách kết nối

Socket socket = new Socket("localhost", 12345);

→ Client kết nối tới server

#### Luồng xử lý

1. Người dùng nhập từ bàn phím
2. Gửi lên server
3. Nhận phản hồi
4. In ra màn hình

#### Gửi dữ liệu

out.println(userInput);

#### Nhận dữ liệu

String response = in.readLine();

#### Vòng lặp

Client chạy vô hạn:
- Luôn cho nhập
- Luôn gửi

---

## 5. Luồng dữ liệu

1. User nhập dữ liệu trên client
2. Client gửi qua socket
3. Server nhận
4. ClientHandler xử lý
5. Server gửi phản hồi
6. Client nhận và hiển thị

---

## 6. Cách chạy chương trình

### Bước 1: Chạy Server
Run class Server.java

Kết quả:
Server run with port: 12345

---

### Bước 2: Chạy Client
Run SimpleClient.java

---

### Test

Input:
hello

Output:
Server receive: hello

---

## 7. Các vấn đề trong code hiện tại

- Tạo thread pool sai vị trí
- Không đóng socket
- Không xử lý reconnect
- Không bảo mật dữ liệu
- Chỉ gửi String (không hỗ trợ object)

---

## 8. Hướng cải tiến

- Sửa thread pool
- Đóng tài nguyên sau khi dùng
- Dùng JSON thay vì String
- Dùng SSL để bảo mật
- Thêm xử lý reconnect
- Xây dựng protocol giao tiếp rõ ràng

---

## 9. Tóm tắt

- Server.java: mở server và nhận client
- ClientHandler.java: xử lý từng client
- SimpleClient.java: client test

Hệ thống hiện tại là mô hình client-server cơ bản, phù hợp để học Socket Java.

---

## 10. Định hướng phát triển

- Xây dựng hệ thống đấu giá real-time
- Thêm JavaFX UI client
- Sử dụng WebSocket
- Kết nối database