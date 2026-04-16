# 🔐 Authentication & Database Module

## 1. Giới thiệu

Phần này xử lý **kết nối cơ sở dữ liệu** và các chức năng **xác thực người dùng** trong hệ thống đấu giá.

Bao gồm 2 class chính:

* `DBConnection`: Quản lý kết nối đến database SQLite
* `AuthService`: Xử lý login, register và lấy danh sách user

---

## 2. DBConnection – Kết nối Database

### 📌 Mục đích

Tạo kết nối đến file database SQLite (`auction.db`)

### 📂 Đường dẫn database

```java
private static final String URL = "jdbc:sqlite:database/auction.db";
```

👉 Database được lưu trong thư mục:

```
/database/auction.db
```

### ⚙️ Cách hoạt động

```java
public static Connection getConnection()
```

* Sử dụng `DriverManager` để mở kết nối
* Nếu thành công → in ra:

```
✅ Connected to: jdbc:sqlite:database/auction.db
```

* Nếu lỗi → in stack trace

### 📌 Lưu ý

* Nếu file `.db` chưa tồn tại → SQLite sẽ tự tạo
* Đảm bảo thư mục `database/` tồn tại

---

## 3. AuthService – Xác thực người dùng

### 3.1. Login

```java
public String login(String username, String password)
```

### 🔍 Chức năng

* Kiểm tra username + password trong DB
* Nếu đúng → trả về `role`
* Nếu sai → trả về `null`

### 🧠 SQL sử dụng

```sql
SELECT role FROM users WHERE username = ? AND password = ?
```

---

### 3.2. Register

```java
public boolean register(String username, String password, String role)
```

### 🔍 Chức năng

1. Kiểm tra username đã tồn tại chưa
2. Nếu chưa → thêm user mới vào DB

### 🧠 SQL sử dụng

**Kiểm tra tồn tại:**

```sql
SELECT id FROM users WHERE username = ?
```

**Thêm user:**

```sql
INSERT INTO users(username, password, role) VALUES (?, ?, ?)
```

### 📌 Kết quả trả về

* `true` → đăng ký thành công
* `false` → username đã tồn tại hoặc lỗi

---

### 3.3. Lấy danh sách user (test)

```java
public List<String> getAllUsers()
```

### 🔍 Chức năng

* Lấy toàn bộ user từ database
* Trả về dạng:

```
username | password | role
```

### 🧠 SQL sử dụng

```sql
SELECT username, password, role FROM users
```

---

## 4. Cấu trúc bảng `users`

| Field    | Type    | Mô tả                |
| -------- | ------- | -------------------- |
| id       | INTEGER | Khóa chính           |
| username | TEXT    | Tên đăng nhập        |
| password | TEXT    | Mật khẩu             |
| role     | TEXT    | Vai trò (admin/user) |

---

## 5. Điểm cần cải thiện ⚠️

* ❌ Mật khẩu đang lưu dạng **plain text** → không an toàn
  👉 Nên dùng hash (ví dụ: BCrypt)

* ❌ Chưa đóng `PreparedStatement` riêng (nên dùng try-with-resources cho từng cái)

* ❌ Chưa validate input (username/password rỗng)

---

## 6. Tóm tắt

* `DBConnection` → mở kết nối SQLite
* `AuthService` → xử lý login & register
* Dữ liệu lưu trong bảng `users`
* Hệ thống hoạt động theo mô hình đơn giản, dễ mở rộng

---

👉 Phù hợp cho:

* Project JavaFX
* Hệ thống CRUD nhỏ
* Demo login/register cơ bản
