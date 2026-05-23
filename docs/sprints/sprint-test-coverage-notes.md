# Tài liệu Tổng kết & Hướng dẫn Bảo vệ Sprint: Tối ưu hóa & Nâng cao Độ phủ Test (100% Coverage)

Tài liệu này tổng hợp toàn bộ các phần việc kiểm thử (Testing) đã thực hiện để đưa độ phủ mã nguồn (Test Coverage) toàn hệ thống lên sát 100% (sử dụng **JUnit 5**, **Mockito** và **JaCoCo**), đồng thời chuẩn bị sẵn các điểm kỹ thuật quan trọng và lưu ý chuyên môn khi bảo vệ trước Hội đồng phản biện.

---

## 🚀 Tổng quan Công việc Hoàn thành

Chúng ta đã thực hiện đợt tối ưu hóa kiểm thử toàn diện, **chỉ tác động duy nhất trên thư mục `src/test/`** (không thay đổi mã nguồn nghiệp vụ chính trong `src/main/` để đảm bảo tính nguyên vẹn của production code). Dưới đây là danh sách chi tiết các File và Test Case đã bổ sung/tối ưu hóa:

### 1. Phân hệ Client & UI Controllers (`src/test/java/com/nhom1/auction/client/`)

#### 📂 File [UserControllerTest.java](file:///c:/Users/ACER/BTL_Nhom1_AuctionSystem/src/test/java/com/nhom1/auction/client/user/controller/UserControllerTest.java)
*   `testAuctionBrowseControllerPushHandlers()`: 
    *   **Mục tiêu:** Kiểm thử bộ nhận tin nhắn đẩy (Push Notification) thời gian thực của màn hình duyệt đấu giá.
    *   **Cách xử lý:** Giả lập đăng ký push handler cho `PUSH_NEW_AUCTION` (tự động nạp lại danh sách), `PUSH_BID_UPDATE` (cập nhật động nhãn giá cao nhất), và `PUSH_AUCTION_DELETED` (xóa đấu giá khỏi danh sách hiển thị).
    *   **Kỹ thuật:** Sử dụng gọi `waitForRunLater()` hai lần liên tiếp để đồng bộ hóa hàng đợi JavaFX UI Thread lồng nhau.
*   `testAuctionDetailControllerPushAndFocus()`:
    *   **Mục tiêu:** Kiểm thử hành vi tương tác tiêu điểm và cập nhật giá chi tiết.
    *   **Cách xử lý:** Sử dụng Reflection gọi trực tiếp phương thức package-private `setFocused(boolean)` trên lớp `Node` của JavaFX để giả lập người dùng click chuột vào ô nhập giá đấu (`txtBidInput`), từ đó kiểm chứng hành vi ẩn nhãn báo lỗi.
*   `testMyListingsControllerDelete()`:
    *   **Mục tiêu:** Kiểm thử các nhánh rẽ của luồng xóa sản phẩm đấu giá (My Listings).
    *   **Cách xử lý:** Sử dụng Mockito `mockConstruction(Alert.class)` để chặn việc hiển thị hộp thoại xác nhận GUI thật, stub phương thức `showAndWait()` trả về `ButtonType.OK` hoặc `ButtonType.CANCEL` để kiểm thử cả hai luồng nghiệp vụ: người dùng xác nhận xóa (gửi request lên server) và người dùng hủy bỏ hành động xóa.
*   `testMyListingsControllerFormatMethods()`:
    *   **Mục tiêu:** Đạt độ phủ 100% cho các phương thức tiện ích định dạng chuỗi và thời gian còn lại của phiên đấu giá trong danh sách.

#### 📂 File [AdminControllerTest.java](file:///c:/Users/ACER/BTL_Nhom1_AuctionSystem/src/test/java/com/nhom1/auction/client/admin/controller/AdminControllerTest.java)
*   `testAuctionManagementControllerHelperMethodsAndStatusPills()`:
    *   **Mục tiêu:** Kiểm thử việc định dạng giao diện quản lý của Admin.
    *   **Cách xử lý:** Đưa vào các trạng thái đấu giá đa dạng (`RUNNING`, `PAID`, `CANCELED`,...) để kiểm tra màu sắc tương ứng của các viên trạng thái (Status Pills), đồng thời kiểm tra hàm rút gọn ID đấu giá (`shortId()`) cho các chuỗi dài hơn 8 ký tự.
*   `testUserManagementControllerHelperMethodsAndAdminRole()`:
    *   **Mục tiêu:** Kiểm thử logic phân quyền giao diện Admin.
    *   **Cách xử lý:** Giả lập phiên đăng nhập với vai trò người dùng thường vs. vai trò `ADMIN` để kiểm chứng logic vô hiệu hóa các nút chức năng tương ứng trên màn hình quản lý thành viên.

---

### 2. Phân hệ Server & Infrastructure (`src/test/java/com/nhom1/auction/server/`)

#### 📂 File [ServerTest.java](file:///c:/Users/ACER/BTL_Nhom1_AuctionSystem/src/test/java/com/nhom1/auction/server/ServerTest.java)
*   `testServerMainWithPortEnv()`:
    *   **Mục tiêu:** Kiểm thử khả năng khởi động Server trên cổng bất kỳ được truyền qua biến môi trường `PORT`.
    *   **Cách xử lý:** Tạo hàm bổ trợ `setEnv` sử dụng Reflection để can thiệp trực tiếp vào Map biến môi trường nội bộ của JVM (`java.lang.ProcessEnvironment` trên Windows / Map lưu trữ trên các HĐH khác). Sau đó, đặt `PORT=9999`, gọi hàm `main()` của server và kiểm chứng server lắng nghe đúng cổng cấu hình thay vì cổng mặc định `8080`.

#### 📂 File [ServerConnectionTest.java](file:///c:/Users/ACER/BTL_Nhom1_AuctionSystem/src/test/java/com/nhom1/auction/client/user/connection/ServerConnectionTest.java)
*   `testHandleRawResponse_MappingFailureCompletesExceptionally()`:
    *   **Mục tiêu:** Xử lý các phản hồi hỏng từ server mà không làm client bị treo vô hạn (UI Freeze).
    *   **Cách xử lý:** Giả lập một request đang chờ kết quả với `requestId` xác định. Sau đó gọi phản hồi giả lập với chuỗi JSON bị lỗi cú pháp cấu trúc nghiêm trọng. Kiểm chứng xem `CompletableFuture` tương ứng có bị kích hoạt trạng thái hoàn thành bất thường (`completedExceptionally`) với ngoại lệ Jackson Mapping hay không.
*   `testStartListeningThread_EofAndException()`:
    *   **Mục tiêu:** Kiểm thử an toàn luồng đọc tin socket của client.
    *   **Cách xử lý:** Giả lập socket ném ra `IOException` hoặc trả về `null` (EOF) để kiểm chứng hệ thống tự động phát hiện mất kết nối và chuyển cờ `isConnected()` về `false`.

#### 📂 File [PaymentRepositoryTest.java](file:///c:/Users/ACER/BTL_Nhom1_AuctionSystem/src/test/java/com/nhom1/auction/server/payment/PaymentRepositoryTest.java)
*   `testRepositoryConnExceptions()`:
    *   **Mục tiêu:** Kiểm thử tính ổn định của tầng thao tác cơ sở dữ liệu (Repository).
    *   **Cách xử lý:** Giả lập `DataSource.getConnection()` hoặc `Connection.prepareStatement()` ném ra `SQLException`. Kiểm chứng các phương thức lưu giao dịch, kiểm tra giao dịch tồn tại, lấy danh sách thanh toán chờ đều bắt được `SQLException`, thực hiện đóng tài nguyên an toàn và ném lại thành `RuntimeException` với thông điệp rõ ràng để báo lỗi cho tầng trên.

#### 📂 File [ClientServiceTest.java](file:///c:/Users/ACER/BTL_Nhom1_AuctionSystem/src/test/java/com/nhom1/auction/client/user/service/ClientServiceTest.java)
*   `testBidderRequests()`:
    *   **Mục tiêu:** Kiểm thử toàn diện cấu trúc dữ liệu chuyển giao (DTO).
    *   **Cách xử lý:** Kiểm thử Constructor không tham số, Constructor đầy đủ tham số và các cặp Getter/Setter của lớp `BiddingClientService.BidderRequest`.

---

## 💡 Các Điểm Kỹ Thuật Trọng Tâm Cần Lưu Ý (Defense Guide)

Dưới đây là chi tiết các điểm kỹ thuật phức tạp và giải pháp đã áp dụng trong quá trình tối ưu hóa bộ test để đạt độ phủ cao, cần ghi nhớ để chuẩn bị giải trình chuyên môn:

### 1. Đồng bộ hóa luồng sự kiện JavaFX (Double-nested `Platform.runLater`)
*   **Chi tiết hoạt động:** Khi Server gửi dữ liệu cập nhật thời gian thực (như giá đấu mới hoặc tạo mới đấu giá), luồng đọc Socket nhận dữ liệu và đẩy một tác vụ xử lý cập nhật UI vào hàng đợi JavaFX Application Thread thông qua `Platform.runLater(handler)`.
*   **Điểm cần lưu ý:**
    *   Bên trong handler này, khi cần vẽ lại các phần tử giao diện hoặc tải lại danh sách các phòng đấu giá, Controller lại tiếp tục gọi một lệnh `Platform.runLater()` thứ hai. Điều này tạo ra một kiến trúc **hàng đợi sự kiện lồng hai cấp**.
    *   Trong unit test, một cuộc gọi `waitForRunLater()` đơn lẻ chỉ đồng bộ và giải phóng hàng đợi ở cấp một.
    *   **Giải pháp:** Để đảm bảo các thay đổi giao diện cấp hai được cập nhật hoàn toàn trước khi kiểm tra (`assertEquals`), bắt buộc phải gọi `waitForRunLater()` hai lần liên tiếp. Nếu chỉ gọi một lần, các Assert dữ liệu sẽ chạy trước khi luồng UI cập nhật xong, dẫn đến lỗi test thất bại ngẫu nhiên (flaky tests).

### 2. Thiết lập tiêu điểm (Focus) giả lập trong môi trường Headless bằng Reflection
*   **Chi tiết hoạt động:** Khi chạy test tự động trên môi trường tích hợp liên tục (CI/CD) hoặc kiểm thử không có card đồ họa (Headless Mode), JavaFX không khởi tạo một màn hình hiển thị thật (no active Stage/Window).
*   **Điểm cần lưu ý:**
    *   Gọi phương thức `txtBidInput.requestFocus()` tiêu chuẩn sẽ không có tác dụng do không có cửa sổ thật để nhận diện tiêu điểm hoạt động.
    *   Nếu cố gắng chỉnh sửa thuộc tính `focusedProperty()` bằng cách ép kiểu sang `ReadOnlyBooleanWrapper` để gán giá trị, JVM sẽ ném lỗi đóng gói module `ClassCastException` từ module `javafx.graphics`.
    *   **Giải pháp:** Sử dụng Reflection để can thiệp trực tiếp và gọi phương thức nội bộ `setFocused(boolean)` cấp gói (package-private) của lớp `Node`. Cách này cho phép chuyển đổi trạng thái tiêu điểm một cách giả lập để kiểm tra tính đúng đắn của logic UI (ví dụ: tự động ẩn/xóa nhãn thông báo lỗi khi người dùng trỏ chuột vào ô nhập liệu).

### 3. Mock hộp thoại Alert (`showAndWait()`) phi giao diện bằng `mockConstruction`
*   **Chi tiết hoạt động:** Các tính năng như xóa sản phẩm trong `MyListingsController` đòi hỏi người dùng xác nhận thông qua hộp thoại `Alert`.
*   **Điểm cần lưu ý:**
    *   Hộp thoại `Alert` thật sẽ gọi phương thức block luồng `showAndWait()` để chờ tương tác của người dùng. Việc này sẽ làm tiến trình chạy test tự động bị treo vĩnh viễn.
    *   **Giải pháp:** Sử dụng tính năng `mockConstruction(Alert.class)` của Mockito để kiểm soát toàn bộ vòng đời khởi tạo của `Alert` trong phạm vi test case. Mỗi khi code nghiệp vụ chạy lệnh `new Alert(...)`, Mockito sẽ thay thế bằng một đối tượng Mock. Ta stub phương thức `showAndWait()` của đối tượng mock này trả về `Optional.of(ButtonType.OK)` hoặc `ButtonType.CANCEL` để kiểm thử toàn diện cả 2 luồng logic: người dùng đồng ý xóa hoặc người dùng hủy bỏ hành động.

### 4. Thay đổi biến môi trường hệ thống (`PORT` binding) qua Reflection
*   **Chi tiết hoạt động:** Để kiểm thử luồng cấu hình khởi chạy server dynamic trên các cổng mạng tùy biến đọc từ biến môi trường `System.getenv("PORT")`.
*   **Điểm cần lưu ý:**
    *   Từ Java 9 trở đi, map biến môi trường trả về từ `System.getenv()` được thiết kế ở dạng chỉ đọc (Immutable Map). Mọi thao tác sửa đổi trực tiếp đều bị chặn để đảm bảo an toàn hệ thống.
    *   **Giải pháp:** Sử dụng Reflection mở khóa quyền truy cập vào các trường ẩn lưu trữ cấu hình môi trường bên trong JVM (như trường `theUnmodifiableEnvironment` hoặc `theCaseInsensitiveEnvironment` của lớp `java.lang.ProcessEnvironment` trên hệ điều hành Windows). Qua đó, nạp cổng PORT tùy biến (ví dụ: `PORT=9999`) vào bản sao môi trường của JVM, cho phép hàm `main` của Server đọc và chạy trên cổng mong muốn một cách hoàn toàn tự động mà không ảnh hưởng tới biến môi trường thật của hệ điều hành.

### 5. Xử lý ngoại lệ Jackson Mapping trên luồng Socket bất động bộ
*   **Chi tiết hoạt động:** Client giao tiếp bất đồng bộ với Server thông qua Socket, mỗi request gửi đi đăng ký một `CompletableFuture` chờ phản hồi dựa trên `requestId`.
*   **Điểm cần lưu ý:**
    *   Nếu gói tin trả về từ Server bị lỗi định dạng hoặc lỗi phân tích cú pháp JSON, Jackson ObjectMapper sẽ ném ra ngoại lệ.
    *   Nếu ngoại lệ này không được xử lý đúng cách, luồng kiểm tra phản hồi sẽ bị ngắt và `CompletableFuture` tương ứng sẽ rơi vào trạng thái chờ đợi vô hạn (treo giao diện/starvation).
    *   **Giải pháp:** Trong `ServerConnection`, luồng xử lý phản hồi thô (`handleRawResponse`) được bao bọc trong khối `try-catch`. Khi xảy ra lỗi parse JSON, hệ thống sẽ truy xuất `CompletableFuture` đang đợi và kích hoạt phương thức `completeExceptionally(exception)`. Điều này đảm bảo trả lỗi về cho luồng gọi ban đầu xử lý (ví dụ hiển thị thông báo lỗi trên UI) và giải phóng tài nguyên lập tức.

### 6. Nạp chồng kết nối (Connection Overloading) trong Repository cho Transaction
*   **Chi tiết hoạt động:** Các thao tác nghiệp vụ phức tạp như lưu thông tin thanh toán trong `PaymentRepository` thường cần phối hợp nhiều bước cập nhật trạng thái dữ liệu (đổi trạng thái đấu giá, tạo bản ghi lịch sử, trừ tiền ví,...).
*   **Điểm cần lưu ý:**
    *   Lớp Repository được thiết kế hai loại phương thức: loại tự quản lý kết nối lấy từ `DataSource` và loại nhận tham số `Connection conn` được truyền trực tiếp từ lớp Service.
    *   **Giải pháp:** Thiết kế nạp chồng này cho phép các Service tạo ra một transaction duy nhất, thực thi đồng thời nhiều câu lệnh SQL trên cùng một kết nối vật lý `conn`. Khi có bất kỳ một thao tác con nào thất bại và ném ra `SQLException`, Service có thể gọi `conn.rollback()` để hoàn trả toàn bộ trạng thái dữ liệu trước đó, đảm bảo tính toàn vẹn (ACID) của hệ thống cơ sở dữ liệu.
