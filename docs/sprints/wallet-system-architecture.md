# Wallet System Architecture & Flow

Tai lieu nay mo ta module wallet va payment theo code hien tai. Schema day du nam o `docs/architecture/database-schema.md`; tai lieu nay chi giu phan lien quan den flow.

## Overview

Moi user co mock wallet voi balance mac dinh `100000.00`. Wallet duoc dung de:

- Kiem tra so du truoc khi dat bid.
- Deposit/withdraw tu man hinh wallet.
- Chuyen tien buyer -> seller khi winner thanh toan auction da `FINISHED`.
- Luu lich su giao dich trong `wallet_transactions`.

## Components

Server:

- `common/entity/Wallet.java`
- `common/entity/WalletTransaction.java`
- `server/wallet/WalletRepository.java`
- `server/wallet/WalletService.java`
- `server/wallet/WalletHandler.java`
- `server/wallet/WalletModule.java`
- `server/payment/PaymentRepository.java`
- `server/payment/PaymentService.java`
- `server/payment/PaymentHandler.java`
- `server/payment/PaymentModule.java`

Client:

- `client/user/service/WalletClientService.java`
- `client/user/service/PaymentClientService.java`
- `client/user/controller/WalletController.java`
- `client/user/controller/PaymentController.java`
- `client/user/controller/UserSidebarController.java`
- `src/main/resources/views/user/wallet.fxml`
- `src/main/resources/views/user/payment.fxml`

Integration:

- `BidService` dung `WalletRepository` de check balance trong transaction dat bid.
- `PaymentService` dung `WalletService.transfer(...)` trong transaction thanh toan.
- `DatabaseInitializer` seed wallet cho user chua co wallet luc startup.

## Schema lien quan

Bang lien quan:

- `wallets(user_id, balance, created_at, updated_at)`
- `wallet_transactions(id, user_id, amount, transaction_type, reference_id, description, created_at)`
- `payment_transactions(id, auction_id, payer_id, payee_id, amount, status, created_at, updated_at)`

Chi tiet cot/index xem `docs/architecture/database-schema.md`.

## Wallet request flow

```text
WalletController
-> WalletClientService
-> GET_WALLET / DEPOSIT_MONEY / WITHDRAW_MONEY
-> WalletHandler
-> WalletService
-> WalletRepository
-> Response WalletResponse
```

`WalletService.deposit(...)` va `withdraw(...)` validate amount > 0, tao wallet neu chua co, cap nhat balance, va luu transaction `DEPOSIT` hoac `WITHDRAW`.

Wallet module hien khong co wallet-specific push event. UI cap nhat balance qua request-response sau khi action thanh cong.

## Bid balance verification

Trong `BidService.placeBid(...)`:

1. Mo transaction tu `DataSource`.
2. Doc auction va expected `version`.
3. Doc wallet cua bidder; neu chua co thi tao wallet mac dinh `100000.00`.
4. Neu `wallet.balance < bid amount`, nem `ValidationException`.
5. Luu bid va cap nhat highest bid bang optimistic update.
6. Commit neu thanh cong, rollback neu loi.

## Payment checkout flow

```text
PaymentController
-> PaymentClientService.processPayment(...)
-> PROCESS_PAYMENT
-> PaymentHandler
-> PaymentService.processPayment(...)
-> PaymentRepository.saveCompletedPayment(...)
-> WalletService.transfer(...)
-> AuctionRepository.updateStatus(..., PAID)
```

`PaymentService` chi cho winner thanh toan auction co trang thai `FINISHED`, chua `PAID`, chua `CANCELED`, va co winning bid. Tat ca thao tac payment record, wallet transfer, va auction status update nam trong cung transaction.

## Message types va DTO

- `GET_WALLET` -> `GetWalletRequest` -> `WalletResponse`
- `DEPOSIT_MONEY` -> `DepositRequest` -> `WalletResponse`
- `WITHDRAW_MONEY` -> `WithdrawRequest` -> `WalletResponse`
- `LIST_PENDING_PAYMENTS` -> `ListPendingPaymentsRequest` -> `PendingPaymentsResponse`
- `LIST_PAYMENT_HISTORY` -> `ListPaymentHistoryRequest` -> `PaymentHistoryResponse`
- `PROCESS_PAYMENT` -> `ProcessPaymentRequest` -> `ProcessPaymentResponse`

## Tests

- `server/wallet/WalletServiceTest.java`
- `server/wallet/WalletHandlerTest.java`
- `server/wallet/WalletRepositoryTest.java`
- `server/payment/PaymentServiceTest.java`
- `server/payment/PaymentRepositoryTest.java`
