## Title
Thread-safety for Auction entity (multi-thread bid/state handling)

## Mục tiêu
Tăng độ an toàn đa luồng cho `Auction` khi nhiều request đồng thời:
- Tránh race condition khi nhiều người cùng đặt giá.
- Đảm bảo chuyển trạng thái phiên đấu giá nhất quán.
- Giữ phạm vi đơn giản, phù hợp dự án bài tập (single JVM).

## Thay đổi chính

### 1) Bổ sung khóa cấp entity với `ReentrantLock`
- Thêm `auctionLock = new ReentrantLock(true)` (fair lock).
- Áp dụng lock cho các thao tác thay đổi trạng thái:
  - `startAuction()`
  - `endAuction()`
  - `markAsPaid()`
  - `cancelAuction()`
  - `placeBid()`

### 2) Đồng bộ hóa thao tác lịch sử bid
- Thêm monitor riêng: `bidHistoryMonitor`.
- Trong `placeBid()`, thao tác `bidHistory.add(...)` được bọc `synchronized`.

### 3) Cập nhật visibility cho trạng thái dùng chung
- Đánh dấu `volatile` cho:
  - `highestBidderId`
  - `currentHighestBid`
  - `status`

## Vấn đề đã xử lý
- Tránh tình huống 2 thread cùng pass validate rồi ghi đè giá cao nhất.
- Giảm rủi ro lệch state khi đang đặt giá và đồng thời chuyển trạng thái phiên.
- Hạn chế mất dữ liệu/không nhất quán do ghi đồng thời vào `bidHistory`.

## Phạm vi
- Chỉ thay đổi trong entity:  
  `src/main/java/com/nhom1/auction/common/entity/Auction.java`
- Không thay đổi API, schema DB, hoặc flow nghiệp vụ bên ngoài.

## Kiểm thử đề xuất

### Manual test
1. Tạo 1 phiên `RUNNING`.
2. Gửi nhiều request `placeBid` đồng thời (2–10 request cùng lúc).
3. Kiểm tra:
   - `currentHighestBid` luôn bằng giá cao nhất hợp lệ cuối cùng.
   - `highestBidderId` tương ứng với giá cao nhất.
   - `bidHistory` không lỗi concurrent modification.

4. Chạy đồng thời:
   - 1 luồng `endAuction()`
   - nhiều luồng `placeBid()`
   - xác nhận không xuất hiện trạng thái bất hợp lệ.

### Regression
- Các case cũ: start/end/cancel/markAsPaid vẫn đúng rule.

## Rủi ro & giới hạn
- Cơ chế lock hiện tại bảo vệ tốt trong **single JVM**.
- Nếu chạy nhiều instance app, cần bổ sung cơ chế đồng bộ ở DB/distributed lock.

## Follow-up khuyến nghị
- Đồng bộ nhất quán monitor khi đọc `bidHistory` (ưu tiên dùng `bidHistoryMonitor` cùng chuẩn với lúc ghi).
- Có thể bổ sung stress test đa luồng cho `placeBid`.

