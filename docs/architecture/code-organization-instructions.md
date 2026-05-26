# Code Organization Instructions

## Purpose

Use this guide when reorganizing existing Java files for readability and defense
preparation. The goal is to make files easier to scan without changing business
logic, APIs, behavior, database queries, or UI behavior.

This work should be treated as a cleanup pass only.

## General Rules

- Do not change logic.
- Do not rename classes, methods, fields, DTO properties, FXML IDs, CSS classes,
  message types, SQL table names, or database columns.
- Do not rewrite streams, lambdas, conditions, loops, or exception handling just
  for style.
- Do not run a global formatter unless the task explicitly asks for it.
- Avoid touching files that are already easy to scan.
- Keep each cleanup focused on one module or one file group at a time.
- Prefer small, obvious movement of existing code over rewriting code.

## Standard Order for Service Classes

Use this order for classes such as `AuthService`, `AuctionService`,
`BidService`, `AutoBidService`, `PaymentService`, `WalletService`, and
`AdminService`.

```text
1. Constants
2. Dependencies / fields
3. Constructor
4. Public business methods
5. Public query/read methods
6. Private validation methods
7. Private mapping/conversion methods
8. Private utility/helper methods
```

Notes:

- Keep related public methods together.
- Keep validation helpers near the business methods that use them.
- Keep DTO mapping methods near the end of the file.
- Do not split a short readable method into smaller methods unless the existing
  method is clearly too hard to follow.

## Standard Order for Repository Classes

Use this order for classes such as `UserRepository`, `AuctionRepository`,
`BidRepository`, `AutoBidRepository`, `PaymentRepository`, and
`WalletRepository`.

```text
1. SQL constants, if any
2. Dependencies / fields
3. Constructor
4. Save / insert / upsert methods
5. Find / list / query methods
6. Update methods
7. Delete methods
8. Row mapping methods
9. Small SQL/date/helper methods
```

Notes:

- Keep overloads next to each other, for example `findById(id)` and
  `findById(id, connection)`.
- Keep transaction-aware repository methods close to their self-borrowing
  versions.
- Do not change SQL behavior while reorganizing.

## Standard Order for Handler Classes

Use this order for classes such as `AuthHandler`, `AuctionHandler`,
`BidHandler`, `AutoBidHandler`, `PaymentHandler`, `WalletHandler`, and
`AdminHandler`.

```text
1. Dependencies / fields
2. Constructor
3. register(...) method
4. Private request handler methods
5. Private payload parsing methods
6. Private response/helper methods
```

Notes:

- `register(...)` should remain easy to find near the top.
- Each `MessageType` route should call a named handler method when possible.
- Avoid putting large business logic inside route lambdas.

## Standard Order for JavaFX Controllers

Use this order for JavaFX controller classes.

```text
1. FXML fields
2. Services and dependencies
3. State fields
4. initialize(...)
5. Public setup/context methods
6. FXML event handlers
7. Data loading methods
8. Render/update UI methods
9. Validation methods
10. Navigation methods
11. Small formatting/helper methods
```

Notes:

- Keep `@FXML` fields grouped together.
- Keep `@FXML` event handlers easy to locate.
- Do not move code in a way that breaks FXML injection or controller lifecycle.
- Do not update UI from background callbacks unless the existing code already
  uses `Platform.runLater(...)`.

## Standard Order for Entity and DTO Classes

Use this order for entities and DTOs.

```text
1. Constants, if any
2. Fields
3. Constructors
4. Domain methods, if any
5. Getters
6. Setters
7. toString / equals / hashCode, if any
```

Notes:

- Do not change field names or constructor signatures.
- Do not add behavior to DTOs during this cleanup.
- Do not reorder fields if serialization or tests depend on constructor order.

## Comment Rules

Remove noisy inline comments that only repeat the code.

Bad examples:

```java
auctionLock.lock(); // Lock to ensure thread safety
return userId; // return user id
```

Prefer short section comments when they make the file easier to scan.

Good examples:

```java
// Request handlers
```

```java
// Business validation
```

```java
// DTO mapping
```

```java
// Database writes
```

Rules:

- Use comments to mark sections, not to explain obvious lines.
- Keep comments short and neutral.
- Do not add comments for every method.
- Do not leave outdated comments after moving code.
- Do not add JavaDoc in this cleanup pass.

## Suggested Section Comments by Class Type

For services:

```java
// Business operations
// Query operations
// Validation
// DTO mapping
```

For repositories:

```java
// Writes
// Reads
// Updates
// Deletes
// Row mapping
```

For handlers:

```java
// Route registration
// Request handlers
// Payload parsing
```

For controllers:

```java
// Initialization
// Event handlers
// Data loading
// Rendering
// Validation
// Helpers
```

Only add a section comment when there are enough methods in that section to make
the comment useful.

## Review Checklist

Before committing a cleanup file, check:

- The diff only moves code, removes noisy comments, or adds section comments.
- No method body logic changed.
- No SQL behavior changed.
- No FXML field name changed.
- No public method signature changed.
- No DTO field name changed.
- No unrelated formatting churn was introduced.
- The file is easier to scan than before.

