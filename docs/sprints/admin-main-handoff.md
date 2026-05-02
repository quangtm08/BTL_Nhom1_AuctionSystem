# Admin Main Branch Handoff

Tai lieu nay tom tat nhung gi da hoan thanh trong branch `feature/admin-main`, cac diem dang chong cheo voi thanh vien khac, va nhung viec can noi tiep de admin flow chay tron.

## 1. Pham vi da lam xong

Branch nay da xu ly phan admin-main theo `team-task-allocation` cua Binh:

- server admin module
  - `AdminService`
  - `AdminHandler`
  - `AdminModule`
- client admin service
  - `AdminClientService`
- admin overview controller
  - goi 2 API song song
  - render tong quan user va auction
- hookup dashboard overview vao FXML
- refactor auth flow client theo huong `BaseClientService` + `AuthClientService`
- fix loi unwrap exception sau refactor de popup login/register hien dung message validation
- tai lieu handoff cho teammate

## 2. Cac file da tao / da sua

### Server

- `src/main/java/com/nhom1/auction/server/admin/AdminAuctionGateway.java`
- `src/main/java/com/nhom1/auction/server/admin/AdminService.java`
- `src/main/java/com/nhom1/auction/server/admin/AdminHandler.java`
- `src/main/java/com/nhom1/auction/server/admin/AdminModule.java`

### Client

- `src/main/java/com/nhom1/auction/client/admin/service/AdminClientService.java`
- `src/main/java/com/nhom1/auction/client/admin/controller/AdminOverviewController.java`
- `src/main/java/com/nhom1/auction/client/user/service/BaseClientService.java`
- `src/main/java/com/nhom1/auction/client/user/service/AuthClientService.java`
- `src/main/java/com/nhom1/auction/client/user/Controller/SignInController.java`
- `src/main/java/com/nhom1/auction/client/user/Controller/RegisterController.java`
- `src/main/resources/views/admin/admin_overview.fxml`

### Shared / support

- `src/main/java/com/nhom1/auction/server/auth/UserRepository.java`
  - da them `deleteById(UUID id)`
- `src/main/java/com/nhom1/auction/server/infrastructure/ServerContext.java`
  - da them comment wiring cho admin

## 3. Logic da hoan thanh

### Server

`AdminHandler`
- register:
  - `ADMIN_LIST_USERS`
  - `ADMIN_DELETE_USER`
  - `ADMIN_LIST_AUCTIONS`

`AdminService`
- `getAllUsers()`
  - xac thuc `callerId` phai la `ADMIN`
  - doc toan bo user tu `UserRepository`
  - map sang `UserSummaryDto`
- `deleteUser(targetUserId, callerId)`
  - validate payload
  - load caller
  - caller phai la `ADMIN`
  - khong cho xoa admin account qua flow nay
  - xoa target user
- `getAllAuctions()`
  - xac thuc `callerId` phai la `ADMIN`
  - lay danh sach `AuctionSummaryDto` qua `AdminAuctionGateway`

### Client

`AdminClientService`
- `listUsers()`
- `listAllAuctions()`
- `deleteUser(targetUserId)`
- ca 3 flow deu tu dien `callerId` tu `AppContext`
- client fail-fast neu current user khong phai `ADMIN`

`AdminOverviewController`
- goi `listUsers()` va `listAllAuctions()` song song
- render:
  - tong user
  - breakdown admin/member
  - so auction dang running
  - so auction finished
- co state loi neu load that bai
- da doi sang `thenCombine(...)` de gom 2 ket qua ro rang hon, tranh `allOf(...).join()` thu cong

`BaseClientService` / `AuthClientService`
- tach logic `ServerConnection` / `RequestMessage` ra khoi controller
- gom unwrap response va fail-fast validation vao service layer
- them helper `extractFailure(...)` de controller lay dung root-cause khi future fail

`SignInController` / `RegisterController`
- da dung `AuthClientService`
- da sua loi cu: validation error client-side khong con lam `NullPointerException` trong `exceptionally(...)`

## 4. Vi sao co `AdminAuctionGateway`

Branch nay chua co concrete `AuctionRepository` / mapper auction cua Duy, nhung admin can endpoint:

- list tat ca auction de admin dashboard dem va hien thi tong quan

De branch compile duoc va dependency ro rang, da tao:

- `server/admin/AdminAuctionGateway`

Gateway nay hien can:

```java
List<AuctionSummaryDto> findAllAuctionSummaries();
```

## 5. Cac diem chong cheo voi thanh vien khac

### Duy (Auction feature)

Can cung cap implementation cho `AdminAuctionGateway`.

Co 2 cach:

1. Cho `AuctionRepository` hoac service auction implement truc tiep `AdminAuctionGateway`
2. Tao adapter rieng

Mau adapter:

```java
public class AuctionRepositoryAdminAdapter implements AdminAuctionGateway {
    private final AuctionRepository auctionRepository;

    public AuctionRepositoryAdminAdapter(AuctionRepository auctionRepository) {
        this.auctionRepository = auctionRepository;
    }

    @Override
    public List<AuctionSummaryDto> findAllAuctionSummaries() {
        return auctionRepository.findAll().stream()
            .map(/* map Auction -> AuctionSummaryDto */)
            .toList();
    }
}
```

### Quang (ServerContext / wiring)

Sau khi `AuctionModule` va `AuctionRepository` da san sang, can wiring:

```java
UserRepository userRepository = AuthModule.init(this.connection, this.router);
AuctionRepository auctionRepo = AuctionModule.init(connection, router);
AdminModule.init(router, userRepository, new AuctionRepositoryAdminAdapter(auctionRepo));
```

Neu ben auction da implement truc tiep `AdminAuctionGateway`, co the truyen thang vao `AdminModule.init(...)`.

### Nguoi lam `user_management.fxml`

UI hien tai dang ve cac cot:

- full name
- status active/banned
- registered date
- action ban/unban

Nhung DTO hien co cua admin user moi chac chan:

- `id`
- `username`
- `email`
- `role`

Nghia la:
- branch nay **chua** noi man `user_management.fxml` vao data that
- day la diem chong cheo can duoc thong nhat truoc khi code tiep:
  - mo rong DTO
  - hoac gian luoc UI cho khop contract hien tai

### Nguoi lam `auction_management.fxml`

UI hien tai dang doi:

- seller display name
- start price
- top bid
- end time
- action cancel

Nhung `AuctionSummaryDto` hien da chot trong team-task-allocation chi chac:

- `auctionId`
- `itemId`
- `itemName`
- `itemCategory`
- `status`
- `currentHighestBid`
- `highestBidderId`
- `endTime`
- `sellerId`

Nghia la:
- branch nay **chua** noi man `auction_management.fxml` vao data that
- can thong nhat tiep:
  - co map sellerId -> seller name hay khong
  - co them start price vao DTO hay khong
  - admin cancel auction co nam trong scope backend branch khac hay khong

## 6. Nhung gioi han hien tai

- `AdminOverviewController` da dung data that, nhung `Recent Activity` va `Session Status` hien la summary placeholder.
- Ly do: current allocation chua dinh nghia DTO/activity feed rieng cho dashboard analytics.
- `user_management.fxml` va `auction_management.fxml` van la mock UI, chua co controller data-binding moi.
- `AdminHandler` hien van gom nhieu loi server ve `ADMIN_ACTION_FAILED`; neu team muon client map loi typed hon thi can chot them error-code convention.
- `UserRepository` hien chu yeu `printStackTrace()` va tra ve rong/false khi SQL loi; branch nay chua doi contract nay de tranh lan scope voi auth/payment.

## 7. Cach kiem tra sau khi dependency da du

1. Dang nhap bang tai khoan `ADMIN`
2. Mo `ADMIN_OVERVIEW`
3. Ky vong:
   - load duoc tong so user
   - load duoc tong so auction
   - hien duoc so auction dang `RUNNING`
4. Thu API delete user
   - admin xoa user thuong -> thanh cong
   - admin xoa admin -> bi chan
   - admin tu xoa chinh minh -> bi chan
   - user thuong goi delete -> bi chan
5. Thu login/register voi input thieu
   - khong goi len server
   - popup phai hien dung message validation, khong duoc vo man hinh loi he thong

## 8. Thu tu merge khuyen nghi

1. `dto`
2. branch cua Duy co `AuctionRepository` / mapping auction summary
3. `feature/admin-main`
4. branch cua Quang wiring `ServerContext`

Neu merge som hon, branch admin-main van an toan vi da tach dependency bang `AdminAuctionGateway`.

## 9. Ghi chu toi uu sau ra soat

- `AdminService.deleteUser(...)` da duoc rut gon:
  - tai su dung `requireAdmin(...)`
  - parse UUID qua helper rieng de message validation de doc hon
- `AdminOverviewController` da duoc rut gon luong async:
  - van goi 2 API song song
  - nhung hop nhat ket qua bang `thenCombine(...)` de code de doc va de maintain hon
- `BaseClientService` da co `extractFailure(...)`:
  - dung cho ca auth flow va admin flow
  - giup controller khong phai tu unwrap `CompletionException` bang tay moi noi
