# 🤖 Module Tự Động Hóa & Đấu Giá Tự Động (Automation) - Bình

Tài liệu này giải thích cơ chế hoạt động tự động của hệ thống Đấu giá, bao gồm luồng quản lý thời gian (Scheduler) và tính năng đấu giá tự động (Auto-bid).

---

## 1. Các thành phần (Classes) tham gia

### Phía Client (Giao diện người dùng)
*   **`AutoBidClientService`**: Chịu trách nhiệm đóng gói cấu hình đấu giá tự động (ví dụ: mức giá tối đa, bước giá) và gửi yêu cầu lên Server.
*   **Giao diện cấu hình (Popup AutoBid)** *(Dự kiến)*: Nơi người dùng nhập thông tin thiết lập Auto-bid.

### Phía Server (Hệ thống trung tâm)

#### Tính năng Đấu giá tự động (Auto-Bid)
*   **`AutoBidModule`**: Khởi tạo và kết nối các thành phần của tính năng Auto-bid.
*   **`AutoBidHandler`**: Tiếp nhận yêu cầu lưu cấu hình Auto-bid từ Client.
*   **`AutoBidService`**: Xử lý logic nghiệp vụ. Khi có một lượt đặt giá mới, class này kiểm tra xem có cấu hình Auto-bid nào hợp lệ để đặt đè lên không. Tích hợp cơ chế chặn đệ quy (`MAX_TRIGGER_DEPTH`) để ngăn chặn vòng lặp vô hạn khi có nhiều Auto-bid cùng kích hoạt.
*   **`AutoBidRepository`**: Lưu trữ thiết lập Auto-bid của người dùng vào cơ sở dữ liệu (sử dụng cơ chế Upsert).

#### Bộ lập lịch (Scheduler)
*   **`AuctionScheduler`**: Luồng xử lý ngầm (background thread) hoạt động định kỳ mỗi giây để tự động chuyển đổi trạng thái của phiên đấu giá theo thời gian thực.
*   **`AuctionGatewayImpl` & `BidGatewayImpl`**: Các lớp trung gian (Adapter). Để duy trì tính độc lập giữa các module, hệ thống tự động không can thiệp trực tiếp vào module `Auction` hay `Bidding`. Thay vào đó, nó tương tác thông qua các Gateway này để thay đổi trạng thái hoặc thực hiện đặt giá.

---

## 2. Luồng hoạt động chi tiết (Flow)

Hệ thống hoạt động dựa trên hai luồng quy trình độc lập:

### Luồng A: Kích hoạt Đấu giá tự động (Auto-Bid Trigger)
Luồng này diễn ra khi có một người dùng đặt giá thủ công thành công.
1. Khách hàng gửi yêu cầu đặt giá lên Server. `BidHandler` xử lý và xác nhận lượt đặt giá này hợp lệ.
2. Ngay lập tức, `BidHandler` gọi đến **`AutoBidService`** để kiểm tra xem có thiết lập Auto-bid nào cần được kích hoạt hay không.
3. **`AutoBidService`** truy xuất `AutoBidRepository` và tìm thấy cấu hình Auto-bid của một người dùng khác (với điều kiện số dư tối đa vẫn đủ để vượt qua mức giá hiện tại).
4. Hệ thống tự động cộng bước giá (increment) vào mức giá hiện tại.
5. `AutoBidService` thông qua **`BidGatewayImpl`** để tự động đặt lệnh giá mới vào hệ thống.
6. Nếu có cấu hình Auto-bid của người thứ 3, quá trình này lặp lại đệ quy cho đến khi tìm ra người trả giá cao nhất hoặc đạt giới hạn an toàn.

### Luồng B: Quản lý vòng đời phiên đấu giá (The Scheduler Tick)
Đây là quy trình tự động cập nhật trạng thái phiên đấu giá dựa trên thời gian thực. Mỗi 1 giây, `AuctionScheduler` sẽ thực hiện:

1. **Kiểm tra các phiên đang chờ (OPEN)**: Nếu thời gian hiện tại đã vượt qua `startTime`, Scheduler sử dụng `AuctionGatewayImpl` để đổi trạng thái sang **RUNNING** (Đang diễn ra). Người dùng có thể bắt đầu đặt giá.
2. **Kiểm tra các phiên đang diễn ra (RUNNING)**: Nếu thời gian hiện tại vượt qua `endTime`:
    *   **Cơ chế chống bắn tỉa (Anti-sniping)**: Scheduler gọi `BidGatewayImpl` để kiểm tra thời điểm của lượt đặt giá cuối cùng. Nếu lượt giá này diễn ra trong khoảng thời gian sát nút (ví dụ: 15 giây cuối), hệ thống sẽ tự động gia hạn thêm thời gian (ví dụ: 30 giây) để đảm bảo công bằng.
    *   **Kết thúc phiên**: Nếu không vi phạm cơ chế Anti-sniping và đã hết giờ, trạng thái được chuyển sang **FINISHED** (Đã kết thúc).
3. Sau khi chuyển sang FINISHED, Scheduler gọi `NotificationService.broadcastAuctionEnded` để đẩy thông báo (push notification) cho tất cả Client về kết quả phiên đấu giá.

---

## 3. Hiện trạng hoàn thành (Status)
Toàn bộ logic xử lý phía Server, hệ thống Auto-bid và Scheduler **đã được khởi tạo và kết nối hoàn chỉnh trong `ServerContext`**.
Phía Client chỉ cần thiết kế UI và gọi `AutoBidClientService.saveConfig(...)` để tính năng tự động hoạt động đồng bộ.
