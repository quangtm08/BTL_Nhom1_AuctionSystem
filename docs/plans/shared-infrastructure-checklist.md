# Shared Infrastructure Checklist

Use this checklist to decide whether the project is ready for parallel feature development.

The project does not need to be perfect before the team starts splitting work.
It does need to be stable enough that people can work without breaking each other constantly.

## A. Architecture and boundaries

- [ ] The team agrees that only the server accesses the database.
- [ ] The team agrees on the rough server flow:
  `ClientHandler -> MessageRouter -> Handler -> Service -> Repository -> Database`
- [ ] The team agrees on where controllers, DTOs, services, repositories, and socket code should live.
- [ ] The team has written down the responsibility of each layer.

## B. Message contract

- [ ] There is one shared request message envelope.
- [ ] There is one shared response message envelope.
- [ ] There is one shared approach for success and error responses.
- [ ] The first core message types are listed and named consistently.
- [ ] The team agrees who can define detailed payloads for each feature area.

## C. Core feature contracts

- [ ] Login and register request/response shapes are defined.
- [ ] Auction listing request/response shape is defined.
- [ ] Auction detail request/response shape is defined.
- [ ] Place bid request/response shape is defined.
- [ ] Create auction request/response shape is defined.
- [ ] Cancel auction request/response shape is defined.

## D. Database foundation

- [ ] The initial schema for `users`, `items`, `auctions`, and `bids` is documented.
- [ ] Keys and relationships are clear enough that multiple people can code against them.
- [ ] The team understands which fields are temporary and which are part of the stable design.
- [ ] The current SQLite decisions do not make later PostgreSQL migration unnecessarily hard.

## E. Business rules

- [ ] Auction states and transitions are written down clearly.
- [ ] Role permissions are written down clearly.
- [ ] Bid validation rules are written down clearly.
- [ ] Concurrency-sensitive behavior has one agreed implementation strategy.
- [ ] The team knows which logic belongs in services rather than controllers or repositories.

## F. Minimal working flow

- [ ] A user can send a request from the client to the server.
- [ ] The server can parse and route the message.
- [ ] A handler can call a service.
- [ ] A service can call a repository.
- [ ] A repository can read or write data.
- [ ] The server can send a structured response back to the client.
- [ ] At least one thin end-to-end flow works:
  `login/register -> list auctions -> auction detail -> place bid`

## G. Team coordination

- [ ] Each of the 4 members has a clear short-term ownership area.
- [ ] Changes to shared contracts must be discussed before being merged.
- [ ] The team has a PR or review habit before code reaches the shared branch.
- [ ] The team understands that weekly lecturer milestones are for pacing, not strict ownership boundaries.

## Ready-to-split rule

If most of the boxes above are checked, especially sections A through F, the team is ready to split into more independent feature work.

If many boxes are still unchecked, the team should stay focused on shared infrastructure a bit longer before assigning full vertical ownership.

## Suggested note for your team meeting

When using this checklist, avoid asking:

- "Is the whole architecture perfect?"

Instead ask:

- "Is the shared foundation stable enough that people can work in parallel with low confusion?"

That is the real decision this checklist is meant to support.

