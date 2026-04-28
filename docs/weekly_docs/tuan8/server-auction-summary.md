# Seller Auction Module (`server/auction`)

Module nay xu ly luong nguoi ban tren server: tao phien dau gia va lay danh sach phien cua seller.

## 1) Tong quan kien truc

```
AuctionModule
   -> AuctionHandler (JSON/protocol layer)
      -> AuctionService (business layer)
         -> ItemRepository + AuctionRepository (data layer)
```

- `Repository`: chi lam viec voi SQL/database.
- `Service`: validate + business rule, khong xu ly JSON.
- `Handler`: parse JSON payload, goi service, tra `ResponseMessage`.
- `Module`: khoi tao va wiring cac thanh phan, register route vao `MessageRouter`.

---

## 2) Chi tiet 5 file

## `ItemRepository.java`

Vai tro: CRUD cho bang `items`, ho tro polymorphism theo `ItemCategory`.

### Ham chinh

- `save(Item item, UUID sellerId)`
  - Insert vao `items`.
  - `Item` la domain object thuần, khong chua `sellerId`; repository tu dien `seller_id`.
  - Dien cac cot rieng theo category:
    - `ELECTRONICS`: `brand`, `warranty_months`
    - `ART`: `artist`, `era`
    - `VEHICLE`: `brand`, `production_year`, `fuel_type`

- `findById(UUID id)`
  - Doc 1 dong trong `items`.
  - Goi `mapResultSetToItem(...)` de khoi tao dung subtype (`Art`/`Electronics`/`Vehicle`).

### Luu y

- Item duoc tai lai dung subtype de service/phia tren khong can `if/else` theo SQL result.
- Thoi gian `created_at`, `updated_at` dang duoc luu dang string `LocalDateTime.toString()`.

---

## `AuctionRepository.java`

Vai tro: thao tac bang `auctions`, va truy van co JOIN `items` de lay `seller_id`.

### Ham chinh

- `save(Auction auction)`: insert phien dau gia moi.
- `findById(UUID id)`: lay 1 auction theo id (JOIN `items`).
- `findAll()`: lay tat ca auction (JOIN `items`).
- `findBySellerId(UUID sellerId)`: lay danh sach auction cua seller (JOIN `items` + `WHERE i.seller_id = ?`).
- `findByItemId(UUID itemId)`: tim auction theo item.
- `updateStatus(...)`: cap nhat trang thai auction.
- `updateHighestBid(...)`: cap nhat `current_highest_bid`, `highest_bidder_id`.
- `updateEndTime(...)`: cap nhat thoi diem ket thuc (phuc vu anti-sniping/scheduler).

### Luu y

- Bang `auctions` khong co cot `seller_id`; vi vay cac query can seller deu JOIN voi `items`.
- `map(ResultSet rs)` su dung constructor load-tu-DB cua `Auction`.

---

## `AuctionService.java`

Vai tro: business logic cho seller flow.

### `createAuction(String sellerId, CreateAuctionRequest dto)`

Luong xu ly:
1. Validate input:
   - `sellerId` hop le UUID
   - `startingPrice > 0`
   - `startTime`, `endTime` khong null
   - `endTime` phai sau `startTime`
   - Neu `dto.sellerId` co gia tri thi phai khop voi `sellerId` param
2. Tao `Item` bang `ItemFactory` (khong tao truc tiep `new Art/Electronics/Vehicle`).
3. Luu `Item` qua `itemRepository.save(item, sellerId)`.
4. Tao `Auction` domain object va luu qua `auctionRepository.save(auction)`.
5. Cap nhat opening price bang `auctionRepository.updateHighestBid(auctionId, startingPrice, null)`.
6. Tra ve `Auction`.

### `getMyListings(String sellerId)`

Luong xu ly:
1. Parse + validate seller UUID.
2. Lay auctions cua seller qua `auctionRepository.findBySellerId(...)`.
3. Lay thong tin item (`itemName`) tu `itemRepository.findById(...)`.
4. Map sang `AuctionSummaryDto`.

### Luu y

- Service khong chua SQL va khong parse JSON.
- Dang map `startingPrice` tu `auction.getCurrentHighestBid()` theo implementation hien tai.

---

## `AuctionHandler.java`

Vai tro: protocol layer cho 2 message type cua seller:

- `CREATE_AUCTION`
- `LIST_MY_LISTINGS`

### `register(MessageRouter router)`

- Dang ky route `CREATE_AUCTION`:
  - Parse `payloadJson` -> `CreateAuctionRequest`.
  - Goi `handleCreateAuction(...)`.
  - Loi parse -> `INVALID_FORMAT`.

- Dang ky route `LIST_MY_LISTINGS`:
  - Parse payload dang JSON node, doc `sellerId`.
  - Goi `handleListMyListings(...)`.
  - Loi parse -> `INVALID_FORMAT`.

### `handleCreateAuction(...)`

- Goi `auctionService.createAuction(...)`.
- Map `Auction` sang `CreateAuctionResponse`.
- Tra `ResponseMessage` success/error (`CREATE_AUCTION_FAILED`).

### `handleListMyListings(...)`

- Goi `auctionService.getMyListings(...)`.
- Wrap vao `MyListingsReponse`.
- Tra `ResponseMessage` success/error (`LIST_MY_LISTINGS_FAILED`).

---

## `AuctionModule.java`

Vai tro: composition root cho feature auction.

### `init(Connection connection, MessageRouter router)`

1. Tao `ItemRepository`.
2. Tao `AuctionRepository`.
3. Tao `AuctionService`.
4. Tao `AuctionHandler`.
5. `handler.register(router)`.
6. `return auctionRepository`.

### Tai sao phai return `AuctionRepository`?

`ServerContext`/module khac (Bid, Admin, Payment, Scheduler) can dung chung `AuctionRepository` de:
- Lay auction detail/list
- Cap nhat status/end time/highest bid
- Tranh khoi tao duplicate repository khong can thiet

---

## 3) Contract message (handler layer)

## `CREATE_AUCTION`

- Input payload: `CreateAuctionRequest`
- Output payload: `CreateAuctionResponse`
- Error code:
  - `INVALID_FORMAT`: payload JSON sai schema
  - `CREATE_AUCTION_FAILED`: loi business/data

## `LIST_MY_LISTINGS`

- Input payload toi thieu:
```json
{
  "sellerId": "uuid-string"
}
```
- Output payload: `MyListingsReponse` (chua `List<AuctionSummaryDto>`)
- Error code:
  - `INVALID_FORMAT`
  - `LIST_MY_LISTINGS_FAILED`

---

## 4) Integration checklist

- `MessageType` phai co:
  - `CREATE_AUCTION`
  - `LIST_MY_LISTINGS`
- Trong `ServerContext`:
  - Goi `AuctionModule.init(connection, router)` va giu reference `AuctionRepository`.
- Client phai gui dung DTO/payload theo contract tren.
