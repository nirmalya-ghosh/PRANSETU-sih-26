# PRANSETU Testing Strategy

## Rule

Critical functionality is not complete because it compiles. It is complete only after appropriate automated tests and real-device/service validation.

## Android

Test:

- app startup without internet
- SOS creation without internet
- local persistence
- app/process restart
- location freshness and accuracy metadata
- duplicate SOS handling
- queue/retry behavior
- localization switching
- relay packet validation
- TTL/hop limit
- acknowledgement handling

## Backend

Test:

- authentication/authorization
- schema validation
- idempotent SOS ingestion
- duplicate packet handling
- geospatial queries
- webhook verification
- retry/idempotency behavior
- audit records

## Relay Physical Tests

Minimum target scenarios:

1. A -> B
2. A -> B -> C
3. A -> B -> C -> Gateway
4. Duplicate packet
5. Gateway disappears during transfer
6. Device restarts with queued SOS
7. Internet returns after offline period
8. Multiple SOS packets in the same area

## IVR

Use a real telephony provider/API in integration testing where possible. Verify:

- call initiation
- answer
- language flow
- DTMF capture
- webhook authenticity
- backend persistence
- EOC update
- retry/failure handling

## AI

Evaluate classification and translation separately from authoritative disaster alerts. Preserve original inputs and record model/version metadata.
