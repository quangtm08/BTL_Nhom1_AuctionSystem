# Luong Giao Tiep Client-Server

Day la tai lieu canonical cho request-response va realtime push. Module docs khong nen lap lai toan bo luong nay; chi can noi message type va service dac thu cua module.

## Request-response

```text
Controller -> ClientService -> BaseClientService -> ServerConnection
-> TCP JSON line -> ClientHandler -> MessageRouter -> FeatureHandler
-> FeatureService -> Repository -> DB -> ResponseMessage -> ServerConnection future
```

## Client side

- Controller JavaFX chi doc UI, validate nhe, goi `*ClientService`, va render ket qua.
- `*ClientService` tao DTO request, chon `MessageType`, goi `BaseClientService.send(...)`.
- `BaseClientService` unwrap `ResponseMessage`; neu fail thi map server error thanh exception typed.
- `ServerConnection` giu socket, gan `requestId`, luu `PendingRequest` trong `ConcurrentHashMap`, serialize bang `JsonUtil`, va doc response tren listener thread.
- Callback cua `CompletableFuture` khong dam bao o JavaFX Application Thread, nen controller phai dung `Platform.runLater(...)` khi sua UI.

## Server side

- `Server.main` set timezone `Asia/Ho_Chi_Minh`, doc `PORT` neu co, khoi tao `ServerContext`, tao `ServerSocket`, va dua moi socket cho `ClientHandler` trong fixed pool.
- `ServerContext` khoi tao `MessageRouter`, `DataSource`, `DatabaseInitializer`, `ClientRegistry`, `NotificationService`, roi wire cac module: auth, auction, wallet, bidding, automation, scheduler, admin, payment.
- `ClientHandler` doc tung dong JSON bang `readLine()`, goi `MessageRouter.handleRequest(json)`, va `push(json)` response ve socket.
- `MessageRouter` doc envelope `type`, `requestId`, `payload`; tim `MessageRouteAction` theo `MessageType`; handler parse payload thanh DTO va tra `ResponseMessage`.

## Envelope JSON

Request co dang logic:

```json
{
  "requestId": "uuid",
  "type": "PLACE_BID",
  "payload": { "auctionId": "..." }
}
```

Response co `requestId` khi la tra loi cho request. Push event khong co `requestId` va duoc phan loai bang `type`.

## Message types hien co

- Auth: `LOGIN`, `REGISTER`
- Bidding: `LIST_AUCTIONS`, `GET_AUCTION_DETAIL`, `PLACE_BID`, `LIST_MY_BIDS`
- Auction listing: `CREATE_AUCTION`, `UPDATE_AUCTION`, `DELETE_AUCTION`, `LIST_MY_LISTINGS`
- Auto-bid: `AUTO_BID_CONFIG`, `GET_AUTO_BID_CONFIG`, `DELETE_AUTO_BID_CONFIG`
- Admin: `ADMIN_LIST_USERS`, `ADMIN_DELETE_USER`, `ADMIN_APPROVE_AUCTION`, `ADMIN_LIST_AUCTIONS`, `ADMIN_CANCEL_AUCTION`
- Payment: `PROCESS_PAYMENT`, `LIST_PENDING_PAYMENTS`, `LIST_PAYMENT_HISTORY`
- Wallet: `GET_WALLET`, `DEPOSIT_MONEY`, `WITHDRAW_MONEY`
- Push: `PUSH_BID_UPDATE`, `PUSH_AUCTION_ENDED`, `PUSH_AUCTION_TIME_EXTENDED`, `PUSH_NEW_AUCTION`, `PUSH_AUCTION_DELETED`, `PUSH_USER_DELETED`, `PUSH_USER_CREATED`

## Realtime push

Server tao event trong `NotificationService`, boc vao `ResponseMessage` co `type`, serialize bang `JsonUtil`, roi `ClientRegistry.broadcast(json)` toi cac `ClientHandler` dang online. `broadcast` hien gui async bang `CompletableFuture.runAsync` de mot client cham khong chan request thread.

Client listener thay JSON khong co `requestId` thi doc `type`, lay raw JSON dua cho push handler da dang ky. `ClientPushService` la lop canonical parse payload thanh DTO typed va expose cac method `onBidUpdate`, `onNewAuction`, `onAuctionDeleted`, `onAuctionEnded`, `onAuctionTimeExtended`, `onUserDeleted`, `onUserCreated`.

## Concurrency

- JavaFX Application Thread: chay controller va render UI.
- Client listener thread: doc socket, complete future, dispatch push.
- Server worker thread: moi client request duoc xu ly trong `ClientHandler` thread.
- Database: service muon transaction lay `Connection` tu HikariCP, set `autoCommit(false)`, commit/rollback ro rang.
- Bid race: `BidService.placeBid` doc `Auction.version`, luu bid va `updateHighestBid(... expectedVersion ...)`; neu update 0 row thi rollback va retry toi gioi han.

## Class chinh

| Tang | Class |
| --- | --- |
| Client UI | `SignInController`, `AuctionBrowseController`, `AuctionDetailController`, `WalletController`, admin controllers |
| Client service | `AuthClientService`, `BiddingClientService`, `MyListingsClientService`, `WalletClientService`, `PaymentClientService`, `AdminClientService` |
| Client infra | `ServerConnection`, `ClientPushService`, `BaseClientService`, `AppNavigator` |
| Server infra | `Server`, `ServerContext`, `ClientHandler`, `MessageRouter`, `ClientRegistry`, `NotificationService`, `ResponseFactory` |
| Common | `RequestMessage`, `ResponseMessage`, `MessageType`, `JsonUtil`, `ErrorCode` |
