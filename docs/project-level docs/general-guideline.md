## [cite_start]Hướng dẫn Bài tập lớn LTNC 2026 [cite: 1]

### [cite_start]Hướng dẫn Bài tập lớn [cite: 2]

[cite_start]**Lập trình nâng cao Học kỳ II, 2025-2026** [cite: 3]
[cite_start]**Hệ thống Đấu giá trực tuyến** [cite: 4]

-----

### [cite_start]A Lưu ý [cite: 6]

* Bây giờ là **Tuần 6**. [cite_start]Tuần 15 các nhóm sẽ trình bày và chấm điểm. [cite: 7]
* [cite_start]Bạn có **9 tuần** để hoàn thành toàn bộ dự án. [cite: 8]
* Bài giảng chỉ còn **5 buổi** (tuần 6-10). [cite_start]Từ tuần 11 trở đi không còn bài giảng, chỉ còn thời gian làm dự án. [cite: 9]
* [cite_start]**Lập trình mạng, JavaFX (UI), Lưu trữ dữ liệu** là nội dung tự học, cần bắt đầu ngay từ tuần này. [cite: 10]
* [cite_start]Mỗi thành viên phải hiểu toàn bộ mã nguồn; nếu không giải thích được, cả nhóm bị 0 điểm. [cite: 11]
* [cite_start]Commit thường xuyên lên GitHub; không chấp nhận chỉ 1 commit cuối kỳ. [cite: 12]

-----

### [cite_start]Tổng quan Bài tập lớn [cite: 13]

[cite_start]Xây dựng hệ thống đấu giá trực tuyến (**Online Auction System**) theo kiến trúc **Client-Server**, sử dụng **Java**, **JavaFX** và mô hình **MVC**. [cite: 14]

#### [cite_start]Các chức năng bắt buộc [cite: 15]

1.  [cite_start]Quản lý người dùng (Bidder / Seller / Admin) [cite: 16]
2.  [cite_start]Quản lý sản phẩm đấu giá (CRUD) [cite: 17]
3.  [cite_start]Tham gia đấu giá (đặt giá, kiểm tra hợp lệ, cập nhật realtime) [cite: 18]
4.  [cite_start]Kết thúc phiên đấu giá (tự động đóng, xác định người thắng) [cite: 19]
5.  [cite_start]Xử lý lỗi & ngoại lệ [cite: 20]
6.  [cite_start]Giao diện GUI (JavaFX) [cite: 21]
7.  [cite_start]Thiết kế OOP (kế thừa, đa hình, trừu tượng, đóng gói) [cite: 22]
8.  [cite_start]Design Patterns (Singleton, Factory, Observer) [cite: 23]
9.  [cite_start]Kiến trúc Client-Server + MVC [cite: 24]
10. [cite_start]Xử lý đấu giá đồng thời (concurrency) [cite: 25]
11. [cite_start]Unit Test (JUnit), CI/CD (GitHub Actions) [cite: 26]

#### [cite_start]Chức năng nâng cao (tuỳ chọn, tối đa +1.5₫) [cite: 27]

* [cite_start]**Auto-Bidding** (đấu giá tự động) [cite: 28]
* [cite_start]**Anti-sniping** (gia hạn phiên đấu giá) [cite: 29]
* [cite_start]**Bid History Visualization** (biểu đồ giá realtime) [cite: 30]

-----

### [cite_start]Nội dung giảng dạy & Nội dung tự học [cite: 32]

* [cite_start]Phân biệt nội dung trên lớp và tự học [cite: 33]
* Khoá học có 10 bài giảng (tuần 1-10). [cite_start]Từ tuần 11 trở đi không còn bài giảng. [cite: 34]
* [cite_start]Các nội dung sau không được dạy trên lớp mà sinh viên phải tự học: [cite: 35]
    * [cite_start]**Lập trình mạng** (Socket, Client-Server) [cite: 36]
    * [cite_start]**Giao diện người dùng** (JavaFX, SceneBuilder, MVC) [cite: 37]
    * [cite_start]**Lưu trữ dữ liệu** (Serialization, File $I/O$) [cite: 38]
* [cite_start]Bắt đầu tự học các nội dung này ngay từ bây giờ [cite: 39]

-----

### [cite_start]Lộ trình thực hiện theo tuần [cite: 40]

#### [cite_start]Tuần 6 – Khởi động, Thiết kế OOP & Bắt đầu JavaFX [cite: 41]

* [cite_start]**Bài giảng tuần này:** Mẫu thiết kế và nguyên lý thiết kế. [cite: 42]
* [cite_start]**Công việc cụ thể:** [cite: 42]
    1.  [cite_start]Đọc kỹ đề bài, thảo luận nhóm về scope và phân công. [cite: 43]
    2.  [cite_start]Tạo GitHub repository, thiết lập nhánh main và dev. [cite: 44]
    3.  [cite_start]Thiết kế sơ đồ lớp (class diagram): [cite: 44]
        * [cite_start]User (abstract) → Bidder, Seller, Admin [cite: 45]
        * [cite_start]Item (abstract) → Electronics, Art, Vehicle [cite: 46]
        * [cite_start]Auction, Bid [cite: 46]
    4.  [cite_start]Code các lớp cơ bản, áp dụng OOP (encapsulation, inheritance, polymorphism). [cite: 47]
    5.  [cite_start]Triển khai Singleton (Auction Manager) và Factory Method (tạo Item). [cite: 48]
    6.  [cite_start]**[TỰ HỌC]** Cài đặt JavaFX + SceneBuilder, chạy thử ứng dụng Hello World. [cite: 49]
* [cite_start]**Tài liệu học:** [cite: 50]
    * [cite_start]Design Patterns: [cite: 51, 52] [cite_start][https://www.youtube.com/watch?v=mE3qTp1TEbg\&list=PLlsmxlJgn1HJpa28yHzkBmUY-Ty71ZUGC](https://www.google.com/search?q=https://www.youtube.com/watch%3Fv%3DmE3qTp1TEbg%26list%3DPLlsmxlJgn1HJpa28yHzkBmUY-Ty71ZUGC) [cite: 53, 54]
    * [cite_start]Refactoring Guru: [https://refactoring.guru/design-patterns](https://refactoring.guru/design-patterns) [cite: 55]
    * [cite_start]JavaFX (Học theo thứ tự): [cite: 56]
        1.  [cite_start]Cài đặt JavaFX: [https://openjfx.io/openjfx-docs/](https://openjfx.io/openjfx-docs/) [cite: 57]
        2.  [cite_start]Video setup IntelliJ: [https://www.youtube.com/watch?v=Ope4icw6bVk](https://www.youtube.com/watch?v=Ope4icw6bVk) [cite: 58]
        3.  [cite_start]Playlist cơ bản (các component): [https://www.youtube.com/watch?v=\_70M-CMYWbQ\&list=PLZPZq0r\_RZOM-8vJA3NQFZB7JroDcMwev](https://www.google.com/search?q=https://www.youtube.com/watch%3Fv%3D_70M-CMYWbQ%26list%3DPLZPZq0r_RZOM-8vJA3NQFZB7JroDcMwev) [cite: 59]
        4.  [cite_start]Setup SceneBuilder + IntelliJ: [https://www.youtube.com/watch?v=IZCwawKILsk](https://www.youtube.com/watch?v=IZCwawKILsk) [cite: 60]
        5.  [cite_start]Controller trong SceneBuilder: [https://www.youtube.com/watch?v=OwQ68q7PD9w\&list=PLfu\_Bpi\_zcDNYL61710p3S1ABtuyFV7Nr\&index=16](https://www.google.com/search?q=https://www.youtube.com/watch%3Fv%3DOwQ68q7PD9w%26list%3DPLfu_Bpi_zcDNYL61710p3S1ABtuyFV7Nr%26index%3D16) [cite: 61]

#### [cite_start]Tuần 7 – Đa luồng, Observer & Phát triển GUI [cite: 64]

* [cite_start]**Bài giảng tuần này:** Lập trình đa luồng và song song. [cite: 65]
* [cite_start]**Công việc cụ thể:** [cite: 65]
    1.  [cite_start]Triển khai Observer Pattern để notify khi có bid mới. [cite: 66]
    2.  [cite_start]Code logic nghiệp vụ: tạo phiên đấu giá, đặt giá, kiểm tra hợp lệ. [cite: 67]
    3.  [cite_start]Viết logic chuyển trạng thái: OPEN → RUNNING → FINISHED → PAID/CANCELED. [cite: 68, 69, 70]
    4.  [cite_start]Xử lý đấu giá đồng thời (concurrent bidding) tránh lost update, race condition. [cite: 71, 72]
    5.  [cite_start]Sử dụng synchronized, ReentrantLock cho các thao tác quan trọng. [cite: 73]
    6.  [cite_start]**[TỰ HỌC]** Tiếp tục học JavaFX: xây dựng các màn hình cơ bản (Login, Danh sách). [cite: 74]
* [cite_start]**Tài liệu học:** [cite: 75]
    * [cite_start]Race Conditions: [https://www.youtube.com/watch?v=RMR75VzYoos](https://www.youtube.com/watch?v=RMR75VzYoos) [cite: 76, 77, 78, 79]

#### [cite_start]Tuần 8 – Kiểm thử, Ngoại lệ & GUI nâng cao [cite: 80]

* [cite_start]**Bài giảng tuần này:** Kiểm thử và Tái cấu trúc mã nguồn. [cite: 81]
* [cite_start]**Công việc cụ thể:** [cite: 81]
    1.  [cite_start]Tạo custom exceptions: InvalidBidException, AuctionClosedException, AuthenticationException. [cite: 82]
    2.  [cite_start]Xử lý ngoại lệ cho: đặt giá thấp hơn hiện tại, đấu giá khi phiên đóng, lỗi dữ liệu. [cite: 83]
    3.  [cite_start]Viết unit test (JUnit) cho logic đấu giá: đặt giá hợp lệ/không hợp lệ, kết thúc phiên. [cite: 84]
    4.  [cite_start]Refactor code: loại bỏ code smells, áp dụng SOLID. [cite: 85]
    5.  [cite_start]**[TỰ HỌC]** Hoàn thiện GUI JavaFX áp dụng MVC, tách logic khỏi Controller, dùng FXML. [cite: 86, 87]
* [cite_start]**Tài liệu học:** [cite: 88]
    * [cite_start]JUnit 5 User Guide: [https://docs.junit.org/5.5.0/user-guide/](https://docs.junit.org/5.5.0/user-guide/) [cite: 90]
    * [cite_start]MVC trong JavaFX: [cite: 91]
        * [cite_start]Repo mẫu MVC: [https://github.com/ZacharyDavidSaunders/Inventory](https://www.google.com/search?q=https://github.com/ZacharyDavidSaunders/Inventory) Management System [cite: 92]
        * [cite_start]Tài liệu MVC: [https://www.pragmaticcoding.ca/javafx/MVC\_In\_JavaFX](https://www.pragmaticcoding.ca/javafx/MVC_In_JavaFX) [cite: 93]
        * [cite_start]Video MVC: [https://www.youtube.com/watch?v=Mu3VNzwpFII](https://www.youtube.com/watch?v=Mu3VNzwpFII) [cite: 94]

#### [cite_start]Tuần 9 – Tích hợp, CI/CD & Lập trình mạng [cite: 97]

* [cite_start]**Bài giảng tuần này:** Tích hợp và triển khai. [cite: 98]
* [cite_start]**Công việc cụ thể:** [cite: 98]
    1.  [cite_start]Cấu hình Maven: quản lý dependencies, build tự động. [cite: 99]
    2.  [cite_start]Tích hợp Checkstyle vào Maven để enforce coding convention. [cite: 100]
    3.  [cite_start]Thiết lập GitHub Actions: tự động build + chạy test khi push. [cite: 101]
    4.  [cite_start]**[TỰ HỌC]** Bắt đầu triển khai Client-Server bằng Java Socket. [cite: 102]
    5.  [cite_start]**[TỰ HỌC]** Triển khai Serialization để lưu/tải dữ liệu. [cite: 103]
* [cite_start]**Tài liệu học:** [cite: 104]
    * [cite_start]CI/CD: [https://www.youtube.com/watch?v=UTb3nNbH7M4](https://www.youtube.com/watch?v=UTb3nNbH7M4) [cite: 106]
    * [cite_start]Checkstyle + Maven: [https://medium.com/@sruthiganesh/integrating-checkstyle-in-java-projects-with-maven-b1ac2cafd016](https://medium.com/@sruthiganesh/integrating-checkstyle-in-java-projects-with-maven-b1ac2cafd016) [cite: 107]
    * [cite_start]Serialization: [cite: 108]
        * [cite_start]Video: [https://www.youtube.com/watch?v=DfbFTVNfkeI](https://www.youtube.com/watch?v=DfbFTVNfkeI) [cite: 108]
        * [cite_start]GeeksforGeeks: [https://www.geeksforgeeks.org/java-serialization-and-deserialization-in-java/](https://www.google.com/search?q=https://www.geeksforgeeks.org/java-serialization-and-deserialization-in-java/) [cite: 110, 111]
    * [cite_start]Lập trình mạng (Học theo thứ tự): [cite: 114]
        1.  [cite_start]Cơ bản Socket: [https://www.youtube.com/watch?v=plh\_cIEQ1Jo](https://www.youtube.com/watch?v=plh_cIEQ1Jo) [cite: 115]
        2.  [cite_start]Baeldung Guide: [https://www.baeldung.com/a-guide-to-java-sockets](https://www.baeldung.com/a-guide-to-java-sockets) [cite: 116, 120]
        3.  [cite_start]Gửi Serialized Objects: [https://www.youtube.com/watch?v=lup-oHjCcis](https://www.google.com/search?q=https://www.youtube.com/watch%3Fv%3Dlup-oHjCcis) [cite: 117, 118, 121]
        4.  [cite_start]App nhắn tin realtime (JavaFX + Socket): [https://www.youtube.com/watch?v=\_1nqY-DKP9A](https://www.youtube.com/watch?v=_1nqY-DKP9A) [cite: 119]

#### [cite_start]Tuần 10 – Hướng dẫn tự học nâng cao & Ôn tập (Buổi giảng cuối) [cite: 122, 123]

* [cite_start]**Bài giảng tuần này:** Hướng dẫn tự học các nội dung nâng cao và Ôn tập. [cite: 124]
* [cite_start]**Công việc cụ thể:** [cite: 124]
    1.  [cite_start]**[TỰ HỌC]** Hoàn thiện kiến trúc Client-Server: Server xử lý nhiều client đồng thời. [cite: 125]
    2.  [cite_start]**[TỰ HỌC]** Tích hợp Observer Pattern qua Socket để realtime update. [cite: 126]
    3.  [cite_start]Hoàn thiện các màn hình JavaFX chính: [cite: 127]
        * [cite_start]Đăng nhập / Đăng ký [cite: 128]
        * [cite_start]Danh sách phiên đấu giá [cite: 129]
        * [cite_start]Chi tiết sản phẩm [cite: 130]
        * [cite_start]Màn hình đấu giá trực tiếp (realtime bidding) [cite: 131]
        * [cite_start]Quản lý sản phẩm (Seller) [cite: 132]
    4.  [cite_start]Bổ sung thêm unit test, đạt code coverage ≥ 60%. [cite: 133]

#### [cite_start]Tuần 11-12 – Tích hợp toàn bộ hệ thống [cite: 134]

* [cite_start]**Không còn bài giảng** - tập trung hoàn toàn vào dự án. [cite: 135]
* [cite_start]**Công việc cụ thể:** [cite: 135]
    1.  [cite_start]Tích hợp toàn bộ: GUI → Logic → Network → Data. [cite: 137]
    2.  [cite_start]Kiểm thử end-to-end: chạy Server + nhiều Client đồng thời. [cite: 138]
    3.  [cite_start]Fix bugs, xử lý edge cases. [cite: 139]
    4.  [cite_start]Hoàn thiện Serialization / lưu trữ dữ liệu. [cite: 140]
    5.  [cite_start]Đảm bảo CI/CD chạy xanh trên GitHub. [cite: 141]

#### [cite_start]Tuần 13-14 – Hoàn thiện & Chức năng nâng cao [cite: 142]

* [cite_start]**Công việc cụ thể:** [cite: 143]
    1.  [cite_start]Polish giao diện, cải thiện UX. [cite: 144]
    2.  [cite_start]Kiểm thử kỹ lưỡng toàn bộ hệ thống. [cite: 145]
    3.  (Nếu kịp) [cite_start]Triển khai chức năng nâng cao: [cite: 146]
        * [cite_start]Auto-Bidding (maxBid, increment, PriorityQueue) [cite: 147]
        * [cite_start]Anti-sniping (gia hạn khi có bid cuối) [cite: 148]
        * [cite_start]Biểu đồ giá realtime (LineChart JavaFX) [cite: 149]
    4.  [cite_start]Viết README.md đầy đủ trên GitHub (hướng dẫn cài đặt, chạy). [cite: 150]
* [cite_start]**Tài liệu học:** [cite: 151]
    * [cite_start]Realtime LineChart: [https://www.youtube.com/watch?v=HWfZPiPulsI](https://www.google.com/search?q=https://www.youtube.com/watch%3Fv%3DHWfZPiPulsI) [cite: 152, 153, 154]

#### [cite_start]Tuần 15 – Trình bày & Chấm điểm [cite: 155, 156]

* [cite_start]**Công việc:** [cite: 157]
    1.  [cite_start]Chuẩn bị slide trình bày (kiến trúc, demo, đóng góp). [cite: 158]
    2.  [cite_start]Mỗi thành viên phải giải thích được mọi phần code. [cite: 159]
    3.  [cite_start]Demo trực tiếp hệ thống (Server + nhiều Client). [cite: 160]
    4.  [cite_start]Phân chia điểm theo đóng góp thực tế. [cite: 161]

-----

### [cite_start]Bảng tổng hợp lộ trình [cite: 162, 163]

| Tuần | Bài giảng trên lớp | Việc cần làm cho BTL | Tự học |
| :--- | :--- | :--- | :--- |
| 6 | Mẫu thiết kế & Nguyên lý thiết kế | Khởi tạo dự án, thiết kế OOP, Singleton, Factory | JavaFX + SceneBuilder |
| 7 | Đa luồng & Song song | Observer, logic đấu giá, concurrency | JavaFX màn hình cơ bản |
| 8 | Kiểm thử & Tái cấu trúc | Custom exceptions, JUnit, refactor SOLID | MVC trong JavaFX |
| 9 | Tích hợp & Triển khai | Maven, Checkstyle, GitHub Actions | Socket, Serialization |
| 10 | Hướng dẫn tự học & Ôn tập (buổi cuối) | Client-Server, hoàn thiện GUI | Networking, lưu trữ |
| 11-12 | Không còn bài giảng | Tích hợp toàn bộ, e2e testing, fix bugs | Tổng hợp |
| 13-14 | Không còn bài giảng | Polish UI, chức năng nâng cao | Tổng hợp |
| 15 | Trình bày | Demo & Chấm điểm | |

-----

### [cite_start]Dự án tham khảo trên GitHub [cite: 167]

1.  [cite_start]**Auction System** (Multi-client, JavaFX): [https://github.com/nlintas/Auction-System-in-Java](https://github.com/nlintas/Auction-System-in-Java) [cite: 168, 169]
2.  [cite_start]**Socket Auction** (Kotlin + JavaFX): [https://github.com/gangulwar/socket-programming-auction-system](https://github.com/gangulwar/socket-programming-auction-system) [cite: 170, 171]
3.  [cite_start]**Auction + MySQL Database**: [https://github.com/Prasanna-icefire/AuctionSystem](https://github.com/Prasanna-icefire/AuctionSystem) [cite: 172, 173]
4.  [cite_start]**Auction + Google Gson + Azure**: [https://github.com/AqibMughal1/Auction-System-JavaFX](https://github.com/AqibMughal1/Auction-System-JavaFX) [cite: 174, 175]

-----

### [cite_start]Thang điểm tóm tắt [cite: 176, 177]

| Nội dung | Điểm | Mức |
| :--- | :--- | :--- |
| Thiết kế lớp và cây kế thừa | 0.5 | Bắt buộc |
| Áp dụng OOP (Encapsulation, Inheritance, Polymorphism, Abstraction) | 1.0 | Bắt buộc |
| Design Patterns phù hợp | 1.0 | Bắt buộc |
| Quản lý người dùng, sản phẩm | 1.0 | Bắt buộc |
| Chức năng đấu giá | 1.0 | Bắt buộc |
| Xử lý lỗi & ngoại lệ | 1.0 | Bắt buộc |
| Xử lý đấu giá đồng thời (concurrency) | 1.0 | Bắt buộc |
| Realtime update (Observer/Socket) | 0.5 | Bắt buộc |
| Kiến trúc Client-Server | 0.5 | Bắt buộc |
| MVC (JavaFX FXML, Controller-Model-DAO) | 0.5 | Bắt buộc |
| Maven/Gradle, coding convention | 0.5 | Bắt buộc |
| Unit Test (JUnit) | 0.5 | Bắt buộc |
| CI/CD (GitHub Actions) | 0.5 | Bắt buộc |
| Auto-Bidding | 0.5 | Tuỳ chọn |
| Anti-sniping | 0.5 | Tuỳ chọn |
| Bid History Visualization | 0.5 | Tuỳ chọn |
| **Tổng** | **10 + 1** | |

-----

### [cite_start]Lời khuyên [cite: 180]

1.  [cite_start]**Bắt đầu học JavaFX ngay từ tuần này** - đây là nội dung tự học, không chờ bài giảng. [cite: 182]
2.  [cite_start]**Lập trình mạng cũng là tự học** - bắt đầu đọc tài liệu Socket từ tuần 9 để kịp tích hợp. [cite: 183]
3.  **Commit thường xuyên**: mỗi ngày hoặc mỗi tính năng hoàn thành. [cite_start]Dùng Conventional Commits. [cite: 184]
4.  **Mỗi người phải code**: không để 1 người làm hết. [cite_start]Phân công theo module (Server, Client GUI, Logic, Test). [cite: 185]
5.  [cite_start]**Chạy song song**: trong khi 1-2 người làm backend logic, người khác học và làm GUI. [cite: 186]
6.  [cite_start]**Đọc code tham khảo**: clone các repo mẫu, chạy thử, đọc hiểu cấu trúc trước khi tự viết. [cite: 187]
7.  [cite_start]**Review code lẫn nhau**: dùng Pull Request trên GitHub để review trước khi merge. [cite: 188]

[cite_start]**A Lưu ý: Nội dung tự học rất quan trọng** [cite: 189]
[cite_start]Ba nội dung **JavaFX**, **Lập trình mạng (Socket)** và **Lưu trữ dữ liệu (Serialization)** đều không được dạy trên lớp nhưng là yêu cầu bắt buộc của bài tập lớn. [cite: 190] [cite_start]Hãy chủ động tự học ngay từ bây giờ. [cite: 191]

[cite_start]**Chúc các bạn hoàn thành tốt Bài tập lớn\!** [cite: 192]