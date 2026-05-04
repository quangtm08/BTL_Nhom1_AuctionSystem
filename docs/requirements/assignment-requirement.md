# Đề bài Tập lớn Lập trình nâng cao: Phát triển hệ thống đấu giá trực tuyến

---

## 1. Giới thiệu bài tập lớn
Bài tập lớn này nhằm mục đích củng cố và mở rộng kiến thức về lập trình nâng cao thông qua việc phát triển một hệ thống đấu giá trực tuyến. Sinh viên sẽ áp dụng các nguyên lý lập trình hướng đối tượng kết hợp với các kỹ thuật nâng cao để xây dựng một hệ thống có cấu trúc rõ ràng, dễ bảo trì và mở rộng.

---

## 2. Mô tả hệ thống
Hệ thống bidding (đấu giá trực tuyến) là một nền tảng phần mềm cho phép nhiều người dùng cùng tham gia cạnh tranh giá để mua một sản phẩm hoặc dịch vụ trong một khoảng thời gian xác định. Thay vì bán với giá cố định, người bán đưa sản phẩm lên hệ thống và giá bán cuối cùng được xác định thông qua quá trình đấu giá giữa các người mua (bidder).

> **Tham khảo:** eBay Auctions.

---

## 3. Các yêu cầu cụ thể

### 3.1 Chức năng bắt buộc
Nhóm chức năng cốt lõi cần có để hệ thống đấu giá trực tuyến có thể vận hành đầy đủ và đảm bảo quy trình đấu giá diễn ra đúng logic nghiệp vụ.

#### 3.1.1 Quản lý người dùng
Hệ thống cần hỗ trợ quản lý người dùng với các vai trò khác nhau:
* Đăng ký / đăng nhập tài khoản.
* **Vai trò:**
    * **Bidder:** tham gia đấu giá.
    * **Seller:** đăng sản phẩm đấu giá.
    * **Admin:** quản lý hệ thống.

#### 3.1.2 Quản lý sản phẩm đấu giá
Cho phép người bán quản lý thông tin sản phẩm:
* Thêm / sửa / xóa sản phẩm.
* **Thông tin sản phẩm:** Tên, mô tả, giá khởi điểm, giá hiện tại cao nhất, thời gian bắt đầu & kết thúc.

#### 3.1.3 Tham gia đấu giá
* Người dùng đặt giá cao hơn giá hiện tại.
* Kiểm tra tính hợp lệ của giá đấu.
* Cập nhật người dẫn đầu phiên đấu giá theo thời gian thực.

#### 3.1.4 Kết thúc phiên đấu giá
* Tự động đóng phiên khi hết thời gian.
* Xác định người thắng cuộc.
* Chuyển trạng thái phiên đấu giá: $OPEN \rightarrow RUNNING \rightarrow FINISHED \rightarrow PAID / CANCELED$.

#### 3.1.5 Xử lý lỗi & ngoại lệ
Cơ chế phát hiện và xử lý lỗi để đảm bảo tính ổn định:
* Đặt giá thấp hơn giá hiện tại.
* Đấu giá khi phiên đã đóng.
* Lỗi dữ liệu, lỗi kết nối.

#### 3.1.6 Giao diện người dùng (GUI)
* Sử dụng JavaFX (khuyến nghị) hoặc Swing.
* **Các màn hình chính:** Danh sách phiên đấu giá, chi tiết sản phẩm, màn hình đấu giá trực tiếp, quản lý sản phẩm cho Seller.

---

### 3.2 Chức năng nâng cao
Nhằm tăng tính cạnh tranh và cải thiện trải nghiệm người dùng:

* **3.2.1 Auto-Bidding (Đấu giá tự động):** Người dùng đặt giá tối đa ($maxBid$) và bước giá ($increment$). Hệ thống tự động trả giá thay người dùng khi có bid mới, ưu tiên theo thời điểm đăng ký và không vượt quá $maxBid$.
* **3.2.2 Xử lý đấu giá đồng thời (Concurrent Bidding):** Đảm bảo không xảy ra Lost update, giá bị rollback hoặc hai người cùng thắng khi đặt giá cùng thời điểm.
* **3.2.3 Gia hạn phiên đấu giá (Anti-sniping Algorithm):** Nếu có bid mới trong $X$ giây cuối, tự động gia hạn thêm $Y$ giây.
* **3.2.4 Realtime Update (Observer nâng cao):** Cập nhật client ngay lập tức khi có bid mới mà không dùng polling liên tục (gợi ý dùng Socket/Event-based).
* **3.2.5 Bid History Visualization:** Hiển thị biểu đồ đường (line chart) giá đấu cao nhất theo thời gian thực ($Timestamp$ vs $Price$).

---

### 3.3 Thiết kế hướng đối tượng (OOP)
#### 3.3.1 Xác định các lớp chính
* **Entity (Abstract/Interface):** lớp cơ sở.
* **Item (Abstract):** Electronics, Art, Vehicle.
* **User (Abstract):** Bidder, Seller, Admin.
* **Auction & BidTransaction:** quản lý trung tâm và giao dịch.

#### 3.3.2 Áp dụng các nguyên tắc OOP
* **Encapsulation:** private/protected + getter/setter.
* **Inheritance:** phân cấp rõ ràng.
* **Polymorphism:** override phương thức (ví dụ: `printInfo()`).
* **Abstraction:** abstract class / interface.

---

### 3.4 Thiết kế kiến trúc hệ thống (Networking & MVC)
* Kiến trúc **Client-Server**.
* Giao tiếp: REST API hoặc Socket (dữ liệu JSON).
* Phía Client: MVC (JavaFX + FXML).
* Phía Server: MVC (Controller $\rightarrow$ Model $\rightarrow$ DAO). Chỉ Server truy cập database.

---

### 3.5 Tích hợp và triển khai
* Sử dụng **Maven** hoặc **Gradle**.
* Tuân thủ **Google Java Style Guide**.
* Viết **Unit Test (JUnit)** cho logic quan trọng.
* Sử dụng Git với commit rõ ràng (Conventional Commits).
* Khuyến khích thiết lập **CI/CD** với GitHub Actions.

---

### 3.6 Design Pattern áp dụng
* **Singleton:** quản lý kết nối.
* **Factory Method:** tạo các loại Item.
* **Observer:** cập nhật realtime.
* **Strategy / Command:** xử lý các loại bid.

---

## 4. Chấm điểm

| Nội dung đánh giá | Điểm | Mức |
| :--- | :--- | :--- |
| **Thiết kế lớp và cây kế thừa** | | |
| Xác định và triển khai các lớp chính (User, Item, Auction...) | 0.5 | Bắt buộc |
| Áp dụng đúng các nguyên tắc OOP | 1 | Bắt buộc |
| Áp dụng design pattern phù hợp | 1 | Bắt buộc |
| **Chức năng chính** | | |
| Quản lý người dùng, sản phẩm | 1 | Bắt buộc |
| Chức năng đấu giá | 1 | Bắt buộc |
| Xử lý lỗi & ngoại lệ | 1 | Bắt buộc |
| **Kỹ thuật quan trọng & concurrency** | | |
| Xử lý đấu giá đồng thời an toàn (tránh lost update, race condition) | 1 | Bắt buộc |
| Realtime update (Observer/Socket) | 0.5 | Bắt buộc |
| **Tích hợp, kiến trúc & chất lượng mã** | | |
| Thiết kế kiến trúc Client-Server rõ ràng | 0.5 | Bắt buộc |
| Áp dụng MVC (JavaFX + FXML, DAO cho server) | 0.5 | Bắt buộc |
| Sử dụng Maven/Gradle, coding convention, mã nguồn sạch | 0.5 | Bắt buộc |
| Unit Test (JUnit) cho logic quan trọng | 0.5 | Bắt buộc |
| Thiết lập CI/CD cơ bản (GitHub Actions) | 0.5 | Bắt buộc |
| **Chức năng nâng cao (tối đa 1.5đ)** | | |
| Auto-Bidding | 0.5 | Tùy chọn |
| Gia hạn phiên đấu giá (Anti-sniping) | 0.5 | Tùy chọn |
| Bid History Visualization (Biểu đồ realtime) | 0.5 | Tùy chọn |
| Tính năng khác tự sáng tạo | 0.5 | Tùy chọn |
| **Tổng điểm** | **10 + 1** | |

---