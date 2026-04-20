# Plan: Authentication Vertical Slice Refactor

**Status:** Draft / Not Started
**Goal:** Implement a fully decoupled Client-Server authentication flow (Login/Register) using Sockets and JSON, following the [Shared Infrastructure Guideline](../plans/shared-infrastructure-guideline.md).

---

## Phase 1: Shared Protocol (`common`)
Establish the "Bridge" that both Client and Server understand.
- [ ] **Step 1.1:** Define `MessageType` enum (`LOGIN`, `REGISTER`).
- [ ] **Step 1.2:** Create `RequestMessage<T>` and `ResponseMessage<T>` envelopes (the "Message Envelope").
- [ ] **Step 1.3:** Create specific Payloads: `LoginRequest`, `RegisterRequest`, and `AuthResponse` (contains User info).

## Phase 2: Server Infrastructure (`server`)
Prepare the Server to receive and route messages correctly.
- [ ] **Step 2.1:** Create `UserRepository` (DAO) to handle JDBC/SQL. Extract this logic from the current `AuthService`.
- [ ] **Step 2.2:** Refactor `AuthService` to be "Pure Logic" (Business Rules only). It must depend on `UserRepository`.
- [ ] **Step 2.3:** Implement `AuthHandler` to bridge `RequestMessage` -> `AuthService`.
- [ ] **Step 2.4:** Implement `MessageRouter` to delegate `MessageType` to `AuthHandler`.
- [ ] **Step 2.5:** Update `ClientHandler` to read JSON lines and pass them to the `MessageRouter`.

## Phase 3: Client Infrastructure (`client`)
Prepare the Client to send messages and handle responses.
- [ ] **Step 3.1:** Update `ServerConnection` to use `BufferedReader`/`PrintWriter` (Text-based JSON) and Jackson.
- [ ] **Step 3.2:** Implement `AuthClientService` to wrap the socket logic:
    - Build Request DTO -> Send -> Wait for Response -> Return Result.
- [ ] **Step 3.3:** Refactor `SignInController` and `RegisterController`:
    - **CRITICAL:** Remove all `com.nhom1.auction.server.*` imports.
    - Call `AuthClientService` instead.

## Phase 4: Validation
- [ ] **Step 4.1:** Verify `REGISTER` flow: User created in `auction.db` via Socket.
- [ ] **Step 4.2:** Verify `LOGIN` flow: User logs in and receives correct Role from Server.

---

## Architectural Constraints
1. **JSON Protocol:** One JSON object per line. No mixed streams (Object vs Text).
2. **Standardization:** Use `username` (not `email`) to match current database schema.
3. **Strict Decoupling:** The `client` package MUST NOT import anything from the `server` package.
4. **Error Handling:** Services throw Exceptions; Handlers catch them and return JSON Error Codes.

---

## Next Action
**Phase 1: Shared Protocol.** 
We will begin by creating the `com.nhom1.auction.common.protocol` package and the basic message envelopes.
