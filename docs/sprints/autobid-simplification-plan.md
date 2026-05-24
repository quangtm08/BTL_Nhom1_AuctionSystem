# Ke Hoach Don Gian Hoa Auto-Bid

## 1. Hien trang hien tai

Tinh nang Auto-Bid hien da hoat dong duoc, nhung phan xu ly trong
`AutoBidService` dang kha phuc tap. Khi co mot luot bid moi, he thong lay danh
sach cau hinh auto-bid, loc cac nguoi dung du dieu kien, sau do chon nguoi co
`maxAmount` cao nhat de dat gia tu dong.

Cach lam hien tai co hai van de chinh:

- Logic kho giai thich trong buoi bao ve vi co nhieu phan xu ly nang cao nhu
  background thread, vong lap nhieu buoc, stream, chon `maxAmount` lon nhat va
  tinh `nextBestMax`.
- Chua khop hoan toan voi yeu cau de bai. De bai co nhac den viec uu tien theo
  thoi diem dang ky auto-bid, trong khi hien tai he thong dang uu tien nguoi co
  muc gia toi da cao nhat.

## 2. Muc tieu chinh sua

Don gian hoa logic Auto-Bid nhung khong viet lai toan bo tinh nang.

Sau khi chinh sua, luong xu ly nen la:

```text
Co bid moi
-> Lay danh sach auto-bid co thong tin created_at
-> Bo qua nguoi dang dan dau
-> Dua cac cau hinh du dieu kien vao PriorityQueue
-> PriorityQueue uu tien nguoi dang ky auto-bid som nhat truoc
-> Poll PriorityQueue de tim nguoi dau tien con du maxAmount de dat bid tiep theo
-> Dat bid tu dong
-> Lap lai den khi khong con ai du dieu kien
```

Cach nay de hieu hon, it logic phu hon, dung duoc PriorityQueue that trong Java
va phu hop hon voi yeu cau uu tien theo thoi diem dang ky.

## 3. Pham vi chinh sua

Khong can thay doi toan bo kien truc hien tai.

Giu nguyen cac phan sau:

- `AutoBidHandler`
- `AutoBidConfig`
- `AutoBidRepository`
- `BidGateway`
- `BidGatewayImpl`
- `BidHandler`
- Co che goi `BidService.placeBid(...)` de dat bid tu dong
- Bang `auto_bid_configs` hien co

Chi nen chinh o hai diem chinh:

- Tra ve hoac map them `created_at` cho cau hinh auto-bid.
- Dung `PriorityQueue` trong `AutoBidService` voi comparator theo thoi diem dang
  ky.
- Don gian hoa logic chon nguoi duoc auto-bid tiep theo.

## 4. Buoc 1: Lay them thoi diem dang ky auto-bid

Trong `AutoBidRepository.findByAuctionId(...)`, sua cau SQL de lay them
`created_at` cua cau hinh auto-bid.

Hien tai:

```sql
SELECT auction_id, bidder_id, max_amount, increment_amount
FROM auto_bid_configs
WHERE auction_id = ?
```

Nen sua thanh:

```sql
SELECT auction_id, bidder_id, max_amount, increment_amount, created_at
FROM auto_bid_configs
WHERE auction_id = ?
```

Y nghia:

- `created_at` la du lieu dung de xac dinh do uu tien.
- Khong sap xep bang SQL la bat buoc neu da dung `PriorityQueue`, nhung co the
  them `ORDER BY created_at ASC` de ket qua doc log/on dinh hon.
- Neu `AutoBidConfig` chua co field `createdAt`, nen them field nay hoac tao
  mot class noi bo nho trong service, vi `PriorityQueue` can gia tri nay de so
  sanh.

## 5. Buoc 2: Dung PriorityQueue de chon auto-bidder

Trong `AutoBidService.runAutoBids(...)`, bo cach chon nguoi co `maxAmount` cao
nhat.

Khong nen tiep tuc dung logic kieu:

```java
eligibleConfigs.stream()
    .max(Comparator.comparing(AutoBidConfig::getMaxAmount))
```

Thay vao do, tao `PriorityQueue` that trong Java. Priority khong phai la
`maxAmount`, ma la thoi diem dang ky auto-bid som hon:

```java
PriorityQueue<AutoBidConfig> queue =
    new PriorityQueue<>(Comparator.comparing(AutoBidConfig::getCreatedAt));
```

Neu can tie-break khi hai config co cung `created_at`, co the them
`thenComparing(AutoBidConfig::getBidderId)` de thu tu on dinh.

Logic mong muon:

```text
Voi tung cau hinh auto-bid:
- Neu la nguoi dang dan dau thi khong dua vao queue
- Tinh gia bid tiep theo = gia hien tai + buoc gia toi thieu
- Neu maxAmount cua nguoi do du tra gia nay thi dua vao queue
- Poll queue de lay nguoi co uu tien cao nhat
```

Trong cach nay, `maxAmount` chi la dieu kien hop le va gioi han gia toi da.
`maxAmount` khong quyet dinh do uu tien.

## 6. Buoc 3: Giu co che dat bid hien co

Khi da chon duoc nguoi auto-bid tiep theo, van goi:

```java
bidGateway.placeAutoBid(...)
```

Khong nen tu cap nhat gia truc tiep trong `AutoBidService`.

Ly do:

- `BidService.placeBid(...)` da co san validate gia bid.
- Logic chong bid sai, bid khi auction dong, bid thap hon gia hien tai van duoc
  tai su dung.
- Co che chong race condition hien tai van duoc giu lai.

## 7. Buoc 4: Giu vong lap nhung lam ro hon

Co the giu `MAX_TRIGGER_DEPTH` de tranh vong lap vo han khi nhieu nguoi cung bat
auto-bid.

Tuy nhien, ben trong vong lap chi nen co cac buoc ro rang:

```text
1. Lay auction hien tai
2. Lay danh sach auto-bid kem created_at
3. Dua cac config du dieu kien vao PriorityQueue theo created_at
4. Poll PriorityQueue de lay nguoi dau tien du dieu kien
5. Neu khong co ai thi dung
6. Dat auto-bid
7. Cap nhat currentHighestBid va currentHighestBidderId
```

Khong can tinh `nextBestMax` neu muc tieu la don gian hoa va uu tien theo thoi
diem dang ky.

## 8. Ket qua mong muon

Sau khi chinh sua:

- Auto-Bid van hoat dong.
- Code de doc hon.
- Luong xu ly de giai thich hon trong buoi bao ve.
- Tinh nang khop hon voi yeu cau de bai ve uu tien theo thoi diem dang ky.
- Khong can viet lai toan bo module.
- Khong lam anh huong den cac phan chinh nhu `BidService`, `BidHandler`,
  database schema hoac realtime notification.
