# Kế Hoạch Đơn Giản Hóa Auto-Bid

## 1. Hiện trạng hiện tại

Tính năng Auto-Bid hiện đã hoạt động được, nhưng phần xử lý trong
`AutoBidService` đang khá phức tạp. Khi có một lượt bid mới, hệ thống lấy danh
sách cấu hình auto-bid, lọc các người dùng đủ điều kiện, sau đó chọn người có
`maxAmount` cao nhất để đặt giá tự động.

Cách làm hiện tại có hai vấn đề chính:

- Logic khó giải thích trong buổi bảo vệ vì có nhiều phần xử lý nâng cao như
  background thread, vòng lặp nhiều bước, stream, chọn `maxAmount` lớn nhất và
  tính `nextBestMax`.
- Chưa khớp hoàn toàn với yêu cầu đề bài. Đề bài có nhắc đến việc ưu tiên theo
  thời điểm đăng ký auto-bid, trong khi hiện tại hệ thống đang ưu tiên người có
  mức giá tối đa cao nhất.

## 2. Mục tiêu chỉnh sửa

Đơn giản hóa logic Auto-Bid nhưng không viết lại toàn bộ tính năng.

Sau khi chỉnh sửa, luồng xử lý nên là:

```text
Có bid mới
-> Lấy danh sách auto-bid có thông tin created_at
-> Bỏ qua người đang dẫn đầu
-> Đưa các cấu hình đủ điều kiện vào PriorityQueue
-> PriorityQueue ưu tiên người đăng ký auto-bid sớm nhất trước
-> Poll PriorityQueue để tìm người đầu tiên còn đủ maxAmount để đặt bid tiếp theo
-> Đặt bid tự động
-> Lặp lại đến khi không còn ai đủ điều kiện
```

Cách này dễ hiểu hơn, ít logic phụ hơn, dùng được PriorityQueue thật trong Java
và phù hợp hơn với yêu cầu ưu tiên theo thời điểm đăng ký.

## 3. Phạm vi chỉnh sửa

Không cần thay đổi toàn bộ kiến trúc hiện tại.

Giữ nguyên các phần sau:

- `AutoBidHandler`
- `AutoBidConfig`
- `AutoBidRepository`
- `BidGateway`
- `BidGatewayImpl`
- `BidHandler`
- Cơ chế gọi `BidService.placeBid(...)` để đặt bid tự động
- Bảng `auto_bid_configs` hiện có

Chỉ nên chỉnh ở hai điểm chính:

- Trả về hoặc map thêm `created_at` cho cấu hình auto-bid.
- Dùng `PriorityQueue` trong `AutoBidService` với comparator theo thời điểm đăng
  ký.
- Đơn giản hóa logic chọn người được auto-bid tiếp theo.

## 4. Bước 1: Lấy thêm thời điểm đăng ký auto-bid

Trong `AutoBidRepository.findByAuctionId(...)`, sửa câu SQL để lấy thêm
`created_at` của cấu hình auto-bid.

Hiện tại:

```sql
SELECT auction_id, bidder_id, max_amount, increment_amount
FROM auto_bid_configs
WHERE auction_id = ?
```

Nên sửa thành:

```sql
SELECT auction_id, bidder_id, max_amount, increment_amount, created_at
FROM auto_bid_configs
WHERE auction_id = ?
```

Ý nghĩa:

- `created_at` là dữ liệu dùng để xác định độ ưu tiên.
- Không sắp xếp bằng SQL là bắt buộc nếu đã dùng `PriorityQueue`, nhưng có thể
  thêm `ORDER BY created_at ASC` để kết quả đọc log/ổn định hơn.
- Nếu `AutoBidConfig` chưa có field `createdAt`, nên thêm field này hoặc tạo
  một class nội bộ nhỏ trong service, vì `PriorityQueue` cần giá trị này để so
  sánh.

## 5. Bước 2: Dùng PriorityQueue để chọn auto-bidder

Trong `AutoBidService.runAutoBids(...)`, bỏ cách chọn người có `maxAmount` cao
nhất.

Không nên tiếp tục dùng logic kiểu:

```java
eligibleConfigs.stream()
    .max(Comparator.comparing(AutoBidConfig::getMaxAmount))
```

Thay vào đó, tạo `PriorityQueue` thật trong Java. Priority không phải là
`maxAmount`, mà là thời điểm đăng ký auto-bid sớm hơn:

```java
PriorityQueue<AutoBidConfig> queue =
    new PriorityQueue<>(Comparator.comparing(AutoBidConfig::getCreatedAt));
```

Nếu cần tie-break khi hai config có cùng `created_at`, có thể thêm
`thenComparing(AutoBidConfig::getBidderId)` để thứ tự ổn định.

Logic mong muốn:

```text
Với từng cấu hình auto-bid:
- Nếu là người đang dẫn đầu thì không đưa vào queue
- Tính giá bid tiếp theo = giá hiện tại + bước giá tối thiểu
- Nếu maxAmount của người đó đủ trả giá này thì đưa vào queue
- Poll queue để lấy người có ưu tiên cao nhất
```

Trong cách này, `maxAmount` chỉ là điều kiện hợp lệ và giới hạn giá tối đa.
`maxAmount` không quyết định độ ưu tiên.

## 6. Bước 3: Giữ cơ chế đặt bid hiện có

Khi đã chọn được người auto-bid tiếp theo, vẫn gọi:

```java
bidGateway.placeAutoBid(...)
```

Không nên tự cập nhật giá trực tiếp trong `AutoBidService`.

Lý do:

- `BidService.placeBid(...)` đã có sẵn validate giá bid.
- Logic chống bid sai, bid khi auction đóng, bid thấp hơn giá hiện tại vẫn được
  tái sử dụng.
- Cơ chế chống race condition hiện tại vẫn được giữ lại.

## 7. Bước 4: Giữ vòng lặp nhưng làm rõ hơn

Có thể giữ `MAX_TRIGGER_DEPTH` để tránh vòng lặp vô hạn khi nhiều người cùng bật
auto-bid.

Tuy nhiên, bên trong vòng lặp chỉ nên có các bước rõ ràng:

```text
1. Lấy auction hiện tại
2. Lấy danh sách auto-bid kèm created_at
3. Đưa các config đủ điều kiện vào PriorityQueue theo created_at
4. Poll PriorityQueue để lấy người đầu tiên đủ điều kiện
5. Nếu không có ai thì dừng
6. Đặt auto-bid
7. Cập nhật currentHighestBid và currentHighestBidderId
```

Không cần tính `nextBestMax` nếu mục tiêu là đơn giản hóa và ưu tiên theo thời
điểm đăng ký.

## 8. Kết quả mong muốn

Sau khi chỉnh sửa:

- Auto-Bid vẫn hoạt động.
- Code dễ đọc hơn.
- Luồng xử lý dễ giải thích hơn trong buổi bảo vệ.
- Tính năng khớp hơn với yêu cầu đề bài về ưu tiên theo thời điểm đăng ký.
- Không cần viết lại toàn bộ module.
- Không làm ảnh hưởng đến các phần chính như `BidService`, `BidHandler`,
  database schema hoặc realtime notification.
