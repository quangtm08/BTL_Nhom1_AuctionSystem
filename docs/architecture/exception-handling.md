# Xử lý Exception

## Mục tiêu

- Mọi lỗi trả về client đi qua `ResponseMessage.error`.
- Lỗi nghiệp vụ dùng typed exception rõ nghĩa.
- Mỗi typed exception tự mang `error.code`.
- `ResponseFactory` không map bằng nhiều nhánh `instanceof`.
- Repository chỉ xử lý lỗi kỹ thuật/database, không tạo lỗi nghiệp vụ.
- Client map `error.code` thành typed exception để controller xử lý dễ hơn.

## Cấu trúc lỗi trả về client

```json
{
  "requestId": "uuid",
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "startingPrice must be greater than 0",
    "details": null
  }
}
```

`code` dùng cho logic xử lý.

`message` dùng để hiển thị lỗi nghiệp vụ cho người dùng.

`details` hiện không dùng cho lỗi hệ thống. Lỗi hệ thống không gửi chi tiết kỹ thuật về client.

## Base exception

Tất cả lỗi nghiệp vụ/app-level kế thừa `AppException`.

```java
public abstract class AppException extends RuntimeException {
    private final String code;

    protected AppException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
```

`AppException` là unchecked exception. Service/entity không cần khai báo `throws ValidationException`, `throws NotFoundException`, v.v.

Unchecked ở đây không có nghĩa là lỗi bất ngờ. Ý nghĩa chính là compiler không bắt buộc khai báo `throws`.

## Typed exceptions hiện có

| Exception | Error code |
| --- | --- |
| `ValidationException` | `VALIDATION_ERROR` |
| `UserAlreadyExistsException` | `VALIDATION_ERROR` |
| `AuthenticationException` | `AUTHENTICATION_FAILED` |
| `UnauthorizedActionException` | `UNAUTHORIZED` |
| `NotFoundException` | `NOT_FOUND` |
| `InvalidAuctionStateException` | `INVALID_AUCTION_STATE` |
| `InvalidBidException` | `INVALID_BID` |
| `AuctionClosedException` | `AUCTION_CLOSED` |
| `PaymentException` | `PAYMENT_FAILED` |
| `ConflictException` | `CONFLICT` |
| `ServerException` | code lấy từ server response |

`AuctionException` không còn dùng.

`ServerException` chỉ dùng ở client khi nhận một `error.code` chưa có typed exception tương ứng hoặc khi không kết nối được server.

## Error code tiêu chuẩn

| Code | Ý nghĩa |
| --- | --- |
| `INVALID_FORMAT` | JSON/request payload sai định dạng. |
| `VALIDATION_ERROR` | Input không hợp lệ theo rule nghiệp vụ. |
| `AUTHENTICATION_FAILED` | Đăng nhập sai hoặc caller không xác thực được. |
| `UNAUTHORIZED` | Caller hợp lệ nhưng không có quyền làm hành động đó. |
| `NOT_FOUND` | Không tìm thấy entity cần xử lý. |
| `INVALID_AUCTION_STATE` | Trạng thái auction không cho phép hành động hiện tại. |
| `INVALID_BID` | Bid vi phạm rule về giá, bidder, thời điểm, bước giá. |
| `AUCTION_CLOSED` | Auction không còn nhận bid. |
| `PAYMENT_FAILED` | Thanh toán thất bại. |
| `CONFLICT` | Dữ liệu/trạng thái bị xung đột. |
| `SERVER_ERROR` | Lỗi kỹ thuật hoặc lỗi runtime không mong muốn. |

## Phân loại lỗi

### Lỗi nghiệp vụ

Lỗi nghiệp vụ là lỗi có thể đoán trước trong flow bình thường.

Ví dụ:

- Giá khởi điểm không hợp lệ.
- Auction không tồn tại.
- User không có quyền xóa auction.
- Bid thấp hơn giá yêu cầu.
- Auction đã đóng.

Service/entity ném typed `AppException`.

```java
throw new NotFoundException("Auction not found");
```

```java
throw new InvalidBidException("amount must be greater than currentHighestBid");
```

Client nhận đúng `error.code` và `message`.

Server không cần in stack trace cho loại lỗi này.

### Lỗi kỹ thuật

Lỗi kỹ thuật là lỗi không nên xảy ra trong flow nghiệp vụ bình thường.

Ví dụ:

- `SQLException`.
- Không kết nối được database.
- Mapping dữ liệu từ `ResultSet` lỗi.
- Transaction commit/rollback lỗi.
- Dữ liệu server rơi vào trạng thái không nhất quán.

Repository hoặc service wrap lỗi kỹ thuật bằng `RuntimeException(message, cause)`.

```java
throw new RuntimeException("Failed to save auction", e);
```

Server in stack trace.

Client chỉ nhận:

```json
{
  "code": "SERVER_ERROR",
  "message": "Unexpected server error"
}
```

Không gửi message kỹ thuật như `"Failed to save auction"` hoặc `"Connection lost"` về client.

## Luồng server

### 1. Client gửi request

Client gửi `RequestMessage` qua `ServerConnection`.

Request có `requestId`, `type`, `payload`.

### 2. MessageRouter chọn handler

`MessageRouter` tìm handler theo `MessageType`.

Không có handler phù hợp thì trả lỗi protocol tương ứng.

### 3. Handler parse JSON

Handler parse `payloadJson` thành DTO bằng `JsonUtil.fromJson(...)`.

Parse fail thì trả ngay:

```java
ResponseFactory.invalidFormat(requestId, "Invalid ... JSON")
```

Kết quả gửi client:

```text
INVALID_FORMAT + message parse lỗi
```

Lỗi parse không đi qua service.

### 4. Handler gọi service

Sau khi parse thành công, handler gọi service.

Service xử lý rule nghiệp vụ.

Repository đọc/ghi database.

### 5. Service/entity ném AppException cho lỗi nghiệp vụ

Ví dụ flow bid:

```text
BidHandler
-> BidService.placeBid(...)
-> Auction.placeBid(...)
-> AuctionBidValidator.validatePlaceBid(...)
-> throw InvalidBidException
```

`InvalidBidException` kế thừa `AppException`.

Exception này có sẵn:

```text
code = INVALID_BID
message = nội dung lỗi
```

### 6. Repository ném RuntimeException cho lỗi kỹ thuật

Repository không ném `NotFoundException`, `InvalidBidException`, v.v.

Repository chỉ biết lỗi database/kỹ thuật.

Ví dụ:

```java
catch (SQLException e) {
    throw new RuntimeException("Failed to find auction", e);
}
```

`cause` phải được giữ lại để debug stack trace.

### 7. Transaction giữ nguyên AppException

Trong service có transaction, `AppException` được rollback rồi ném lại nguyên bản.

```java
try {
    connection.setAutoCommit(false);
    // work
    connection.commit();
} catch (AppException e) {
    connection.rollback();
    throw e;
} catch (Exception e) {
    connection.rollback();
    throw new RuntimeException("Transaction failed", e);
} finally {
    connection.setAutoCommit(oldAutoCommit);
}
```

Ý nghĩa:

- `AppException`: lỗi nghiệp vụ, giữ nguyên `code` và `message`.
- Exception khác: lỗi kỹ thuật, wrap thành `RuntimeException`.

### 8. Handler catch exception

Handler catch exception từ service và gọi:

```java
ResponseFactory.fromException(requestId, e)
```

Handler không tự map exception sang code.

## ResponseFactory

`ResponseFactory` là ranh giới chuyển Java exception thành protocol response.

### AppException

`ResponseFactory.fromException(...)` tìm `AppException` trong cause chain.

```java
AppException appException = findAppException(throwable);
```

Nếu tìm thấy:

```java
return new ResponseMessage<>(
    requestId,
    appException.getCode(),
    appException.getMessage()
);
```

Không cần `mapCode()`.

Không cần nhiều nhánh `instanceof`.

### System exception

Nếu không tìm thấy `AppException`:

```java
throwable.printStackTrace();
return new ResponseMessage<>(
    requestId,
    ErrorCode.SERVER_ERROR,
    "Unexpected server error"
);
```

Stack trace chỉ nằm ở server terminal.

Client không nhận chi tiết kỹ thuật.

### Cause chain

`findAppException(...)` đi qua cause chain.

Trường hợp `AppException` bị bọc trong `RuntimeException` hoặc `CompletionException`, `ResponseFactory` vẫn tìm được.

```text
RuntimeException("Transaction failed",
    NotFoundException("Auction not found"))
-> tìm thấy NotFoundException
-> trả NOT_FOUND
```

## Luồng client

### 1. BaseClientService nhận ResponseMessage

Client service gọi server và nhận `ResponseMessage<T>`.

Nếu `response.isSuccess()`:

```java
return response.getPayload();
```

Nếu lỗi:

```java
throw new CompletionException(mapServerError(response.getError()));
```

### 2. mapServerError chuyển code thành typed exception

`BaseClientService.mapServerError(...)` map `error.code` như sau:

| Error code | Client exception |
| --- | --- |
| `VALIDATION_ERROR` | `ValidationException` |
| `INVALID_FORMAT` | `ValidationException` |
| `AUTHENTICATION_FAILED` | `AuthenticationException` |
| `UNAUTHORIZED` | `UnauthorizedActionException` |
| `NOT_FOUND` | `NotFoundException` |
| `INVALID_BID` | `InvalidBidException` |
| `AUCTION_CLOSED` | `AuctionClosedException` |
| `INVALID_AUCTION_STATE` | `InvalidAuctionStateException` |
| `PAYMENT_FAILED` | `PaymentException` |
| `CONFLICT` | `ConflictException` |
| code khác | `ServerException` |

Client giữ được typed exception để controller xử lý theo type khi cần.

### 3. CompletionException được unwrap

Async flow dùng `CompletableFuture`, nên lỗi thường bị bọc trong `CompletionException`.

Controller dùng:

```java
Throwable cause = BaseClientService.extractFailure(ex);
```

`extractFailure(...)` bóc `CompletionException` để lấy exception thật.

### 4. Controller xử lý lỗi

Controller có 2 cách xử lý.

Cách 1: hiển thị message.

```java
showError(cause.getMessage());
```

Cách 2: xử lý theo type.

```java
if (cause instanceof AuthenticationException) {
    AppNavigator.navigateTo(AppView.SIGN_IN);
    return;
}
```

Lỗi nghiệp vụ có message rõ ràng nên có thể hiển thị trực tiếp.

Lỗi hệ thống dùng message chung, ví dụ `"Unexpected server error"` hoặc `"Server unreachable: ..."` tùy lỗi phía client.

## Flow đầy đủ

### Flow lỗi nghiệp vụ

```text
Client
-> gửi RequestMessage
-> Server MessageRouter
-> Handler parse JSON thành DTO
-> Service xử lý rule
-> Entity/validator phát hiện lỗi
-> throw InvalidBidException("amount must be greater than currentHighestBid")
-> Handler catch exception
-> ResponseFactory.fromException(...)
-> findAppException(...) tìm thấy InvalidBidException
-> trả ResponseMessage error:
   code = INVALID_BID
   message = amount must be greater than currentHighestBid
-> BaseClientService nhận response lỗi
-> mapServerError(...) tạo InvalidBidException phía client
-> CompletableFuture hoàn tất lỗi
-> Controller extractFailure(...)
-> Controller hiển thị message hoặc xử lý theo type
```

### Flow lỗi kỹ thuật

```text
Client
-> gửi RequestMessage
-> Server MessageRouter
-> Handler parse JSON thành DTO
-> Service gọi repository
-> Repository gặp SQLException
-> throw RuntimeException("Failed to save bid", sqlException)
-> Handler catch exception
-> ResponseFactory.fromException(...)
-> không tìm thấy AppException
-> server printStackTrace()
-> trả ResponseMessage error:
   code = SERVER_ERROR
   message = Unexpected server error
-> BaseClientService nhận response lỗi
-> mapServerError(...) tạo ServerException hoặc typed fallback tương ứng
-> Controller extractFailure(...)
-> Controller hiển thị lỗi chung
```

### Flow lỗi JSON

```text
Client
-> gửi RequestMessage payload sai JSON/schema
-> Handler parse JSON fail
-> ResponseFactory.invalidFormat(...)
-> trả ResponseMessage error:
   code = INVALID_FORMAT
   message = Invalid ... JSON
-> BaseClientService map INVALID_FORMAT thành ValidationException
-> Controller hiển thị message
```

## Quy tắc khi thêm code mới

### Khi thêm lỗi nghiệp vụ mới

Tạo exception mới kế thừa `AppException`.

```java
public class NewBusinessException extends AppException {
    public NewBusinessException(String message) {
        super(ErrorCode.SOME_CODE, message);
    }
}
```

Không sửa `ResponseFactory` nếu exception đã có code.

Nếu client cần xử lý type riêng, thêm mapping trong `BaseClientService.mapServerError(...)`.

### Khi viết service

Ném typed `AppException` cho lỗi nghiệp vụ.

Không khai báo `throws` cho typed app exceptions.

Trong transaction:

- catch `AppException`, rollback, throw lại.
- catch exception khác, rollback, wrap `RuntimeException(message, cause)`.

### Khi viết repository

Không ném domain exception.

Không quyết định `NOT_FOUND`, `INVALID_BID`, `UNAUTHORIZED`.

Repository trả `Optional.empty()`, `false`, hoặc dữ liệu cho service quyết định rule nghiệp vụ.

Khi bắt `SQLException`, luôn giữ cause:

```java
throw new RuntimeException("Failed to load user", e);
```

### Khi viết handler

Tách parse JSON khỏi gọi service.

Parse fail trả `INVALID_FORMAT`.

Service fail gọi `ResponseFactory.fromException(...)`.

Không map exception thủ công trong handler.

### Khi viết controller

Luôn dùng:

```java
BaseClientService.extractFailure(ex)
```

Không tự bóc nhiều lớp `CompletionException`.

Hiển thị trực tiếp message của lỗi nghiệp vụ.

Branch theo exception type khi UI cần hành vi đặc biệt.
