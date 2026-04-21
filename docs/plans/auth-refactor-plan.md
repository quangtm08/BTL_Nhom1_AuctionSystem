# Kế hoạch Refactor Hệ thống Xác thực (Authentication)

**Trạng thái:** Đang thực hiện (In Progress)
**Mục tiêu:** Tách biệt hoàn toàn Client và Server. Mọi giao tiếp Login/Register phải đi qua Socket bằng JSON thay vì gọi trực tiếp Class của nhau.

---

## 1. Nhật ký tiến độ (Progress Log)

### Phase 1: Giao thức chung (`common`) - **HOÀN THÀNH** ✅
Chúng ta đã xây dựng bộ "ngôn ngữ chung" mà cả Client và Server đều hiểu.
- [x] **Step 1.1:** Định nghĩa `MessageType` (LOGIN, REGISTER, LIST_AUCTIONS...).
- [x] **Step 1.2:** Tạo "Vỏ bọc tin nhắn" (Message Envelope): `RequestMessage<T>` và `ResponseMessage<T>`.
- [x] **Step 1.3:** Tạo các "Gói dữ liệu" (Payload): `LoginRequest`, `RegisterRequest`, và `AuthResponse`.

### Phase 2: Hạ tầng Server (`server`) - **HOÀN THÀNH** ✅
- [x] **Step 2.1:** Tạo `UserRepository` (DAO) để xử lý SQL.
- [x] **Step 2.2:** Refactor `AuthService` (Logic nghiệp vụ).
- [x] **Step 2.3:** Cài đặt `AuthHandler` (Thông dịch DTO <-> Entity).
- [x] **Step 2.4:** Cài đặt `MessageRouter` (Phân loại JSON bằng Jackson).
- [x] **Step 2.5:** Update `ClientHandler` & `Server` (Kết nối hạ tầng).

### Phase 3: Hạ tầng Client (`client.connection`) - **TIẾP THEO** 🚀
- [ ] **Step 3.1:** Cài đặt Jackson tại Client để đóng gói/giải mã JSON.
- [ ] **Step 3.2:** Refactor `ServerConnection` để gửi `RequestMessage` và nhận `ResponseMessage` không đồng bộ (Asynchronous).
- [ ] **Step 3.3:** Xây dựng cơ chế Callback/Future để UI nhận kết quả từ luồng nhận tin nhắn.

### Phase 4: Tích hợp UI & Kiểm thử 🎨
- [ ] **Step 4.1:** Cập nhật `SignInController` để gọi `ServerConnection`.
- [ ] **Step 4.2:** Cập nhật `RegisterController`.
- [ ] **Step 4.3:** Xử lý hiển thị lỗi (ErrorResponse) lên giao diện người dùng.

---

## 2. Ghi chú nâng cấp (Future Refactoring)
- **Registry Pattern:** Chuyển đổi `MessageRouter` từ `switch-case` sang `Map<MessageType, Handler>` để đảm bảo OCP.
- **Service Container:** Gom nhóm các khởi tạo tại `Server.main` vào một `ServerContext` để dọn dẹp code.

### Tại sao dùng Generic `<T>` cho tin nhắn?
Hãy tưởng tượng `RequestMessage` là một **Chiếc hộp bưu điện tiêu chuẩn**.
- **Cái hộp (Envelope):** Luôn có mã ID và loại hàng (Type) ghi bên ngoài để bưu tá (Router) biết phải giao đi đâu.
- **Hàng bên trong (Payload `T`):** Có thể là bất cứ thứ gì (Thông tin Login, Thông tin Đấu giá...).
- **Lợi ích:** Chúng ta chỉ cần viết 1 Class hộp duy nhất, nhưng có thể đựng mọi loại hàng khác nhau.

### Tại sao cần 2 Constructor (Hàm khởi tạo) trong Entity?
Tất cả các Class kế thừa `BaseEntity` đều có 2 cách để sinh ra:

1. **Cách 1: Tạo mới hoàn toàn (Dành cho đăng ký/tạo mới)**
   - Dùng khi người dùng nhập dữ liệu từ UI.
   - **Cơ chế:** Tự động sinh ra một mã UUID mới và lấy thời gian hiện tại làm `createdAt`.
   - *Lưu ý:* Client không bao giờ được tự sinh ID để tránh bị trùng lặp hoặc hack thời gian.

2. **Cách 2: Tái tạo từ Database (Dành cho Login/Xem dữ liệu)**
   - Dùng khi chúng ta lấy dữ liệu từ DB lên.
   - **Cơ chế:** Chúng ta "ép" Object phải nhận lại đúng cái ID và mốc thời gian cũ đã lưu trong DB thay vì sinh cái mới.

### Tại sao ID và Timestamps lại là `final`?
Chúng ta muốn dữ liệu có tính **bất biến (Immutable)**. Một khi một User hoặc một món hàng đã được tạo ra:
- ID của nó không bao giờ được thay đổi.
- Ngày tạo (`createdAt`) không bao giờ được sửa.
Điều này giúp hệ thống cực kỳ ổn định và dễ track lỗi.

---

## 3. Quy ước DTO (Data Transfer Object)
- **LoginRequest:** Dùng `identifier` (có thể là username HOẶC email) + `password`.
- **RegisterRequest:** Client gửi `username`, `email`, `password`. Server sẽ tự gán `role = USER` mặc định để bảo mật (không để Client tự chọn role ADMIN).
- **AuthResponse:** Sau khi login thành công, Server gửi lại `userId`, `username`, `email`, `role` để Client biết đường hiển thị giao diện Admin hay User.

---

## 4. Bước tiếp theo (Next Action)
Tiếp tục **Step 2.1**: Viết Class `UserRepository` để thực hiện các câu lệnh SQL thực tế vào file `auction.db`.
