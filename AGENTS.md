# PRANSETU Development Rules

## Project

PRANSETU is an Odisha-first, India-scalable disaster communication and emergency response platform. It is designed to preserve citizen-to-government communication during disasters when normal connectivity is degraded.

The system has three primary surfaces:

- Android citizen application
- Emergency Operations Centre web platform
- Backend and integrations

## Core Principles

1. Reliability over feature count.
2. Offline-first for critical citizen functions.
3. Never fake connectivity, delivery, relay, GPS, IVR or government integration.
4. Never claim an SOS was delivered without an explicit acknowledgement.
5. Persist critical data locally before attempting transmission.
6. Every SOS must have a globally unique ID and be idempotent end-to-end.
7. Relay packets must use TTL, hop limits, deduplication and acknowledgement.
8. Preserve the original citizen data and language for auditability.
9. Do not expose secrets, API keys or credentials in source code.
10. Critical functionality must be tested on physical Android devices and with failure scenarios.
11. Do not make unrelated changes.
12. Prefer small, reversible changes and stable contracts.

## Android Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Kotlin Coroutines and Flow
- Android Location APIs
- A supported nearby-device transport such as Nearby Connections, subject to physical-device validation

## Backend Stack

- FastAPI
- Supabase PostgreSQL
- PostGIS for geospatial operations
- Realtime where appropriate

## Web

The EOC web application provides operational visibility, incident management, mapping, shelter/safe-place information, resource tracking, verification workflows and analytics.

## Multilingual Requirement

PRANSETU must be multilingual from the beginning.

Initial Odisha deployment:

- English
- Odia
- Hindi

The architecture must be extensible to all Indian languages and scripts.

### Localization Rules

1. Never hard-code user-facing strings in application logic.
2. Use stable localization keys.
3. Store language-independent enums/codes in databases.
4. Store localized presentation separately from canonical data.
5. Persist the user's language preference.
6. Allow language switching without reinstalling the application.
7. Emergency instructions must have verified translations.
8. Citizen-facing IVR must support localized language flows.
9. Preserve the original citizen message alongside any translation.
10. AI translation must never overwrite canonical citizen data.
11. Support locale-aware date, time and number formatting.
12. Architecturally support right-to-left languages such as Urdu.
13. Critical emergency terminology must have human-reviewed translations.
14. Machine translation must not silently replace verified emergency wording.

## SOS Canonical Model

An SOS may contain:

- sos_id
- protocol_version
- created_at
- source
- device/reference identifier as appropriate
- latitude
- longitude
- location_timestamp
- location_accuracy
- severity_code
- people_count
- medical_required
- vulnerability indicators where explicitly supplied
- hop_count
- ttl
- delivery_state
- acknowledgement state

The database stores canonical meaning, not translated UI labels.

## SOS Lifecycle

CREATED -> STORED -> QUEUED -> RELAYING -> GATEWAY_RECEIVED -> SERVER_RECEIVED -> ACKNOWLEDGED -> CLOSED

Failure and retry states must be explicit.

## Offline Relay

Target chain:

Phone A -> Phone B -> Phone C -> Internet-capable Gateway -> Backend -> EOC

Do not assume Bluetooth, Wi-Fi or any particular Android transport works in every environment. The transport must be implemented against documented Android capabilities and validated with physical devices.

Relay requirements:

- local persistence
- bounded retries
- TTL
- hop limit
- deduplication
- packet integrity
- acknowledgement
- replay protection where applicable
- privacy-aware metadata
- eventual delivery when a gateway becomes reachable

## Voice/IVR

The production design must support:

Incoming automated call -> language-aware IVR -> DTMF response -> secure webhook -> backend -> database -> EOC

The hackathon implementation may use a real third-party telephony API for demonstration, but must not claim that it is an official government service unless an authorized government integration exists.

## AI

AI capabilities may include:

- multilingual language detection and assistance
- SOS/incident classification
- incident clustering
- rescue prioritization
- cascading/"domino effect" risk analysis
- situation summarization

AI recommendations must be explainable to operators where feasible and must not silently replace authoritative disaster alerts or human operational decisions.

## Development Workflow

Plan -> Implement -> Build -> Test -> Review -> Physical Validation -> Commit

Do not implement the entire platform in one task.

For critical communication features, success must be demonstrated with real devices/services, not simulated logs alone.

## APK Release & Publishing Policy

- **Target Repository**: `https://github.com/nirmalya-ghosh/PRANSETU-sih-26`
- **Rule**: Every time code updates or builds are completed, build the APK (`./gradlew assembleDebug`) and publish all required APK releases to `https://github.com/nirmalya-ghosh/PRANSETU-sih-26`.
