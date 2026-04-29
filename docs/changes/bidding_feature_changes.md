# Thay đổi: feature `bidding_feature`

Phiên làm việc: Thực hiện tinh chỉnh và hoàn thiện module đấu giá (server + client service).

Tóm tắt các thay đổi chính
- Server:
  - `src/main/java/com/nhom1/auction/server/bidding/BidRepository.java` — hiện thực đầy đủ các truy vấn lưu và lấy bid (save, findByAuctionId, findByBidderId, findLastBidTime).
  - `src/main/java/com/nhom1/auction/server/bidding/BidService.java` — logic nghiệp vụ: `placeBid`, `getAuctionDetail`, `listAllAuctions`, `getMyBids`.
  - `src/main/java/com/nhom1/auction/server/bidding/BidHandler.java` — tinh chỉnh xử lý ngoại lệ: thay `catch (Exception)` bằng các catch cụ thể và mã lỗi rõ ràng.
  - `src/main/java/com/nhom1/auction/server/bidding/BidModule.java` — wiring/DI (đã giữ theo pattern hiện có).

- Client:
  - `src/main/java/com/nhom1/auction/client/user/service/BiddingClientService.java` — thêm mới `listAuctions()`, `getAuctionDetail(String)`, `placeBid(String, BigDecimal)`, `getMyBids()`; tất cả trả `CompletableFuture` theo chuẩn `BaseClientService`.

- Repo khác (nhập từ seller-main và sửa cho tương thích):
  - `src/main/java/com/nhom1/auction/server/auction/AuctionRepository.java` — điều chỉnh tạo `Auction` để tương thích constructor hiện tại (điền `startingPrice=null` khi DB không lưu).
  - `src/main/java/com/nhom1/auction/server/auction/ItemRepository.java` — sửa kiểu kiểm tra null cho các trường boxed (warranty_months, production_year).

Lý do và tác động
- Cải thiện xử lý lỗi server: client giờ có thể phân biệt lỗi `INVALID_BID`, `AUCTION_CLOSED`, `UNAUTHORIZED`, `VALIDATION_ERROR`, `INVALID_FORMAT`, v.v.
- Thêm `BiddingClientService` theo mẫu `AuthClientService` để các `Controller` UI gọi đồng nhất (fail-fast validation, tự điền `bidderId` từ `AppContext`).

File được thêm/sửa (đường dẫn workspace):
- [src/main/java/com/nhom1/auction/server/bidding/BidHandler.java](src/main/java/com/nhom1/auction/server/bidding/BidHandler.java#L1)
- [src/main/java/com/nhom1/auction/client/user/service/BiddingClientService.java](src/main/java/com/nhom1/auction/client/user/service/BiddingClientService.java#L1)
- [src/main/java/com/nhom1/auction/server/bidding/BidService.java](src/main/java/com/nhom1/auction/server/bidding/BidService.java#L1)
- [src/main/java/com/nhom1/auction/server/bidding/BidRepository.java](src/main/java/com/nhom1/auction/server/bidding/BidRepository.java#L1)

Kiểm tra & Kết quả
- Đã chạy `mvn -DskipTests compile` → Biên dịch thành công (0 lỗi) sau khi sửa.
- Cảnh báo còn lại: trước đây có vài `printStackTrace` và kiểm tra null không an toàn (đã xử lý cho `ItemRepository`).

Gợi ý triển khai tiếp
- (Server) Kết nối `BidModule.init()` vào `ServerContext` nếu cần (thuộc scope thành viên chịu trách nhiệm điều phối).
- (Client) Refactor `AuctionBrowseController`, `AuctionDetailController`, `MyBidsController` để dùng `BiddingClientService`.

Người thực hiện: Ngọc (bidding_feature)
Thời gian: (phiên này)
