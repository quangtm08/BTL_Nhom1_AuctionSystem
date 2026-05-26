# Module: Bidding Feature

Tai lieu nay mo ta trang thai hien tai cua module bidding theo code trong `server/bidding`, `client/user/service/BiddingClientService.java`, va cac DTO trong `common/dto/bidding`.

## Thanh phan chinh

- `BidModule`: tao `BidRepository`, `BidService`, `BidHandler` va dang ky route vao `MessageRouter`.
- `BidHandler`: xu ly `LIST_AUCTIONS`, `GET_AUCTION_DETAIL`, `PLACE_BID`, `LIST_MY_BIDS`; parse DTO bang `JsonUtil`; tra loi bang `ResponseFactory`.
- `BidService`: nghiep vu browse/detail/my bids/place bid. Khi place bid, service mo transaction rieng tu `DataSource`.
- `BidRepository`: luu va doc `BidTransaction`, tim bid theo auction, bidder, va last bid time cho anti-sniping.
- `AuctionRepository`, `ItemRepository`, `ItemImageRepository`: duoc inject de lay auction/item/image khi render danh sach va chi tiet.
- `WalletRepository`: duoc inject de validate so du truoc khi commit bid.
- `BiddingClientService`: tao request DTO, chon `MessageType`, unwrap response cho controller.

## Luong place bid

```text
AuctionDetailController / AuctionBrowseController
-> BiddingClientService.placeBid(...)
-> PLACE_BID
-> BidHandler
-> BidService.placeBid(...)
-> BidRepository + AuctionRepository + WalletRepository trong transaction
-> NotificationService.broadcastBidUpdate(...)
-> AutoBidService trigger neu duoc cau hinh
```

`BidService.placeBid` doc auction trong transaction, validate bang domain logic, kiem tra wallet balance, luu `BidTransaction`, roi cap nhat highest bid bang expected `auction.version`. Neu update 0 row do co bid dong thoi, service rollback va retry toi gioi han.

## Quy tac dat gia

- Auction phai o trang thai cho phep bidding.
- Bidder khong duoc la seller cua auction.
- Bid dau tien phai dat toi thieu `startingPrice`.
- Bid tiep theo phai dat toi thieu `currentHighestBid + auction.getMinBidIncrement()`.
- `Auction.getMinBidIncrement()` hien tinh 5% cua `startingPrice`, lam tron theo logic trong entity.
- Wallet cua bidder phai co `balance >= amount`.

Loi nghiep vu duoc nem bang exception typed nhu `InvalidBidException`, `AuctionClosedException`, `UnauthorizedActionException`, `ValidationException`, `ConflictException`; `ResponseFactory` map thanh response loi cho client.

## Realtime va auto-bid

Sau bid manual thanh cong, `BidHandler` broadcast `PUSH_BID_UPDATE`. Neu `AutoBidService` da duoc wire trong `ServerContext`, handler tiep tuc kich hoat auto-bid. Auto-bid co the dat them mot chuoi bid tu dong va broadcast lai khi chuoi da on dinh.

## Test lien quan

- `server/bidding/BidServiceTest.java`
- `server/bidding/BidHandlerTest.java`
- `server/bidding/BidRepositoryTest.java`
- `server/bidding/BidGatewayImplTest.java`
- `common/entity/AuctionTest.java`
- `common/entity/BidTransactionTest.java`

## Trung lap docs

Luong socket/request-response chung nam o `docs/architecture/client-server-communication.md`. Schema bang `bids` va `auctions.version` nam o `docs/architecture/database-schema.md`.
