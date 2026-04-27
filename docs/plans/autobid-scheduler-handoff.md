# Autobid Scheduler Branch Handoff

Tai lieu nay chot phan da lam trong branch `feature/autobid-scheduler`, diem giao nhau voi thanh vien khac, va checklist tiep noi de merge an toan.

## 1) Pham vi da hoan thanh trong branch nay

Da implement khung day du cho module `server/automation` + client service auto-bid:

- Route server `AUTO_BID_CONFIG` (handler + module)
- Luu cau hinh auto-bid vao DB (upsert theo cap `auction_id` + `bidder_id`)
- Auto-bid trigger engine co chan de quy
- Auction scheduler tick moi giay (OPEN -> RUNNING, RUNNING -> FINISHED, anti-sniping extend)
- `AutoBidClientService` de client goi API `AUTO_BID_CONFIG`
- Chu thich trong code cac diem phu thuoc lien-team (auction/bidding/infra)

## 2) Cac file moi / da sua

### Server automation (moi)

- `src/main/java/com/nhom1/auction/server/automation/AutoBidConfig.java`
- `src/main/java/com/nhom1/auction/server/automation/AutoBidRepository.java`
- `src/main/java/com/nhom1/auction/server/automation/AutoBidService.java`
- `src/main/java/com/nhom1/auction/server/automation/AutoBidHandler.java`
- `src/main/java/com/nhom1/auction/server/automation/AutoBidModule.java`
- `src/main/java/com/nhom1/auction/server/automation/AuctionScheduler.java`
- `src/main/java/com/nhom1/auction/server/automation/BidGateway.java`
- `src/main/java/com/nhom1/auction/server/automation/AuctionGateway.java`

### Client (moi)

- `src/main/java/com/nhom1/auction/client/user/service/AutoBidClientService.java`

### Infrastructure (khong sua file so huu chung)

- Khong commit thay doi vao `server/infrastructure/ServerContext.java` de tranh xung dot file so huu chung cua thanh vien 4.
- Huong dan wiring duoc ghi day du trong tai lieu handoff nay.

## 3) Logic da duoc code

### AutoBidRepository

- Tu tao bang `auto_bid_configs` neu chua ton tai.
- Luu config bang `INSERT OR REPLACE`.
- Doc danh sach config theo `auctionId`.

### AutoBidService

- `saveConfig(...)`:
  - validate UUID, `maxAmount > 0`, `increment > 0`, `maxAmount >= increment`
  - luu xuong DB va tra `CONFIG_SAVED`
- `triggerAutoBids(...)`:
  - tim bidder auto-hop-le (khong phai nguoi dang dan dau, maxAmount con du)
  - tinh gia tiep theo = `currentHighestBid + increment`
  - goi gateway bid de dat auto-bid
  - de quy tiep neu co bidder khac overbid
  - chan vong lap bang `MAX_TRIGGER_DEPTH`

### AuctionScheduler

- Chay nen moi giay.
- Khi `OPEN` va qua `startTime` -> `RUNNING`.
- Khi `RUNNING` va qua `endTime`:
  - neu bid cuoi nam trong cua so anti-sniping -> gia han `endTime`
  - nguoc lai -> `FINISHED` + goi `NotificationService.broadcastAuctionEnded(...)`

### AutoBidClientService

- API `saveConfig(auctionId, maxAmount, increment)`.
- Tu dien `bidderId` tu `AppContext.getCurrentUser()`.
- Validate fail-fast truoc khi gui server.

## 4) Cac diem phu thuoc voi thanh vien khac (quan trong)

## 4.1 Thanh vien bidding

Can cung cap adapter implement `BidGateway`:

- `placeAutoBid(UUID bidderId, UUID auctionId, BigDecimal amount)`
- `findLastBidTime(UUID auctionId)`

Khuyen nghi adapter map den:

- `BidService.placeBid(...)` cho dat gia tu dong
- `BidRepository.findLastBidTime(...)` cho anti-sniping

Ngoai ra, can goi `autoBidService.triggerAutoBids(...)` trong `BidHandler`
ngay sau khi bid thu cong thanh cong (khong goi trong `BidService` de tranh phu thuoc vong).

## 4.2 Thanh vien auction

Can cung cap adapter implement `AuctionGateway`:

- `findAll()`
- `updateStatus(UUID, AuctionStatus)`
- `updateEndTime(UUID, LocalDateTime)`

Khuyen nghi adapter map den `AuctionRepository` contract da chot trong team-task-allocation.

## 4.3 Thanh vien infrastructure/push

`AuctionScheduler` da goi:

- `notificationService.broadcastAuctionEnded(auctionId, winnerId)`

Can hoan thien `NotificationService` (serialize + push protocol) de client nhan duoc thong bao ket thuc phien.

## 4.4 Thanh vien wiring ServerContext

Can noi cac module theo thu tu:

1. khoi tao `BidGateway` adapter
2. `AutoBidModule.init(connection, router, bidGateway)`
3. khoi tao `AuctionGateway` adapter
4. tao `AuctionScheduler` voi `auctionGateway`, `bidGateway`, `notificationService`
5. `scheduler.start()`

## 5) Viec chua lam trong branch nay (de tranh cham scope)

- Chua tao adapter concrete toi `BidService`/`BidRepository` vi branch nay khong co bidding module concrete.
- Chua tao adapter concrete toi `AuctionRepository` vi branch nay khong co auction module concrete.
- Chua wiring scheduler start that trong `ServerContext` (chi de huong dan).
- Chua noi popup auto-bid vao `auction_detail.fxml`/controller.
- Chua them rule "min increment global cho manual bid"; hien contract bidding chi rang buoc `amount > currentHighestBid` (va bid dau `>= startingPrice`).

## 6) Checklist tiep noi de xong end-to-end

1. [Bidding] Tao `BidGatewayAdapter`.
2. [Auction] Tao `AuctionGatewayAdapter`.
3. [Infra] Hoan thien `NotificationService` push event.
4. [Wiring] Noi `AutoBidModule` + start `AuctionScheduler` trong `ServerContext`.
5. [Client UI] Them popup auto-bid (maxAmount, increment) va goi `AutoBidClientService`.
6. [QA] Test 3 case:
   - manual bid -> auto-bid chain
   - anti-sniping extend
   - het anti-sniping -> FINISHED + push ended

## 7) Ghi chu merge

Thu tu merge khuyen nghi:

1. branch co `AuctionRepository` + `BidService/BidRepository` contracts on dinh
2. `feature/autobid-scheduler`
3. branch wiring `ServerContext` + push protocol

Neu merge som, phan automation van an toan vi da tach phu thuoc qua `AuctionGateway` va `BidGateway`.
