# Server Development Guidelines

---

## 1. Feature-Based Packaging 

Every major feature (Auth, Auction, Bidding, etc.) lives in its **own package**.
- `com.nhom1.auction.server.auth`
- `com.nhom1.auction.server.auction`
- `com.nhom1.auction.server.bid`

**The Rule:** If you are assigned a feature, you **own** that package. Don't edit other teammates' packages without asking first.

---

## 2. The Architecture/Layers

Each feature package should follow this structure:
1. **`Handler.java`**: Translate JSON into DTOs using `JsonUtil`. Send DTO to server for processing. 
2. **`Service.java`**: Business logic. Call repository to get data.
3. **`Repository.java`**: SQL queries, database access. 
4. **`Module.java`**: Connects everything together and register to the message router.

---

## 3. IMPORTANT IMPORTANT IMPORTANT: "Talk to Services, Never Repositories"

When you need data from a table that *someone else* owns:
- ✅ **DO:** Call the other feature's `Service`.
- ❌ **DON'T:** Create a new `Repository` for their table or call their `Repository` directly.

*Example:* If the `Auction` module needs to know if a user is banned, it calls `AuthService.isUserBanned()`. 
-> Talk to the person who owns Auth service (Auth package) so he can add methods (if needed).

---

## 4. How to Add a New Feature

To add a feature (e.g., "Place Bid"):
1.  Create a new package `com.nhom1.auction.server.bid`.
2.  Build your `BidRepository`, `BidService`, and `BidHandler`.
3.  Implement a `BidModule` with an `init(conn, router)` method.
4.  **The only shared file you touch:** Add ONE line to `ServerContext.java`:
    E.g. `BidModule.init(this.connection, this.router);`

---

## 5. Communication (Envelopes & Goods)

- **The Envelope:** `requestId` and `type`. Handled by the `MessageRouter`.
- **The Goods:** `payload` (JSON text). Handled by the `Handler` using `JsonUtil`.

**Remember:** Always return a `ResponseMessage` with the same `requestId` you received, so the Client knows which answer belongs to which question.

---

*By following these rules, we avoid most of Git Merge Conflicts and keep our "Brain" (The Router) safe from bugs.*
