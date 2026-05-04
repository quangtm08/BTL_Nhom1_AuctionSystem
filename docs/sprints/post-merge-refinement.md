# 🛠️ Tài liệu Tinh chỉnh Sau Hợp nhất (Post-Merge Refinement)

**Người lập:** Quang (Điều phối viên)
**Trạng thái:** Bắt buộc thực hiện để hoàn thiện luồng Bid (End-to-End)

Tài liệu này cung cấp hướng dẫn chi tiết về các lỗ hổng kỹ thuật cần lấp đầy sau khi merge code. Các thành viên cần bám sát các chỉ dẫn này để đảm bảo hệ thống chạy trơn tru.

---

## 1. Duy (Thành viên 1) — Chuẩn hóa Luồng Người bán

Mã nguồn hiện tại của Duy đã có nền tảng tốt nhưng đang thiếu sự tách biệt giữa tầng Giao diện (UI) và tầng Nghiệp vụ (Service).

### Nhiệm vụ cụ thể:
*   **Tách biệt logic mạng (Client-side)**: 
    *   **Vấn đề**: `MyListingsController` và `CreateAuctionController` đang gọi trực tiếp `ServerConnection.sendRequest`.
    *   **Giải pháp**: Duy cần tạo `AuctionClientService` kế thừa `BaseClientService`. Chuyển toàn bộ các lệnh `sendRequest` (cho `CREATE_AUCTION`, `LIST_MY_LISTINGS`, `DELETE_AUCTION`) vào service này. 
    *   **Mục tiêu**: Controller chỉ nhận về `CompletableFuture` dữ liệu sạch, không còn dính dáng đến JSON hay Request/Response message.
*   **Hoàn thiện logic Xóa (Server-side)**:
    *   **Vấn đề**: Trong `AuctionService.deleteAuction`, việc xóa Đấu giá và xóa Item đang được thực hiện rời rạc. Nếu xóa Auction thành công nhưng xóa Item thất bại, dữ liệu sẽ bị "mồ côi".
    *   **Giải pháp**: Duy cần đảm bảo logic xóa nằm trong một **Transaction**. Sử dụng `connection.setAutoCommit(false)`, thực hiện xóa cả hai bản ghi, sau đó mới `commit()`. (Lưu ý: Logic này đã được Duy viết nháp, cần kiểm tra và chạy thực tế).
*   **Thông báo Real-time**: 
    *   **Vấn đề**: Người mua không biết khi nào có hàng mới trừ khi họ khởi động lại App.
    *   **Giải pháp**: Trong `AuctionService.createAuction`, ngay sau khi lưu thành công vào DB, hãy gọi `notificationService.broadcastNewAuction(...)` (Quang đã chuẩn bị sẵn phương thức này).

---

## 2. Ngọc (Thành viên 2) — Động hóa UI & Luồng Trải nghiệm

Hạ tầng Server cho việc đặt Bid đã xong, nhưng UI hiện tại vẫn là "vỏ tĩnh", chưa kết nối thực sự với dữ liệu động.

### Nhiệm vụ cụ thể:
*   **Dynamic Card Rendering**:
    *   **Vấn đề**: `auction_browse.fxml` đang chứa các thẻ VBox cứng. Khi gọi `listAuctions()`, dữ liệu trả về không có chỗ để hiển thị.
    *   **Giải pháp**: Ngọc cần xóa các VBox tĩnh trong FXML, thay bằng một `GridPane` trống. Trong Controller, lặp qua danh sách `AuctionSummaryDto` nhận được và tạo các Card (VBox) tương ứng bằng Java code hoặc nạp từ một file FXML template nhỏ.
*   **Kích hoạt luồng chi tiết (Navigation)**:
    *   **Vấn đề**: Phương thức `navigateToDetail` đã có nhưng chưa được gán vào bất kỳ nút bấm nào trên giao diện.
    *   **Giải pháp**: Khi render Card động, hãy gán `setOnAction` cho nút "Raise Bid" hoặc "View" để nó gọi `navigateToDetail(dto.getId())`. Nhắc lại: Luôn gọi `AppContext.setSelectedAuctionId(id)` trước khi chuyển màn hình.
*   **Lắng nghe sự kiện Real-time**:
    *   **Vấn đề**: Giá hiện tại trên màn hình Detail không tự cập nhật khi có người khác đặt Bid cao hơn.
    *   **Giải pháp**: Trong `initialize()` của `AuctionDetailController`, hãy đăng ký:
        ```java
        ServerConnection.getInstance().registerPushHandler(MessageType.PUSH_BID_UPDATE, json -> {
            // Parse JSON và cập nhật Price Label trên UI (dùng Platform.runLater)
        });
        ```

---

## 3. Ghi chú về Scheduler (Bình vắng mặt)

Vì Bình chưa thể hoàn thành `AuctionScheduler`, hệ thống sẽ không tự động chuyển trạng thái từ `PENDING` sang `LIVE`.
*   **Yêu cầu tạm thời cho Duy**: Khi tạo Auction mới, hãy set mặc định status là `LIVE` ngay lập tức để Ngọc có thể test luồng Bid. Khi Bình quay lại, chúng ta sẽ khôi phục logic PENDING -> LIVE tự động.

---

**Quang (Coordinator)**: Đã hoàn thành wiring `ServerContext` và mở rộng `NotificationService`. Các bạn có thể sử dụng ngay các phương thức broadcast và push handler.
