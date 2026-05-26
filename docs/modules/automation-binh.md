# Module Automation va Auto-Bid

Tai lieu nay mo ta code hien tai trong `server/automation` va cac diem tich hop voi `server/bidding`, `server/auction`, `NotificationService`.

## Thanh phan

- `AutoBidModule`: tao `AutoBidRepository`, `AutoBidService`, `AutoBidHandler` va dang ky route.
- `AutoBidHandler`: xu ly `AUTO_BID_CONFIG`, `GET_AUTO_BID_CONFIG`, `DELETE_AUTO_BID_CONFIG`.
- `AutoBidRepository`: luu cau hinh vao bang `auto_bid_configs` bang upsert `(auction_id, bidder_id)`, doc `created_at` de uu tien.
- `AutoBidConfig`: value object gom auction, bidder, `maxAmount`, `increment`, `createdAt`.
- `AutoBidService`: validate config, luu/xoa/doc config, va chay auto-bid chain.
- `AuctionScheduler`: background thread tick dinh ky de cap nhat vong doi auction va anti-sniping.
- `AuctionGateway` / `BidGateway`: interface giup automation khong phu thuoc truc tiep vao repository/service noi bo cua module khac.

## Luu cau hinh auto-bid

Client goi `AutoBidClientService` de gui `AUTO_BID_CONFIG`. Server validate:

- `auctionId`, `bidderId` phai la UUID hop le.
- Auction phai dang `RUNNING`.
- `maxAmount > 0`.
- Increment client gui phai lon hon hoac bang minimum increment cua auction.
- `maxAmount >= increment`.

Sau khi save config, `AutoBidService.scheduleAutoBids(...)` co the chay ngay neu config moi du suc vuot gia hien tai.

## Luong auto-bid khi co bid moi

```text
Bid manual thanh cong
-> BidHandler broadcast bid update
-> AutoBidService.triggerAutoBids(...)
-> doc tat ca AutoBidConfig cua auction
-> bo qua bidder dang dan dau
-> dua config du dieu kien vao PriorityQueue theo createdAt, tie-break bidderId
-> goi BidGateway.placeAutoBid(...)
-> lap toi khi khong con config du dieu kien hoac dat MAX_TRIGGER_DEPTH
-> broadcast bid update mot lan khi chain ket thuc
```

`maxAmount` la gioi han va dieu kien hop le, khong phai tieu chi uu tien chinh. Thu tu uu tien hien tai la thoi diem tao config som hon (`createdAt`).

## Scheduler va anti-sniping

`AuctionScheduler` duoc start trong `ServerContext`. Scheduler dung `AuctionGateway` de tim/cap nhat auction va `BidGateway` de doc bid cuoi:

- Auction chua toi gio bat dau se cho.
- Auction toi gio bat dau duoc chuyen sang trang thai dang chay theo gateway.
- Auction dang chay ma het gio se kiem tra bid cuoi. Neu bid cuoi nam trong cua so anti-sniping, scheduler gia han end time va broadcast `PUSH_AUCTION_TIME_EXTENDED`.
- Neu khong gia han, scheduler ket thuc auction va broadcast `PUSH_AUCTION_ENDED`.

## Test lien quan

- `server/automation/AutoBidServiceTest.java`
- `server/automation/AutoBidRepositoryTest.java`
- `server/automation/AuctionSchedulerTest.java`
- `server/auction/AuctionGatewayImplTest.java`
- `server/bidding/BidGatewayImplTest.java`

## Trung lap docs

Chi tiet schema `auto_bid_configs` nam o `docs/architecture/database-schema.md`. Luong push chung nam o `docs/architecture/client-server-communication.md` va `docs/modules/realtime-push-quang.md`.
