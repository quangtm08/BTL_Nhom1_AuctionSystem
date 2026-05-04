# 🧩 Seller Module – Branching Strategy (README)

## 📌 Mục tiêu

Tài liệu này hướng dẫn cách chia branch cho **Thành viên 1 – Luồng Người bán**, đảm bảo:

* Không conflict
* Đúng thứ tự dependency
* Dễ merge & review

---

## 🌳 Tổng quan cấu trúc branch

Tạo một nhánh gốc:

```
feature/seller-main
```

Từ đó tách thành các branch nhỏ theo từng phần:

```
feature/seller-dto
feature/seller-repository
feature/seller-service
feature/seller-handler
feature/seller-ui-create-auction
feature/seller-ui-my-listings
```

---

## 🔄 Thứ tự thực hiện (BẮT BUỘC)

```
1. DTO
2. Repository
3. Service
4. Handler + Module
5. UI Create Auction
6. UI My Listings
```

⚠️ Làm sai thứ tự → lỗi dependency

---

## 🌱 1. DTO Layer

### Branch

```
feature/seller-dto
```

### File cần tạo

```
common/dto/auction/
  ├── CreateAuctionRequest.java
  ├── CreateAuctionResponse.java
  ├── AuctionSummaryDto.java
  └── MyListingsResponse.java
```

### Lưu ý

* `AuctionSummaryDto` phải **thống nhất với TV2 trước khi code**
* Tất cả ID dùng `String`

### Commit mẫu

```
feat(dto): add CreateAuctionRequest
feat(dto): add CreateAuctionResponse
feat(dto): add AuctionSummaryDto
feat(dto): add MyListingsResponse
```

---

## 🗄️ 2. Repository Layer

### Branch

```
feature/seller-repository
```

### File

```
server/auction/
  ├── ItemRepository.java
  └── AuctionRepository.java
```

### Nhiệm vụ

* Viết SQL thuần
* Không chứa business logic

### Lưu ý quan trọng

* `AuctionRepository` phải JOIN với bảng `items` để lấy `seller_id`
* Public sớm để TV2 & TV3 dùng

---

## ⚙️ 3. Service Layer

### Branch

```
feature/seller-service
```

### File

```
server/auction/AuctionService.java
```

### Nhiệm vụ

* `createAuction()`
* `getMyListings()`

### Quy tắc

* ❌ Không SQL
* ❌ Không JSON
* ✅ Chỉ business logic

---

## 🌐 4. Handler + Module

### Branch

```
feature/seller-handler
```

### File

```
server/auction/
  ├── AuctionHandler.java
  └── AuctionModule.java
```

### Nhiệm vụ

* Map MessageType:

  * `CREATE_AUCTION`
  * `LIST_MY_LISTINGS`
* Parse JSON → DTO → gọi Service → trả JSON

### ⚠️ BẮT BUỘC

```
AuctionModule.init() phải return AuctionRepository
```

---

## 💻 5. UI – Create Auction

### Branch

```
feature/seller-ui-create-auction
```

### File

```
client/controller/CreateAuctionController.java
client/view/CreateAuction.fxml
```

### Nhiệm vụ

* Lấy dữ liệu từ form
* Gửi request `CreateAuctionRequest`
* Navigate về My Listings nếu thành công

---

## 📋 6. UI – My Listings

### Branch

```
feature/seller-ui-my-listings
```

### File

```
client/controller/MyListingsController.java
```

### Nhiệm vụ

* Load danh sách auction của user
* Render UI

---

## 🔀 Quy trình merge

Sau mỗi branch:

```
feature/seller-xxx → feature/seller-main
```

Cuối cùng:

```
feature/seller-main → develop/main
```

---

## 🚨 Các lỗi thường gặp (CẦN TRÁNH)

### ❌ Sai DTO chung

* `AuctionSummaryDto` lệch → TV2 crash

---

### ❌ Quên JOIN items

* Không lấy được `seller_id`
* My Listings sai dữ liệu

---

### ❌ Tạo object sai cách

```
new Art(...) ❌
```

✅ Phải dùng:

```
ItemFactory
```

---

### ❌ Nhét logic vào Repository

→ Vi phạm kiến trúc

---

### ❌ Không return AuctionRepository

→ TV4 không connect được hệ thống

---

## 🧠 Best Practices

* Mỗi branch chỉ làm **1 việc duy nhất**
* Commit nhỏ, rõ ràng
* Test từng layer trước khi merge
* Sync thường xuyên với `feature/seller-main`

---

## ✅ Tổng kết

Bạn sẽ có:

| Layer       | Branch                           |
| ----------- | -------------------------------- |
| DTO         | feature/seller-dto               |
| Repository  | feature/seller-repository        |
| Service     | feature/seller-service           |
| Handler     | feature/seller-handler           |
| UI Create   | feature/seller-ui-create-auction |
| UI Listings | feature/seller-ui-my-listings    |

---

## 🚀 Gợi ý tiếp theo

Sau khi setup xong:

* Viết skeleton trước (empty methods)
* Chạy compile toàn project
* Sau đó fill logic dần

---

**Done — bạn có thể copy file này làm README.md và dùng trực tiếp.**
