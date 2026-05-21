# Controller Audit Checklist

## Goal

Clean up and standardize client/admin controllers without changing behavior first. Controllers should stay focused on UI state, user interaction, and navigation. Network calls, request construction, server protocol details, push parsing, and shared formatting/error behavior should move behind services or small shared helpers.

## Scope

- User controllers: `src/main/java/com/nhom1/auction/client/user/controller`
- User component controllers: `src/main/java/com/nhom1/auction/client/user/controller/components`
- Admin controllers: `src/main/java/com/nhom1/auction/client/admin/controller`
- Supporting client services/connection only when needed to remove controller coupling.

## Priority 1: Correctness And Architecture
- [x] Move raw push parsing out of controllers.
  - Current issue: controllers use `ObjectMapper`, `JsonNode`, and manual payload extraction.
  - Affected files: `AuctionBrowseController`, `AuctionDetailController`, `MyListingsController`.
  - Target outcome: controllers receive typed push/event objects or already-processed callbacks.

- [x] Remove direct `ServerConnection` usage for push subscriptions.
  - Current issue: controllers reached into socket/protocol infrastructure for realtime push.
  - Affected files: admin management controllers, browse/detail push registration, my listings push registration.
  - Target outcome: controllers receive typed push events through `ClientPushService`.

- [x] Remove `RequestMessage` and `MessageType` construction from controllers.
  - Current issue: `MyListingsController` builds requests directly for listing load and delete.
  - Target outcome: request construction lives in a client service.

## Priority 2: User-Visible Reliability

- [ ] Standardize async exception extraction.
  - Current issue: some controllers use `ex.getCause().getMessage()` directly, which can throw null pointer errors.
  - Affected files: `AuctionBrowseController`, `MyBidsController`, plus any similar future cases.
  - Target outcome: controllers use `BaseClientService.extractFailure(...)` or a shared UI error helper.

- [ ] Standardize user-facing error display.
  - Current issue: some failures go to labels, some to custom alert modals, some only to `System.err`.
  - Affected files: auth, browse, bids, listings, detail, admin, payment.
  - Target outcome: predictable error UI by screen type.

- [ ] Fix logout/session clearing flow.
  - Current issue: sidebars navigate to sign-in but do not clear `AppContext`.
  - Affected files: `UserSidebarController`, `AdminSidebarController`.
  - Target outcome: logout always clears the current session before navigation.

- [ ] Guard nullable IDs before calling `.equals(...)`.
  - Current issue: row actions assume IDs are non-null.
  - Affected files: `AuctionManagementController`, `UserManagementController`, `PaymentController`.
  - Target outcome: no row-render crash from incomplete DTOs.

- [ ] Make auction detail image loading non-blocking or timeout-safe.
  - Current issue: waiting for image load can hang if listeners miss an already-loaded image.
  - Affected file: `AuctionDetailController`.
  - Target outcome: detail content renders even if image loading is slow or broken.

## Priority 3: Separation Of Concerns

- [ ] Move browse filtering logic into a service/view-model.
  - Current issue: `AuctionBrowseController` combines auctions with my bids, filters own auctions, filters already-bid auctions, and mutates selected auction state.
  - Target outcome: controller receives a ready-to-render list.

- [ ] Add service methods for my listings.
  - Current issue: `MyListingsController` owns list/delete request details.
  - Target methods: `listMyListings()` and `deleteListing(auctionId)`.

- [ ] Move delete confirmation flow into a reusable helper or small UI component.
  - Current issue: `MyListingsController` has a large inline alert setup.
  - Target outcome: controller action is readable and focused.

- [ ] Avoid direct concrete service construction where practical.
  - Current issue: controllers instantiate services with `new`.
  - Target outcome: controller dependencies are easier to substitute in tests, even if using simple constructors/factories.

## Priority 4: Standardization And Cleanup

- [ ] Create shared money/date/time/status formatting utilities.
  - Current issue: `formatMoney`, `formatTimeLeft`, status labels, and date formats are duplicated and inconsistent.
  - Target outcome: consistent display across browse, bids, listings, detail, admin, and payment.

- [ ] Normalize empty/loading/success/failure states.
  - Current issue: each controller invents its own loading and empty text behavior.
  - Target outcome: consistent screen states and easier testing.

- [ ] Split large programmatic row builders.
  - Current issue: admin/payment/detail controllers manually create many JavaFX nodes inline.
  - Target outcome: reusable row factories or FXML components for repeated rows.

- [ ] Remove unused imports and dead comments.
  - Current issue: `CreateAuctionController` imports protocol classes it no longer uses; several comments describe old behavior.
  - Target outcome: controllers read cleanly and do not imply stale architecture.

- [ ] Reformat dense one-line controller methods.
  - Current issue: several methods hide logic in compressed one-liners.
  - Affected files: `MyListingsController`, `MyBidsController`, `AuctionBrowseController`.
  - Target outcome: readable methods before deeper refactors.

## Suggested Work Order

1. Push subscription model and lifecycle cleanup.
2. Typed push/event service.
3. `MyListingsController` service boundary cleanup.
4. Shared error handling and null-safety pass.
5. Logout/session cleanup.
6. Shared formatting utilities.
7. Browse filtering service/view-model.
8. UI component/row factory cleanup.

## Done Criteria

- [ ] No controller imports `ServerConnection`.
- [ ] No controller imports `RequestMessage`.
- [ ] No controller imports `MessageType` unless it is strictly UI-level event metadata.
- [ ] No controller manually parses push JSON.
- [ ] Push events support multiple subscribers.
- [ ] Push subscriptions can be removed or scoped to the active view.
- [ ] Async errors are extracted safely.
- [ ] User-facing error display is consistent per screen type.
- [ ] Logout clears session state.
- [ ] Formatting output is consistent across user and admin screens.
