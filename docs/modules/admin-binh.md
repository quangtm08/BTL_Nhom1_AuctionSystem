# Module Quan Tri He Thong

Tai lieu nay mo ta module admin theo code hien tai trong `client/admin` va `server/admin`.

## Thanh phan

Client:

- `AdminOverviewController`: hien thong ke tong quan tu user va auction list.
- `UserManagementController`: list user, xoa user, nghe push user created/deleted.
- `AuctionManagementController`: list auction, approve/cancel auction, nghe push auction events.
- `AdminClientService`: tao request DTO, chon `MessageType`, unwrap response cho controller.

Server:

- `AdminModule`: tao `SqlAdminAuctionGateway`, `AdminService`, `AdminHandler` va dang ky route.
- `AdminHandler`: xu ly `ADMIN_LIST_USERS`, `ADMIN_LIST_AUCTIONS`, `ADMIN_DELETE_USER`, `ADMIN_CANCEL_AUCTION`, `ADMIN_APPROVE_AUCTION`.
- `AdminService`: validate caller la admin, chay nghiep vu quan tri va transaction can thiet.
- `UserRepository`, `AuctionRepository`, `ItemRepository`, `BidRepository`: duoc inject de doc/xoa/cap nhat du lieu lien quan.
- `SqlAdminAuctionGateway`: adapter SQL cho list/approve/cancel auction phuc vu admin.
- `NotificationService`: broadcast `PUSH_USER_DELETED`, `PUSH_AUCTION_DELETED` hoac event lien quan sau thao tac thanh cong.

## Luong list dashboard

```text
AdminOverviewController
-> AdminClientService.listUsers/listAllAuctions
-> ADMIN_LIST_USERS / ADMIN_LIST_AUCTIONS
-> AdminHandler
-> AdminService.requireAdmin(...)
-> UserRepository / SqlAdminAuctionGateway
-> AdminUserListResponse / AdminAuctionListResponse
```

Controller cap nhat UI trong `Platform.runLater(...)` vi response den tu background listener thread.

## Luong xoa user

`AdminService.deleteUser(targetUserId, callerId)`:

1. Validate target/caller.
2. `requireAdmin(callerId)`.
3. Chan admin tu xoa chinh minh.
4. Chan xoa target co role `ADMIN`.
5. Trong transaction: clear highest bidder, xoa bid cua target, xoa auction/item cua seller target, xoa user.
6. Commit va broadcast `PUSH_USER_DELETED`.

## Luong approve/cancel auction

- `ADMIN_APPROVE_AUCTION`: admin approve auction qua `AdminService.approveAuction(...)`, gateway cap nhat status phu hop.
- `ADMIN_CANCEL_AUCTION`: admin cancel auction qua `AdminService.cancelAuction(...)`, gateway cap nhat auction va service broadcast de client bo/refresh item lien quan.

## Quyen va loi

Moi operation admin deu di qua `requireAdmin`. Neu caller khong ton tai hoac khong co role `ADMIN`, service nem exception typed (`UnauthorizedActionException`, `ValidationException`, `NotFoundException`) va `ResponseFactory` map ve response loi.

## Trung lap docs

Luong socket/request-response chung nam o `docs/architecture/client-server-communication.md`. Schema user/auction/item/bid nam o `docs/architecture/database-schema.md`.
