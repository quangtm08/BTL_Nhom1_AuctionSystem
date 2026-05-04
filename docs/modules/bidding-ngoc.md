# Module: Bidding Feature (Đấu giá)

Tài liệu này tổng hợp chi tiết về module Đấu giá (Bidding), bao gồm kiến trúc hệ thống, các thành phần chính và quy tắc nghiệp vụ về bước giá tối thiểu.

---

## 1. Kiến Trúc & Thành Phần Chính

Hệ thống tuân thủ mô hình phân lớp để tách biệt logic nghiệp vụ và giao diện.

### Phía Server (Logic & Dữ liệu)
- **BidRepository**: Quản lý lưu trữ và truy vấn dữ liệu bid từ Database (SQLite). Hỗ trợ các thao tác: `save`, `findByAuctionId`, `findByBidderId`, `findLastBidTime`.
- **BidService**: "Bộ não" xử lý logic đặt giá. Thực hiện kiểm tra trạng thái auction, validate người dùng và xử lý giao dịch đặt giá.
- **BidHandler**: Đóng vai trò bộ định tuyến (router) phía server, chuyển đổi giữa dữ liệu JSON và đối tượng Java, đồng thời chuẩn hóa các mã lỗi phản hồi (`INVALID_BID`, `AUCTION_CLOSED`, `UNAUTHORIZED`, v.v.).
- **BidModule**: Điểm khởi tạo và kết nối các thành phần (wiring/DI) vào hệ thống server.

### Phía Client (Giao diện & Dịch vụ)
- **BiddingClientService**: Tầng dịch vụ trung gian kế thừa từ `BaseClientService`. 
  - Quản lý các yêu cầu: `listAuctions()`, `getAuctionDetail()`, `placeBid()`, `getMyBids()`.
  - Sử dụng `CompletableFuture` để xử lý bất đồng bộ, tránh treo giao diện.
  - Tự động lấy thông tin người dùng từ `AppContext`.

---

## 2. Quy tắc Đặt giá (Bid Rules)

Để đảm bảo tính công bằng và sôi nổi cho phiên đấu giá, hệ thống áp dụng quy tắc **Bước giá tối thiểu (Min Bid Increment)**.

### Cơ chế hoạt động
- **Mức tăng**: 5% dựa trên giá khởi điểm (`startingPrice`).
- **Công thức**:
  - Lần bid đầu tiên: `amount >= startingPrice`.
  - Các lần bid tiếp theo: `amount >= currentHighestBid + (startingPrice * 0.05)`.
- **Làm tròn**: Giá trị bước giá được làm tròn đến 2 chữ số thập phân (HALF_UP).

### Thành phần tham gia xác thực
- **Auction Entity**: Chứa logic tính toán `getMinBidIncrement()`.
- **AuctionBidValidator**: Thực hiện kiểm tra điều kiện mỗi khi có yêu cầu đặt giá mới. Nếu không thỏa mãn, hệ thống sẽ ném `InvalidBidException` kèm thông báo rõ ràng cho người dùng.

---

## 3. Chi tiết thực hiện & File liên quan

### Các file chính:
- `com.nhom1.auction.server.bidding`: `BidHandler.java`, `BidService.java`, `BidRepository.java`.
- `com.nhom1.auction.client.user.service`: `BiddingClientService.java`.
- `com.nhom1.auction.common.entity`: `Auction.java`, `AuctionBidValidator.java`.

### Tác động hệ thống:
- Cải thiện khả năng xử lý lỗi, giúp Client hiển thị thông báo chính xác cho người dùng.
- Tương thích tốt với các module Seller và Auction đã có.

---

## 4. Kiểm tra & Kết quả

- **Biên dịch**: Đã chạy `mvn compile` thành công, không có lỗi xung đột.
- **Unit Test**: Đã bổ sung và chạy thành công các test case trong `AuctionTest.java`:
  - `testPlaceBid_RejectWhenLessThanMinIncrement`: Kiểm tra từ chối bid nếu không đủ bước giá.
  - `testPlaceBid_AcceptedWhenEqualOrGreaterThanMinIncrement`: Kiểm tra chấp nhận bid khi đủ bước giá.

---
**Người thực hiện:** Ngọc
