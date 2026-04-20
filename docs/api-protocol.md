# Client-Server API Protocol

This document defines the communication protocol between the Client and Server. All communication happens via JSON messages over TCP Sockets.

---

## 1. Message Envelope
Every message sent over the socket must follow this basic structure to ensure the `MessageRouter` can properly identify and process it.

### 1.1 Request Envelope (Client -> Server)
```json
{
  "type": "MESSAGE_TYPE_ENUM",
  "requestId": "uuid-string",
  "payload": { ... }
}
```

### 1.2 Response Envelope (Server -> Client)
```json
{
  "requestId": "uuid-string",
  "success": true,
  "payload": { ... },
  "error": {
    "code": "ERROR_CODE_STRING",
    "message": "Human readable message"
  }
}
```

---

## 2. Authentication Features

### 2.1 `LOGIN`
**Request Payload:**
```json
{
  "email": "abc@gmail.com",
  "password": "plain_text_password"
}
```
**Response Payload:**
```json
{
  "user": {
    "id": "uuid",
    "email": "johndoe",
    "fullName": "John Doe",
    "role": "USER"
  }
}
```

### 2.2 `REGISTER`
**Request Payload:**
```json
{
  "email": "@",
  "password": "password123",
  "fullName": "New User",
  "role": "USER"
}
```

---

## 3. Auction Browsing (Explore & My Bids)

### 3.1 `LIST_AUCTIONS`
Used for both the **Explore** screen and **My Bids** screen by using different filters.

**Request Payload:**
```json
{
  "filter": {
    "status": "RUNNING", 
    "category": "ELECTRONICS",
    "ownerId": "uuid-string",
    "bidderId": "uuid-string",
    "searchText": "vintage"
  },
  "sort": {
    "field": "endTime",
    "order": "ASC"
  }
}
```

**Response Payload (`List<AuctionSummaryDTO>`):**
```json
{
  "auctions": [
    {
      "id": "uuid",
      "title": "Vintage Fender Guitar",
      "category": "ART",
      "currentBid": 5000.0,
      "endTime": "2026-04-22T15:00:00",
      "status": "RUNNING",
      "isWinning": true 
    }
  ]
}
```

### 3.2 `GET_USER_BID_STATS`
Used for the header on the **My Bids** screen.

**Response Payload:**
```json
{
  "activeBids": 7,
  "winning": 7,
  "losing": 0,
  "endingSoon": 3
}
```

---

## 4. Bidding Logic

### 4.1 `PLACE_BID`
**Request Payload:**
```json
{
  "auctionId": "uuid",
  "amount": 5500.0,
  "bidType": "MANUAL"
}
```

### 4.2 `AUTO_BID_CONFIG`
**Request Payload:**
```json
{
  "auctionId": "uuid",
  "maxAmount": 10000.0,
  "increment": 100.0
}
```

---

## 5. Error Codes Reference
Standardized error codes for consistent Client-side handling:

| Code | Description |
| :--- | :--- |
| `AUTH_INVALID_CREDENTIALS` | Wrong username or password |
| `BID_TOO_LOW` | Bid amount is less than current highest + increment |
| `AUCTION_CLOSED` | Attempted to bid on an auction that has ended |
| `FORBIDDEN_SELF_BID` | Seller trying to bid on their own item |
| `INTERNAL_SERVER_ERROR` | Unexpected crash on server |
