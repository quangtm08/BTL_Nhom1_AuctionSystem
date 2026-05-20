# Thực thi Connection Pool — Toàn bộ thay đổi 3 Phase A/B/C

> **Mục đích tài liệu:** Giải thích toàn diện refactor migrate từ singleton `Connection` sang HikariCP DataSource. Mỗi quyết định đều có cặp **How** (cách làm) + **Why** (lý do). Người đọc xong sẽ hiểu được kiến trúc cuối và lý do từng bước.
>
> **Đối tượng:** Thành viên team chưa theo dõi quá trình refactor. Người sẽ làm tính năng mới chạm vào DB.
>
> **Trạng thái:** Phase A/B/C đã commit. 108/108 tests pass. Branch `refactor/connection-pool`.
>
> **Tham chiếu:**
> - Kế hoạch tổng thể: [`ke-hoach-di-chuyen-connection-pool.md`](ke-hoach-di-chuyen-connection-pool.md)
> - Doc chi tiết Phase C: [`phase-c-explicit-connection-passing.md`](phase-c-explicit-connection-passing.md)

---

## Mục lục

1. [Tại sao cần refactor — Bối cảnh khởi đầu](#1-tại-sao-cần-refactor)
2. [Kiến trúc đích — Đích đến của 3 phase](#2-kiến-trúc-đích)
3. [Phase A — HikariCP DataSource infrastructure](#3-phase-a)
4. [Phase B — Repository + Service Connection-passing](#4-phase-b)
5. [Phase C — Service tự quản lý transaction](#5-phase-c)
6. [Kiến trúc cuối — Run-time flow](#6-kiến-trúc-cuối)
7. [Hệ quả lên hiệu năng và đồng thời](#7-hệ-quả)
8. [Decision log — Quyết định lớn](#8-decision-log)
9. [Chiến lược test](#9-chiến-lược-test)
10. [Files affected — Tổng hợp](#10-files-affected)
11. [FAQ](#11-faq)
12. [Flow phụ — AutoBid, deleteUser, AuctionScheduler](#12-flow-phụ--autobid-deleteuser-auctionscheduler)
13. [Tham chiếu](#13-tham-chiếu)

---

## 1. Tại sao cần refactor

### 1.1 Tình trạng trước Phase A

```
DBConnection.getConnection()  → return java.sql.Connection (singleton)
   │
   └─ Mọi repository giữ field `Connection connection` mượn lúc startup
       │
       └─ Mọi service giữ field `Connection connection` lúc startup
           │
           └─ Mọi transaction bọc trong synchronized(connection) { ... }
```

**3 vấn đề cốt lõi:**

1. **Một connection duy nhất phục vụ toàn server.** SQLite + JDBC `Connection` không thread-safe. Nhiều thread cùng `execute()` trên cùng connection = undefined behavior. Code cũ dùng `synchronized` để tránh — đúng nhưng giết khả năng concurrent.
2. **Không có pool.** Nếu connection bị drop (network blip với PostgreSQL cloud, hoặc lock contention với SQLite WAL), không có fallback. Server phải restart.
3. **Khó test.** Mỗi unit test mượn connection thật → tốc độ chậm + cần cleanup DB. Service không có cách inject mock.

### 1.2 Tại sao chọn HikariCP

| Lựa chọn | Lý do loại / chọn |
|---|---|
| **HikariCP (chọn)** | Library connection pool de-facto cho JVM. Fast, low overhead, ổn định, có config detailed (max size, idle timeout, leak detection). |
| ~~Apache DBCP~~ | Chậm hơn HikariCP đáng kể. Cộng đồng nhỏ hơn. |
| ~~C3P0~~ | Legacy, ít được maintain. |
| ~~Tự viết pool~~ | Out of scope BTL. Có sẵn library tốt thì không tự viết. |

**Quyết định:** HikariCP 5.1.0 (`pom.xml` dep `com.zaxxer:HikariCP`).

### 1.3 Migration plan 3 phase

Doc gốc [`ke-hoach-di-chuyen-connection-pool.md`](ke-hoach-di-chuyen-connection-pool.md) chia refactor thành 3 giai đoạn:

| Phase | Tên doc | Mục tiêu chính |
|---|---|---|
| **A** | Infrastructure | Đưa HikariCP vào hạ tầng. `DBConnection.getDataSource()` trả `DataSource`. Backward-compatible (services chưa đổi). |
| **B** | Explicit Connection Passing | Repository nhận `DataSource`. Mỗi method có 2 overload: `(args)` self-borrow và `(args, Connection)` dùng connection truyền vào. Service dùng overload thứ 2 trong transaction. |
| **C** | Service per-tx Borrow | Service drop field `Connection`, drop `synchronized`. Mỗi method transactional tự mượn connection per-call. Race protection chuyển lên tầng DB. |

**Why 3 phase thay vì 1 PR khổng lồ:**

- Mỗi phase commit-by-commit dễ review. Refactor lớn 1 lần = không ai dám approve.
- Phase A backward-compatible → có thể merge sớm, các phase sau làm dần. Nếu Phase B/C delay không block code khác.
- Tests pass sau mỗi phase. Bisect bug dễ.

---

## 2. Kiến trúc đích

### 2.1 Pattern target

```
DBConnection
  └─ HikariDataSource (pool 10 connection)
       │
       ├─ Repository tầng (auction, item, bid, user, autobid)
       │    field: DataSource
       │    method A: foo(args)              ← self-borrow, mượn + đóng tại chỗ
       │    method B: foo(args, Connection)  ← caller cung cấp connection
       │
       └─ Service tầng (auction, bid, admin)
            field: DataSource (KHÔNG có Connection field)
            method transactional:
              try (Connection conn = dataSource.getConnection()) {
                  conn.setAutoCommit(false);
                  try {
                      repo.foo(args, conn);   ← gọi overload có Connection
                      conn.commit();
                  } catch (...) {
                      conn.rollback();
                  } finally {
                      conn.setAutoCommit(true);
                  }
              }
```

### 2.2 Nguyên tắc thiết kế

| Nguyên tắc | Áp dụng |
|---|---|
| **Single Responsibility** | Repo = CRUD. Service = transaction + business logic. Infrastructure = pool + schema. |
| **Explicit over implicit** | Connection truyền tham số (không global). Caller biết rõ scope. |
| **Atomic at lowest layer** | Race condition giải quyết bằng SQL predicate, không bằng JVM lock. |
| **Pool sized for workload** | Pool 10 đủ cho BTL (~vài chục client đồng thời). Có thể tăng. |
| **Fail fast** | Connection leak detected qua HikariCP (chưa bật `leakDetectionThreshold`, có thể bật sau). |

---

## 3. Phase A

> **Commit:** `7526aff` (HikariCP dep) + `460c442` (migrate infrastructure)
>
> **Scope:** 7 files — `pom.xml` + `DBConnection.java` + 6 modules + `ServerContext`.

### 3.1 Mục tiêu

Đưa HikariCP vào hạ tầng. **Backward-compatible**: service và repo chưa đụng đến, vẫn dùng `Connection` như cũ. Chỉ thay nguồn gốc của `Connection` từ singleton sang pool.

### 3.2 Thay đổi cốt lõi

#### A1. `pom.xml` — thêm HikariCP

```xml
<dependency>
    <groupId>com.zaxxer</groupId>
    <artifactId>HikariCP</artifactId>
    <version>5.1.0</version>
</dependency>
```

**Why version 5.1.0:** stable, hỗ trợ Java 17, không xung đột với SLF4J có sẵn.

#### A2. `DBConnection.java` — chuyển sang DataSource

**Trước:**
```java
public static Connection getConnection() {
    if (connection == null) {
        connection = DriverManager.getConnection(URL);
    }
    return connection;  // singleton
}
```

**Sau:**
```java
public static synchronized DataSource getDataSource() {
    if (dataSource == null) {
        String pgHost = System.getenv("PGHOST");
        if (pgHost != null) {
            dataSource = buildPostgresPool();
        } else {
            dataSource = buildSQLitePool();
        }
    }
    return dataSource;
}

private static HikariDataSource buildSQLitePool() {
    SQLiteConfig sqliteConfig = new SQLiteConfig();
    sqliteConfig.setJournalMode(SQLiteConfig.JournalMode.WAL);
    sqliteConfig.setBusyTimeout(5000);
    sqliteConfig.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
    sqliteConfig.enforceForeignKeys(true);

    SQLiteDataSource sqliteDS = new SQLiteDataSource(sqliteConfig);
    sqliteDS.setUrl("jdbc:sqlite:database/auction-system.db");

    HikariConfig config = new HikariConfig();
    config.setDataSource(sqliteDS);
    config.setMaximumPoolSize(10);
    config.setConnectionTimeout(30000);
    return new HikariDataSource(config);
}
```

**Why từng config:**

| Config | Giá trị | Why |
|---|---|---|
| `JournalMode.WAL` | Write-Ahead Log | Cho phép 1 writer + N reader đồng thời (SQLite default DELETE serialize tất cả). Bắt buộc cho concurrent. |
| `busyTimeout=5000` | 5s | Khi writer thứ 2 đụng lock, đợi 5s thay vì lỗi ngay. Đủ cho tx ngắn. |
| `synchronous=NORMAL` | giảm fsync | An toàn với WAL, nhanh hơn `FULL`. |
| `enforceForeignKeys=true` | bật FK | SQLite mặc định **TẮT** FK. Phải bật thủ công, nếu không `bids.bidder_id REFERENCES users(id)` không có hiệu lực. |
| `maximumPoolSize=10` | pool 10 | Hợp lý cho BTL (~vài chục client). Tăng sau nếu cần. |
| `connectionTimeout=30000` | 30s đợi connection | Nếu pool cạn 30s mới throw. |

PostgreSQL có nhánh riêng dùng env vars (cloud deployment).

#### A3. Module init — nhận DataSource

**Trước:**
```java
public static AuctionService init(Connection connection, ...) {
    AuctionRepository repo = new AuctionRepository(connection);
    AuctionService service = new AuctionService(repo, connection);
    return service;
}
```

**Sau Phase A:**
```java
public static AuctionService init(DataSource dataSource, ...) {
    try {
        Connection connection = dataSource.getConnection();  // mượn 1 connection permanent
        AuctionRepository repo = new AuctionRepository(connection);
        AuctionService service = new AuctionService(repo, connection);
        return service;
    } catch (SQLException e) {
        throw new RuntimeException("Module init failed", e);
    }
}
```

**Why mượn permanent ở Phase A:**

- Phase A **không** đổi API repo/service. Chỉ đổi nguồn của `Connection`.
- Chấp nhận tạm thời mượn 1 connection vĩnh viễn → pool 10 còn 7 chỗ trống.
- Phase C sẽ sửa "vĩnh viễn" thành "per-call".

Bước đệm này quan trọng: nó cho phép tách Phase A ra commit riêng (chạy được, tests pass) mà không phải refactor 3 service cùng lúc.

#### A4. `ServerContext.java`

```java
this.dataSource = DBConnection.getDataSource();
```

Thay vì `DBConnection.getConnection()`. Truyền `dataSource` xuống tất cả module.

### 3.3 Kết quả sau Phase A

- HikariCP active, pool 10 connection.
- API repo/service không đổi → tests pass nguyên trạng.
- Server boot vẫn dùng 1 connection per module (5 module × 1 = 5 connection ghim) — chưa phải kiến trúc đẹp nhưng đã thoát singleton.

---

## 4. Phase B

> **Commits:** `b37bd64`, `8b26bde`, `0a1276a`, `3b07c35`, `a5c59a0`
>
> **Scope:** Repository nhận `DataSource`. Mỗi method có 2 overload. Service gọi overload có Connection trong transaction. Tests adapt.

### 4.1 Mục tiêu

Repo chuyển từ "nhận Connection" sang "nhận DataSource". Thêm overload `(args, Connection)` cho mọi method được gọi trong transaction → service có thể truyền connection xuống thay vì repo tự borrow.

### 4.2 Pattern repo dual-API

```java
public class AuctionRepository {
    private final DataSource dataSource;

    public AuctionRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    // === SELF-BORROW: cho caller ngoài transaction (UI list, single query) ===
    public Optional<Auction> findById(UUID id) {
        try (Connection conn = dataSource.getConnection()) {
            return findById(id, conn);  // delegate xuống overload
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find auction", e);
        }
    }

    // === EXPLICIT-CONNECTION: cho caller trong transaction ===
    public Optional<Auction> findById(UUID id, Connection conn) {
        String sql = "SELECT ... FROM auctions WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.toString());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapAuction(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to find auction", e);
        }
        return Optional.empty();
    }
}
```

**Why 2 overload (không phải 1):**

- **Self-borrow (`foo(args)`):** caller không trong transaction muốn 1-shot query. Repo tự mượn + đóng. Try-with-resources đảm bảo trả connection về pool ngay.
- **Explicit (`foo(args, Connection)`):** caller đang trong transaction. Phải dùng cùng connection để tx atomic. Repo **không đóng** connection — đó là việc của caller.

**Why không gộp thành 1 method:** Nếu chỉ có `foo(args, Connection conn)`, caller ngoài tx phải tự `dataSource.getConnection()` ở mọi gọi → boilerplate ở caller. Pattern dual-API gọn cho cả 2 use case.

**Why thân của self-borrow gọi xuống explicit:** DRY — SQL prepared + mapping logic ở 1 chỗ. Tránh duplicate.

### 4.3 Service dùng overload có Connection trong tx

**Trước Phase B (service vẫn ghim `Connection connection`):**
```java
synchronized (connection) {
    connection.setAutoCommit(false);
    auctionRepository.save(auction);             // ❌ repo dùng connection của chính nó (cùng cái, trùng hợp)
    auctionRepository.updateHighestBid(...);     // ❌ không guarantee cùng tx
    connection.commit();
}
```

**Sau Phase B (service vẫn ghim Connection, nhưng truyền xuống repo):**
```java
synchronized (connection) {
    connection.setAutoCommit(false);
    auctionRepository.save(auction, connection);          // ✓ explicit cùng tx
    auctionRepository.updateHighestBid(..., connection);  // ✓ cùng tx
    connection.commit();
}
```

**Why đây là cải tiến quan trọng dù chưa drop synchronized:**

- Trước: repo có field `Connection` riêng. Service tx và repo gọi không bảo đảm cùng connection → tx không atomic dù trông giống.
- Sau: connection truyền tham số. Compiler bắt buộc service nghĩ về scope tx.

### 4.4 Module wiring đơn giản hoá phần nào

**Phase B `AuthModule.init`:**

**Trước:**
```java
try {
    Connection connection = dataSource.getConnection();
    UserRepository repo = new UserRepository(connection);
    return repo;
} catch (SQLException e) {
    throw new RuntimeException(e);
}
```

**Sau Phase B:**
```java
UserRepository repo = new UserRepository(dataSource);  // không try-catch
return repo;
```

Repo tự lo connection lifecycle. Module chỉ wire.

**Why Phase B chưa xoá `Connection` field ở service:** Service vẫn cần tx control (`setAutoCommit`/`commit`/`rollback`). Đợi Phase C giải quyết tx pattern hoàn chỉnh.

### 4.5 Tests adapt

`AuctionServiceTest` etc.: vẫn dùng `@Mock Connection connection` cho service field. Repo verify thêm matcher `eq(connection)`:

```java
verify(auctionRepository).save(any(Auction.class), eq(connection));
```

Đảm bảo service truyền đúng connection xuống repo.

### 4.6 Kết quả sau Phase B

- Tất cả repo nhận `DataSource`, có dual-API.
- Service tx pattern explicit (truyền connection xuống repo).
- Vẫn còn 3 service ghim `Connection connection` permanent + 4 chỗ `synchronized(connection)` — chưa phải đích cuối nhưng kiến trúc rõ ràng hơn rất nhiều.

---

## 5. Phase C

> **Commits:** `ecb9a80`, `86dff73`, `3909365`, `7cdd03b`, `a0a04fc`, `5a4684f` + simplify `a2a571b`
>
> **Scope:** Service drop `Connection` field + `synchronized`. Per-tx borrow. Atomic SQL race protection. Schema bootstrap tập trung. Test adapt + thêm race-loss test.

### 5.1 Hai vấn đề Phase B chưa giải quyết

1. **Permanent borrow.** 3 service ghim 3 connection. Pool 10 còn 7 cho concurrent requests.
2. **JVM-level lock.** 4 khối `synchronized(connection)` tuần tự hoá toàn bộ tx của mỗi service. Pool to mấy cũng không giúp.

### 5.2 Pattern service per-tx borrow

```java
public class AuctionService {
    private final AuctionRepository auctionRepository;
    private final DataSource dataSource;  // KHÔNG còn field Connection

    public AuctionService(AuctionRepository repo, ..., DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Auction createAuction(...) {
        try (Connection conn = dataSource.getConnection()) {
            boolean oldAutoCommit = conn.getAutoCommit();
            conn.setAutoCommit(false);
            try {
                itemRepository.save(item, sellerId, conn);
                auctionRepository.save(auction, conn);
                auctionRepository.updateHighestBid(auction.getId(), startingPrice, null, conn);
                conn.commit();
                return auction;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(oldAutoCommit);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Create auction transaction failed", ex);
        }
    }
}
```

**How từng phần hoạt động:**

| Đoạn | Tác dụng |
|---|---|
| `try (Connection conn = dataSource.getConnection())` | Mượn connection từ pool. `close()` trong `finally` ngầm của try-with-resources sẽ **trả về pool**, không đóng physical. |
| `boolean oldAutoCommit = conn.getAutoCommit();` | Lưu state cũ. HikariCP mặc định `true`. |
| `conn.setAutoCommit(false);` | Bắt đầu transaction. JDBC: từ giờ mọi statement không auto-commit, chờ explicit `commit()`. |
| Repo calls với `conn` | Mọi statement gắn vào cùng tx. |
| `conn.commit();` | Persist. |
| `catch → conn.rollback();` | Hủy toàn bộ statement đã chạy trong tx. |
| `finally → conn.setAutoCommit(oldAutoCommit);` | Restore state trước khi trả về pool. |

**Why try lồng nhau:**

- **Outer (try-with-resources):** lifecycle `Connection`. Catch lỗi không lấy được connection (pool cạn, DB down).
- **Inner (try-catch-finally):** tx control. Phân biệt domain exception (rethrow nguyên) với unknown (wrap RuntimeException).

**Why restore `oldAutoCommit` thay vì hardcode `true`:**

Defensive. Nếu pool config tương lai đổi mặc định autoCommit=false (PostgreSQL with explicit tx), code vẫn không hỏng. Tốn 1 dòng, đáng giá.

### 5.3 Drop synchronized — chuyển race protection lên SQL

#### 5.3.1 `placeBid` — atomic UPDATE với predicate

**Repo:**
```java
public int updateHighestBid(UUID auctionId, BigDecimal amount, UUID bidderId, Connection conn) {
    String sql = """
        UPDATE auctions
        SET current_highest_bid = ?, highest_bidder_id = ?, updated_at = ?
        WHERE id = ? AND (current_highest_bid IS NULL OR current_highest_bid < ?)
        """;
    // ... return ps.executeUpdate();
}
```

**Service:**
```java
int updated = auctionRepository.updateHighestBid(auctionId, amount, bidderId, conn);
if (updated == 0) {
    throw new ConflictException("Bid lost race — concurrent bid placed with equal or higher amount");
}
```

**Why predicate `IS NULL OR <`:**

| Trường hợp | Predicate | Behavior |
|---|---|---|
| Auction mới (`current_highest_bid = 0`, set trong save) | `0 < startingPrice` (startingPrice luôn > 0 do validation) | match → set giá khởi điểm. |
| Auction đang chạy (có bid trước) | `oldBid < newBid` | match nếu newBid > oldBid → cập nhật. |
| Race-loss (oldBid ≥ newBid) | False | 0 rows → service rollback. |

**Race timeline:**

```
T0: auction.current_highest_bid = $100
T1 (Client A): SELECT thấy $100, validate amount=$105 OK
T2 (Client B): SELECT thấy $100, validate amount=$103 OK
T3 (A): UPDATE WHERE 100 < 105 → 1 row → commit ✓
T4 (B): UPDATE WHERE 105 < 103 → 0 rows → ConflictException → rollback
```

**Why xảy ra race ở T2:** Domain validation (`amount > currentHighest + minIncrement`) chạy in-memory với snapshot từ T1's SELECT. T2 không biết A đã thắng giữa SELECT và UPDATE.

**Why predicate bảo vệ được:** DB engine evaluate predicate atomically trong context của UPDATE — không có window nào khác có thể chen vào giữa "check" và "update".

**Why không dùng `SELECT ... FOR UPDATE`:** SQLite không hỗ trợ. Atomic UPDATE-with-predicate portable cho cả SQLite + PostgreSQL + MySQL.

**Why dùng `ConflictException` thay `InvalidBidException`:**

| Type | Semantic | Client UX |
|---|---|---|
| `InvalidBidException` | Bid sai (amount < minimum) | User phải sửa form. |
| `ConflictException` | Bid hợp lệ nhưng đến muộn. | UI có thể tự retry với highest mới. |

Doc gốc §6.2 chỉ định rõ `ConflictException`. Đã có sẵn type này, map sang `ErrorCode.CONFLICT` trong `ResponseFactory`.

#### 5.3.2 `deleteUser` — dựa FK + busy_timeout

`AdminService.deleteUser` cũ bọc `synchronized(connection)`. Phase C drop hoàn toàn.

**Lý do an toàn không cần lock JVM:**

1. **FK constraint:** `bids.bidder_id REFERENCES users(id)`, `auctions.highest_bidder_id REFERENCES users(id)`. SQLite có `enforceForeignKeys=true` từ Phase A. Bất kỳ INSERT bid mới trong khi delete đang chạy → FK violation → fail.
2. **SQLite WAL + busy_timeout=5000:** writer thứ 2 đợi tối đa 5s, không lỗi ngay.
3. **Thứ tự xoá cố định:** `bids.bidder_id` → `bids.auction_id` → `auctions` → `items` → `users`. Tất cả instance cùng order → không deadlock.

JVM-level lock không cho thêm guarantee gì DB chưa cho. Bỏ đi để tận dụng pool.

#### 5.3.3 `createAuction` / `deleteAuction` — không có race thật

Mỗi user chỉ thao tác trên auction của chính mình (validation `requireOwner`). `synchronized` cũ defensive nhưng quá rộng. Bỏ.

### 5.4 Modules drop permanent borrow

```java
// Trước Phase C
public static AuctionRepositories init(DataSource dataSource, ...) {
    try {
        Connection connection = dataSource.getConnection();    // ❌ permanent
        AuctionService service = new AuctionService(repo, connection);
        ...
    } catch (SQLException e) { ... }
}

// Sau Phase C
public static AuctionRepositories init(DataSource dataSource, ...) {
    AuctionService service = new AuctionService(repo, dataSource);  // ✓ pass DataSource
    ...
}
```

3 module: `AuctionModule`, `BidModule`, `AdminModule`. Bỏ try-catch SQLException (không còn lệnh gây ra).

**Why:** Module init giờ thuần wiring — không I/O, không lỗi. Đơn giản, dễ test, dễ đọc.

### 5.5 `DatabaseInitializer` — schema bootstrap tập trung

**Trước Phase C:** mỗi repository constructor gọi `ensureTable()`:
```java
public AuctionRepository(...) {
    ensureTable();  // gây tác dụng phụ ở constructor
}

private void ensureTable() {
    try (Connection c = dataSource.getConnection();
         Statement s = c.createStatement()) {
        s.execute("CREATE TABLE IF NOT EXISTS auctions ...");
    }
}
```

**Sau Phase C:** file mới [`infrastructure/database/DatabaseInitializer.java`](../../src/main/java/com/nhom1/auction/server/infrastructure/database/DatabaseInitializer.java):

```java
public final class DatabaseInitializer {
    private static final String[] STATEMENTS = {
        "CREATE TABLE IF NOT EXISTS users (...)",
        "CREATE TABLE IF NOT EXISTS items (...)",
        "CREATE INDEX IF NOT EXISTS idx_items_seller_id ...",
        "CREATE TABLE IF NOT EXISTS auctions (...)",
        ...
        "CREATE TABLE IF NOT EXISTS auto_bid_configs (...)"
    };

    public static void init(DataSource dataSource) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String ddl : STATEMENTS) stmt.execute(ddl);
        }
    }
}
```

`ServerContext` gọi `DatabaseInitializer.init(this.dataSource);` ngay sau khi tạo DataSource.

**Why:**

| Lý do | Giải thích |
|---|---|
| **Single Responsibility** | Repo lo CRUD. Schema thuộc infrastructure. |
| **Discoverability** | Đọc 1 file biết toàn bộ schema thay vì lục 4 repo. |
| **FK order rõ ràng** | Statements theo thứ tự dependency: users → items → auctions → bids → auto_bid_configs. Nhìn array thấy ngay. |
| **Migration path** | Đổi engine = đổi 1 file. |
| **Idempotency cost ≈ 0** | `CREATE TABLE IF NOT EXISTS` là no-op khi đã có. Chạy mỗi startup tốn vài ms — không đáng kể. |

**Why `String[]` không `String.split(";")`:**

Phase C ban đầu dùng `SCHEMA_SQL` blob + `split(";")`. /simplify pass chỉ ra footgun: dấu `;` trong CHECK constraint string literal hoặc trigger body sẽ vỡ split. Đổi sang array → không cần parse, an toàn cho mọi DDL tương lai.

---

## 6. Kiến trúc cuối

### 6.1 Startup flow

```
main()
  └─ new ServerContext()
       │
       ├─ DBConnection.getDataSource()
       │    └─ HikariDataSource init (pool 10 idle)
       │
       ├─ DatabaseInitializer.init(dataSource)
       │    └─ mượn 1 conn → execute 12 DDL → trả pool
       │
       ├─ AuthModule.init(dataSource, router)
       │    └─ new UserRepository(dataSource)        // không borrow
       │
       ├─ AuctionModule.init(dataSource, router, ns)
       │    └─ new AuctionRepository(dataSource)
       │    └─ new ItemRepository(dataSource)
       │    └─ new AuctionService(repos, dataSource)  // không borrow
       │
       ├─ BidModule.init(dataSource, router, ...)
       │    └─ new BidRepository(dataSource)
       │    └─ new BidService(repos, dataSource)
       │
       ├─ AutoBidModule.init(dataSource, router, ...)
       │    └─ new AutoBidRepository(dataSource)
       │    └─ new AutoBidService(repos, dataSource)
       │
       └─ AdminModule.init(router, ..., dataSource)
            └─ new SqlAdminAuctionGateway(dataSource)
            └─ new AdminService(repos, dataSource)

→ Pool 10 idle. KHÔNG ai ghim connection.
```

### 6.2 Request flow — placeBid happy path

```
Client → SocketHandler → MessageRouter → BidHandler
                                            │
                                            └─ BidService.placeBid(bidderId, auctionId, amount)
                                                  │
                                                  ├─ dataSource.getConnection()         [pool: 10→9]
                                                  ├─ conn.setAutoCommit(false)
                                                  │
                                                  ├─ auctionRepository.findById(auctionId, conn)
                                                  │     SELECT FROM auctions WHERE id=?
                                                  │
                                                  ├─ auction.placeBid(...)               [in-memory validate]
                                                  │
                                                  ├─ bidRepository.save(bidTx, conn)
                                                  │     INSERT INTO bids ...
                                                  │
                                                  ├─ auctionRepository.updateHighestBid(..., conn)
                                                  │     UPDATE auctions SET ... WHERE id=? AND (NULL OR <)
                                                  │
                                                  ├─ updated == 1 → conn.commit()
                                                  ├─ conn.setAutoCommit(true)
                                                  └─ conn.close()                       [pool: 9→10]
```

### 6.3 Request flow — placeBid race-loss

```
Hai client A + B đồng thời, current_highest_bid = $100:

A: getConnection() → pool: 10→9
B: getConnection() → pool: 9→8

A: setAutoCommit(false)
B: setAutoCommit(false)

A: findById → $100
B: findById → $100  (cả 2 thấy snapshot cũ)

A: auction.placeBid($105) → in-memory OK
B: auction.placeBid($103) → in-memory OK

A: bidRepository.save(bidA, conn)  ✓
B: bidRepository.save(bidB, conn)  ✓

A: updateHighestBid WHERE 100 < 105 → 1 row → commit ✓
                  auctions.current_highest_bid = $105

B: updateHighestBid WHERE 105 < 103 → 0 rows ❌
   → ConflictException thrown
   → catch → conn.rollback() (bidB INSERT undone)
   → finally → setAutoCommit(true)
   → close → pool: 9→10

A: conn.close() → pool: 8→9 (eventually 10)

Client B nhận: ErrorCode.CONFLICT "Bid lost race"
```

---

## 7. Hệ quả

### 7.1 Concurrency

| Metric | Trước refactor | Sau Phase A | Sau Phase B | Sau Phase C |
|---|---|---|---|---|
| Connection physical | 1 singleton | Pool 10 | Pool 10 | Pool 10 |
| Connection idle | 0 | 5 (5 module ghim) | 7 (3 service ghim) | 10 |
| Concurrent placeBid | 1 (synchronized) | 1 (synchronized) | 1 (synchronized) | **10** (pool size) |
| Concurrent placeBid khác auction | 1 | 1 | 1 | **10** |
| Concurrent listAuctions | 1 | nhiều (read-only) | nhiều | nhiều |
| Race protection | JVM lock | JVM lock | JVM lock | **DB atomic UPDATE** |

### 7.2 Pool sizing rationale

- BTL workload: ~20-50 client đồng thời, mỗi tx ngắn (~vài chục ms).
- Pool 10 đủ vì connection trả pool nhanh sau commit.
- HikariCP queue: nếu cạn, request thứ 11 đợi tối đa 30s (`connectionTimeout`).

Khi tăng tải production, có thể tăng `maximumPoolSize` mà không sửa code service.

### 7.3 Connection leak detection

Hiện chưa bật `leakDetectionThreshold`. Có thể bật sau:
```java
config.setLeakDetectionThreshold(10000); // log warning nếu giữ conn > 10s
```

Phase C đã đảm bảo try-with-resources khắp nơi → leak khó xảy ra, nhưng leak detection là safety net cho code mới.

---

## 8. Decision log

| # | Quyết định | Why |
|---|---|---|
| 1 | HikariCP thay vì DBCP/C3P0 | De-facto, fast, low overhead, hỗ trợ Java 17. |
| 2 | Pool size 10 | Đủ cho BTL. Có thể tăng cấu hình. |
| 3 | SQLite WAL + busy_timeout=5000 | 1 writer + N reader đồng thời. Chờ thay vì lỗi ngay. |
| 4 | `enforceForeignKeys=true` cho SQLite | Mặc định TẮT, phải bật để FK có hiệu lực. |
| 5 | 3 phase thay vì 1 PR | Mỗi phase commit-by-commit dễ review, tests pass sau mỗi phase, bisect bug dễ. |
| 6 | Repo dual-API (`foo(args)` + `foo(args, Connection)`) | Self-borrow cho 1-shot, explicit cho transaction. Caller chọn semantic. |
| 7 | Drop `synchronized(connection)` | JVM lock không cho thêm guarantee DB chưa có. Atomic SQL + FK + busy_timeout đủ. |
| 8 | Atomic UPDATE với predicate | Race protection ở DB. Portable cho cả SQLite + PostgreSQL. Không cần `SELECT FOR UPDATE`. |
| 9 | `ConflictException` cho race-loss | Khác semantic với `InvalidBidException`. UI có thể auto-retry. |
| 10 | Per-tx borrow (`try (Connection conn = dataSource.getConnection())`) | Connection chỉ giữ ~vài chục ms. Pool 10 phục vụ 100% concurrent. |
| 11 | Save + restore `oldAutoCommit` | Defensive. Future-proof nếu pool default đổi. |
| 12 | Tách `DatabaseInitializer` ra khỏi repo | Single Responsibility. Repo lo CRUD. |
| 13 | DDL trong `String[]` không `split(";")` | Tránh footgun với `;` trong string literal hoặc trigger body. |
| 14 | Xoá self-borrow wrapper `updateHighestBid(id, amt, bidder)` (no Connection) | 0 caller. YAGNI. |
| 15 | Bỏ try-catch SQLException trong module init | Module giờ thuần wiring, không I/O. |

---

## 9. Chiến lược test

### 9.1 Unit test service (Mockito)

```java
@Mock private XxxRepository xxxRepository;
@Mock private DataSource dataSource;
@Mock private Connection connection;

private XxxService service;

@BeforeEach
public void setUp() throws SQLException {
    MockitoAnnotations.openMocks(this);
    when(dataSource.getConnection()).thenReturn(connection);
    service = new XxxService(xxxRepository, dataSource);
}
```

**Why mock DataSource thay vì Connection trực tiếp:**

- Service API là `DataSource` field, không phải `Connection`. Mock phản ánh đúng API.
- Test chứng minh service `getConnection()` đúng cách — nếu service quên borrow, test fail.

**Verify pattern:**

```java
verify(connection).setAutoCommit(false);
verify(connection).commit();
verify(connection).setAutoCommit(true);
verify(xxxRepository).save(any(...), eq(connection));  // repo nhận đúng connection
```

### 9.2 Race-loss test (Phase C mới thêm)

```java
@Test
public void testPlaceBid_LostRace_RollsBackAndThrowsConflictException() throws Exception {
    when(auctionRepository.updateHighestBid(eq(auctionId), eq(amount), eq(bidderId), eq(connection)))
            .thenReturn(0);  // simulate race-loss

    ConflictException thrown = assertThrows(ConflictException.class,
            () -> bidService.placeBid(bidderId, auctionId, amount, BidType.MANUAL));

    assertTrue(thrown.getMessage().contains("Bid lost race"));
    verify(connection).rollback();
    verify(connection, never()).commit();
}
```

**Why test này quan trọng:** race-loss path là failure mode mới sinh ra do Phase C. Không có test = regression vào predicate logic không bị bắt.

### 9.3 Manual concurrent test (deferred — user tự chạy)

1. Start server.
2. 2 JavaFX client cùng PLACE_BID liên tục một auction.
3. Quan sát:
   - `auctions.current_highest_bid` tăng đơn điệu.
   - Một số PLACE_BID từ loser nhận `ConflictException("Bid lost race")`.
   - Không deadlock, không cạn pool.
4. Client thứ 3 LIST_AUCTIONS song song → response time không bị block.

---

## 10. Files affected

### Phase A

| File | Loại |
|---|---|
| `pom.xml` | + HikariCP dep |
| `infrastructure/database/DBConnection.java` | refactor sang HikariDataSource |
| 5 module files (Admin/Auction/Auth/AutoBid/Bid) | nhận DataSource thay Connection |
| `infrastructure/ServerContext.java` | gọi `getDataSource()` |

### Phase B

| File | Loại |
|---|---|
| 5 repository files | nhận DataSource, dual-API (`foo(args)` + `foo(args, Connection)`) |
| 3 service files (Auction/Bid/Admin) + `SqlAdminAuctionGateway` | dùng overload có Connection trong tx |
| 5 module files | wire DataSource (bỏ try-catch ở 2 module) |
| 3 test files | adapt API + matcher `eq(connection)` |

### Phase C

| File | Loại |
|---|---|
| `infrastructure/database/DatabaseInitializer.java` | NEW — schema bootstrap tập trung |
| `infrastructure/ServerContext.java` | + `DatabaseInitializer.init(dataSource)` |
| 4 repository files | drop `ensureTable()` |
| `auction/AuctionRepository.java` | atomic predicate + `int` return cho `updateHighestBid` |
| 3 service files (Auction/Bid/Admin) | drop Connection field, drop synchronized, per-tx borrow |
| 3 module files (Auction/Bid/Admin) | drop permanent borrow |
| 3 test files | mock DataSource, thêm race-loss test |

**Tổng:** ~30 file Java + 1 file pom.xml + tests.

---

## 11. FAQ

### Q: Tại sao không gộp 3 phase thành 1 PR cho gọn?

A: 1 PR khổng lồ = không ai dám review. 3 phase commit-by-commit:
- Mỗi phase tests pass riêng.
- Phase A backward-compat → merge sớm, không block.
- Bisect dễ nếu sau này có bug.

### Q: Tại sao service không dùng `@Transactional` như Spring?

A: BTL không dùng Spring framework. Tự viết tx pattern thủ công. `@Transactional` về cốt lõi cũng là try-with-resources + commit/rollback — chỉ khác là Spring AOP wrap quanh method.

### Q: Có thể bị connection leak không?

A: Try-with-resources `Connection` đảm bảo `close()` chạy mọi path. `close()` trên HikariCP connection = trả về pool, không physical close. Leak xảy ra nếu code không dùng try-with-resources hoặc quên close — Phase C đã enforce pattern này khắp service.

Nếu vẫn muốn safety net: bật `config.setLeakDetectionThreshold(10000)`.

### Q: Race condition còn chỗ nào không?

A:
- **`placeBid`:** atomic UPDATE predicate — bảo vệ.
- **`deleteUser`:** FK + busy_timeout — bảo vệ.
- **`createAuction`:** user chỉ tạo cho chính mình — không race thật.
- **`deleteAuction`:** owner check + FK — không race thật.

Có thể còn race trong AutoBid (chưa được audit kỹ ở Phase C — phạm vi out of scope). Đáng xem xét ở phase D nếu cần.

### Q: Khi nào nên dùng overload `foo(args)` (self-borrow) vs `foo(args, Connection)`?

A:
- **Self-borrow `foo(args)`:** 1-shot query, không trong transaction (ví dụ: list view, single read).
- **Explicit `foo(args, Connection)`:** trong transaction (đa-statement, cần atomic). Caller phải đã `setAutoCommit(false)` và sẽ `commit()`/`rollback()`.

Nguyên tắc: nếu method bạn viết đang trong `try (Connection conn = ...) { conn.setAutoCommit(false); ... }`, dùng overload có Connection.

### Q: Test mock DataSource có đại diện đúng thực tế?

A: Đại diện đúng **service logic** (transaction lifecycle, exception path). Không đại diện DB behavior (SQL syntax, FK violation, race condition thật) — đó là việc của integration test.

Manual concurrent test (đoạn 9.3) cover phần DB.

### Q: Phase D có gì?

A: Out of scope hiện tại. Ý tưởng tương lai:
- `TransactionTemplate` extract: dedup pattern `try-with-resources Connection + setAutoCommit/commit/rollback` ở 3 service.
- Bulk delete trong `AdminService.deleteUser` (N+1 → 3 query).
- `BidService.placeBid` đẩy validation lên DB predicate (giảm 1 SELECT).
- AutoBid race audit.
- `leakDetectionThreshold` bật trong production.

---

## 12. Flow phụ — AutoBid, deleteUser, AuctionScheduler

Phần này bổ sung 3 flow không phải đường đi chính của Phase C nhưng quan trọng cho người maintain.

### 12.1 AutoBid trigger sau placeBid

**Vai trò:** Khi user A đặt bid manual, các user khác đã setup AutoBidConfig (max + increment) cần được trigger để tự động đặt bid phản công.

**Components:**

- `AutoBidService.executor` — `Executors.newSingleThreadExecutor` (daemon, tên `"auto-bid-worker"`).
- `BidGateway.placeAutoBid(...)` — wrapper gọi `BidService.placeBid(..., BidType.AUTO)`. Đi qua đầy đủ tx + atomic predicate.
- `MAX_TRIGGER_DEPTH = 20` — bound số bid trong 1 chain.

#### Flow đầy đủ

```
T0: Client A → BidHandler → BidService.placeBid(A, auctionId, $105, MANUAL)
                              │
                              ├─ getConnection() [pool: 10→9]
                              ├─ tx: save bid + updateHighestBid → commit ✓
                              └─ close [pool: 9→10]
    BidHandler → autoBidService.scheduleAutoBids(auctionId, $105, A)
                              │
                              └─ executor.submit(() -> runAutoBids(...))   ← async, return ngay

T1: Client A nhận response NGAY (không đợi auto-bid).

T2 (background, auto-bid-worker thread): runAutoBids loop
    depth=0:
      autoBidRepository.findByAuctionId(auctionId)   ← self-borrow [pool: 10→9→10]
      filter: bidderId != A, maxAmount > $105
      → eligible = [B (max=$200, inc=$5), C (max=$150, inc=$10)]
      max(nextAmount) → C: nextAmt = $105 + $10 = $115
      bidGateway.placeAutoBid(C, auctionId, $115)
        → BidService.placeBid(C, auctionId, $115, AUTO)
          → getConnection() [pool: 10→9]
          → tx: save bid + updateHighestBid WHERE 105 < 115 → 1 row → commit ✓
          → close [pool: 9→10]
      currentHighestBid = $115, currentHighestBidderId = C

    depth=1:
      findByAuctionId → eligible = [B (max=$200, inc=$5)]  (C giờ là highest, loại)
      B: nextAmt = $115 + $5 = $120
      placeAutoBid(B, $120) → commit
      currentHighestBid = $120, bidder = B

    depth=2:
      eligible = [C (max=$150, inc=$10)]  (B là highest, loại)
      C: nextAmt = $120 + $10 = $130
      placeAutoBid(C, $130) → commit
      currentHighestBid = $130, bidder = C

    ...lặp đến khi:
      - eligible empty (mọi người đã exceed max), HOẶC
      - depth == 20, HOẶC
      - nextAmt > selected.maxAmount

T_END: notificationService.broadcastBidUpdate(auctionId, finalBid, finalBidder)
       ← chỉ 1 broadcast cho cả chain, không spam UI.
```

#### How — cơ chế quan trọng

| Cơ chế | Tác dụng |
|---|---|
| `newSingleThreadExecutor` | Mọi auto-bid chain xếp hàng tuần tự. Không 2 chain chạy song song trên cùng auction → loại trừ race giữa 2 trigger. |
| `executor.submit()` async | `BidHandler` return cho client A ngay sau bid manual commit. Client UX không đợi 20 bid auto. |
| Daemon thread | Server shutdown không bị thread treo. |
| `MAX_TRIGGER_DEPTH = 20` | Bound finite. Ngăn infinite chain nếu config logic có bug. |
| Mỗi `placeAutoBid` = 1 tx riêng | Mỗi bid trong chain commit độc lập. Chain bị fail giữa chừng không rollback bid đã thắng. |
| `findByAuctionId` self-borrow | Read-only, ngoài tx. Connection trả pool ngay sau query. |
| Broadcast 1 lần cuối chain | Tránh flood NotificationService với 20 update liên tiếp. |

#### Why — quyết định thiết kế

| Quyết định | Why |
|---|---|
| Single-thread executor (không thread pool) | Auction là contested resource. 2 auto-bid chain song song trên cùng auction = race nội bộ AutoBid (đã có race protection ở BidService nhưng tăng noise). Đơn luồng = đơn giản. |
| Async submit thay vì inline | Manual bid path không chậm vì auto chain dài. |
| Đi qua `BidService.placeBid` thay vì gọi repo trực tiếp | Reuse atomic predicate + validation. Auto-bid cũng phải tuân race protection. |
| Catch `Exception` rồi `break` | 1 auto-bid fail (vd: race-loss vì manual bid kế tiếp đến) không kill cả chain. Chain dừng ở chỗ đó, broadcast state đã đạt. |

#### Tương tác với pool

- Chain 20 bid = 20 lần borrow/release connection. Mỗi tx ~vài chục ms. Tổng chain ~vài trăm ms.
- Auto-bid-worker chiếm tối đa 1 connection tại 1 thời điểm (single-thread).
- Pool 10 còn 9 cho concurrent requests khác.

#### Race scenario chưa audit

User B manual bid $200 xen vào giữa chain auto-bid:

```
auto-bid C tries $115 → predicate 105 < 115 OK → commit ✓
[user B manual bid $200] → predicate 115 < 200 OK → commit ✓
auto-bid B tries $120 → predicate 200 < 120 SAI → ConflictException → break chain
```

Đây không phải bug — đúng semantic: chain dừng khi state đổi quá lớn. Nhưng audit sâu hơn cần kiểm tra: `findByAuctionId` ở đầu loop dùng snapshot cũ, nên nếu user B set AutoBidConfig giữa chain thì depth tiếp theo mới thấy. Acceptable cho BTL.

---

### 12.2 `deleteUser` step-by-step

**Mục tiêu:** Admin xoá user khỏi hệ thống. Phải dọn sạch mọi data liên quan để không vi phạm FK constraint.

#### FK dependency graph

```
users (root)
  ├─ items.seller_id → users.id           (user is seller)
  │    └─ auctions.item_id → items.id
  │         ├─ auctions.highest_bidder_id → users.id  (any user)
  │         └─ bids.auction_id → auctions.id
  │              └─ bids.bidder_id → users.id (any user)
```

Xoá user trực tiếp = FK violation ở nhiều bảng. Phải xoá theo thứ tự **dependent trước, depended sau**.

#### Flow đầy đủ

```
AdminService.deleteUser(adminId, targetUserId)
  │
  ├─ requireAdmin(adminId)               ← validate caller
  ├─ findById(targetUserId) → target
  ├─ check target.role != ADMIN          ← không cho xoá admin khác
  │
  └─ try (Connection conn = dataSource.getConnection()) {     [pool: 10→9]
       conn.setAutoCommit(false)
       │
       ├─ STEP 1: auctionRepository.clearHighestBidderByUserId(targetId, conn)
       │     UPDATE auctions SET highest_bidder_id = NULL
       │     WHERE highest_bidder_id = ?
       │     → bỏ FK reference từ auctions sang user (user là bidder cao nhất)
       │
       ├─ STEP 2: bidRepository.deleteByBidderId(targetId, conn)
       │     DELETE FROM bids WHERE bidder_id = ?
       │     → xoá tất cả bid của user (user là bidder ở các auction khác)
       │
       ├─ STEP 3: findBySellerId(targetId, conn) → List<Auction>
       │     SELECT FROM auctions JOIN items WHERE seller_id = ?
       │     → tìm các auction mà user này là seller
       │
       ├─ STEP 4: cho mỗi auction:
       │     │
       │     ├─ bidRepository.deleteByAuctionId(auctionId, conn)
       │     │   DELETE FROM bids WHERE auction_id = ?
       │     │   → xoá bid của user khác đặt lên auction của target
       │     │
       │     ├─ auctionRepository.deleteById(auctionId, conn) → int rows
       │     │   DELETE FROM auctions WHERE id = ?
       │     │   → xoá auction
       │     │
       │     └─ itemRepository.deleteById(itemId, conn) → int rows
       │         DELETE FROM items WHERE id = ?
       │         → xoá item của auction đó
       │
       │     if rows == 0 cho auction hoặc item → IllegalStateException → rollback
       │
       ├─ STEP 5: userRepository.deleteById(targetId, conn) → boolean
       │     DELETE FROM users WHERE id = ?
       │
       ├─ conn.commit()                  ← atomic toàn bộ
       │
       └─ conn.close()                   [pool: 9→10]
     }
```

#### How — phân tích từng step

| Step | SQL | Lý do thứ tự |
|---|---|---|
| 1 | `UPDATE auctions SET highest_bidder_id = NULL` | Phải null trước khi delete user, không thì FK `auctions.highest_bidder_id → users.id` violate khi step 5 chạy. |
| 2 | `DELETE bids WHERE bidder_id` | Phải trước step 5, không thì FK `bids.bidder_id → users.id` violate. |
| 3 | `SELECT auctions WHERE seller_id` | Tìm scope cần xoá. Không thay đổi state. |
| 4a | `DELETE bids WHERE auction_id` | Xoá bid của các user khác trên auction của target. Phải trước 4b vì `bids.auction_id → auctions.id`. |
| 4b | `DELETE auctions WHERE id` | Phải trước 4c vì `auctions.item_id → items.id`. |
| 4c | `DELETE items WHERE id` | Item-of-auction-of-target. Phải trước step 5 vì `items.seller_id → users.id`. |
| 5 | `DELETE users WHERE id` | Cuối cùng, mọi FK reference đã sạch. |

#### Why — quyết định thiết kế

| Quyết định | Why |
|---|---|
| Toàn bộ trong 1 transaction | Nếu step 4 fail giữa chừng (vd: 1 trong 10 auction xoá lỗi), rollback toàn bộ. User vẫn tồn tại với data nguyên vẹn. Không có state nửa vời. |
| `clearHighestBidderByUserId` thay vì NULL ở `setHighestBidder` mỗi auction | 1 UPDATE quét tất cả auction. Nếu user là highest bidder của 50 auction, 1 query thay 50 query. |
| Loop step 4 (N+1) | **Trade-off chấp nhận:** mã đơn giản, hiếm khi user có nhiều auction. Có thể optimize sau bằng `deleteBySellerId` bulk method (gợi ý Phase D). |
| Check `deletedAuctions == 0 || deletedItems == 0` | Defense in depth. Nếu auction đã bị xoá bởi concurrent admin → rowcount 0 → throw → rollback toàn tx. Tránh phantom delete. |
| Drop `synchronized(connection)` | FK + busy_timeout của DB serialize đủ. Concurrent placeBid trên auction của user đang bị xoá sẽ fail ở FK INSERT, không cần JVM lock. |

#### Tương tác với pool

- 1 tx chiếm 1 connection trong toàn bộ thời gian. Với 10 auction × 3 query + 4 step khác = ~34 round-trip → vài trăm ms.
- Pool còn 9 cho request khác trong khi tx chạy.

#### Race với placeBid

```
T0: Admin deleteUser(B) start tx
T1: Client B placeBid(auction X) start tx
T2: Admin step 2: DELETE bids WHERE bidder_id = B → 0 rows (B chưa có bid)
T3: Client B INSERT bids (bidder_id = B) commit ✓
T4: Admin step 5: DELETE users WHERE id = B
    → FK violation (bids.bidder_id = B vẫn tồn tại)
    → IllegalStateException → rollback admin tx
    → user B không bị xoá
```

Đây là behavior **đúng**: race resolved bằng FK, admin retry sau.

Reverse case: admin commit trước, B placeBid sau:

```
T0: Admin deleteUser(B) commit ✓ (user B đã bị xoá)
T1: Client B placeBid → BidService.placeBid INSERT bids (bidder_id = B)
    → FK violation (users.id = B không còn)
    → SQLException → wrap RuntimeException → client nhận error
```

Cũng đúng: bidder đã bị xoá, không cho phép bid mới.

---

### 12.3 AuctionScheduler — background thread interaction với pool

**Vai trò:** Background thread chạy mỗi giây để chuyển trạng thái auction theo thời gian thực:
- `OPEN → RUNNING` khi đến `startTime`.
- `RUNNING → FINISHED` khi đến `endTime` (hoặc extend nếu anti-sniping).

#### Components

- `ScheduledExecutorService` (single-thread): `scheduleAtFixedRate(this::safeTick, 1, 1, SECONDS)`.
- `AuctionGateway` (`AuctionGatewayImpl`): wrapper repo `findAll`, `updateStatus`, `updateEndTime`.
- `BidGateway.findLastBidTime`: query để quyết định anti-sniping.

#### Flow 1 tick

```
T0 (mỗi 1s): safeTick()
  └─ tick()
      ├─ now = LocalDateTime.now()
      ├─ auctions = auctionGateway.findAll()         ← self-borrow [pool: 10→9→10]
      │
      └─ for mỗi auction:
           │
           ├─ if status==OPEN && startTime <= now:
           │     auctionGateway.updateStatus(id, RUNNING)
           │       └─ UPDATE auctions SET status='RUNNING' WHERE id=?
           │          [pool: 10→9→10]
           │     continue
           │
           └─ if status==RUNNING && endTime <= now:
                 handleRunningAuctionTimeout(auction, now)
                   │
                   ├─ lastBidTime = bidGateway.findLastBidTime(id)
                   │   SELECT MAX(created_at) FROM bids WHERE auction_id=?
                   │   [pool: 10→9→10]
                   │
                   ├─ if shouldExtend(...):
                   │     // last bid trong 15s cuối → extend 30s
                   │     newEnd = endTime + 30s
                   │     auctionGateway.updateEndTime(id, newEnd)
                   │       └─ UPDATE auctions SET end_time=? WHERE id=?
                   │       [pool: 10→9→10]
                   │
                   └─ else:
                       auctionGateway.updateStatus(id, FINISHED)
                       notificationService.broadcastAuctionEnded(...)
```

#### How — pool footprint

- Mỗi tick: 1 SELECT findAll + N update (cho mỗi auction đến hạn) + cho mỗi RUNNING auction kết thúc: 1 SELECT findLastBidTime + 1 UPDATE.
- Mỗi query là 1 lần borrow/release. Không có long-running transaction.
- Worst case 1 tick: 10 auction OPEN→RUNNING (10 UPDATE), 5 RUNNING→FINISHED (5 SELECT + 5 UPDATE) = ~21 round-trip × ~ms each ~20ms tổng. < 1s tick interval → không bao giờ overlap.

#### Why — quyết định thiết kế

| Quyết định | Why |
|---|---|
| `ScheduledExecutorService` single-thread | Tick không cần parallel. Tránh 2 tick chồng nhau nếu 1 tick chậm. |
| `findAll()`/`updateStatus()`/`updateEndTime()` **self-borrow** (overload không Connection) | Mỗi update là 1 statement độc lập, không cần tx. Tự borrow + đóng đơn giản hơn nhiều so với mở tx. |
| `safeTick` catch + log dedupe (`lastErrorMessage`) | Nếu DB down 60s, log 1 lần thay vì 60 lần. Tránh spam console. Reset khi tick lại OK. |
| Không transaction quanh tick | Mỗi auction update độc lập. Nếu auction A update fail, auction B vẫn tiến hành ở tick tiếp theo. Resilient. |

#### Race với placeBid

```
Scenario A: scheduler đang đóng auction, client cùng lúc bid
T0: client bid → BidService.placeBid getConnection, tx start, findById thấy RUNNING ✓
T1: scheduler tick: updateStatus(id, FINISHED) commit
T2: client tx tiếp tục: updateHighestBid WHERE 100 < 105 → 1 row → commit ✓
    → auction giờ FINISHED nhưng có bid sau khi đã đóng.
```

**Đây là race nhỏ chưa được fix.** Predicate `updateHighestBid` không check status. Acceptable cho BTL (window race ~vài ms, không chí mạng). Phase D có thể thêm `AND status = 'RUNNING'` vào predicate.

```
Scenario B: scheduler extend, client bid:
T0: client bid commit ✓ (highest_bidder_id = A, bid time = now)
T1: scheduler tick: findLastBidTime = T0, in 15s window → updateEndTime +30s
T2: ✓ anti-sniping hoạt động đúng
```

Anti-sniping race-safe vì dựa trên `findLastBidTime` snapshot tại tick — bid trước tick sẽ được thấy, bid sau tick chờ tick kế tiếp.

#### Tương tác với pool

- Tick mỗi giây = ~3600 borrow/release mỗi giờ. Đầu vào pool stable, không leak.
- Scheduler thread KHÔNG ghim connection — chỉ borrow per-query.
- Worst case: nếu pool cạn (10 client đang tx) → tick chờ tối đa 30s (connectionTimeout) → `safeTick` log dedupe → retry next tick. Server không crash.

---

## 13. Tham chiếu

- [`ke-hoach-di-chuyen-connection-pool.md`](ke-hoach-di-chuyen-connection-pool.md) — kế hoạch tổng thể.
- [`phase-c-explicit-connection-passing.md`](phase-c-explicit-connection-passing.md) — chi tiết Phase C.
- [`database-schema.md`](database-schema.md) — schema reference (sync với `DatabaseInitializer.STATEMENTS`).
- [`exception-handling.md`](exception-handling.md) — phân loại exception, mapping ErrorCode.
- [HikariCP docs](https://github.com/brettwooldridge/HikariCP) — config reference.
- [SQLite WAL](https://www.sqlite.org/wal.html) — journal mode.
