# PRANSETU SOS Protocol

## Canonical Packet

Every SOS packet should contain, as applicable:

- sos_id
- protocol_version
- created_at
- source_type
- device_reference
- latitude
- longitude
- location_timestamp
- location_accuracy
- severity_code
- people_count
- medical_required
- hop_count
- ttl
- delivery_state
- acknowledgement state

## Lifecycle

```text
CREATED
  -> STORED
  -> QUEUED
  -> RELAYING
  -> GATEWAY_RECEIVED
  -> SERVER_RECEIVED
  -> ACKNOWLEDGED
  -> CLOSED
```

Failure/retry states must be explicit and bounded.

## Relay Rules

1. Persist locally before transmission.
2. Use a globally unique SOS ID.
3. Receivers must deduplicate by SOS ID.
4. Forwarding increments hop_count.
5. TTL/hop limit prevents infinite circulation.
6. Receivers acknowledge successful local persistence.
7. Senders retain packets until appropriate acknowledgement.
8. Server ingestion is idempotent.
9. Last-known location is always labelled with its timestamp and must not be represented as a live GPS fix.
10. Relay metadata should be minimized to protect citizen privacy.
