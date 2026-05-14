# Image Upload and Retrieval Flow (Function-by-Function)

## Scope
Tài liệu này giải thích chi tiết theo từng hàm code cho luồng:
- Upload ảnh từ client lên imgBB.
- Gửi URL ảnh về server khi tạo auction.
- Lưu và đọc ảnh từ bảng `item_images`.
- Hiển thị ảnh ở màn hình chi tiết.

## A. Client side

### 1) `CreateAuctionController.handleChoosePhotos()`
- File: `src/main/java/com/nhom1/auction/client/user/controller/CreateAuctionController.java`
- Mục đích: Cho user chọn nhiều file ảnh từ máy.
- Input: thao tác click nút `Choose photos`.
- Xử lý:
  - Tạo `FileChooser`, lọc extension `*.png`, `*.jpg`, `*.jpeg`.
  - Gọi `showOpenMultipleDialog(...)`.
  - Nếu user không chọn file: clear danh sách và set label `No photo selected`.
  - Nếu có file: cập nhật `selectedImageFiles`.
  - Hiển thị tên 1-2 file đầu + số file còn lại.
- Output:
  - Danh sách file local trong `selectedImageFiles`.
  - UI label mô tả số ảnh đã chọn.

### 2) `CreateAuctionController.handlePublishListing()`
- Mục đích: Điều phối toàn bộ flow publish.
- Input: click nút `Publish listing`.
- Xử lý:
  - Gọi `validateInput()` để kiểm tra dữ liệu form.
  - Set trạng thái UI `Uploading images...`.
  - Gọi `uploadImagesToImgbb()` để upload ảnh (nếu có).
  - Sau khi có `List<String> imageUrls`, gọi `sendCreateAuctionRequest(imageUrls)`.
  - Nhận response:
    - success: báo `Published successfully`, điều hướng `MY_LISTINGS`.
    - fail: hiển thị lỗi từ server.
  - Nếu exception: hiển thị qua `resolveErrorMessage(ex)`.
- Output:
  - Auction được tạo hoặc thông báo lỗi chi tiết.

### 3) `CreateAuctionController.validateInput()`
- Mục đích: Fail fast trước khi gọi network/API.
- Kiểm tra:
  - User đăng nhập hợp lệ.
  - Title không rỗng.
  - Category/Condition đã chọn.
  - Starting bid parse được `BigDecimal`.
  - Duration > 0.
- Output:
  - `null` nếu hợp lệ.
  - Chuỗi lỗi nếu không hợp lệ.

### 4) `CreateAuctionController.uploadImagesToImgbb()`
- Mục đích: Upload tất cả ảnh local lên imgBB.
- Xử lý:
  - Nếu không có ảnh: trả `CompletableFuture.completedFuture(List.of())`.
  - Nếu có ảnh:
    - map từng `File` -> `imageUploadService.upload(file)` thành danh sách future.
    - `CompletableFuture.allOf(...)` đợi tất cả hoàn thành.
    - `join()` lấy URL theo đúng thứ tự danh sách ban đầu.
- Output:
  - `CompletableFuture<List<String>>` chứa URL public ảnh.

### 5) `CreateAuctionController.sendCreateAuctionRequest(List<String> imageUrls)`
- Mục đích: Gửi request tạo auction về server.
- Xử lý:
  - Gọi `buildCreateAuctionRequest()`.
  - Gán `dto.setImageUrls(imageUrls)`.
  - Bọc vào `RequestMessage<>(MessageType.CREATE_AUCTION, dto)`.
  - Gửi qua `ServerConnection.getInstance().sendRequest(...)`.
- Output:
  - `CompletableFuture<ResponseMessage<CreateAuctionResponse>>`.

### 6) `CreateAuctionController.buildCreateAuctionRequest()`
- Mục đích: Chuẩn hóa dữ liệu từ form thành DTO backend.
- Xử lý:
  - Set sellerId, name, description, category, condition.
  - Tính `startTime = now`, `endTime = now + duration`.
  - Set default field theo category:
    - `ART`: artist/era = `Unknown`.
    - `ELECTRONICS`: brand = `Unknown`, warrantyMonths = `0`.
    - `VEHICLE`: brand = `Unknown`, productionYear = `2000`.
- Output:
  - `CreateAuctionRequest` hoàn chỉnh (trừ `imageUrls` được gán ở bước gửi).

### 7) `CreateAuctionController.resolveErrorMessage(Throwable ex)`
- Mục đích: Chuyển exception thành text dễ hiểu cho UI.
- Quy tắc:
  - Không có message -> `Connection error.`
  - Chứa `IMGBB_API_KEY` -> báo thiếu key cấu hình.
  - Còn lại -> dùng message gốc.

### 8) `ImageUploadService.upload(File imageFile)`
- File: `src/main/java/com/nhom1/auction/client/user/service/ImageUploadService.java`
- Mục đích: Upload 1 file ảnh lên imgBB.
- Xử lý:
  - Gọi `resolveApiKey()`.
  - Validate file tồn tại và là file thường.
  - `CompletableFuture.supplyAsync(...)` gọi `doUpload(...)`.
- Output:
  - `CompletableFuture<String>` là URL public imgBB.

### 9) `ImageUploadService.resolveApiKey()`
- Mục đích: Lấy API key an toàn.
- Thứ tự:
  1. Env `IMGBB_API_KEY`.
  2. File local `config.local.properties` với key `imgbb.api.key`.
- Nếu không có: ném `IllegalStateException("IMGBB_API_KEY is not configured")`.

### 10) `ImageUploadService.doUpload(String apiKey, File imageFile)`
- Mục đích: Thực hiện HTTP call thực tế đến imgBB.
- Xử lý:
  - Đọc bytes file.
  - Base64 encode.
  - Tạo body form-url-encoded: `key=...&image=...`.
  - POST đến `https://api.imgbb.com/1/upload`.
  - Kiểm tra status code 2xx.
  - Parse JSON:
    - `success == true`
    - `data.display_url` không rỗng
- Output:
  - Trả `display_url`.
- Exception:
  - IO/interrupt -> wrap thành `CompletionException`.

## B. Server side

### 11) `AuctionService.createAuction(String sellerId, CreateAuctionRequest dto)`
- File: `src/main/java/com/nhom1/auction/server/auction/AuctionService.java`
- Mục đích: Tạo auction theo transaction.
- Xử lý:
  - Validate request qua `validateCreateAuctionRequest(...)`.
  - Tạo `Item` qua `createItem(dto)`.
  - Mở transaction (`autoCommit=false`):
    1. `itemRepository.save(item, sellerId)`
    2. `itemImageRepository.saveImageUrls(item.getId(), dto.getImageUrls())`
    3. `auctionRepository.save(auction)`
    4. `auctionRepository.updateHighestBid(...)`
  - `commit` nếu thành công, `rollback` nếu lỗi.
- Output:
  - Trả `Auction` vừa tạo.

### 12) `ItemImageRepository.saveImageUrls(UUID itemId, List<String> imageUrls)`
- File: `src/main/java/com/nhom1/auction/server/auction/ItemImageRepository.java`
- Mục đích: Lưu danh sách URL ảnh vào DB.
- Xử lý:
  - Nếu null/rỗng thì return sớm.
  - Duyệt từng URL hợp lệ:
    - tạo `id` UUID
    - set `item_id`
    - tạo `object_key` qua `buildObjectKey(...)`
    - set `public_url`
    - set `is_primary` cho ảnh đầu
    - set `sort_order` tăng dần
    - set timestamps
  - Dùng batch insert.
- Output:
  - Không trả dữ liệu; ném exception nếu insert fail.

### 13) `ItemImageRepository.buildObjectKey(UUID itemId, int sortOrder, String imageUrl)`
- Mục đích: Sinh khóa logic để lưu metadata ảnh.
- Format:
  - `items/{itemId}/{sortOrder}-{filename}`
- `filename` lấy từ phần cuối URL.

### 14) `ItemImageRepository.findImageUrlsByItemId(UUID itemId)`
- Mục đích: Lấy danh sách URL ảnh theo thứ tự hiển thị.
- Query order:
  - `is_primary DESC`
  - `sort_order ASC`
  - `created_at ASC`
- Output:
  - `List<String>` chỉ gồm `public_url`.

## C. Rendering image on client

### 15) `AuctionDetailController` (đoạn set ảnh)
- File: `src/main/java/com/nhom1/auction/client/user/controller/AuctionDetailController.java`
- Mục đích: Hiển thị ảnh item từ URL server trả về.
- Xử lý:
  - Lấy URL đầu tiên trong `dto.getImageUrls()`.
  - Render: `itemImageView.setImage(new Image(imageUrl, true));`

## D. Database contract

- File: `database/schema.sql`
- Bảng `item_images` hiện tại:
  - `id`, `item_id`, `object_key`, `public_url`, `is_primary`, `sort_order`, `created_at`, `updated_at`.
  - FK `item_id -> items(id) ON DELETE CASCADE`.
- Ý nghĩa:
  - Server chỉ lưu metadata/URL, không lưu binary.
  - Ảnh thật nằm trên imgBB CDN.
