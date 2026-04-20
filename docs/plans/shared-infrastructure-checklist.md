# Danh sách kiểm tra (Checklist) Hạ tầng dùng chung

Sử dụng checklist này để quyết định xem dự án đã sẵn sàng cho việc phát triển các tính năng song song (parallel feature development) hay chưa.

Dự án không cần phải hoàn hảo trước khi nhóm bắt đầu chia việc. Tuy nhiên, nó cần đủ ổn định để mọi người có thể làm việc mà không gây xung đột (breaking) code của nhau liên tục.

## A. Kiến trúc và Ranh giới (Architecture and boundaries)

- [ ] Nhóm thống nhất rằng chỉ có **Server** mới được truy cập **Database**.
- [ ] Nhóm thống nhất về luồng xử lý cơ bản của Server:
  `ClientHandler -> MessageRouter -> Handler -> Service -> Repository -> Database`
- [ ] Nhóm thống nhất về nơi đặt các Controller, DTO, Service, Repository và code Socket.
- [ ] Nhóm đã ghi chú rõ trách nhiệm của từng lớp (layer).

## B. Hợp đồng tin nhắn (Message contract)

- [ ] Có một **Message envelope** chung cho các Request.
- [ ] Có một **Message envelope** chung cho các Response.
- [ ] Có một cách tiếp cận chung cho các phản hồi thành công (success) và lỗi (error).
- [ ] Danh sách các **Core message types** đầu tiên đã được liệt kê và đặt tên nhất quán.
- [ ] Nhóm thống nhất về việc ai có quyền định nghĩa chi tiết **Payload** cho từng mảng tính năng.

## C. Hợp đồng tính năng cốt lõi (Core feature contracts)

- [ ] Cấu trúc Request/Response cho Login và Register đã được định nghĩa.
- [ ] Cấu trúc Request/Response cho danh sách đấu giá (Auction listing) đã được định nghĩa.
- [ ] Cấu trúc Request/Response cho chi tiết đấu giá (Auction detail) đã được định nghĩa.
- [ ] Cấu trúc Request/Response cho đặt giá (Place bid) đã được định nghĩa.
- [ ] Cấu trúc Request/Response cho tạo đấu giá (Create auction) đã được định nghĩa.
- [ ] Cấu trúc Request/Response cho hủy đấu giá (Cancel auction) đã được định nghĩa.

## D. Nền tảng Cơ sở dữ liệu (Database foundation)

- [ ] **Schema** ban đầu cho các bảng `users`, `items`, `auctions`, và `bids` đã được ghi chép lại.
- [ ] Các khóa (Keys) và mối quan hệ (Relationships) đủ rõ ràng để nhiều người có thể cùng viết code dựa trên đó.
- [ ] Nhóm hiểu rõ trường dữ liệu nào là tạm thời và trường nào là một phần của thiết kế ổn định.
- [ ] Các quyết định hiện tại với **SQLite** không gây khó khăn vô ích cho việc chuyển đổi sang **PostgreSQL** sau này.

## E. Quy tắc nghiệp vụ (Business rules)

- [ ] Các trạng thái (States) và quy trình chuyển đổi (Transitions) của Auction đã được viết rõ ràng.
- [ ] Phân quyền vai trò (Role permissions) đã được viết rõ ràng.
- [ ] Các quy tắc kiểm tra giá thầu (Bid validation) đã được viết rõ ràng.
- [ ] Các hành vi nhạy cảm với xử lý đồng thời (Concurrency) đã có một chiến lược thực thi thống nhất.
- [ ] Nhóm biết logic nào thuộc về Service thay vì Controller hay Repository.

## F. Luồng hoạt động tối thiểu (Minimal working flow)

- [ ] User có thể gửi một Request từ Client tới Server.
- [ ] Server có thể phân tích (parse) và điều hướng (route) tin nhắn.
- [ ] Một **Handler** có thể gọi một **Service**.
- [ ] Một **Service** có thể gọi một **Repository**.
- [ ] Một **Repository** có thể đọc hoặc ghi dữ liệu.
- [ ] Server có thể gửi một Response có cấu trúc ngược lại cho Client.
- [ ] Ít nhất một luồng xuyên suốt (end-to-end) đơn giản đã hoạt động:
  `login/register -> list auctions -> auction detail -> place bid`

## G. Phối hợp nhóm (Team coordination)

- [ ] Mỗi thành viên trong số 4 người đều có mảng sở hữu (ownership) ngắn hạn rõ ràng.
- [ ] Các thay đổi đối với **Shared contracts** phải được thảo luận trước khi merge.
- [ ] Nhóm có thói quen tạo PR hoặc review trước khi code được đưa vào nhánh chung (shared branch).
- [ ] Nhóm hiểu rằng các cột mốc (milestones) hàng tuần của giảng viên là để giữ nhịp độ, không phải là ranh giới sở hữu cứng nhắc.

## Quy tắc Sẵn sàng chia việc (Ready-to-split rule)

Nếu hầu hết các mục trên đã được tích chọn, đặc biệt là các phần từ A đến F, nhóm đã sẵn sàng để chia nhỏ công việc phát triển tính năng độc lập hơn.

Nếu nhiều mục vẫn chưa được hoàn thành, nhóm nên tập trung vào hạ tầng dùng chung (shared infrastructure) lâu hơn một chút trước khi phân chia sở hữu theo chiều dọc (vertical ownership).

## Lưu ý gợi ý cho cuộc họp nhóm

Khi sử dụng checklist này, hãy tránh hỏi:
- "Toàn bộ kiến trúc đã hoàn hảo chưa?"

Thay vào đó hãy hỏi:
- "Nền tảng dùng chung đã đủ ổn định để mọi người có thể làm việc song song mà ít gây nhầm lẫn hay chưa?"

Đó mới là quyết định thực sự mà checklist này hướng tới.
