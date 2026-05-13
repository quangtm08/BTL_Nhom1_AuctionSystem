# Exception Handling

## Goals
- Standardize every server error as `ResponseMessage.error`.
- Keep handler code thin: parse request, call service, map exception.
- Let client services convert `error.code` into typed exceptions for controllers.

## Error response shape

```json
{
  "requestId": "uuid",
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "startingPrice must be greater than 0",
    "details": "optional extra context"
  }
}
```

## Standard error codes

| Code | Meaning |
| --- | --- |
| `INVALID_FORMAT` | JSON shape or UUID/input format is invalid |
| `VALIDATION_ERROR` | Business validation failed before mutation |
| `AUTHENTICATION_FAILED` | Caller is unknown or credentials are invalid |
| `UNAUTHORIZED` | Caller exists but is not allowed to perform the action |
| `NOT_FOUND` | Requested entity does not exist |
| `INVALID_AUCTION_STATE` | Auction state blocks the requested transition |
| `INVALID_BID` | Bid amount/rules are invalid |
| `AUCTION_CLOSED` | Bid attempted on a closed auction |
| `PAYMENT_FAILED` | Payment transaction failed or already completed |
| `CONFLICT` | State changed concurrently or conflicts with current data |
| `SERVER_ERROR` | Unexpected infrastructure/runtime failure |

## Server mapping

Shared mapping lives in `server/infrastructure/ResponseFactory`.

| Exception type | Error code |
| --- | --- |
| `ValidationException`, `IllegalArgumentException`, `UserAlreadyExistsException` | `VALIDATION_ERROR` |
| `AuthenticationException` | `AUTHENTICATION_FAILED` |
| `UnauthorizedActionException` | `UNAUTHORIZED` |
| `NotFoundException` | `NOT_FOUND` |
| `InvalidAuctionStateException` | `INVALID_AUCTION_STATE` |
| `InvalidBidException` | `INVALID_BID` |
| `AuctionClosedException` | `AUCTION_CLOSED` |
| `PaymentException` | `PAYMENT_FAILED` |
| `ConflictException` | `CONFLICT` |
| fallback / SQL / runtime | `SERVER_ERROR` |

## Flow
1. Client sends `RequestMessage`.
2. `MessageRouter` picks the registered handler.
3. Handler parses JSON. Parse failure returns `INVALID_FORMAT`.
4. Handler calls service and wraps success with `ResponseFactory.success(...)`.
5. Any service exception is converted with `ResponseFactory.fromException(...)`.
6. Client `BaseClientService` unwraps the response and maps `error.code` back to typed exceptions.
7. Controllers display the final message and keep UI responsive.

## UI rules
- Validation/business errors are shown directly to the user.
- Network/server failures fall back to a generic unavailable message when no typed error exists.
- Controllers should use `BaseClientService.extractFailure(...)` instead of manually unwrapping `CompletionException`.
