# Kế hoạch Brush-up Luồng Nghiệp vụ Cốt lõi (Core Flow Brush-up)

**Bối cảnh:** 
Sau khi hoàn tất đợt hợp nhất (merge) mã nguồn lớn, mục tiêu hiện tại là trau chuốt và tối ưu hóa các luồng nghiệp vụ cốt lõi: từ việc tạo đấu giá (người bán) đến việc tham gia đấu giá và cập nhật thời gian thực (người mua). Chúng ta cần đảm bảo trải nghiệm người dùng (UX) mượt mà và hệ thống hoạt động ổn định nhất có thể. 
*Lưu ý: Các tính năng dành cho Admin tạm thời chưa được ưu tiên trong đợt này.*

---

## 📋 Phân công công việc

### 🧑‍💻 Quang
- **Nhiệm vụ 1:** Khắc phục lỗi phân tích cú pháp JSON cho kiểu dữ liệu thời gian.
  - **Mô tả:** Xử lý lỗi `DateTimeParseException` do sự khác biệt định dạng timestamp giữa PostgreSQL và Jackson.
- **Nhiệm vụ 2:** Chuẩn hóa phong cách thiết kế (Style) cho toàn bộ nút bấm.
  - **Mô tả:** Thêm hiệu ứng hover, đồng bộ hóa độ bo góc (corner radius) và các thông số padding/border để tạo sự nhất quán cho toàn ứng dụng.
- **Nhiệm vụ 3:** Hiển thị tên người dùng thực tế trên giao diện.
  - **Mô tả:** Thay thế các placeholder tĩnh trên Sidebar và màn hình Explore bằng tên người dùng thật được lấy từ `AppContext`.
- **Nhiệm vụ 4:** Xử lý loại bỏ khoảng trắng (Trim) trong Form xác thực.
  - **Mô tả:** Thêm logic vào Controller của màn hình Đăng ký/Đăng nhập để loại bỏ các khoảng trắng dư thừa ở Username và Email.

### 🧑‍💻 Duy
- **Nhiệm vụ 1:** Kết nối `NotificationService` vào luồng xử lý tại `AuctionHandler`.
  - **Mục tiêu:** Kích hoạt thông báo đẩy ngay khi đấu giá mới được tạo thành công.
- **Nhiệm vụ 2:** Tối ưu hóa màn hình `My Listings`.
  - **Vấn đề:** Loại bỏ các giá trị tĩnh (hardcoded) trong phần "Accumulated Revenue", "Ending Soon" và dọn dẹp các nút lọc không phù hợp.
- **Nhiệm vụ 3:** Sửa lỗi layout màn hình Tạo đấu giá mới (`create_auction.fxml`).
  - **Vấn đề:** Điều chỉnh padding/spacing và khả năng co giãn để giao diện cân đối hơn.
- **Nhiệm vụ 4:** Xây dựng `AuctionClientService` (Refactoring).
  - **Vấn đề:** Hiện tại Controller đang gọi trực tiếp `ServerConnection`.
  - **Yêu cầu:** Tách logic xử lý network ra một lớp Service riêng (`AuctionClientService`) để tuân thủ kiến trúc của dự án (tham khảo `BiddingClientService`).
- **Nhiệm vụ 5:** Chuyển đổi Card UI sang FXML Template.
  - **Vấn đề:** Logic tạo Card hiện đang được viết thủ công bằng code (hardcoded) trong Controller, gây khó khăn cho việc bảo trì.
  - **Yêu cầu:** Đưa giao diện Card vào một file FXML riêng và sử dụng `FXMLLoader` để hiển thị danh sách đấu giá.

### 🧑‍💻 Ngọc
- **Nhiệm vụ 1:** Đăng ký và xử lý Push Notification tại màn hình Explore.
  - **Mục tiêu:** Giúp người mua thấy ngay các đấu giá mới mà không cần load lại trang.
- **Nhiệm vụ 2:** Nâng cấp cấu trúc dữ liệu và hiển thị chi tiết đấu giá (`auction_detail.fxml`).
  - **Công việc:** 
    1. **Backend:** Bổ sung trường `sellerName` vào `AuctionDetailDto` và `bidderName` vào `BidSummaryDto`. Cập nhật logic Server để JOIN bảng lấy tên người dùng.
    2. **Frontend:** Thay thế các trường dữ liệu tĩnh (Tên hàng, người bán, mô tả...) bằng dữ liệu thật từ response mới.
- **Nhiệm vụ 3:** Đơn giản hóa UI hình ảnh sản phẩm.
  - **Mô tả:** Thay thế thiết kế 4 ảnh nhỏ hiện tại bằng một khung hình lớn duy nhất để tập trung vào ảnh chính.
// Tạo PR -> lên railways tạo môi trường ảo -> lấy port  về sửa client, sau khi merge vào main, sửa lại port main tránh conflict
//Bây giờ Merge Main : Xử lí Duy làm BroadCast, PushNotification
//Show ERROR UI khi BidException
//Xử lí BroadCast khi có Push Notification để khi client ở màn hình Explore sẽ trực tiếp refresh trang ngay khi có Auction mới 3

- **Nhiệm vụ 4:** Chuyển đổi Card UI sang FXML Template.
  - **Vấn đề:** Tương tự Duy, logic tạo Card tại màn hình Explore đang bị hardcoded trong code Java.
  - **Yêu cầu:** Phối hợp với Duy để thống nhất thiết kế Card, đảm bảo tính nhất quán về UI/UX trên toàn ứng dụng.

### 🧑‍💻 Bình
- **Nhiệm vụ:** Duy trì và mở rộng hệ thống kiểm thử (Writing Tests).
- **Mục tiêu:** Đảm bảo các luồng core flow không gây ra lỗi hồi quy (regression). Tập trung vào Integration tests cho toàn bộ vòng đời của một cuộc đấu giá.

---

## ⚠️ Lưu ý chung cho cả nhóm
- **Tính nhất quán:** Giữ vững các chuẩn mực code đã thống nhất sau khi merge.
- **Giao tiếp:** Nếu có thay đổi liên quan đến cấu trúc DTO hoặc Message Protocol, cần thông báo ngay cho các thành viên khác.
- **Trải nghiệm:** Luôn đặt câu hỏi "Bước này người dùng có cảm thấy tự nhiên không?" khi chỉnh sửa các yếu tố UX.