# Database Connection and Transaction Review

## 1. Current Problems

### 1.1 Some repositories swallow database errors instead of propagating them

The biggest example is `UserRepository`.

- Several methods catch `SQLException`, print the stack trace, and then return a fallback value such as:
  - `false`
  - `Optional.empty()`
  - an empty or partial list
  - `void` with no failure signal
- This means the service layer can incorrectly assume the database operation succeeded.
- In the registration flow, this can produce a "fake success" where the client is told registration worked and stores the returned user in memory, but the `users` table was never actually updated.

This behavior is inconsistent across the codebase.

- `UserRepository` often hides failures.
- `AuctionRepository`, `BidRepository`, and `AutoBidRepository` more often rethrow as `RuntimeException`.
- `ItemRepository` is mixed: some write methods rethrow properly, while some read methods still suppress DB failures.

This inconsistency makes bugs much harder to reason about.

### 1.2 Transaction handling is inconsistent and not standardized

Only some service methods use explicit transaction handling with:

- `setAutoCommit(false)`
- `commit()`
- `rollback()`

Examples that do use manual transaction handling:

- `BidService.placeBid()`
- `AuctionService.deleteAuction()`
- `AdminService.deleteUser()`

Examples that do not, even though they perform multiple writes:

- `AuctionService.createAuction()`
- `AuthService.register()` depends on repository write behavior but does not have a reliable transactional flow

This inconsistency creates several risks:

- multi-step write operations may partially succeed
- some failed operations may not be rolled back cleanly
- transaction state handling differs from one service to another
- there is no single standard for when and how transactions must be used

### 1.3 The current rollback/transaction style is not standardized even where it exists

There are discrepancies between classes that already use manual transactions.

- `BidService` uses `synchronized(connection)` and always resets with `setAutoCommit(true)`.
- `AuctionService.deleteAuction()` and `AdminService.deleteUser()` restore `oldAutoCommit` instead of always forcing `true`.
- `BidService` has helper methods like `safeRollback()` and `safeSetAutoCommit()`, while other services inline rollback logic directly.
- `BidService` protects the shared connection with synchronization, but other transactional services do not.

So even the "transaction-aware" parts of the system are not following one common pattern.

### 1.4 The entire server currently shares a single JDBC `Connection`

`DBConnection.getConnection()` creates one static connection and the same object is reused across modules and across client requests.

This means:

- all clients share one database connection
- all modules share one database connection
- transaction state is shared across unrelated requests
- one broken transaction can affect later requests from completely different users

This is the core architectural issue behind the "poisoned connection" behavior.

### 1.5 A failed transaction on the shared connection can poison later requests

When PostgreSQL is inside an explicit transaction and one SQL statement fails, the transaction enters an aborted state until `rollback()` is called.

Because the app reuses one shared connection:

- one request can fail
- that connection can remain in an aborted transaction state
- later requests reuse the same connection
- later requests fail with errors like:
  - `current transaction is aborted, commands ignored until end of transaction block`

This explains why one failure in one flow can later break login, register, or other unrelated operations.

### 1.6 The current architecture mixes three different concerns together

Right now the codebase is mixing:

- connection lifecycle management
- transaction management
- concurrency control

This leads to tactical fixes such as `synchronized(connection)` being used partly as a concurrency fix and partly as a safety guard for the shared connection architecture.

That made sense as a short-term patch for the autobid/bidding issue, but it is not a good long-term foundation.

### 1.7 Client-side session state can diverge from actual database state

The client stores the authenticated user in `AppContext` after a successful login/register response.

If the server incorrectly returns success even though the database write failed, then:

- the client thinks the user exists
- later requests reuse that in-memory user ID
- the database may reject related writes because the referenced row is not actually present

This is one reason a logged-in user can still trigger a foreign key failure on `seller_id`.

## 2. Solutions Going Forward

### 2.1 Standardize repository error handling immediately

All repositories should follow one rule:

- if the database operation fails, do not swallow the exception
- propagate the failure upward clearly

In practice:

- do not use `printStackTrace()` plus fallback return values for real database failures
- rethrow a consistent unchecked exception such as `RuntimeException` with clear context
- reserve fallback values like `Optional.empty()` or `false` for actual business results, not hidden infrastructure failures

This is the first fix because it makes failures visible instead of silently corrupting application behavior.

### 2.2 Standardize transaction handling for multi-step write operations

Any service method that performs multiple writes which must succeed together should use one consistent transaction pattern.

Examples in this project include:

- placing a bid
- creating an auction
- deleting an auction
- deleting a user and related auction/bid data

These methods should follow one standard flow:

1. start transaction
2. perform all related writes
3. commit on success
4. rollback on failure
5. restore connection state or close/return the connection

Read-only operations generally should not need this pattern.

### 2.3 Stop relying on one global shared JDBC connection

The long-term fix is to replace the current singleton `Connection` design with a shared `DataSource` plus pooled connections.

Recommended direction:

- use `HikariCP`

Why:

- it is much lighter than migrating to Spring
- it solves the single-connection bottleneck
- it isolates transaction state per request/operation
- it fits the current architecture with much less rewrite than a full framework migration

With HikariCP:

- the application holds one shared `DataSource`
- each request or service operation borrows its own `Connection`
- the connection is returned to the pool afterward

This prevents one failed request from poisoning all later requests.

### 2.4 Reduce or eliminate `synchronized(connection)` as architecture improves

With the current single-connection architecture, `synchronized(connection)` was used as a practical workaround, especially in bidding.

With pooled connections, this broad synchronization should usually no longer be necessary.

Instead:

- each transactional operation should use its own borrowed connection
- concurrency should be handled at the correct level
  - transaction boundaries
  - database row locking if needed
  - business-level protections for the same auction/item

This will reduce unnecessary blocking across unrelated requests.

### 2.5 Add a small transaction helper to reduce duplicated boilerplate

Even without Spring, the project can still reduce duplication by introducing a small internal transaction helper.

For example, a utility can centralize:

- disabling auto-commit
- commit
- rollback
- restoring connection state
- propagating failures consistently

This would make service code shorter and more standardized without requiring a framework rewrite.

### 2.6 Prioritize multi-write service fixes in this order

Recommended practical order:

1. fix repository exception swallowing, especially in `UserRepository`
2. add a standardized transaction pattern for `createAuction()`
3. review all other multi-write services for consistency
4. replace single shared connection with `HikariCP` and a `DataSource`
5. then simplify special-case synchronization where possible

This order gives the best balance between risk reduction and implementation effort.

### 2.7 Keep the autobid/bid fix as a useful short-term patch, but not the final architecture

The autobid/bid concurrency fix addressed a real problem and was a reasonable tactical solution under the current design.

However, it should be treated as:

- a short-term stabilization patch
- not the ideal long-term database architecture

The long-term goal should be:

- pooled connections
- standardized transaction handling
- consistent exception propagation
- concurrency control at the transaction/business-data level rather than by locking one shared JDBC connection

