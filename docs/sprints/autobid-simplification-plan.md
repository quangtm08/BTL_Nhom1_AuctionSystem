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
-> Lay danh sach auto-bid theo thu tu dang ky som nhat truoc
-> Bo qua nguoi dang dan dau
-> Tim nguoi dau tien con du maxAmount de dat bid tiep theo
-> Dat bid tu dong
-> Lap lai den khi khong con ai du dieu kien
```

Cach nay de hieu hon, it logic phu hon va phu hop hon voi yeu cau uu tien theo
thoi diem dang ky.

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

- Sap xep danh sach auto-bid theo thoi diem dang ky.
- Don gian hoa logic chon nguoi duoc auto-bid tiep theo trong `AutoBidService`.

## 4. Buoc 1: Sap xep auto-bid theo thoi diem dang ky

Trong `AutoBidRepository.findByAuctionId(...)`, sua cau SQL de tra ve cau hinh
auto-bid theo `created_at` tang dan.

Hien tai:

```sql
SELECT auction_id, bidder_id, max_amount, increment_amount
FROM auto_bid_configs
WHERE auction_id = ?
```

Nen sua thanh:

```sql
SELECT auction_id, bidder_id, max_amount, increment_amount
FROM auto_bid_configs
WHERE auction_id = ?
ORDER BY created_at ASC
```

Y nghia:

- Nguoi dang ky auto-bid som hon se duoc xet truoc.
- Khong can tao them `PriorityQueue` trong Java.
- Co the giai thich rang he thong dung `created_at` lam do uu tien.

## 5. Buoc 2: Don gian hoa logic chon auto-bidder

Trong `AutoBidService.runAutoBids(...)`, bo cach chon nguoi co `maxAmount` cao
nhat.

Khong nen tiep tuc dung logic kieu:

```java
eligibleConfigs.stream()
    .max(Comparator.comparing(AutoBidConfig::getMaxAmount))
```

Thay vao do, vi danh sach da duoc sap xep theo `created_at`, chi can duyet tu
dau danh sach va chon nguoi dau tien du dieu kien.

Logic mong muon:

```text
Voi tung cau hinh auto-bid trong danh sach:
- Neu la nguoi dang dan dau thi bo qua
- Tinh gia bid tiep theo = gia hien tai + buoc gia toi thieu
- Neu maxAmount cua nguoi do du tra gia nay thi chon nguoi do
- Dung vong duyet
```

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
2. Lay danh sach auto-bid theo thu tu uu tien
3. Tim nguoi dau tien du dieu kien
4. Neu khong co ai thi dung
5. Dat auto-bid
6. Cap nhat currentHighestBid va currentHighestBidderId
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
