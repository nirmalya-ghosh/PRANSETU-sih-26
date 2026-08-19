# PRANSETU System Architecture

## Scope

Odisha-first disaster communication and response platform, designed for later India-wide expansion.

## High-Level Flow

```text
Citizen Android App
      |
      +---- Internet ----> FastAPI ----> Supabase/PostGIS ----> EOC
      |
      +---- No Internet -> Nearby Relay -> Gateway -> Internet -> Backend

Automated Voice Call -> DTMF -> Telephony Webhook -> Backend -> EOC

Disaster data/alerts -> PRANSETU Intelligence -> risk/priority/situation insights -> EOC
```

## Android Responsibilities

- Localized UI
- Permissions and location
- SOS creation
- Local persistence
- Offline queue
- Nearby discovery/transport
- Store-carry-forward
- Gateway sync
- Delivery state
- Diagnostics

## Backend Responsibilities

- Authentication and authorization
- SOS ingestion
- Idempotency
- Relay/gateway synchronization
- IVR webhook processing
- Geospatial queries
- Safe-place verification data
- Audit events
- Government integration adapters
- Notifications

## Web EOC Responsibilities

- Live incident map
- Incident triage
- SOS state visibility
- Rescue priority
- Safe-place/shelter status
- Resource management
- IVR activity
- Analytics
- AI situation summaries

## Data Principles

- Canonical data is language-independent.
- Presentation text is localized at the client/UI layer.
- Original citizen-language content is preserved when translation is used.
- SOS ingestion is idempotent.
- Critical state transitions are auditable.

## Reliability Principles

- Store before send.
- Retry with bounds.
- Deduplicate by SOS ID.
- Use TTL/hop limits for relay packets.
- Acknowledge persistence/delivery explicitly.
- Never infer successful delivery from an attempted network operation.
