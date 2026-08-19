# PRANSETU Implementation Roadmap

## Phase 0 - Foundation

- Repository rules
- Documentation
- Localization architecture
- CI baseline

## Phase 1 - Android Foundation

- Kotlin/Compose app
- Navigation
- Theme
- English/Odia/Hindi localization
- Home/SOS UI
- diagnostics

## Phase 2 - Real Location

- permissions
- current location
- last-known location
- timestamp/accuracy

## Phase 3 - Offline SOS Engine

- Room
- canonical SOS model
- local queue
- lifecycle/state machine
- restart persistence

## Phase 4 - Online Synchronization

- FastAPI
- Supabase/PostGIS
- authenticated API
- idempotent ingestion
- acknowledgements

## Phase 5 - Nearby Relay

- device discovery
- A -> B
- packet transfer
- deduplication
- TTL/hop limit
- acknowledgement

## Phase 6 - Multi-hop Gateway

- A -> B -> C -> Gateway
- internet synchronization
- failure/retry scenarios

## Phase 7 - Voice/IVR

- real telephony API
- multilingual IVR
- DTMF
- secure webhook
- SOS/safe-place verification workflows

## Phase 8 - EOC Web

- live map
- incident management
- shelters/safe places
- resources
- analytics

## Phase 9 - PRANSETU Intelligence

- multilingual intelligence
- incident classification
- clustering
- rescue priority
- cascading-risk analysis
- situation summaries

## Phase 10 - Odisha to India

Expand localization, operational configuration, integrations and disaster profiles beyond Odisha.
