# Hướng dẫn Hạ tầng dùng chung (Shared Infrastructure)

Tài liệu này định nghĩa hạ tầng tối thiểu mà nhóm nên hoàn thành trước khi chia ra phát triển các tính năng (feature work) độc lập.

Mục tiêu không phải là chốt mọi quyết định thiết kế cho toàn bộ hệ thống. Mục tiêu là hoàn thành 
những phần mà nhiều tính năng phụ thuộc vào, để mọi người không bị chặn (block) lẫn nhau.

Nếu nhóm hoàn thành các mục trong tài liệu này, việc phân chia thành viên vào các khu vực công việc 
riêng biệt sẽ an toàn hơn và giảm thiểu rủi ro sai lệch.

## Tại sao giai đoạn này quan trọng

Nếu nhóm bắt đầu chia việc tính năng quá sớm, các vấn đề phổ biến sẽ xuất hiện:
- Một người thay đổi định dạng socket và làm hỏng code client của người khác.
- Một người viết SQL trực tiếp trong các Controller hoặc Handler.
- Nhiều người cùng thực hiện một quy tắc nghiệp vụ ở các nơi khác nhau.
- Schema của database thay đổi mà không phải ai cũng nhận ra.
- Công việc UI bị đình trệ do các giao diện (interface) backend chưa hoàn thành.

Mục đích của giai đoạn hạ tầng dùng chung này là để ngăn chặn những vấn đề đó.

## Nguyên tắc chính

Nhóm trước hết nên thống nhất về các **Shared contracts** (hợp đồng dùng chung) và ranh giới (boundaries) dùng chung.

Sau đó, mỗi thành viên có thể sở hữu một khu vực công việc với sự tự do hơn nhiều. Nói cách khác:
- **Hạ tầng dùng chung trước.**
- **Tính năng độc lập sau.**

## Những gì được tính là hạ tầng dùng chung

Các phần sau đây nên được thảo luận và giải quyết sớm vì nhiều phần của hệ thống phụ thuộc vào chúng.

### 1. Cấu trúc Package

Như hiện tại và thống nhất khi có chỉnh sửa thêm

### 2. Ranh giới Client-Server

Server nên sở hữu:
- Quyền truy cập cơ sở dữ liệu (Database access).
- Logic nghiệp vụ làm thay đổi trạng thái hệ thống.
- Các hoạt động nhạy cảm với xử lý đồng thời (Concurrency).

Client nên sở hữu:
- Các màn hình JavaFX.
- Logic điều khiển (Controller logic) cho tương tác của người dùng.
- Gửi yêu cầu (Request) tới Server.
- Hiển thị phản hồi (Response) từ Server.

**Client không nên truy cập cơ sở dữ liệu trực tiếp.** (

## Luồng Client đề xuất

```text
JavaFX UI (View)
-> Controller
-> Client Service (tạo connection với server + dto)
-> ServerConnection (dto -> json)
-> Socket Output
```

### Trách nhiệm của các lớp (Client layers)

#### `JavaFX UI`
- Hiển thị dữ liệu từ DTO.
- Thu thập input từ người dùng.

#### `Controller`
- Lắng nghe các sự kiện UI (click, type).
- Gọi đến các phương thức tương ứng trong Client Service.
- Không nên chứa logic nghiệp vụ phức tạp.

#### `Client Service`
- Chứa logic nghiệp vụ phía client.
- Tạo các đối tượng Request DTO để chuẩn bị gửi đi.
- Xử lý các phản hồi từ Server để cập nhật trạng thái ứng dụng.

#### `ServerConnection`
- Đóng gói (serialize) DTO thành JSON.
- Gửi dữ liệu qua Socket.
- Nhận dữ liệu phản hồi và chuyển ngược lại cho Service.

## Luồng Server đề xuất

```text
Client UI
-> ServerConnection
(Server bắt đầu từ đây)
-> ClientHandler (json -> object)
-> MessageRouter
-> Feature Handler 
-> Service (business logic)
-> Repository (SQL)
-> Database
```

### Trách nhiệm của các lớp (Layer responsibilities)

#### `ClientHandler`
- Đọc tin nhắn từ Socket.
- Ghi phản hồi ngược lại cho Client.
- Không nên chứa logic nghiệp vụ cụ thể của tính năng.

#### `MessageRouter`
- Kiểm tra loại tin nhắn (Message type).
- Chọn Handler chính xác.
- Ví dụ: `LOGIN`, `REGISTER`, `PLACE_BID`, `CREATE_AUCTION`.

#### `Feature Handler`
- Nhận Request đã được phân tích (parsed).
- Gọi Service thích hợp.
- Chuyển đổi kết quả từ Service thành các Response DTO.

#### `Service`
- Chứa các quy tắc nghiệp vụ (Business rules).
- Ví dụ:
  - Xác thực luồng Login.
  - Áp dụng các thay đổi trạng thái Auction.
  - Xác thực giá thầu (Bids).
  - Quyết định người chiến thắng khi đấu giá kết thúc.
  - Áp dụng Anti-sniping nếu được thực thi.

#### `Repository`
- Chứa code truy cập SQL hoặc JDBC.
- Đóng vai trò như một lớp DAO.
- Ví dụ:
  - Tìm User theo username.
  - Lưu Auction.
  - Liệt kê các Auction.
  - Chèn một dòng lịch sử giá thầu (Bid history).

#### `Database`
- Sử dụng SQLite hiện tại.
- Schema nên được thiết kế để việc chuyển sang PostgreSQL sau này dễ quản lý.

## Thuật ngữ dùng chung

### DTO
DTO có nghĩa là **Data Transfer Object**. Một DTO là một đối tượng đơn giản được dùng để truyền dữ liệu giữa 
các phần của hệ thống, đặc biệt là giữa client và server. Nó không nên chứa logic nghiệp vụ.
- Ví dụ: `LoginRequestDTO`, `LoginResponseDTO`, `PlaceBidRequestDTO`, `AuctionSummaryDTO`.

### Repository
Repository là lớp truy cập dữ liệu (Data access layer). Công việc của nó là nói chuyện với cơ sở 
dữ liệu và chạy các truy vấn SQL. Trong nhiều cuộc thảo luận, khái niệm này rất gần với DAO.

### Service

Service là lớp logic nghiệp vụ (Business logic layer). Trong kiến trúc này, chúng ta phân biệt rõ:
- **Server Service**: Nắm giữ "sự thật" và thực thi luật chơi (Business Rules). Chỉ có lớp này mới được gọi Repository để đọc/ghi Database.
- **Client Service**: Hỗ trợ UI. Không chạm vào Database. Nhiệm vụ chính là đóng gói dữ liệu vào DTO, gửi qua Socket và xử lý phản hồi từ Server để điều hướng màn hình hoặc hiện thông báo.



## Các Hợp đồng dùng chung (Shared Contracts) cần thống nhất trước

Nhóm không cần phải định nghĩa mọi chi tiết tương lai ngay lập tức. Nhóm nên định nghĩa các 
hợp đồng chung để gỡ bỏ sự tắc nghẽn trong phát triển song song.

### 1. Định dạng Message envelope
Nhóm nên thống nhất về một định dạng Request chung và một định dạng Response chung. Ví dụ:

```json
{
  "type": "LOGIN",
  "requestId": "uuid",
  "payload": {
    "username": "alice",
    "password": "123456"
  }
}
```

```json
{
  "requestId": "uuid",
  "success": true,
  "payload": {
    "role": "ADMIN"
  },
  "error": null
}
```

Các trường dữ liệu chính xác có thể thay đổi. Điểm quan trọng là tất cả các tin nhắn đều tuân theo một cấu trúc dùng chung.

### 2. Các loại tin nhắn cốt lõi (Core message types)
Nhóm nên thống nhất về bộ tin nhắn đầu tiên cần thiết cho hệ thống tích hợp cơ bản. Đề xuất:
- `LOGIN`, `REGISTER`, `LIST_AUCTIONS`, `GET_AUCTION_DETAIL`, `PLACE_BID`, `CREATE_AUCTION`, `CANCEL_AUCTION`.

### 3. Quy tắc sở hữu Payload (Payload ownership rule)
Toàn bộ nhóm nên thống nhất về:
- Message envelope.
- Quy ước đặt tên.
- Cấu trúc phản hồi lỗi (Error response shape).
- Các loại tin nhắn cốt lõi ban đầu.

Sau đó, người sở hữu từng mảng tính năng có thể tự định nghĩa chi tiết Payload cho tin nhắn của họ, nhưng những chi tiết đó vẫn nên được review với nhóm trước khi chốt chính thức. Điều này giúp giữ tính nhất quán mà không làm chậm tốc độ làm việc của mọi người.

### 4. Database schema
Nhóm nên sớm chốt phiên bản đầu tiên của Schema. Ít nhất là định nghĩa mục đích và các trường chính của: `users`, `items`, `auctions`, `bids`.
Cần trả lời:
- Bảng nào sở hữu dữ liệu nào?
- Khóa chính (Primary keys) là gì? Khóa ngoại (Foreign keys) là gì?
- Những trường nào đại diện cho trạng thái đấu giá (Auction state)?

### 5. Sở hữu quy tắc nghiệp vụ (Business rule ownership)
Một số quy tắc nên được viết xuống tại một nơi để nhóm không thực hiện chúng khác nhau ở các module khác nhau. Ví dụ: Các trạng thái và bước chuyển của đấu giá, ai có quyền đặt giá, ai có quyền hủy đấu giá, v.v.

## Những gì cần tránh trong giai đoạn này

- Không để Client Controller truy cập database trực tiếp.
- Không viết các quy tắc nghiệp vụ quan trọng bên trong JavaFX Controller.
- Không đặt SQL trực tiếp trong Socket Handler nếu logic đó có thể được tái sử dụng ở nơi khác.
- Không cho phép mỗi chủ sở hữu tính năng tự chế ra định dạng tin nhắn khác nhau.

## Cột mốc (Milestone) đề xuất cho cuối giai đoạn này

Trước khi chia ra làm việc độc lập, nhóm nên đặt mục tiêu chạy được một luồng xuyên suốt (end-to-end flow) đơn giản:
`register or login -> list auctions -> open auction detail -> place bid -> receive server response`

Luồng này không cần phải được trau chuốt. Nó chỉ cần chứng minh rằng: Client có thể nói chuyện với Server, Server có thể điều hướng tin nhắn, Service có thể thực thi quy tắc, Repository có thể lưu và tải dữ liệu.

## Phân chia công việc sau khi hạ tầng đã sẵn sàng

Đối với nhóm 4 thành viên, một sự phân chia thực tế là:
- **Thành viên 1:** Vận chuyển và Giao thức (Transport & Protocol - code socket, message router).
- **Thành viên 2:** Xác thực và Dữ liệu người dùng (Auth & User data).
- **Thành viên 3:** Đấu giá và Logic đặt giá (Auction & Bidding logic).
- **Thành viên 4:** Tích hợp Client và kết nối UI (kết nối màn hình UI với phản hồi thực từ Server).

## Hướng dẫn quy trình làm việc của nhóm

- **Sử dụng milestones hàng tuần, không phải ownership hàng tuần:** Kế hoạch của giảng viên nên được coi là cột mốc tiến độ, không phải là cách chính xác để phân việc.
- **Thảo luận về các thay đổi giao diện (interface) sớm:** Nếu một thành viên muốn thay đổi định dạng tin nhắn, DB schema, cấu trúc package, hoặc chữ ký phương thức service dùng chung, thay đổi đó phải được thảo luận trước khi thực hiện.
- **Ưu tiên Interface mỏng (Thin interface) giữa các thành viên:** Người làm UI phụ thuộc vào DTO và Client Service interface, không phải nội bộ server thô. Người làm Service phụ thuộc vào Repository interface, không phải trực tiếp logic controller.
