# Shared Infrastructure Guideline

This document defines the minimum shared infrastructure the team should complete before splitting into mostly independent feature work.

The goal is not to finalize every design decision in the whole system.
The goal is to finish the parts that many features depend on, so people stop blocking each other.

If the team completes the items in this document, it should be safe to assign members to separate work areas with much lower risk of misalignment.

## Why this phase matters

Right now the project already has:

- JavaFX UI screens and controllers
- core domain classes such as `Auction`
- a SQLite database
- an early socket prototype

This is good progress, but the boundaries between client, server, business logic, and data access are not settled yet.

If the team starts splitting feature work too early, common problems will appear:

- one person changes the socket format and breaks someone else's client code
- one person writes SQL directly in controllers or handlers
- multiple people implement the same business rule in different places
- the database schema changes without everyone noticing
- UI work gets blocked by unfinished backend interfaces

The purpose of this shared infrastructure phase is to prevent those problems.

## Main principle

The team should first agree on shared contracts and shared boundaries.

After that, each member can own a work area with much more freedom.

In other words:

- shared infrastructure first
- feature verticals second

## What counts as shared infrastructure

The following pieces should be discussed and settled early because many parts of the system depend on them.

### 1. Package structure

The codebase should have a package structure that clearly separates responsibilities.

A suggested direction:

```text
com.nhom1.auction
  client
    controller
    connection
    service
  server
    transport
    router
    handler
    service
    repository
  common
    dto
    entity
    enums
    exception
```

This is only a suggested structure, not a strict rule.
The important point is that everyone understands where new code should go.

### 2. Client-server boundary

The server should own:

- database access
- business logic that changes system state
- concurrency-sensitive operations

The client should own:

- JavaFX screens
- controller logic for user interaction
- sending requests to the server
- rendering server responses

The client should not access the database directly.

## Suggested server flow

One good direction for this project is:

```text
Client UI
-> ServerConnection
-> ClientHandler
-> MessageRouter
-> Feature Handler
-> Service
-> Repository
-> Database
```

### Layer responsibilities

#### `ClientHandler`

- reads messages from the socket
- writes responses back to the client
- should not contain feature-specific business logic

#### `MessageRouter`

- checks message type
- chooses the correct handler
- example: `LOGIN`, `REGISTER`, `PLACE_BID`, `CREATE_AUCTION`

#### `Feature Handler`

- receives a parsed request
- calls the appropriate service
- converts service results into response DTOs

#### `Service`

- contains business rules
- examples:
  - validate login flow
  - enforce auction state transitions
  - validate bids
  - decide winner when auction ends
  - apply anti-sniping if implemented

#### `Repository`

- contains SQL or JDBC access code
- acts like a DAO layer
- examples:
  - find user by username
  - save auction
  - list auctions
  - insert bid history row

#### `Database`

- SQLite for now
- schema should be designed so migration to PostgreSQL later is manageable

## Shared terminology

### DTO

DTO means Data Transfer Object.

A DTO is a simple object used to transfer data between parts of the system, especially between client and server.
It should not contain business logic.

Examples:

- `LoginRequestDTO`
- `LoginResponseDTO`
- `PlaceBidRequestDTO`
- `AuctionSummaryDTO`

### Repository

Repository is the data access layer.

Its job is to talk to the database and run SQL queries.
In many discussions, this is very close to the DAO concept.

### Service

Service is the business logic layer.

Its job is to enforce system rules and coordinate repositories or domain logic.

## Shared contracts the team should settle first

The team does not need to define every future detail immediately.
The team should define the common contracts that unblock parallel development.

### 1. Message envelope format

The team should agree on one common request format and one common response format.

For example:

```json
{
  "type": "LOGIN",
  "requestId": "uuid",
  "payload": {
    "username": "alice",
    "password": "123456"
  }
}
```

```json
{
  "requestId": "uuid",
  "success": true,
  "payload": {
    "role": "ADMIN"
  },
  "error": null
}
```

The exact fields can change.
The important point is that all messages follow one shared structure.

### 2. Core message types

The team should agree on the first set of messages needed for the basic end-to-end system.

Suggested first set:

- `LOGIN`
- `REGISTER`
- `LIST_AUCTIONS`
- `GET_AUCTION_DETAIL`
- `PLACE_BID`
- `CREATE_AUCTION`
- `CANCEL_AUCTION`

This list is enough to unblock the first real system integration.

### 3. Payload ownership rule

The whole team should agree on:

- the message envelope
- naming conventions
- error response shape
- the initial core message types

After that, the owner of each feature area can define the detailed payloads for their messages, but those details should still be reviewed with the team before becoming final.

This keeps consistency without slowing everyone down too much.

### 4. Database schema

The team should settle a first version of the schema early.

At minimum, define the purpose and core fields of:

- `users`
- `items`
- `auctions`
- `bids`

Useful questions to answer:

- Which table owns which data?
- What are the primary keys?
- What are the foreign keys?
- Which fields are required?
- Which fields represent auction state?
- Which fields are needed later for PostgreSQL migration?

The team does not need perfect normalization on day one, but it should avoid letting each feature invent its own storage model independently.

### 5. Business rule ownership

Some rules should be written down in one place so the team does not implement them differently in different modules.

Examples:

- valid auction states and transitions
- who can bid
- who can cancel an auction
- what makes a bid valid
- how concurrent bids are resolved
- what happens when an auction reaches its end time

## What to avoid during this phase

- Do not let client controllers access the database directly.
- Do not write important business rules inside JavaFX controllers.
- Do not put SQL directly in socket handlers if the logic may be reused elsewhere.
- Do not allow each feature owner to invent a different message format.
- Do not over-design every optional feature before the basic path works.

## Suggested milestone for the end of this phase

Before splitting into mostly independent work, the team should aim to make one thin end-to-end flow work:

```text
register or login
-> list auctions
-> open auction detail
-> place bid
-> receive server response
```

This flow does not need to be polished.
It only needs to prove that:

- the client can talk to the server
- the server can route messages
- services can enforce rules
- repositories can save and load data
- the team's contracts are usable in practice

Once this flow works, feature work can split much more safely.

## How to divide work after infrastructure is ready

For a 4-member team, a practical split is:

### Member 1: transport and protocol

- socket communication basics
- message envelope
- message router
- common request and response conventions

### Member 2: auth and user data

- login and register flow
- `AuthService`
- user repository
- role handling

### Member 3: auction and bidding logic

- auction state logic
- bid validation
- concurrency handling
- auction and bid repositories

### Member 4: client integration and UI wiring

- client-side service or gateway
- replace direct DB usage from client controllers
- connect UI screens to real server responses

This split is not permanent forever.
After the shared contracts settle, the team can rebalance based on progress and difficulty.

## Team workflow guidance

### Use weekly milestones, not weekly ownership

The lecturer's weekly plan is helpful for pacing.
It should be treated as a milestone plan, not as the exact way to assign people.

For example:

- by the end of the week, the protocol should be frozen
- by the end of the week, auth should work end-to-end
- by the end of the week, one bid flow should work

But ownership during that week should still be based on modules and responsibilities.

### Discuss interface changes early

If a member wants to change:

- message format
- DB schema
- package structure
- service method signatures used by others

that change should be discussed before implementation, not after.

### Prefer thin interfaces between team members

Examples:

- UI teammate depends on DTOs and client service interfaces, not raw server internals
- service teammate depends on repository interfaces, not direct controller logic
- transport teammate depends on message types, not auction business rules

The thinner the interface, the easier it is to work independently.

## Decision checklist for discussion meetings

When the team meets to discuss the shared foundation, the goal is not to debate everything.
The goal is to leave the meeting with answers to these questions:

- What is the agreed request and response message envelope?
- Which message types are needed first?
- Which package or folder owns each kind of code?
- Which responsibilities belong to handlers, services, and repositories?
- What is the initial DB schema?
- Which business rules are already fixed and written down?
- What is the first end-to-end flow we will make work?
- Who owns each part for the next iteration?

If those questions are answered, the team is likely ready to begin independent feature work.

