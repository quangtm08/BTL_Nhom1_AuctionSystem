# Min bid increment: 5% starting price

## Muc tieu

Them quy tac: moi bid moi phai lon hon bid cao nhat hien tai va it nhat bang (currentHighestBid + 5% startingPrice). Gia tri 5% lam tron 2 chu so thap phan theo HALF_UP.

## Tom tat thay doi

- Tinh min increment tu starting price (5%) tren server
- Xac thuc min increment trong validator khi dat bid
- Them tests cho truong hop bi tu choi va chap nhan

## Hanh vi moi

- Lan bid dau tien: phai >= startingPrice (giu nguyen)
- Cac lan bid sau: phai >= currentHighestBid + (startingPrice * 0.05)
- Min increment duoc lam tron 2 chu so thap phan, HALF_UP

## Chi tiet thay doi

### 1) Auction tinh min increment

- Them `getMinBidIncrement()` trong [src/main/java/com/nhom1/auction/common/entity/Auction.java](src/main/java/com/nhom1/auction/common/entity/Auction.java)
- Cong thuc: `startingPrice * 0.05`, scale=2, HALF_UP

### 2) Validator bat quy tac min increment

- Cap nhat [src/main/java/com/nhom1/auction/common/entity/AuctionBidValidator.java](src/main/java/com/nhom1/auction/common/entity/AuctionBidValidator.java)
- Neu da co `currentHighestBid`:
  - Bat buoc `amount >= currentHighestBid + minIncrement`
  - Neu khong dat, nem `InvalidBidException` voi thong diep ro rang

### 3) Tests

- Cap nhat [src/test/java/com/nhom1/auction/common/entity/AuctionTest.java](src/test/java/com/nhom1/auction/common/entity/AuctionTest.java)
- Them test:
  - `testPlaceBid_RejectWhenLessThanMinIncrement`
  - `testPlaceBid_AcceptedWhenEqualOrGreaterThanMinIncrement`

## Tac dong / tuong thich

- Khong thay doi DB schema
- Quy tac moi anh huong den bid thu 2 tro di
- Client co the tinh min increment tu startingPrice de hien thi thong tin

## Cach kiem tra

Chay unit tests:

```bash
mvn -Dtest=AuctionTest test
```

## Ghi chu

Neu trong tuong lai can min increment theo % rieng tung auction, can them cot DB va map gia tri vao entity.
