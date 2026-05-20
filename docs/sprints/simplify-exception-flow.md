# Đề xuất: Làm sạch Exception Handling

## Mục tiêu
1. Vẫn giữ typed domain exceptions như `ValidationException`, `NotFoundException`, `InvalidBidException`, v.v.
2. Thay `AuctionException` bằng base exception tên rõ hơn: `AppException`.
3. Mọi domain/app exception phải extend `AppException`.
4. `AppException` extend `RuntimeException` để giảm `throws` rườm rà trong service.
5. `AppException` mang sẵn `error.code`, nên `ResponseFactory` không cần map bằng nhiều nhánh `instanceof`.
6. Repository tiếp tục wrap lỗi database/kỹ thuật bằng `RuntimeException(message, cause)`.
7. Client có thể tiếp tục map `error.code` về typed exception nếu muốn controller xử lý theo type.

---

## Hiện trạng trong code

### Domain exceptions chưa có hierarchy sạch

Hiện tại trong `src/main/java/com/nhom1/auction/common/exception`:

```text
AuctionException extends Exception
AuthenticationException extends AuctionException
ValidationException extends Exception
NotFoundException extends Exception
InvalidBidException extends Exception
AuctionClosedException extends Exception
InvalidAuctionStateException extends Exception
UnauthorizedActionException extends Exception
UserAlreadyExistsException extends Exception
PaymentException extends Exception
ConflictException extends Exception
```

Vấn đề: chỉ `AuthenticationException` đi qua base `AuctionException`. Các exception còn lại không cùng một họ, nên `ResponseFactory` phải đoán code bằng nhiều nhánh `instanceof`.

### ResponseFactory đang map type sang code thủ công

Hiện tại `ResponseFactory.fromException(...)` làm:

```java
Throwable cause = unwrap(throwable);
String code = mapCode(cause);
String message = cause.getMessage() == null || cause.getMessage().isBlank()
        ? "Unexpected server error"
        : cause.getMessage();
```

Sau đó `mapCode(...)` có nhiều nhánh:

```java
ValidationException -> VALIDATION_ERROR
AuthenticationException -> AUTHENTICATION_FAILED
UnauthorizedActionException -> UNAUTHORIZED
NotFoundException -> NOT_FOUND
InvalidAuctionStateException -> INVALID_AUCTION_STATE
InvalidBidException -> INVALID_BID
AuctionClosedException -> AUCTION_CLOSED
PaymentException -> PAYMENT_FAILED
ConflictException -> CONFLICT
SQLException -> SERVER_ERROR
fallback -> SERVER_ERROR
```

Vấn đề: `ResponseFactory` phải biết quá nhiều class cụ thể. Mỗi lần thêm domain exception mới, phải nhớ update mapping.

### Client đã có mapping code -> typed exception

`BaseClientService.mapServerError(...)` hiện đã map:

```java
VALIDATION_ERROR / INVALID_FORMAT -> ValidationException
AUTHENTICATION_FAILED -> AuthenticationException
UNAUTHORIZED -> UnauthorizedActionException
NOT_FOUND -> NotFoundException
INVALID_BID -> InvalidBidException
AUCTION_CLOSED -> AuctionClosedException
INVALID_AUCTION_STATE -> InvalidAuctionStateException
PAYMENT_FAILED -> PaymentException
CONFLICT -> ConflictException
default -> AuctionException
```

Phần này là hợp lý nếu team muốn controller có thể viết logic theo exception type:

```java
if (cause instanceof AuthenticationException) {
    AppNavigator.navigateTo(AppView.SIGN_IN);
}
```

Vấn đề hiện tại không phải là mapping này sai. Vấn đề là controller chưa tận dụng nhiều, nên giá trị của nó chưa rõ.

### Repository đang làm đúng hướng

Repository hiện thường bắt `SQLException` hoặc lỗi mapping dữ liệu rồi wrap bằng `RuntimeException(message, cause)`, ví dụ:

```java
throw new RuntimeException("Failed to save auction", e);
```

Đây là hướng đúng: repository không nên tạo domain exception kiểu `InvalidBidException` hoặc `NotFoundException`. Repository chỉ biết lỗi kỹ thuật/database.

### Service đang bị checked exception làm rườm rà

Service hiện có nhiều method phải khai báo:

```java
throws ValidationException, NotFoundException, UnauthorizedActionException
```

hoặc:

```java
throws InvalidBidException, AuctionClosedException,
       UnauthorizedActionException, NotFoundException, IllegalStateException
```

Vì các domain exceptions đang extend `Exception`, Java bắt service phải khai báo `throws`. Điều này làm chữ ký method dài nhưng không giúp nhiều, vì lỗi cuối cùng vẫn bubble lên handler và `ResponseFactory`.

---

## Thiết kế mới

### 1. Tạo AppException làm base class

`AppException` thay thế `AuctionException`. Tên `AuctionException` gây hiểu nhầm vì project có nhiều module: auth, auction, bidding, admin, automation.

```java
package com.nhom1.auction.common.exception;

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

`AppException` extend `RuntimeException` không có nghĩa đây là lỗi không đoán trước. Trong Java, unchecked exception chỉ có nghĩa compiler không bắt buộc method phải khai báo `throws`.

Trong project này, domain exception nên unchecked vì:

- service không tự recover lỗi này;
- handler là nơi gom lỗi lại;
- `ResponseFactory` là nơi chuyển lỗi thành response;
- signatures `throws ...` hiện đang gây nhiễu nhiều hơn là giúp hiểu code.

---

## Typed domain exceptions

Vẫn giữ các exception cụ thể, nhưng tất cả phải extend `AppException` và tự khai báo code của mình.

Ví dụ:

```java
public class ValidationException extends AppException {
    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }
}
```

```java
public class NotFoundException extends AppException {
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
```

```java
public class InvalidBidException extends AppException {
    public InvalidBidException(String message) {
        super(ErrorCode.INVALID_BID, message);
    }
}
```

Tương tự:

| Exception | Code |
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

Sau refactor, không class nào nên extend `AuctionException`, và `AuctionException` có thể bị xóa hoặc rename thành `AppException`.

---

## Quy tắc ném lỗi

### Service: ném typed AppException cho lỗi nghiệp vụ

Service là nơi hiểu rule của ứng dụng. Khi user/request vi phạm rule có thể đoán trước, service ném typed domain exception.

Ví dụ:

```java
if (auction.isEmpty()) {
    throw new NotFoundException("Auction not found");
}
```

```java
if (amount.compareTo(currentHighestBid) <= 0) {
    throw new InvalidBidException(
        "Bid must be greater than current highest bid"
    );
}
```

```java
if (!sellerId.equals(auction.getSellerId())) {
    throw new UnauthorizedActionException(
        "You are not allowed to delete this auction"
    );
}
```

Rule:

```text
Expected business/app problem -> throw typed AppException subclass
```

Sau khi các exception này extend `RuntimeException`, service method không cần khai báo `throws ValidationException, NotFoundException, ...` nữa.

### Repository: ném RuntimeException cho lỗi kỹ thuật

Repository làm việc với database/JDBC. Nó không nên quyết định lỗi nghiệp vụ cho user.

Repository nên tiếp tục làm:

```java
try {
    // JDBC code
} catch (SQLException e) {
    throw new RuntimeException("Failed to save auction", e);
}
```

Rule:

```text
Unexpected database/technical problem -> throw RuntimeException(message, cause)
```

Luôn truyền `cause` vào constructor. Không viết:

```java
throw new RuntimeException("Failed to save auction");
```

nếu đang có exception gốc `e`, vì như vậy sẽ làm mất stack trace gốc.

### Transaction/service wrapping

Trong service transaction code, không nên biến domain exception thành lỗi hệ thống.

Nên viết:

```java
try {
    connection.setAutoCommit(false);
    // service/repository work
    connection.commit();
} catch (AppException e) {
    connection.rollback();
    throw e;
} catch (Exception e) {
    connection.rollback();
    throw new RuntimeException("Create auction transaction failed", e);
} finally {
    connection.setAutoCommit(oldAutoCommit);
}
```

Ý nghĩa:

```text
AppException -> lỗi nghiệp vụ, giữ nguyên code/message.
Exception khác -> lỗi hệ thống, wrap RuntimeException để log/debug.
```

---

## ResponseFactory

`ResponseFactory` vẫn là một phần quan trọng. Nó là ranh giới chuyển Java exception thành protocol response.

Sau refactor, `ResponseFactory` không cần `mapCode()` nhiều nhánh nữa.

Thiết kế đề xuất:

```java
public static <T> ResponseMessage<T> fromException(
    String requestId,
    Throwable throwable
) {
    AppException appException = findAppException(throwable);
    if (appException != null) {
        return new ResponseMessage<>(
            requestId,
            appException.getCode(),
            appException.getMessage()
        );
    }

    throwable.printStackTrace();
    return new ResponseMessage<>(
        requestId,
        ErrorCode.SERVER_ERROR,
        "Unexpected server error"
    );
}
```

Vẫn nên có helper tìm `AppException` trong cause chain:

```java
private static AppException findAppException(Throwable throwable) {
    Throwable current = throwable;
    while (current != null && current.getCause() != current) {
        if (current instanceof AppException appException) {
            return appException;
        }
        current = current.getCause();
    }
    return null;
}
```

Lý do:

- Flow đúng là service ném `AppException` trực tiếp.
- Nhưng nếu lỗi bị bọc trong transaction hoặc `CompletionException`, factory vẫn tìm được lỗi app-level.
- Không cần map `SQLException` riêng. `SQLException` là technical error, nên fallback thành `SERVER_ERROR`.

Không nên gửi raw message của lỗi hệ thống về client. Ví dụ message như `"Failed to save auction"` hoặc `"Database connection error"` chỉ nên nằm ở server terminal.

---

## Handler

Handler nên tách rõ:

1. Parse JSON/request.
2. Gọi service.

Không nên dùng một `catch (Exception)` bao quanh cả parse và service, vì service error có thể bị trả nhầm thành `INVALID_FORMAT`.

Nên viết:

```java
router.register(MessageType.LOGIN, (requestId, payloadJson) -> {
    LoginRequest dto;
    try {
        dto = JsonUtil.fromJson(payloadJson, LoginRequest.class);
    } catch (Exception e) {
        return ResponseFactory.invalidFormat(requestId, "Invalid Login JSON");
    }

    return handleLogin(requestId, dto);
});
```

Sau đó:

```java
private ResponseMessage<AuthResponse> handleLogin(
    String requestId,
    LoginRequest dto
) {
    try {
        User user = authService.login(dto.getIdentifier(), dto.getPassword());
        return ResponseFactory.success(requestId, toResponse(user));
    } catch (Exception e) {
        return ResponseFactory.fromException(requestId, e);
    }
}
```

---

## Client

Client có thể tiếp tục theo standard approach: map `error.code` về typed exception object.

`BaseClientService.mapServerError(...)` hiện đã có logic này và có thể giữ lại. Nhưng sau khi đổi `AuctionException` thành `AppException`, fallback nên trả một subclass chung như `ServerException`, không trả `AuctionException`.

```java
public class ServerException extends AppException {
    public ServerException(String code, String message) {
        super(code, message);
    }
}
```

Ví dụ:

```java
private Exception mapServerError(ErrorResponse error) {
    String message = error != null && error.getMessage() != null
        ? error.getMessage()
        : "Unknown server error";
    String code = error != null
        ? error.getCode()
        : ErrorCode.SERVER_ERROR;

    return switch (code) {
        case ErrorCode.VALIDATION_ERROR,
             ErrorCode.INVALID_FORMAT -> new ValidationException(message);
        case ErrorCode.AUTHENTICATION_FAILED -> new AuthenticationException(message);
        case ErrorCode.UNAUTHORIZED -> new UnauthorizedActionException(message);
        case ErrorCode.NOT_FOUND -> new NotFoundException(message);
        case ErrorCode.INVALID_BID -> new InvalidBidException(message);
        case ErrorCode.AUCTION_CLOSED -> new AuctionClosedException(message);
        case ErrorCode.INVALID_AUCTION_STATE -> new InvalidAuctionStateException(message);
        case ErrorCode.PAYMENT_FAILED -> new PaymentException(message);
        case ErrorCode.CONFLICT -> new ConflictException(message);
        default -> new ServerException(code, message);
    };
}
```

Controller có thể dùng type khi cần:

```java
Throwable cause = BaseClientService.extractFailure(ex);
if (cause instanceof AuthenticationException) {
    AppNavigator.navigateTo(AppView.SIGN_IN);
    return null;
}
showError(cause.getMessage());
```

Hoặc vẫn dùng message trực tiếp khi không cần xử lý đặc biệt:

```java
showError(cause.getMessage());
```

---

## Logging / Server Terminal

Quy tắc terminal:

- `AppException`: lỗi nghiệp vụ dự kiến, không cần stack trace.
- Exception khác: lỗi hệ thống, in stack trace ở server terminal.
- Client không nhận chi tiết kỹ thuật của lỗi hệ thống.

Ví dụ:

```text
InvalidBidException("Bid is too low")
-> client nhận INVALID_BID + message rõ ràng
-> server không cần stack trace

SQLException("connection failed")
-> repository wrap RuntimeException("Failed to save bid", cause)
-> ResponseFactory print stack trace
-> client nhận SERVER_ERROR + "Unexpected server error"
```

Nếu cần debug trong demo, có thể in một dòng ngắn cho app errors:

```java
System.err.println(
    "[" + requestId + "] " + appException.getCode()
        + ": " + appException.getMessage()
);
```

Nhưng không nên in stack trace cho mọi lỗi nghiệp vụ bình thường.

---

## So sánh trước và sau

| | Hiện tại | Sau refactor |
|---|---|---|
| Base exception | `AuctionException`, nhưng hầu như không được dùng | `AppException` dùng chung cho mọi domain exception |
| Domain exceptions | Nhiều class extend trực tiếp `Exception` | Mọi class extend `AppException` |
| Checked/unchecked | Checked, gây nhiều `throws` trong service | Unchecked, service signature sạch hơn |
| Error code | `ResponseFactory.mapCode()` đoán từ type | Mỗi `AppException` subclass tự mang code |
| Repository errors | Wrap `RuntimeException(message, cause)` | Giữ nguyên hướng này |
| System error gửi client | Có thể lộ raw message/details | Gửi `SERVER_ERROR` generic |
| Client mapping | Code -> typed exception | Giữ lại |
| Controller behavior | Chủ yếu `getMessage()` | Có thể dần thêm logic theo exception type |

---

## Flow cuối cùng

```text
1. Service phát hiện lỗi nghiệp vụ
   -> throw new InvalidBidException("Bid is too low")
   -> InvalidBidException extends AppException
   -> AppException chứa code INVALID_BID

2. Repository gặp lỗi database
   -> throw new RuntimeException("Failed to save bid", sqlException)

3. Handler catch exception
   -> ResponseFactory.fromException(requestId, e)

4. ResponseFactory
   -> nếu tìm thấy AppException: gửi appException.code + message
   -> nếu không: print stack trace, gửi SERVER_ERROR generic

5. Client nhận ResponseMessage
   -> success=true: lấy payload
   -> success=false: BaseClientService map error.code về typed exception

6. Controller
   -> nếu cần: branch bằng instanceof
   -> nếu không: show cause.getMessage()
```

Đây là hướng "standard nhưng sạch":

- Giữ typed domain exceptions.
- Có base class đúng nghĩa.
- Không còn `throws` rườm rà.
- Không còn server-side mapping thủ công quá dài.
- Repository và service có ranh giới rõ ràng.
- Client vẫn có option xử lý exception theo type khi UI cần.
