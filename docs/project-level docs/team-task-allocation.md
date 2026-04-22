# Team Task Allocation Plan

> **Prerequisite reading:** `feature-development-guideline.md` — every member must understand the
> Handler → Service → Repository pattern before starting.

---

## Architecture Reminder

Every feature follows the same 4-file server pattern. Use `AuthModule` as your reference:

```
common/dto/<feature>/          ← JSON contract (DTOs + MessageType entries)
server/<feature>/
  ├── FeatureRepository.java   (SQL only — no logic)
  ├── FeatureService.java      (logic only — no JSON, no SQL)
  ├── FeatureHandler.java      (JSON ↔ DTO bridge, calls Service)
  └── FeatureModule.java       (static init(), wires all 3, called from ServerContext)
```

**Client side** (use `SignInController` as reference):
1. Build DTO from form fields
2. Wrap in `RequestMessage<T>(MessageType.XYZ, dto)`
3. Call `ServerConnection.getInstance().sendRequest(req, ResponseClass.class)`
4. In `.thenAccept(response -> Platform.runLater(() -> { ... }))`: check `response.isSuccess()`,
   read `response.getPayload()`, call `AppNavigator.navigateTo(AppView.XYZ)` if needed

---

## Shared Files — Ownership Rules

These files will be touched by everyone. To avoid conflicts, **only Member 4 commits to them directly**. Everyone else raises the change in chat, Member 4 adds it.

| File | Owner | Why dangerous |
|---|---|---|
| `common/protocol/MessageType.java` | Member 4 | All 4 members need new entries |
| `server/infrastructure/ServerContext.java` | Member 4 | Every module registers here |
| `client/AppView.java` | Member 4 | New screens require new entries |

---

## Member 1 — Seller Flow

**Role:** Owns the path from "Seller submits an item for auction" to "the auction is stored and
visible in the system."

---

### Step 1 — Declare the common contract

Create `common/dto/auction/`:

**`CreateAuctionRequest.java`**
```
String name
String description
ItemCategory category          // ELECTRONICS | ART | VEHICLE
ItemCondition condition
BigDecimal startingBid
LocalDateTime startTime
LocalDateTime endTime
// Category-specific optional fields (null if unused):
String artist                  // ART
String era                     // ART
String brand                   // ELECTRONICS | VEHICLE
int warrantyMonths             // ELECTRONICS
int productionYear             // VEHICLE
String fuelType                // VEHICLE — store as String, parse to VehicleFuelType in Service
```

**`AuctionSummaryDto.java`** — **agree fields with Member 2 before writing this**
```
String auctionId               // UUID as String (easier JSON round-trip)
String itemId
String itemName
String itemCategory            // ItemCategory.name()
String status                  // AuctionStatus.name()
double currentHighestBid       // 0.0 if no bids yet
String endTime                 // LocalDateTime.toString()
String sellerId
```

**`CreateAuctionResponse.java`**
```
String auctionId
String status                  // AuctionStatus.name()
```

**`MyListingsResponse.java`**
```
List<AuctionSummaryDto> listings
```

Ask Member 4 to add to `MessageType.java`:
```java
CREATE_AUCTION,
LIST_MY_LISTINGS,   // already present — confirm no rename
```

---

### Step 2 — Server: `server/auction/`

**`ItemRepository.java`**
```java
public ItemRepository(Connection connection)
public void save(Item item, UUID sellerId)
// Item is a pure domain object — it does NOT carry sellerId.
// The repository handles the DB's seller_id column using the separate parameter.
// INSERT INTO items(id, seller_id, name, description, category, condition, ...).
// Use item.getCategory() to decide which extra columns to fill:
// Art   → artist, era
// Electronics → brand, warranty_months
// Vehicle → brand, production_year, fuel_type
public Optional<Item> findById(UUID id)
// SELECT, then switch on category column to reconstruct Art/Electronics/Vehicle
```

**`AuctionRepository.java`** — **publish these signatures to Members 2 and 3 on day 1**
```java
public AuctionRepository(Connection connection)
public void save(Auction auction)
// INSERT INTO auctions. Columns: id, item_id, start_time, end_time, status,
// current_highest_bid (0.0), highest_bidder_id (null), created_at, updated_at
// NOTE: auctions table has NO seller_id column — seller lives on the items table.
public Optional<Auction> findById(UUID id)
// JOIN with items to get seller_id, then reconstruct via DB constructor:
// new Auction(id, itemId, sellerId, startTime, endTime, highestBidderId,
//             currentHighestBid, status, createdAt, updatedAt)
public List<Auction> findAll()
// JOIN with items to include seller_id. Used by Member 2 (browse) and Member 3 (scheduler).
public List<Auction> findBySellerId(UUID sellerId)
// SELECT a.* FROM auctions a JOIN items i ON a.item_id = i.id WHERE i.seller_id = ?
public void updateStatus(UUID auctionId, AuctionStatus status)
// UPDATE auctions SET status=?, updated_at=? WHERE id=?
public void updateHighestBid(UUID auctionId, BigDecimal amount, UUID bidderId)
// UPDATE auctions SET current_highest_bid=?, highest_bidder_id=?, updated_at=? WHERE id=?
public void updateEndTime(UUID auctionId, LocalDateTime newEndTime)
// UPDATE auctions SET end_time=?, updated_at=? WHERE id=?
// Used by Member 3's anti-sniping scheduler (calls auction.extendEndTime() for in-memory update)
```

> **Resolved:** `auctions` table has no `seller_id` column per the schema. The seller is stored on
> the `items` table via `seller_id`. All queries that need seller info (`findById`, `findAll`,
> `findBySellerId`) must JOIN with `items`. This avoids schema changes and keeps the DB normalized.

**`AuctionService.java`**
```java
public AuctionService(ItemRepository itemRepository, AuctionRepository auctionRepository)

public Auction createAuction(String sellerId, CreateAuctionRequest dto) throws ValidationException
// 1. Validate: endTime must be after startTime; startingBid > 0 (validation only — not stored)
// 2. Call ItemFactory to build the Item — do NOT call new Art/Vehicle/Electronics directly:
//    ItemFactory.createArt(...)  /  createElectronics(...)  /  createVehicle(...)
// 3. itemRepository.save(item, UUID.fromString(sellerId))  ← sellerId passed separately
// 4. new Auction(item.getId(), UUID.fromString(sellerId), dto.startTime, dto.endTime)
// 5. auctionRepository.save(auction)
// 6. return auction

public List<AuctionSummaryDto> getMyListings(String sellerId)
// 1. auctionRepository.findBySellerId(UUID.fromString(sellerId))
// 2. For each auction, load item via itemRepository.findById(auction.getItemId())
// 3. Map to AuctionSummaryDto and return list
```

**`AuctionHandler.java`**
```java
public AuctionHandler(AuctionService auctionService)
public void register(MessageRouter router)
// Registers:
//   MessageType.CREATE_AUCTION  → handleCreateAuction(requestId, payloadJson)
//   MessageType.LIST_MY_LISTINGS → handleMyListings(requestId, payloadJson)
```

Inside `handleCreateAuction`:
```java
CreateAuctionRequest dto = JsonUtil.fromJson(payloadJson, CreateAuctionRequest.class);
// Extract sellerId from a wrapper or pass it in the DTO itself (see note below)
Auction auction = auctionService.createAuction(dto.getSellerId(), dto);
return new ResponseMessage<>(requestId, new CreateAuctionResponse(auction.getId().toString(), auction.getStatus().name()));
```

> **Important:** The server has no session — it does not know who is calling. The client must
> include the logged-in user's ID in the request DTO. Add `String sellerId` to
> `CreateAuctionRequest` and have the controller populate it from `AppContext.getCurrentUser().getUserID()`.

Inside `handleMyListings`:
```java
// Payload: { "userId": "..." }  — define a small GetMyListingsRequest DTO or just read the field
List<AuctionSummaryDto> listings = auctionService.getMyListings(userId);
return new ResponseMessage<>(requestId, new MyListingsResponse(listings));
```

**`AuctionModule.java`**
```java
public static void init(Connection connection, MessageRouter router) {
    ItemRepository itemRepo = new ItemRepository(connection);
    AuctionRepository auctionRepo = new AuctionRepository(connection);
    AuctionService service = new AuctionService(itemRepo, auctionRepo);
    AuctionHandler handler = new AuctionHandler(service);
    handler.register(router);
}
```

> `AuctionRepository` must also be returned so `ServerContext` can pass it to other modules.
> Change the signature: `public static AuctionRepository init(Connection, MessageRouter)` and
> return `auctionRepo`. Member 4 stores it in `ServerContext`.

---

### Step 3 — Client screens

**`MyListingsController.java`** (file exists, add logic):
```java
// On initialize():
String userId = AppContext.getCurrentUser().getUserID();
RequestMessage<Map<String,String>> req = new RequestMessage<>(
    MessageType.LIST_MY_LISTINGS, Map.of("userId", userId));
ServerConnection.getInstance().sendRequest(req, MyListingsResponse.class)
    .thenAccept(response -> Platform.runLater(() -> {
        if (response.isSuccess()) renderListings(response.getPayload().getListings());
    }));
```

**`CreateAuctionController.java`** (new file + new FXML):
```java
// On submit button:
CreateAuctionRequest dto = new CreateAuctionRequest();
dto.setSellerId(AppContext.getCurrentUser().getUserID());
// ... populate all fields from form ...
RequestMessage<CreateAuctionRequest> req = new RequestMessage<>(MessageType.CREATE_AUCTION, dto);
ServerConnection.getInstance().sendRequest(req, CreateAuctionResponse.class)
    .thenAccept(response -> Platform.runLater(() -> {
        if (response.isSuccess()) AppNavigator.navigateTo(AppView.MY_LISTINGS);
        else showError(response.getError().getMessage());
    }));
```

Ask Member 4 to add `CREATE_AUCTION` to `AppView` if a dedicated screen view is needed.

---

### Deliverables
- `server/auction/ItemRepository.java`, `AuctionRepository.java`, `AuctionService.java`, `AuctionHandler.java`, `AuctionModule.java`
- `common/dto/auction/CreateAuctionRequest.java`, `AuctionSummaryDto.java`, `CreateAuctionResponse.java`, `MyListingsResponse.java`
- `MyListingsController.java` (functional)
- `CreateAuctionController.java` + FXML (functional)

### Discuss With

| Topic | With | Deadline |
|---|---|---|
| `AuctionSummaryDto` exact field list | Member 2 | Before writing DTO |
| `AuctionRepository` method signatures | Members 2 & 3 | Day 1 — both depend on these |
| `AuctionModule.init()` return type change | Member 4 | Member 4 wires `ServerContext` |
| `CREATE_AUCTION` message type addition | Member 4 | Before writing Handler |

---

## Member 2 — Core Bidding Flow

**Role:** Owns the buyer experience — browsing auctions, viewing detail, placing bids, and seeing
bid history.

---

### Step 1 — Declare the common contract

Create `common/dto/bidding/`:

**`PlaceBidRequest.java`**
```
String auctionId
double amount
String bidderId     // from AppContext.getCurrentUser().getUserID()
```

**`PlaceBidResponse.java`**
```
String bidId
double newHighestBid
String newHighestBidderId
```

**`AuctionDetailDto.java`**
```
String auctionId
String itemId
String itemName
String itemDescription
String itemCategory
String itemCondition
String sellerId
double currentHighestBid
String highestBidderId      // null if no bids
String status
String startTime
String endTime
List<BidSummaryDto> bidHistory
```

**`BidSummaryDto.java`** (used inside `AuctionDetailDto`)
```
String bidId
String bidderId
double amount
String bidType              // MANUAL | AUTO
String createdAt
```

**`BidWithAuctionDto.java`** (used in My Bids screen)
```
String auctionId
String itemName
double yourBid
double currentHighestBid
String status
String endTime
boolean isWinning           // yourBid == currentHighestBid
```

**`MyBidsResponse.java`**
```
List<BidWithAuctionDto> bids
```

**`ListAuctionsResponse.java`**
```
List<AuctionSummaryDto> auctions   // AuctionSummaryDto is owned by Member 1
```

**`GetAuctionDetailRequest.java`**
```
String auctionId
```

`MessageType` entries `PLACE_BID`, `LIST_AUCTIONS`, `GET_AUCTION_DETAIL`, `LIST_MY_BIDS` already
exist — confirm with Member 4 no rename is needed.

---

### Step 2 — Server: `server/bidding/`

**`BidRepository.java`**
```java
public BidRepository(Connection connection)
public void save(BidTransaction bid)
// INSERT INTO bids(id, auction_id, bidder_id, amount, bid_type, created_at)
// NOTE: bids table has NO updated_at column — only insert created_at from bid.getCreatedAt()
public List<BidTransaction> findByAuctionId(UUID auctionId)
// SELECT * FROM bids WHERE auction_id=? ORDER BY created_at ASC
public List<BidWithAuctionDto> findByBidderId(UUID bidderId)
// SELECT bids.*, auctions.status, items.name as item_name,
//        auctions.current_highest_bid, auctions.end_time
// FROM bids JOIN auctions ON bids.auction_id=auctions.id
//           JOIN items ON auctions.item_id=items.id
// WHERE bids.bidder_id=?
```

**`BidService.java`** — **publish `placeBid()` signature to Member 3 on day 1**
```java
public BidService(BidRepository bidRepository, AuctionRepository auctionRepository)
// AuctionRepository is instantiated by Member 1 and passed in via ServerContext.

public BidTransaction placeBid(UUID bidderId, UUID auctionId, BigDecimal amount)
    throws InvalidBidException, AuctionClosedException
// CONCURRENCY — this is requirement 3.2.2. The correct approach given the existing code:
// 1. Load Auction from auctionRepository.findById(auctionId)
// 2. Call auction.placeBid(bidderId, amount, BidType.MANUAL, LocalDateTime.now())
//    — this method already acquires auctionLock internally (see Auction.java line ~130)
//    — DO NOT acquire the lock a second time here; Auction.java owns the locking
// 3. bidRepository.save(bidTransaction)
// 4. auctionRepository.updateHighestBid(auctionId, amount, bidderId)
// 5. Return the BidTransaction
// After step 5, BidHandler calls notificationService.broadcastBidUpdate(...)

public AuctionDetailDto getAuctionDetail(UUID auctionId)
// 1. auctionRepository.findById(auctionId)
// 2. Load item via itemRepository.findById(auction.getItemId())  ← need ItemRepository from M1
// 3. bidRepository.findByAuctionId(auctionId) → map to List<BidSummaryDto>
// 4. Assemble and return AuctionDetailDto

public ListAuctionsResponse listAllAuctions()
// auctionRepository.findAll() → for each, load item name → map to AuctionSummaryDto

public MyBidsResponse getMyBids(UUID bidderId)
// bidRepository.findByBidderId(bidderId) → return wrapped in MyBidsResponse
```

> `BidService` needs both `AuctionRepository` (Member 1) and `ItemRepository` (Member 1).
> `BidModule.init()` must receive both as parameters from `ServerContext`.

**`BidHandler.java`**
```java
public BidHandler(BidService bidService, NotificationService notificationService)
// NotificationService is Member 4's class. Receive as constructor arg.
public void register(MessageRouter router)
// Registers 4 routes:
//   PLACE_BID         → handlePlaceBid
//   LIST_AUCTIONS     → handleListAuctions
//   GET_AUCTION_DETAIL → handleGetAuctionDetail
//   LIST_MY_BIDS      → handleMyBids
```

Inside `handlePlaceBid`:
```java
PlaceBidRequest dto = JsonUtil.fromJson(payloadJson, PlaceBidRequest.class);
BidTransaction bid = bidService.placeBid(
    UUID.fromString(dto.getBidderId()),
    UUID.fromString(dto.getAuctionId()),
    BigDecimal.valueOf(dto.getAmount())
);
// Fire push notification to all clients watching this auction:
notificationService.broadcastBidUpdate(
    UUID.fromString(dto.getAuctionId()),
    bid.getAmount(),
    bid.getBidderId()
);
return new ResponseMessage<>(requestId, new PlaceBidResponse(...));
```

**`BidModule.java`**
```java
public static void init(
    Connection connection,
    MessageRouter router,
    AuctionRepository auctionRepository,   // passed from ServerContext (built by M1's module)
    ItemRepository itemRepository,          // passed from ServerContext (built by M1's module)
    NotificationService notificationService // passed from ServerContext (built by M4)
) {
    BidRepository bidRepo = new BidRepository(connection);
    BidService service = new BidService(bidRepo, auctionRepository, itemRepository);
    BidHandler handler = new BidHandler(service, notificationService);
    handler.register(router);
}
```

---

### Step 3 — Client screens

**`AuctionBrowseController.java`** (file exists, add logic):
```java
// On initialize():
RequestMessage<Void> req = new RequestMessage<>(MessageType.LIST_AUCTIONS, null);
ServerConnection.getInstance().sendRequest(req, ListAuctionsResponse.class)
    .thenAccept(response -> Platform.runLater(() -> renderGrid(response.getPayload().getAuctions())));
// On card click → store selected auctionId somewhere accessible, then:
AppContext.setSelectedAuctionId(auctionId);   // Member 4 adds this field to AppContext
AppNavigator.navigateTo(AppView.AUCTION_DETAIL);
```

**`AuctionDetailController.java`** (file exists, add logic):
```java
// On initialize():
String auctionId = AppContext.getSelectedAuctionId();
RequestMessage<GetAuctionDetailRequest> req = new RequestMessage<>(
    MessageType.GET_AUCTION_DETAIL, new GetAuctionDetailRequest(auctionId));
ServerConnection.getInstance().sendRequest(req, AuctionDetailDto.class)
    .thenAccept(response -> Platform.runLater(() -> renderDetail(response.getPayload())));

// On "Place Bid" button:
PlaceBidRequest dto = new PlaceBidRequest(auctionId,
    Double.parseDouble(bidAmountField.getText()),
    AppContext.getCurrentUser().getUserID());
ServerConnection.getInstance().sendRequest(
    new RequestMessage<>(MessageType.PLACE_BID, dto), PlaceBidResponse.class)
    .thenAccept(response -> Platform.runLater(() -> {
        if (response.isSuccess()) updatePriceLabel(response.getPayload().getNewHighestBid());
        else showError(response.getError().getMessage());
    }));

// Real-time push (wired by Member 4):
// ServerConnection.getInstance().registerPushHandler(MessageType.PUSH_BID_UPDATE, json -> {
//     BidUpdateEvent event = JsonUtil.fromJson(json, BidUpdateEvent.class);
//     if (event.getAuctionId().equals(auctionId)) Platform.runLater(() -> updatePriceLabel(...));
// });
```

**`MyBidsController.java`** (file exists, add logic):
```java
String userId = AppContext.getCurrentUser().getUserID();
RequestMessage<Map<String,String>> req = new RequestMessage<>(
    MessageType.LIST_MY_BIDS, Map.of("userId", userId));
ServerConnection.getInstance().sendRequest(req, MyBidsResponse.class)
    .thenAccept(response -> Platform.runLater(() -> renderBids(response.getPayload().getBids())));
```

---

### Deliverables
- `server/bidding/BidRepository.java`, `BidService.java`, `BidHandler.java`, `BidModule.java`
- `common/dto/bidding/PlaceBidRequest.java`, `PlaceBidResponse.java`, `AuctionDetailDto.java`, `BidSummaryDto.java`, `BidWithAuctionDto.java`, `MyBidsResponse.java`, `ListAuctionsResponse.java`, `GetAuctionDetailRequest.java`
- Functional `AUCTION_BROWSE`, `AUCTION_DETAIL`, `MY_BIDS` screens

### Discuss With

| Topic | With | Deadline |
|---|---|---|
| `AuctionSummaryDto` fields | Member 1 | Before writing DTO |
| `AuctionRepository` + `ItemRepository` signatures | Member 1 | Day 1 |
| `BidModule.init()` parameter list | Member 4 | Member 4 wires `ServerContext` |
| `NotificationService.broadcastBidUpdate()` signature | Member 4 | Before writing `BidHandler` |
| `AutoBidService.triggerAutoBids()` call site in `BidService` | Member 3 | Before writing `BidService` — agree whether M3's service is injected or called after |
| `PUSH_BID_UPDATE` DTO fields | Member 4 | Before writing `BidHandler` |
| `AppContext.setSelectedAuctionId()` helper | Member 4 | Member 4 adds this to `AppContext` |

---

## Member 3 — System Automation & Payment Flow

**Role:** Owns time-driven auction lifecycle, auto-bidding bot, payment, and admin dashboard data.

---

### Step 1 — Declare the common contract

Create `common/dto/admin/`:

**`AdminUserListResponse.java`**: `List<UserSummaryDto> users`

**`UserSummaryDto.java`**: `String id`, `String username`, `String email`, `String role`

**`AdminDeleteUserRequest.java`**: `String targetUserId`, `String callerId`

**`AdminAuctionListResponse.java`**: `List<AuctionSummaryDto> auctions`

Create `common/dto/autobid/`:

**`AutoBidConfigRequest.java`**: `String auctionId`, `String bidderId`, `double maxAmount`, `double increment`

**`AutoBidConfigResponse.java`**: `String status`

Create `common/dto/payment/`:

**`ProcessPaymentRequest.java`**: `String auctionId`, `String bidderId`

**`ProcessPaymentResponse.java`**: `String status`

Ask Member 4 to add to `MessageType.java`:
```java
ADMIN_LIST_USERS,
ADMIN_DELETE_USER,
ADMIN_LIST_AUCTIONS,
PROCESS_PAYMENT,
// AUTO_BID_CONFIG already exists — confirm no rename
```

---

### Step 2 — Server

**Module: `server/admin/`**

**`AdminService.java`**
```java
public AdminService(UserRepository userRepository, AuctionRepository auctionRepository)
// UserRepository already exists in server/auth/. Receive it as a constructor arg.
// AuctionRepository is Member 1's class. Receive from ServerContext.

public List<UserSummaryDto> getAllUsers()
public void deleteUser(UUID targetId, UUID callerId) throws UnauthorizedActionException
// Verify callerId has ADMIN role via userRepository.findByIdentifier() or store role in session
public List<AuctionSummaryDto> getAllAuctions()
```

**`AdminHandler.java`**
```java
public AdminHandler(AdminService adminService)
public void register(MessageRouter router)
// ADMIN_LIST_USERS, ADMIN_DELETE_USER, ADMIN_LIST_AUCTIONS
```

**`AdminModule.java`**
```java
public static void init(
    Connection connection,
    MessageRouter router,
    UserRepository userRepository,        // from ServerContext (AuthModule already builds this)
    AuctionRepository auctionRepository   // from ServerContext (AuctionModule builds this)
) { ... }
```

> **Already done:** `AuthModule.init()` now returns `UserRepository`. `ServerContext` already
> captures the return value and can pass it to `AdminModule`.

---

**Module: `server/automation/`**

**`AutoBidRepository.java`**
```java
public AutoBidRepository(Connection connection)
public void saveConfig(UUID auctionId, UUID bidderId, BigDecimal maxAmount, BigDecimal increment)
// INSERT OR REPLACE INTO auto_bids(auction_id, bidder_id, max_amount, increment, created_at)
// SQLite's INSERT OR REPLACE handles the upsert on composite PK (auction_id, bidder_id)
public List<AutoBidConfig> findByAuctionId(UUID auctionId)
// SELECT * FROM auto_bids WHERE auction_id=? ORDER BY created_at ASC
// Map each row to an AutoBidConfig value object (define a simple class with the 4 fields)
```

**`AutoBidConfig.java`** (simple value class, put in `server/automation/`):
```java
public UUID auctionId; public UUID bidderId;
public BigDecimal maxAmount; public BigDecimal increment;
```

**`AutoBidService.java`** — **publish `triggerAutoBids()` signature to Member 2 on day 1**
```java
public AutoBidService(AutoBidRepository autoBidRepository, BidService bidService)
// BidService is Member 2's class. Receive via constructor — Member 4 wires in ServerContext.

public void saveConfig(AutoBidConfigRequest dto)
// autoBidRepository.saveConfig(...)

public void triggerAutoBids(UUID auctionId, BigDecimal newHighestBid, UUID currentHighestBidderId)
// 1. autoBidRepository.findByAuctionId(auctionId)
// 2. Find first config where bidderId != currentHighestBidderId && config.maxAmount > newHighestBid
// 3. Calculate nextBid = newHighestBid + config.increment
// 4. If nextBid <= config.maxAmount: call bidService.placeBid(config.bidderId, auctionId, nextBid)
//    which will trigger another broadcastBidUpdate and re-enter triggerAutoBids recursively
//    via BidHandler → this continues until no auto-bidder can outbid
// IMPORTANT: add a recursion guard (max depth or check list exhaustion) to prevent infinite loop
```

> `BidService.placeBid()` already calls the notification. But it also needs to call
> `triggerAutoBids()` again after a MANUAL bid. Agree with Member 2: the cleanest approach is for
> `BidHandler.handlePlaceBid()` to call `autoService.triggerAutoBids(...)` after a successful
> manual bid, rather than inside `BidService` (keeps Service layer clean of cross-module deps).

**`AuctionScheduler.java`** (background task — does NOT follow Handler/Service/Repository)
```java
public AuctionScheduler(AuctionRepository auctionRepository, NotificationService notificationService)

public void start()
// ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
// scheduler.scheduleAtFixedRate(this::tick, 0, 1, TimeUnit.SECONDS);

private void tick()
// 1. auctionRepository.findAll() — get all auctions
// 2. For each auction with status RUNNING:
//    a. If auction.getEndTime().isBefore(LocalDateTime.now()):
//       — Anti-sniping check: if most recent bid's createdAt is within last X seconds,
//         auctionRepository.updateEndTime(id, endTime.plusSeconds(Y))
//       — Otherwise: auctionRepository.updateStatus(id, AuctionStatus.FINISHED)
//                    notificationService.broadcastAuctionEnded(id, auction.getHighestBidderId())
//    b. If auction.getStatus() == OPEN && auction.getStartTime().isBefore(now()):
//         auctionRepository.updateStatus(id, AuctionStatus.RUNNING)
// 3. For OPEN auctions whose startTime has passed: transition to RUNNING

public void stop()
// scheduler.shutdown();
```

> Anti-sniping requires knowing the timestamp of the last bid. Either query the DB for
> `MAX(created_at) FROM bids WHERE auction_id=?` inside `tick()`, or cache it. The DB query
> approach is simpler — add `Optional<LocalDateTime> findLastBidTime(UUID auctionId)` to
> `BidRepository` (Member 2 adds this method).

**`AutoBidModule.java`**
```java
public static void init(Connection connection, MessageRouter router, BidService bidService) {
    AutoBidRepository repo = new AutoBidRepository(connection);
    AutoBidService service = new AutoBidService(repo, bidService);
    // Register AUTO_BID_CONFIG route inline or via a small handler:
    router.register(MessageType.AUTO_BID_CONFIG, (requestId, payloadJson) -> {
        AutoBidConfigRequest dto = JsonUtil.fromJson(payloadJson, AutoBidConfigRequest.class);
        service.saveConfig(dto);
        return new ResponseMessage<>(requestId, new AutoBidConfigResponse("OK"));
    });
    // Expose service so BidHandler can call triggerAutoBids via ServerContext
}
```

---

**Module: `server/payment/`**

**`PaymentService.java`**
```java
public PaymentService(AuctionRepository auctionRepository, UserRepository userRepository)

public void processPayment(UUID auctionId, UUID bidderId) throws UnauthorizedActionException
// 1. Load auction from auctionRepository.findById(auctionId)
// 2. Verify auction.getStatus() == FINISHED
// 3. Verify auction.getHighestBidderId().equals(bidderId)
// 4. auctionRepository.updateStatus(auctionId, AuctionStatus.PAID)
```

**`PaymentHandler.java`**: registers `PROCESS_PAYMENT`

**`PaymentModule.java`**: `init(Connection, MessageRouter, AuctionRepository, UserRepository)`

---

### Step 3 — Client screens

**`AdminOverviewController.java`** (file exists, add logic):
```java
// On initialize(), send two requests:
ServerConnection.getInstance().sendRequest(
    new RequestMessage<>(MessageType.ADMIN_LIST_USERS, null), AdminUserListResponse.class)
    .thenAccept(r -> Platform.runLater(() -> renderUserTable(r.getPayload().getUsers())));

ServerConnection.getInstance().sendRequest(
    new RequestMessage<>(MessageType.ADMIN_LIST_AUCTIONS, null), AdminAuctionListResponse.class)
    .thenAccept(r -> Platform.runLater(() -> renderAuctionTable(r.getPayload().getAuctions())));
```

**`PaymentController.java`** (file exists, add logic):
```java
// On "Pay Now":
ProcessPaymentRequest dto = new ProcessPaymentRequest(
    AppContext.getSelectedAuctionId(), AppContext.getCurrentUser().getUserID());
ServerConnection.getInstance().sendRequest(
    new RequestMessage<>(MessageType.PROCESS_PAYMENT, dto), ProcessPaymentResponse.class)
    .thenAccept(r -> Platform.runLater(() -> {
        if (r.isSuccess()) AppNavigator.navigateTo(AppView.AUCTION_BROWSE);
        else showError(r.getError().getMessage());
    }));
```

**Auto-bid popup** (new small FXML + controller):
```java
AutoBidConfigRequest dto = new AutoBidConfigRequest(
    AppContext.getSelectedAuctionId(),
    AppContext.getCurrentUser().getUserID(),
    Double.parseDouble(maxBidField.getText()),
    Double.parseDouble(incrementField.getText()));
ServerConnection.getInstance().sendRequest(
    new RequestMessage<>(MessageType.AUTO_BID_CONFIG, dto), AutoBidConfigResponse.class)
    .thenAccept(r -> Platform.runLater(() -> popup.close()));
```

---

### Deliverables
- `server/admin/` — `AdminService.java`, `AdminHandler.java`, `AdminModule.java`
- `server/automation/AutoBidRepository.java`, `AutoBidService.java`, `AutoBidConfig.java`, `AutoBidModule.java`, `AuctionScheduler.java`
- `server/payment/PaymentService.java`, `PaymentHandler.java`, `PaymentModule.java`
- All DTOs in `common/dto/admin/`, `common/dto/autobid/`, `common/dto/payment/`
- Functional `ADMIN_OVERVIEW`, `PAYMENT` screens, auto-bid popup

### Discuss With

| Topic | With | Deadline |
|---|---|---|
| `AuctionRepository` signatures (updateStatus, updateEndTime) | Member 1 | Day 1 |
| `AutoBidService.triggerAutoBids()` call site | Member 2 | Before Member 2 writes `BidHandler` |
| `BidRepository.findLastBidTime()` for anti-sniping | Member 2 | Before writing `tick()` |
| ~~`AuthModule.init()` returning `UserRepository`~~ | ~~Member 4~~ | **Already done** — returns `UserRepository` now |
| `NotificationService.broadcastAuctionEnded()` signature | Member 4 | Before writing `tick()` |
| `BidService` ref passed to `AutoBidModule` | Member 4 | Member 4 wires `ServerContext` |

---

## Member 4 — Coordinator

**Role:** Owns the real-time push pipeline, gatekeeps all shared files, wires `ServerContext`,
and unblocks teammates on day 1.

---

### Day 1 Deliverables (must be done before others start coding)

1. **Add `selectedAuctionId` to `AppContext.java`:**
   ```java
   private static String selectedAuctionId;
   public static void setSelectedAuctionId(String id) { selectedAuctionId = id; }
   public static String getSelectedAuctionId() { return selectedAuctionId; }
   ```

2. **Add all `MessageType` entries** (collect from teammates, add in one commit):
   ```java
   CREATE_AUCTION,
   ADMIN_LIST_USERS,
   ADMIN_DELETE_USER,
   ADMIN_LIST_AUCTIONS,
   PROCESS_PAYMENT,
   PUSH_BID_UPDATE,       // server-push only
   PUSH_AUCTION_ENDED,    // server-push only
   // LIST_MY_LISTINGS, PLACE_BID, LIST_AUCTIONS, GET_AUCTION_DETAIL,
   // LIST_MY_BIDS, AUTO_BID_CONFIG already exist
   ```

3. **Publish `NotificationService` stub** (empty methods) so Members 2 & 3 can compile:
   ```java
   // server/infrastructure/NotificationService.java
   public class NotificationService {
       public NotificationService(ClientRegistry registry) { ... }
       public void broadcastBidUpdate(UUID auctionId, BigDecimal newBid, UUID newHighestBidderId) {}
       public void broadcastAuctionEnded(UUID auctionId, UUID winnerId) {}
   }
   ```

4. **Publish `registerPushHandler` stub on `ServerConnection`** so Member 2 can compile
   client-side real-time code.

---

### Server Tasks

**`ClientRegistry.java`** (new file in `server/infrastructure/`):
```java
public class ClientRegistry {
    private final ConcurrentHashMap<String, ClientHandler> clients = new ConcurrentHashMap<>();
    public void register(String clientId, ClientHandler handler)
    public void unregister(String clientId)
    public void broadcast(String json)
    // Iterate all ClientHandlers and call handler.push(json) on each
    public void send(String userId, String json)
    // Find handler by userId and call push(json)
}
```

**Extend `ClientHandler.java`**:
```java
// Add field:
private final PrintWriter out;   // already exists as local variable — promote to field

// Add method:
public void push(String json) {
    if (out != null) out.println(json);
}
```

> `ClientHandler` currently creates `PrintWriter out` as a local variable inside `run()`. Promote
> it to a field so `push()` can access it. Also store a `clientId` (UUID) so `ClientRegistry` can
> map users to handlers. Pass `clientId` and `ClientRegistry` through the constructor.

**Extend `Server.java`** to pass `ClientRegistry` to each new `ClientHandler`:
```java
// In the accept loop:
ClientHandler handler = new ClientHandler(socket, router, clientId, registry);
registry.register(clientId, handler);
```

**`NotificationService.java`** (fill in real implementation):
```java
public void broadcastBidUpdate(UUID auctionId, BigDecimal newBid, UUID newHighestBidderId) {
    BidUpdateEvent event = new BidUpdateEvent(auctionId.toString(), newBid.doubleValue(),
        newHighestBidderId.toString());
    // Wrap in a push envelope — use a ResponseMessage with a special type but no requestId:
    ResponseMessage<BidUpdateEvent> msg = new ResponseMessage<>(null, event);
    // Set type field or wrap differently — see push protocol note below
    clientRegistry.broadcast(JsonUtil.toJson(msg));
}
```

> **Push protocol note:** `ResponseMessage` currently has no `type` field. Server-push messages
> need a way for the client to know which push handler to invoke. Add a `String pushType` field
> to `ResponseMessage` (or create a separate `PushMessage` wrapper). The client listener checks:
> if `requestId == null` && `pushType != null` → route to push handler. Agree this with the team.

**Extend `ServerConnection.java`** with push handling:
```java
// Add field:
private final Map<String, Consumer<String>> pushHandlers = new ConcurrentHashMap<>();

// New public method:
public void registerPushHandler(MessageType type, Consumer<String> handler) {
    pushHandlers.put(type.name(), handler);
}

// In handleRawResponse(), add branch before the existing requestId lookup:
if (!root.has("requestId") || root.get("requestId").isNull()) {
    String pushType = root.has("pushType") ? root.get("pushType").asText() : null;
    if (pushType != null) {
        Consumer<String> h = pushHandlers.get(pushType);
        if (h != null) h.accept(json);
    }
    return;
}
```

---

### Shared File Tasks

**`ServerContext.java`** — final wired version:
```java
public ServerContext() throws Exception {
    this.router = new MessageRouter();
    this.connection = DBConnection.getConnection();
    this.clientRegistry = new ClientRegistry();
    this.notificationService = new NotificationService(clientRegistry);

    // Auth — AuthModule.init() already returns UserRepository (done)
    UserRepository userRepository = AuthModule.init(connection, router);

    // Auction — change AuctionModule.init() to return AuctionRepository
    AuctionRepository auctionRepo = AuctionModule.init(connection, router);

    // Bidding — needs AuctionRepository + ItemRepository from AuctionModule
    // Also needs NotificationService and AutoBidService (circular dep — see note)
    AutoBidRepository autoBidRepo = new AutoBidRepository(connection);
    // Temporarily pass null for AutoBidService; set it after BidService is constructed:
    BidService bidService = BidModule.init(connection, router, auctionRepo,
        itemRepository, notificationService);

    AutoBidService autoBidService = new AutoBidService(autoBidRepo, bidService);
    AutoBidModule.registerRoutes(router, autoBidService);   // just route registration

    AdminModule.init(connection, router, userRepository, auctionRepo);
    PaymentModule.init(connection, router, auctionRepo, userRepository);

    this.scheduler = new AuctionScheduler(auctionRepo, notificationService);
    this.scheduler.start();
}
```

> There is a circular dependency risk between `BidService` and `AutoBidService` (each needs the
> other). Resolve it by having `BidHandler` call `AutoBidService.triggerAutoBids()` directly
> (Handler receives both services), rather than `BidService` calling `AutoBidService`. This keeps
> both services clean.

**`AppView.java`** — add entry for Create Auction screen if Member 1 builds it as a separate view:
```java
CREATE_AUCTION("/views/user/create_auction.fxml", null),
```

**`AuthModule.java`** — ✅ **already changed** to return `UserRepository`:
```java
public static UserRepository init(Connection connection, MessageRouter router) {
    UserRepository repository = new UserRepository(connection);
    ...
    return repository;
}
```

**`AuctionModule.java`** — change return type per Member 1's module (coordinate):
```java
public static AuctionRepository init(Connection connection, MessageRouter router) { ... }
```

---

### Deliverables
- `server/infrastructure/ClientRegistry.java`
- `server/infrastructure/NotificationService.java`
- Extended `ClientHandler.java` (push method + registry integration)
- Extended `ServerConnection.java` (push handler registration)
- Final `ServerContext.java` with all modules wired
- `MessageType.java` with all entries
- `AppView.java` updated
- `AppContext.java` with `selectedAuctionId`
- `AuthModule.java` returning `UserRepository`

### Discuss With

| Topic | With | Deadline |
|---|---|---|
| Push protocol: `pushType` field on `ResponseMessage` | All | Day 1 — everyone's code touches this |
| `AuctionModule.init()` return type | Member 1 | Before writing `ServerContext` |
| `BidModule.init()` parameter list | Member 2 | Before writing `ServerContext` |
| Circular dep: `BidService` ↔ `AutoBidService` resolution | Members 2 & 3 | Day 1 |
| ~~`AuthModule.init()` return type change~~ | ~~Existing code~~ | **Already done** — compiles cleanly |

---

## Overlap & Conflict Risk Map

| Risk | Resolution |
|---|---|
| Multiple people edit `MessageType.java` | Member 4 owns; text message Member 4 the entry you need |
| Multiple people edit `ServerContext.java` | Member 4 owns; submit module signature, Member 4 wires it |
| `AuctionRepository` instantiated twice → separate lock states | Instantiate once in `ServerContext`, pass as arg to every module |
| `BidService` needs `AuctionRepository` before Member 1 is done | Member 1 delivers a stub `AuctionRepository` (empty method bodies) on day 1 |
| `BidHandler` needs `NotificationService` before Member 4 is done | Member 4 delivers a stub `NotificationService` on day 1 |
| `AutoBidService` needs `BidService` and vice versa | Resolve by injecting `AutoBidService` into `BidHandler`, not into `BidService` |
| `AuctionDetailController` registers push handler — but push system may not be ready | Member 4 delivers `registerPushHandler()` stub on day 1; real push fires later |
| ~~`UserRepository` trapped inside `AuthModule`~~ | ✅ Resolved — `AuthModule.init()` now returns `UserRepository` |

---

## Contract Signatures — Suggested Definitions

These are the exact signatures that must be agreed before coding starts. They are suggestions
based on the existing codebase conventions (see `AuthHandler` / `UserRepository` / `AuthService`
as style reference).

### Group 1: Member 1 publishes — Members 2 & 3 depend on these

```java
// ── AuctionRepository ─────────────────────────────────────────────────────
// All date columns stored as ISO string (LocalDateTime.toString()) — same as UserRepository
// All UUID columns stored as String — same as UserRepository
// seller_id is on the items table, NOT on auctions. Use JOIN queries.

public void save(Auction auction)
// Maps to: id, item_id, start_time, end_time, status, current_highest_bid, highest_bidder_id,
// created_at, updated_at
// NOTE: no seller_id column on auctions — seller comes from items table.

public Optional<Auction> findById(UUID id)
// JOIN with items to get seller_id. Returns Auction loaded via the DB constructor:
// new Auction(id, itemId, sellerId, startTime, endTime,
//             highestBidderId, currentHighestBid, status, createdAt, updatedAt)

public List<Auction> findAll()
// JOIN with items to include seller_id. Used by browse + scheduler.

public List<Auction> findBySellerId(UUID sellerId)
// SELECT a.* FROM auctions a JOIN items i ON a.item_id = i.id WHERE i.seller_id = ?

public void updateStatus(UUID auctionId, AuctionStatus status)
// UPDATE auctions SET status=?, updated_at=? WHERE id=?

public void updateHighestBid(UUID auctionId, BigDecimal amount, UUID bidderId)
// UPDATE auctions SET current_highest_bid=?, highest_bidder_id=?, updated_at=? WHERE id=?

public void updateEndTime(UUID auctionId, LocalDateTime newEndTime)
// UPDATE auctions SET end_time=?, updated_at=? WHERE id=?
// Needed by anti-sniping in AuctionScheduler (also call auction.extendEndTime() in-memory)
```

**Why these exact signatures:**
- `findAll()` with no parameters is used by two consumers (browse + scheduler). Filtering can be
  done in the caller.
- Separate `updateStatus` and `updateHighestBid` rather than a single `update(Auction)` — avoids
  accidentally overwriting the bid cache when only status changes, and vice versa.
- All UUIDs passed as `UUID` objects not `String` — matches how `UserRepository` receives params.
  The SQL layer calls `.toString()` internally.
- All read queries JOIN `auctions` with `items` to get `seller_id`, since it is not stored on the
  `auctions` table.

---

```java
// ── ItemRepository ────────────────────────────────────────────────────────

public void save(Item item, UUID sellerId)
// Item is a pure domain object — it does NOT carry sellerId.
// The sellerId is passed as a separate parameter and written to the items.seller_id column.

public Optional<Item> findById(UUID id)
// SELECT, then switch on category column to reconstruct Art/Electronics/Vehicle
```

**Why `save(Item, UUID sellerId)` instead of putting sellerId on Item:**
- An `Item` represents the physical object (a laptop, a painting). Who is selling it is a
  relationship concern, not an intrinsic property of the item.
- The DB has `seller_id` on `items` for referential integrity, but the Repository layer handles
  that mapping — the entity stays clean.

---

### Group 2: Member 2 publishes — Member 3 depends on this

```java
// ── BidService ────────────────────────────────────────────────────────────

public BidTransaction placeBid(UUID bidderId, UUID auctionId, BigDecimal amount)
    throws InvalidBidException, AuctionClosedException
```

**Why this exact signature:**
- Takes `UUID` for bidderId and auctionId so the caller does not have to parse strings.
- `BigDecimal` for amount — consistent with `Auction.placeBid()` which already uses `BigDecimal`.
  `double` would lose precision for monetary values.
- Only two checked exceptions declared — the same ones `Auction.placeBid()` throws. Member 3's
  `AutoBidService.triggerAutoBids()` must catch both.
- No `BidType` parameter — `placeBid()` always stores `BidType.MANUAL`. `AutoBidService` calls a
  separate internal path or overloads with `BidType bidType` if needed. Suggested overload:
  ```java
  public BidTransaction placeBid(UUID bidderId, UUID auctionId, BigDecimal amount, BidType bidType)
  // The 3-arg version calls this with BidType.MANUAL
  // AutoBidService calls this with BidType.AUTO
  ```

**Additionally, Member 2 must add:**
```java
// ── BidRepository ─────────────────────────────────────────────────────────
public Optional<LocalDateTime> findLastBidTime(UUID auctionId)
// SELECT MAX(created_at) FROM bids WHERE auction_id=?
// Used by AuctionScheduler for anti-sniping check
```

---

### Group 3: Member 4 publishes — Members 2 & 3 depend on these

```java
// ── NotificationService ───────────────────────────────────────────────────

public void broadcastBidUpdate(UUID auctionId, BigDecimal newBid, UUID newHighestBidderId)
// Called by BidHandler after placeBid() succeeds

public void broadcastAuctionEnded(UUID auctionId, UUID winnerId)
// Called by AuctionScheduler.tick() when status transitions to FINISHED
// winnerId may be null if no bids were placed
```

**Why these exact signatures:**
- Pass `UUID` and `BigDecimal` — same types as the domain. The implementation does `.toString()`
  and `.doubleValue()` internally when building the push JSON.
- Two separate methods rather than a generic `broadcast(event)` — keeps each call site explicit
  and prevents Members 2 & 3 from having to know about the push DTO internals.

```java
// ── ServerConnection (client-side) ────────────────────────────────────────

public void registerPushHandler(MessageType type, Consumer<String> handler)
// handler receives the raw JSON string of the push payload
// Usage in AuctionDetailController:
// ServerConnection.getInstance().registerPushHandler(MessageType.PUSH_BID_UPDATE, json -> {
//     BidUpdateEvent event = JsonUtil.fromJson(json, BidUpdateEvent.class);
//     Platform.runLater(() -> updatePriceLabel(event.getNewHighestBid()));
// });
```

**Why `Consumer<String>` instead of a typed callback:**
- `ServerConnection` is a generic infrastructure class. Using `Consumer<String>` + `JsonUtil` in
  the controller keeps the connection layer unaware of any specific DTO type.
- If a strongly-typed version is preferred, use `Consumer<BidUpdateEvent>` and have
  `ServerConnection` deserialize — but then `ServerConnection` must know about every push DTO,
  which couples infrastructure to feature code.

---

### Group 4: Shared DTO — Members 1 & 2 must agree

```java
// ── AuctionSummaryDto ─────────────────────────────────────────────────────
// Used by: ListAuctionsResponse (M2), MyListingsResponse (M1), AdminAuctionListResponse (M3)

public class AuctionSummaryDto {
    private String auctionId;       // UUID.toString()
    private String itemId;          // UUID.toString()
    private String itemName;
    private String itemCategory;    // ItemCategory.name() — "ELECTRONICS" | "ART" | "VEHICLE"
    private String status;          // AuctionStatus.name()
    private double currentHighestBid;  // 0.0 if no bids
    private String highestBidderId; // null if no bids
    private String endTime;         // LocalDateTime.toString()
    private String sellerId;        // needed by MyListingsController to verify ownership
    // constructors, getters, setters
}
```

**Why these exact fields:**
- All IDs as `String` — consistent with how `AuthResponse` stores `id` as `String`. Avoids
  Jackson UUID deserialization edge cases.
- `double` for monetary value — acceptable for display. If you need exact arithmetic, use
  `String` and parse to `BigDecimal` in the calling code.
- `sellerId` included so `MyListingsController` can verify ownership client-side without a second
  request.
- No item description or bid history — those belong in `AuctionDetailDto`. Summary is for lists.

---

### Group 5: Push event DTOs — Members 4 defines, Members 2 & 3 use

```java
// common/dto/notification/BidUpdateEvent.java
public class BidUpdateEvent {
    private String auctionId;
    private double newHighestBid;
    private String newHighestBidderId;
    private String timestamp;   // LocalDateTime.now().toString()
}

// common/dto/notification/AuctionEndedEvent.java
public class AuctionEndedEvent {
    private String auctionId;
    private String winnerId;    // null if no bids
    private double finalPrice;  // 0.0 if no bids
}
```

---

## Summary Table

| Member | Server | Client | Key Challenge |
|---|---|---|---|
| 1 (Seller) | `server/auction/` (5 files) | Create Auction, My Listings | ItemFactory pattern; JOIN with items for seller_id |
| 2 (Bidder) | `server/bidding/` (4 files) | Browse, Detail, My Bids | `Auction.placeBid()` already locks — do not double-lock |
| 3 (System) | `server/automation/`, `server/admin/`, `server/payment/` (9 files) | Admin, Payment, Auto-bid popup | Circular dep with BidService; recursion guard in triggerAutoBids |
| 4 (Coordinator) | `server/infrastructure/` extensions (3 files) | Push registration in AuctionDetail | Push protocol design; ServerContext wiring |
