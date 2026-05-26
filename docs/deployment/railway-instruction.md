# Huong Dan Trien Khai Auction Server Len Railway

Tai lieu nay bam theo code hien tai trong `DBConnection`, `Server`, `ServerConnection`, va `pom.xml`.

## Moi truong database

`DBConnection.getDataSource()` chon database theo bien moi truong:

- Neu co `PGHOST`, server tao PostgreSQL Hikari pool bang `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`.
- Neu khong co `PGHOST`, server tao SQLite Hikari pool tai `jdbc:sqlite:database/auction-system.db`.

`DatabaseInitializer.init(dataSource)` chay luc server startup de tao/migrate schema. File schema canonical nam o `docs/architecture/database-schema.md`.

## Port server

`Server.main` doc bien `PORT`. Neu Railway set `PORT`, server bind vao port do. Neu khong co `PORT`, code hien tai fallback ve `41177`.

Luu y: `ServerConnection` phia client hien thu cloud truoc:

```java
String cloudHost = "zephyr.proxy.rlwy.net";
int cloudPort = 30411;
```

Neu cloud fail, client fallback ve:

```java
String localHost = "localhost";
int localPort = 12345;
```

Khi test local, can dam bao server local dang lang nghe dung port ma client fallback su dung. Hien tai server fallback `41177`, client fallback `12345`, nen hai gia tri nay can duoc dong bo neu chay local khong qua Railway.

## Build va start command

`pom.xml` dung `maven-shade-plugin` de tao fat JAR co main class `com.nhom1.auction.server.Server`.

Build:

```bash
./mvnw package
```

Run:

```bash
java -jar target/auction-app-1.0-SNAPSHOT.jar
```

Repo co production profile rong de dap ung build command co `-Pproduction` neu Railway dung mac dinh do.

## Railway dashboard

1. Tao service cho app tu GitHub repo.
2. Them PostgreSQL service neu muon dung cloud database.
3. Dam bao app service nhan du cac bien `PG...` tu PostgreSQL service.
4. Tao TCP Proxy trong Settings -> Networking de JavaFX client co the ket noi socket TCP.
5. Cap nhat `cloudHost` va `cloudPort` trong `ServerConnection` theo proxy Railway hien tai.

## Luu y tuong thich SQL

- Dung `setTimestamp` / `getTimestamp` cho cot thoi gian.
- Tranh syntax chi rieng SQLite nhu `INSERT OR REPLACE`.
- `DatabaseInitializer` da co xu ly rieng cho SQLite voi `ALTER COLUMN` va `ADD COLUMN IF NOT EXISTS`.
- `ON CONFLICT (auction_id, bidder_id) DO UPDATE` dang duoc dung trong auto-bid upsert; can test lai neu doi backend hoac version driver.
