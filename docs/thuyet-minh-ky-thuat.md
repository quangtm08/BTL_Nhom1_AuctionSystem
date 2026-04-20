# Tài liệu Kỹ thuật: Tái cấu trúc Hệ thống Xác thực (Authentication Refactor)

Tài liệu này tóm tắt các thay đổi về hạ tầng và logic nghiệp vụ trong nhánh refactor hệ thống xác thực. Các thay đổi này nhằm mục đích tách biệt hoàn toàn (decoupling) giữa Client và Server thông qua giao thức Socket và JSON.

---

## 1. Thay đổi tại BaseEntity & Entity (Infrastructure)

Để hỗ mapping được dữ liệu từ Database vào đối tượng Java, `BaseEntity` và các lớp kế thừa (`User`, `Auction`, `Item`, `BidTransaction`) đã được cập nhật cơ chế khởi tạo.

### Cơ chế 2 Constructor:
- **Constructor mặc định (New):** Không tham số, tự động sinh `UUID` và `createdAt`. Dùng cho các đối tượng tạo mới từ phía Server.
- **Constructor tái tạo (Reconstruction):** Nhận `UUID`, `createdAt`, `updatedAt` làm tham số. Dùng cho `Repository` khi load dữ liệu cũ từ Database để đảm bảo tính nhất quán của ID và mốc thời gian.

---

## 2. Giao thức giao tiếp (Protocol)

Hệ thống chuyển sang sử dụng mô hình Message Envelope để chuẩn hóa dữ liệu truyền qua Socket.

- **RequestMessage<T>:** Chứa `type` (MessageType), `requestId` (UUID) và `payload` (Dữ liệu yêu cầu).
- **ResponseMessage<T>:** Chứa `requestId`, `success` (boolean), `payload` (Dữ liệu phản hồi) và `error` (nếu có).
- **Generics (`<T>`):** Cho phép tái sử dụng lớp vỏ (Envelope) cho nhiều loại dữ liệu khác nhau mà vẫn đảm bảo an toàn về kiểu dữ liệu (type-safety).

---

## 3. Cấu trúc DTO (Data Transfer Object)

Các lớp DTO được tách biệt hoàn toàn khỏi lớp Entity để tránh lộ thông tin nhạy cảm và giảm phụ thuộc.

- **LoginRequest:** Sử dụng trường `identifier` (chấp nhận cả Username hoặc Email) và `password`.
- **RegisterRequest:** Bao gồm `username`, `email`, và `password`. 
    - *Lưu ý:* Không bao gồm trường `role` ở phía Client để đảm bảo an ninh. Server sẽ mặc định gán quyền `USER`.
- **AuthResponse:** Dữ liệu trả về sau khi xác thực thành công, bao gồm: `userId` (String/UUID), `username`, `email`, và `role` (UserRole).

---


## 4. Phân lớp trách nhiệm (Layering)

- **Repository:** Chịu trách nhiệm thực thi các truy vấn SQL thuần túy.
- **Service:** Chứa logic nghiệp vụ (kiểm tra mật khẩu, kiểm tra trùng lặp).
- **Handler:** Chuyển đổi dữ liệu từ RequestMessage sang tham số cho Service và đóng gói kết quả trả về.
- **MessageRouter:** Điều hướng tin nhắn dựa trên `MessageType`.
