# Xử lý Exception (Exception Handling)

## Mục tiêu
- Chuẩn hóa mọi lỗi server dưới dạng `ResponseMessage.error`.
- Giữ code handler mỏng: parse request, gọi service, map exception.
- Cho phép client services chuyển đổi `error.code` thành typed exceptions cho các controller.

## Cấu trúc Error response

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

## Các Error code tiêu chuẩn

| Code | Ý nghĩa |
| --- | --- |
| `INVALID_FORMAT` | Cấu trúc JSON hoặc định dạng UUID/input không hợp lệ |
| `VALIDATION_ERROR` | Business validation thất bại trước khi thực hiện mutation |
| `AUTHENTICATION_FAILED` | Người gọi không xác định hoặc thông tin xác thực không hợp lệ |
| `UNAUTHORIZED` | Người gọi tồn tại nhưng không được phép thực hiện hành động này |
| `NOT_FOUND` | Entity được yêu cầu không tồn tại |
| `INVALID_AUCTION_STATE` | Trạng thái auction chặn transition được yêu cầu |
| `INVALID_BID` | Số tiền bid hoặc quy tắc bid không hợp lệ |
| `AUCTION_CLOSED` | Cố gắng bid trên một auction đã kết thúc |
| `PAYMENT_FAILED` | Giao dịch thanh toán thất bại hoặc đã hoàn tất |
| `CONFLICT` | Trạng thái thay đổi đồng thời hoặc xung đột với dữ liệu hiện tại |
| `SERVER_ERROR` | Lỗi hạ tầng (infrastructure) hoặc runtime không mong muốn |

## Server mapping

Shared mapping nằm trong `server/infrastructure/ResponseFactory`.

| Exception type | Error code | Mô tả / Cách dùng |
| --- | --- | --- |
| `ValidationException` | `VALIDATION_ERROR` | Input của người dùng không hợp lệ về mặt ngữ nghĩa (vd: giá âm, tên trống). |
| `IllegalArgumentException` | `VALIDATION_ERROR` | Fallback cho các đối số không hợp lệ được truyền vào method. |
| `UserAlreadyExistsException` | `VALIDATION_ERROR` | Cố gắng đăng ký với email/username đã được sử dụng. |
| `AuthenticationException` | `AUTHENTICATION_FAILED` | Đăng nhập thất bại, hoặc token bị thiếu/hết hạn. |
| `UnauthorizedActionException` | `UNAUTHORIZED` | Người dùng đã xác thực nhưng không có quyền (vd: xóa auction của người khác). |
| `NotFoundException` | `NOT_FOUND` | ID được yêu cầu không tồn tại trong database. |
| `InvalidAuctionStateException` | `INVALID_AUCTION_STATE` | Hành động hợp lệ nhưng không phải lúc này (vd: sửa auction đã bắt đầu). |
| `InvalidBidException` | `INVALID_BID` | Bid quá thấp hoặc vi phạm quy tắc bước giá. |
| `AuctionClosedException` | `AUCTION_CLOSED` | Cố gắng bid hoặc sửa đổi một auction đã kết thúc. |
| `PaymentException` | `PAYMENT_FAILED` | Provider thanh toán bên ngoài thất bại hoặc không đủ số dư. |
| `ConflictException` | `CONFLICT` | Lỗi optimistic locking hoặc xung đột cập nhật trạng thái đồng thời. |
| `SQLException` | `SERVER_ERROR` | Lỗi database driver (mất kết nối, lỗi cú pháp). |
| `IllegalStateException` | `SERVER_ERROR` | **System Failure:** Server ở trạng thái không nhất quán (vd: dữ liệu đã verify tồn tại nhưng delete trả về 0 row). |
| *fallback* | `SERVER_ERROR` | Bất kỳ `RuntimeException` nào khác chưa được xử lý. |

## Domain vs. System Exceptions

Để giữ việc báo lỗi sạch sẽ, hãy tuân theo quy tắc sau:

1.  **Domain Exceptions (Expected):** Sử dụng các typed exceptions ở trên cho những thứ *có thể* xảy ra trong luồng nghiệp vụ thông thường. Những lỗi này cho người dùng biết họ đã làm sai điều gì hoặc tại sao quy tắc nghiệp vụ chặn họ.
2.  **System Exceptions (Unexpected):** Sử dụng `IllegalStateException` hoặc `RuntimeException` cho những thứ *không bao giờ nên xảy ra* nếu code chạy đúng. Những lỗi này được map thành `SERVER_ERROR` để team có thể kiểm tra log. KHÔNG hiển thị các chi tiết này cho người dùng.

## Luồng xử lý (Flow)
1. Client gửi `RequestMessage`.
2. `MessageRouter` chọn handler đã đăng ký.
3. Handler parse JSON. Parse thất bại trả về `INVALID_FORMAT`.
4. Handler gọi service và wrap kết quả thành công bằng `ResponseFactory.success(...)`.
5. Bất kỳ exception nào từ service đều được chuyển đổi bằng `ResponseFactory.fromException(...)`.
6. Client `BaseClientService` unwrap response và map `error.code` ngược lại thành các typed exceptions.
7. Controller hiển thị thông báo cuối cùng và giữ UI phản hồi tốt.

## Quy tắc UI
- Lỗi Validation/business được hiển thị trực tiếp cho người dùng.
- Lỗi Network/server fallback về một thông báo "không khả dụng" chung khi không có typed error cụ thể.
- Controllers nên sử dụng `BaseClientService.extractFailure(...)` thay vì unwrap `CompletionException` thủ công.
