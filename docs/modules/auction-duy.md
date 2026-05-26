# Module Auction Listing

Tai lieu nay mo ta module `server/auction` theo code hien tai. Module nay phu trach tao, sua, xoa va lay danh sach listing cua seller; browse/detail bidding nam trong module `server/bidding`.

## Thanh phan

- `AuctionModule`: tao `ItemRepository`, `ItemImageRepository`, `AuctionRepository`, `AuctionService`, `AuctionHandler`; dang ky route vao `MessageRouter`; tra ve repository bundle cho module khac dung.
- `AuctionHandler`: parse DTO va xu ly `CREATE_AUCTION`, `UPDATE_AUCTION`, `DELETE_AUCTION`, `LIST_MY_LISTINGS`.
- `AuctionService`: validate input, tao/sua/xoa item va auction trong transaction, map listing ve `AuctionSummaryDto`.
- `AuctionRepository`: luu/doc/cap nhat/xoa bang `auctions`, gom ca status, highest bid, version, seller lookup.
- `ItemRepository`: luu/doc/cap nhat/xoa bang `items`, dung `ItemFactory` de tao subtype theo category.
- `ItemImageRepository`: luu va doc anh item trong bang `item_images`.
- `AuctionGatewayImpl`: adapter cho automation/scheduler cap nhat auction lifecycle.

## Message types

- `CREATE_AUCTION` -> `CreateAuctionRequest` -> `CreateAuctionResponse`
- `UPDATE_AUCTION` -> `UpdateAuctionRequest` -> `"Updated"`
- `DELETE_AUCTION` -> `DeleteAuctionRequest` -> `"Deleted"`
- `LIST_MY_LISTINGS` -> `ListMyListingsRequest` -> `MyListingsResponse`

## Tao auction

```text
CreateAuctionController
-> CreateAuctionClientService
-> CREATE_AUCTION
-> AuctionHandler.handleCreateAuction
-> AuctionService.createAuction
-> ItemRepository.save
-> ItemImageRepository.saveImageUrls
-> AuctionRepository.save
-> AuctionRepository.updateStatus(..., PENDING)
```

Validation chinh:

- `sellerId` phai la UUID hop le.
- `startingPrice > 0`.
- `startTime` phai co va nam trong tuong lai.
- `endTime` neu co phai sau `startTime`; neu khong, service resolve theo `durationDays`.
- `name` khong duoc blank.
- `category` va `condition` bat buoc.

Create chay trong mot transaction de item, image va auction khong bi lech nhau.

## Sua auction

`AuctionService.updateAuction(...)` chi cho seller cua auction sua khi auction con editable:

- Trang thai phai la `PENDING` hoac `OPEN`.
- Neu status dang `RUNNING` nhung `startTime` van o tuong lai, service chuyen lai `OPEN` roi validate tiep.
- Auction da co `highestBidderId` thi khong duoc sua.
- `endTime` phai sau `startTime` va sau hien tai.
- `startingPrice > 0`, `name` khong blank, `category`/`condition` bat buoc.

Transaction cap nhat ca item basic info va auction price/end time.

## Xoa auction

`AuctionService.deleteAuction(...)`:

1. Parse seller/auction UUID.
2. Tim auction.
3. Kiem tra seller hien tai la owner.
4. Chi cho xoa khi status editable (`PENDING` hoac `OPEN`).
5. Trong transaction, xoa auction roi xoa item.
6. `AuctionHandler` broadcast `PUSH_AUCTION_DELETED` sau khi xoa thanh cong.

## List my listings

`getMyListings(sellerId)` doc auction theo seller, lay item tuong ung, bo qua row hong neu item bi thieu, va map sang `AuctionSummaryDto` de client render trong `MyListingsController`.

## Tich hop module khac

- `BidModule` dung `AuctionRepository`, `ItemRepository`, `ItemImageRepository` de browse/detail/place bid.
- `AdminModule` dung repository va `SqlAdminAuctionGateway` de list/approve/cancel auction.
- `PaymentModule` dung `AuctionRepository` de validate winner va cap nhat `PAID`.
- `AuctionScheduler` dung `AuctionGatewayImpl` de doi status theo thoi gian.

## Trung lap docs

Schema `auctions`, `items`, `item_images` nam trong `docs/architecture/database-schema.md`. Luong request-response chung nam trong `docs/architecture/client-server-communication.md`.
