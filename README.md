<p align="center">
  <img src="PRANSETU(android)/docs/assets/pransetu_logo.png" width="140" height="140" alt="PRANSETU Logo" style="border-radius: 20px;" />
</p>

<h1 align="center">PRANSETU &nbsp;(ପ୍ରାଣସେତୁ)</h1>

<p align="center">
  <strong>Resilient Emergency Communication &amp; Zero-Cellular Disaster Mesh Network</strong><br/>
  <em>Bridging every life to help — even when every network fails.</em>
</p>

<p align="center">
  <a href="PRANSETU(android)/apk/PRANSETU-latest.apk">
    <img src="https://img.shields.io/badge/Download-Android%20APK%20v1.0-brightgreen?style=for-the-badge&logo=android&logoColor=white" alt="Download APK" />
  </a>
  &nbsp;
  <img src="https://img.shields.io/badge/Platform-Android%20%7C%20Web%20EOC-blue?style=for-the-badge&logo=react" alt="Platform" />
  &nbsp;
  <img src="https://img.shields.io/badge/Built%20For-SIH%202026-orange?style=for-the-badge" alt="SIH 2026" />
  &nbsp;
  <img src="https://img.shields.io/badge/Backend-FastAPI%20%2B%20Supabase-purple?style=for-the-badge&logo=fastapi" alt="Backend" />
</p>

---

## Table of Contents

1. [Overview](#1-overview)
2. [System Architecture](#2-system-architecture)
3. [Android Citizen App](#3-android-citizen-app)
   - [Tech Stack](#31-android-tech-stack)
   - [Offline Mesh Protocol](#32-offline-mesh-protocol--store-and-forward-relay)
   - [SOS State Machine](#33-sos-dispatch-state-machine)
   - [Core Modules](#34-core-android-modules)
4. [Web Emergency Operations Center (EOC)](#4-web-emergency-operations-center-eoc)
   - [Tech Stack](#41-web-tech-stack)
   - [Command Center](#42-command-center--real-time-kpi-engine)
   - [GIS Mission Map](#43-gis-mission-map)
   - [AI Priority Engine](#44-ai-priority-triage-engine)
   - [IVR Voice Campaign System](#45-ivr-voice-campaign-system)
5. [Backend API](#5-backend-api)
   - [Tech Stack & Architecture](#51-backend-tech-stack--architecture)
   - [Data Model](#52-data-model--geospatial-schema)
   - [API Reference](#53-api-reference)
6. [AI / PRANSETU Intelligence](#6-ai--pransetu-intelligence-module)
7. [Security Model](#7-security-model)
8. [Multilingual Architecture](#8-multilingual-architecture)
9. [Repository Structure](#9-repository-structure)
10. [Development Setup](#10-development-setup)
    - [Android](#101-android-setup)
    - [Web EOC](#102-web-eoc-setup)
    - [Backend](#103-backend-setup)
11. [CI / CD & Deployment](#11-ci--cd--deployment)
12. [Development Philosophy](#12-development-philosophy)
13. [License](#13-license)

---

## 1. Overview

**PRANSETU** *(ପ୍ରାଣସେତୁ — "Bridge of Life")* is an Odisha-first, India-scalable, full-stack disaster management platform engineered to maintain citizen-to-government emergency communication even under complete cellular and internet blackout conditions.

The system is composed of three tightly integrated subsystems:

| Subsystem | Role | Primary Users |
|---|---|---|
| **Android Citizen App** | SOS dispatch, family check-in, offline mesh relay, shelter navigation | Citizens, field responders |
| **Web EOC Dashboard** | Real-time situational awareness, resource command, AI triage, IVR coordination | Duty officers, OSDMA, NDRF |
| **FastAPI Backend** | SOS ingestion, geospatial indexing, IVR webhook, RBAC, event streaming | Services-to-services |

The platform has been designed around a **resilience-first** engineering principle: every critical feature degrades gracefully and continues to function across zero-connectivity, partial-connectivity, and fully-online operational modes.

---

## 2. System Architecture

```
┌──────────────────────────────────────────────────────────────────────────┐
│                        PRANSETU SYSTEM TOPOLOGY                          │
│                                                                          │
│   ┌──────────────┐     Bluetooth/WiFi-Direct     ┌──────────────────┐   │
│   │  Android     │◄────── P2P Mesh Relay ────────►│  Android (Peer)  │   │
│   │  Citizen App │                                │  (Internet Gate) │   │
│   │  (Offline)   │                                └────────┬─────────┘   │
│   └──────────────┘                                         │ HTTPS       │
│         │ SMS Fallback                                      ▼             │
│         │                                        ┌──────────────────┐   │
│         └───────────────────────────────────────►│  FastAPI Backend  │   │
│                                                  │  (Supabase/PG)   │   │
│   ┌──────────────┐  Supabase Realtime WebSocket  └────────┬─────────┘   │
│   │  Web EOC     │◄──────────────────────────────────────►│             │
│   │  Dashboard   │                                         │ PostGIS     │
│   │  (React TSX) │     IVR Webhook (Twilio/Exotel)        │ Realtime    │
│   └──────────────┘◄─────────────────────────────────────── ┘             │
│                                                                          │
│   LoRa Repeater Nodes ──► MQTT/HTTP ──► Backend (SNR, Latency Telemetry)│
└──────────────────────────────────────────────────────────────────────────┘
```

**Data flow invariant:** An SOS packet originated offline on an Android device MUST reach the EOC backend via at least one of the following channels, in priority order:

1. **Direct cellular uplink** (online mode) → Supabase PostgreSQL
2. **Store-and-forward P2P mesh** (Bluetooth/Wi-Fi Direct via Google Nearby Connections API) → internet-capable gateway peer → Supabase
3. **SMS fallback** → IVR webhook → backend ingestion pipeline

---

## 3. Android Citizen App

### 3.1 Android Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.x |
| Architecture | MVVM + Clean Architecture (Unidirectional Data Flow) |
| UI Framework | Jetpack Compose (Material 3) |
| State Management | Kotlin `StateFlow` + `combine()` operators |
| Async / Concurrency | Kotlin Coroutines + `viewModelScope` |
| P2P Mesh Transport | Google Nearby Connections API (Bluetooth + Wi-Fi Direct) |
| Remote Data | Supabase Kotlin SDK (REST + Realtime) |
| Authentication | Firebase Phone Auth (SMS OTP, auto-retrieval, graceful fallback) |
| Local Persistence | Room Database (offline SOS queue, citizen profile) |
| Location | Android Fused Location Provider, GPS sensor |
| Hardware Sensors | `SensorManager` (barometer — `Sensor.TYPE_PRESSURE`) |
| Dependency Injection | Manual DI via `AppViewModelFactory` |
| Navigation | Jetpack Navigation Compose |
| Localization | Android `strings.xml` (EN / Odia / Hindi), runtime language switching |

### 3.2 Offline Mesh Protocol — Store-and-Forward Relay

PRANSETU implements a **TTL-bounded, hop-limited opportunistic relay** using the Google Nearby Connections API across Bluetooth and Wi-Fi Direct channels simultaneously.

**Packet Structure:**

```
SOSPacket {
  packet_id:          UUID v4          // Global deduplication key
  origin_device_id:   String           // Originator's Nearby endpoint name
  latitude:           Double?          // GPS coordinate (nullable — degraded mode)
  longitude:          Double?
  location_accuracy:  Float?           // Horizontal accuracy (metres)
  location_timestamp: Long             // Unix epoch (ms)
  message:            String
  user_name:          String?
  user_phone:         String?
  ttl:                Int              // Remaining hops (decremented per relay)
  created_at_ms:      Long
  acknowledged:       Boolean          // Set true by EOC after delivery confirmed
}
```

**Relay Engine Logic (`NearbyConnectionsManager`):**

1. Originator creates an `SOSPacket` (TTL = 5) and persists it to Room DB.
2. Packet is serialised to JSON and broadcast to all currently-connected Nearby peers.
3. Each relay peer checks its own deduplication log (`packet_id`). If unseen:
   - Decrements TTL; drops if TTL ≤ 0.
   - Persists locally.
   - Forwards to its own connected peers.
   - Attempts direct backend uplink if it has internet access.
4. Power-save mode (battery < 15%) automatically throttles Nearby advertising frequency via `setPowerSaveMode()`.

**Deduplication guarantee:** Packet IDs are persisted in a Room table. A relay node will never re-broadcast a packet it has already seen, preventing broadcast storms.

### 3.3 SOS Dispatch State Machine

The `HomeViewModel` implements an explicit dispatch state machine:

```
[USER PRESSES SOS]
        │
        ▼
[Acquire GPS fix]  ─── timeout ──► [Use last-known / null]
        │
        ▼
[Build SosCanonicalModel]
        │
        ├─── isOnline = true ──► [Direct HTTP → Supabase REST]
        │                              │ success ──► [EOC ACK]
        │                              │ failure ──► [Fallback to Mesh]
        │
        └─── isOnline = false ──► [Room DB persist]
                                       └──► [Nearby broadcast to all peers]
                                                  └──► [Gateway peer → Supabase]
```

### 3.4 Core Android Modules

| Module | Package | Responsibility |
|---|---|---|
| `auth` | `core.auth` | Firebase Phone Auth OTP flow, ForceResendingToken management, demo bypass |
| `network` | `core.network` | `NetworkConnectivityObserver` (Flow-based), Supabase client singleton, system alert service |
| `nearby` | `core.network.nearby` | `NearbyConnectionsManager` — P2P mesh advertising, discovery, payload relay |
| `sync` | `core.network.sync` | Offline queue drain — uploads Room-persisted SOS packets when connectivity is restored |
| `sos` | `core.sos` | `SosRepository`, `SosCanonicalModel`, idempotent submission logic |
| `location` | `core.location` | `LocationProvider`, `LocationAvailabilityObserver` (GPS status Flow) |
| `sensor` | `core.sensor` | Barometric pressure sensor reader — real-time `hPa` telemetry |
| `battery` | `core.battery` | `BatteryMonitor` — battery % and power-save mode state (Flow) |
| `hardware` | `core.hardware` | Vibration haptic impulse and device capability detection |
| `relay` | `core.relay` | Packet serialisation, TTL management, deduplication log |
| `localization` | `core.localization` | `LanguagePreferencesRepository` — runtime locale persistence (DataStore) |
| `tts` | `core.tts` | Text-to-speech for accessibility and low-literacy user support |
| `ai` | `core.ai` | On-device inference hooks (future: offline classification) |
| `dispatch` | `core.dispatch` | Shelter compass HUD bearing calculations, azimuth vector rendering |

---

## 4. Web Emergency Operations Center (EOC)

### 4.1 Web Tech Stack

| Layer | Technology | Version |
|---|---|---|
| Framework | React | 19.x |
| Language | TypeScript | 6.0 |
| Build Tool | Vite | 8.x |
| Styling | Tailwind CSS | v4 |
| GIS / Mapping | Leaflet + React-Leaflet | 1.9 / 5.0 |
| Basemaps | CartoDB Dark Matter, Esri World Imagery | — |
| Real-time | Supabase JS Client (WebSocket Realtime) | 2.x |
| State Management | React Context + `useReducer` (`EOCContext`) | — |
| Audio Engine | Web Audio API (tactical siren synthesizer) | Browser-native |
| Icons | Material Symbols (Google Fonts CDN) | — |
| Linter | oxlint | 1.75 |
| Deployment | Vercel + GitHub Actions | — |

### 4.2 Command Center — Real-Time KPI Engine

The `CommandCenter` component is the primary situational awareness dashboard. It polls the `/api/v1/command-center/kpis` endpoint with **exponential back-off** (3 s normal → 5 s on failure → 30 s when backend is confirmed offline via `isBackendOffline()`), and live-updates the following KPI surfaces:

| KPI Group | Metrics Tracked |
|---|---|
| **SOS Telemetry** | `active_sos`, `critical_sos`, `assistance_required`, `pending_synchronization`, `average_sos_delivery_time` |
| **Population** | `safe_confirmed`, `unaccounted`, `total_affected_people`, `active_incidents` |
| **Shelter Network** | `open_shelters`, `shelter_occupancy_percent`, `total_shelter_capacity`, `total_shelter_occupancy` |
| **Logistics Fleet** | `available_ambulances`, `dispatched_ambulances`, `available_rescue_teams`, `active_rescue_teams`, `available_boats`, `available_medical_teams` |

A pulsing `bg-secondary` indicator signals live backend connectivity; it degrades to `"Seed data — API offline"` state automatically without operator intervention.

### 4.3 GIS Mission Map

The `InteractiveEOCMap` component renders a full-canvas Leaflet map with the following operational layers:

| Layer | Type | Description |
|---|---|---|
| **Flood Surge Zones** | GeoJSON Polygon | Coastal inundation extents (dynamic opacity) |
| **Evacuation Corridors** | GeoJSON LineString | NH-316 and Marine Drive routes with directional arrows |
| **Cyclone Shelters** | Point markers | 42 monitored shelters — colour-coded by occupancy tier |
| **Active Rescue Units** | Animated markers | Real-time fleet positions |
| **SOS Distress Beacons** | Pulsating circle markers | Click-to-expand incident popup with triage metadata |

**Basemap modes:** `dark` (CartoDB Dark Matter), `light` (CartoDB Positron), `satellite` (Esri World Imagery) — toggled via the `onMapTypeToggle` callback without re-mounting the map.

**AI Route GNN overlay:** Triggers the `AIRouteInspector` modal, which performs graph-neural-network-based route viability analysis and displays SNR/latency metrics for each LoRa relay hop along the selected corridor.

**Cascading Domino Risk modal:** The `DisasterDominoEffect` component models second-order infrastructure cascade failures (e.g., shelter generator loss → communication blackout) and renders a risk propagation graph.

### 4.4 AI Priority Triage Engine

The triage scoring system computes a deterministic risk score (0–100) for each active SOS signal. The score is composed of three weighted factors:

| Factor | Weight | Computation |
|---|---|---|
| **Medical urgency** | Up to 40 pts | +40 if `medicalRequired = true` |
| **Cluster density** | Up to 30 pts | `min(30, peopleCount × 2.5)` |
| **Relay propagation delay** | Up to 30 pts | `min(30, hopCount × 8)` |

The circular gauge SVG is computed directly from `triage.total` via `strokeDasharray`, ensuring the gauge and the breakdown table are **always mathematically consistent** (a previous implementation had a hardcoded 98% clip diverging from the 94-point sum — this has been corrected).

Signals above 80 points render in `text-error`; below in `text-tertiary`. The highest-priority signal drives the "Deploy" CTA, pre-populating the `RescueDispatchModal` with its incident ID.

### 4.5 IVR Voice Campaign System

The `VoiceCampaigns` component interfaces with the backend IVR pipeline to execute mass citizen welfare check-ins. Campaign lifecycle:

```
[LAUNCH] → [DIALLING: iterating registered_citizens table]
         → [IN PROGRESS: collecting DTMF responses via webhook]
         → [COMPLETE / ABORTED]
```

**DTMF response schema:**

| Key | Meaning | EOC Action |
|---|---|---|
| `1` | Confirmed Safe | Increment `safeCount`; mark citizen confirmed |
| `2` | Logistical Aid Needed | Increment `foodWaterCount`; flag for supply dispatch |
| `3` | Critical — Trapped / Medical | Immediately injects an emergency SOS beacon onto the GIS mission map |
| `4` | Medical Emergency | Increment `medicalCount`; escalate to nearest ALS ambulance |

Live campaign reach progress is displayed as a progress bar: `answeredCount / totalReach × 100%`. The `EOCContext.recordDTMF()` function updates the campaign state synchronously for demo/testing purposes; in production, updates arrive via Supabase Realtime.

**IVR audio flow:**

```
Call Initiated (Twilio/Exotel)
    → TTS: "Attention. This is an emergency broadcast from PRANSETU..."
    → Language selection (EN / Odia / Hindi)
    → Structured welfare questions
    → DTMF keypad gathering
    → Webhook POST → /api/v1/voice/webhook
    → Backend ingestion → Supabase → EOC Realtime event
```

---

## 5. Backend API

### 5.1 Backend Tech Stack & Architecture

| Component | Technology |
|---|---|
| Web Framework | FastAPI ≥ 0.109 (async, OpenAPI auto-docs) |
| ASGI Server | Uvicorn ≥ 0.27 |
| Data Validation | Pydantic v2 |
| Database | Supabase PostgreSQL (PostGIS extension for geospatial) |
| ORM / Client | `supabase-py` ≥ 2.3 |
| Auth / RBAC | PyJWT ≥ 2.8 — signed token validation, RBAC permission decorators |
| IVR Integration | Twilio ≥ 9.0 (production), `MockExotelProvider` (development) |
| Test Framework | pytest ≥ 8.0 + HTTPX async test client |
| Config | `pydantic-settings` + `.env` (validated schema, no raw `os.environ`) |

**Architecture pattern:** The backend follows a layered FastAPI router structure. All routers are mounted with a versioned prefix (`/api/v1/`). Each endpoint uses `Depends()` injection for both the Supabase client (`get_supabase_client`) and RBAC enforcement (`require_permissions`).

### 5.2 Data Model & Geospatial Schema

**`registered_citizens`**

```sql
CREATE TABLE registered_citizens (
  id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  phone_number    TEXT NOT NULL UNIQUE,
  full_name       TEXT,
  firebase_uid    TEXT,
  registered_at   TIMESTAMPTZ DEFAULT now()
);
```

**`sos_signals`** (core ingestion table)

```sql
CREATE TABLE sos_signals (
  id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  location            GEOGRAPHY(POINT, 4326),  -- PostGIS spatial index
  location_accuracy   FLOAT,
  message             TEXT,
  user_name           TEXT,
  user_phone          TEXT,
  user_email          TEXT,
  device_identifier   TEXT,
  hop_count           INT DEFAULT 0,
  status              TEXT DEFAULT 'active',   -- active | acknowledged | resolved
  severity            TEXT DEFAULT 'MEDIUM',
  medical_required    BOOLEAN DEFAULT false,
  people_count        INT DEFAULT 1,
  received_at         TIMESTAMPTZ DEFAULT now(),
  acknowledged_by     TEXT,
  acknowledged_at     TIMESTAMPTZ
);

CREATE INDEX idx_sos_location ON sos_signals USING GIST (location);
CREATE INDEX idx_sos_status   ON sos_signals (status);
```

**`operational_events`** (Supabase Realtime event bus)

```sql
CREATE TABLE operational_events (
  event_id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  event_type         TEXT NOT NULL,
  sos_id             UUID REFERENCES sos_signals(id),
  device_id          TEXT,
  source             TEXT,
  payload            JSONB,
  sequence           BIGINT GENERATED ALWAYS AS IDENTITY,
  occurred_at        TIMESTAMPTZ DEFAULT now(),
  server_received_at TIMESTAMPTZ DEFAULT now()
);
```

The EOC subscribes to `operational_events` via Supabase Realtime PostgreSQL change listeners (`INSERT` events) — this is what powers the live **PRANSETU Real-Time Operational Event Bus** widget in the Command Center.

### 5.3 API Reference

| Method | Endpoint | Auth | Description |
|---|---|---|---|
| `POST` | `/api/v1/sos/` | Public (rate-limited) | Ingest a new SOS signal; idempotent by `device_identifier + timestamp` |
| `GET` | `/api/v1/sos/` | `SOS_VIEW` | List active SOS signals with geospatial proximity sort |
| `PATCH` | `/api/v1/sos/{id}/acknowledge` | `SOS_MANAGE` | Acknowledge and update signal status |
| `GET` | `/api/v1/command-center/kpis` | `SOS_VIEW` | Aggregate KPI snapshot (counts, shelter stats, fleet availability) |
| `GET` | `/api/v1/citizens/` | `SOS_VIEW` | List registered citizens (ordered by `registered_at DESC`) |
| `POST` | `/api/v1/voice/test-call` | `SOS_VIEW` | Initiate a single test IVR call to a phone number |
| `POST` | `/api/v1/voice/broadcast-call` | `SOS_VIEW` | Broadcast IVR welfare check to all registered citizens |
| `POST` | `/api/v1/voice/webhook` | Signed (Twilio) | Receive DTMF response from IVR provider; injects beacon if key `3` |
| `GET` | `/api/v1/shelters/` | `SOS_VIEW` | Shelter network status — occupancy, water, generator telemetry |
| `GET` | `/api/v1/resources/` | `SOS_VIEW` | Fleet resource availability (ambulances, rescue teams, boats) |

---

## 6. AI / PRANSETU Intelligence Module

The AI layer provides decision-support tooling for EOC operators. It is explicitly **non-authoritative** — it augments human judgment and never silently replaces emergency alerts or human decisions.

| Capability | Method | Description |
|---|---|---|
| **Incident Classification** | Zero-shot text classification | Categorises free-text SOS messages into `medical`, `entrapment`, `flood`, `infrastructure`, `evacuation_request` |
| **SOS Clustering** | DBSCAN spatial clustering on PostGIS coordinates | Groups geographically proximate signals into unified incident clusters |
| **Rescue Priority Scoring** | Rule-based weighted scoring (see §4.4) | Deterministic triage: medical urgency + population density + relay delay |
| **Vulnerability-Aware Triage** | Demography overlay | Adjusts priority for clusters known to contain elderly or differently-abled populations |
| **Cascading Risk Analysis** | Graph propagation model | Models "domino effect" infrastructure failures (power → comm → shelter → hospital cascade) |
| **Multilingual Detection** | `langdetect` / character-script analysis | Identifies Odia script (ଓଡ଼ିଆ), Devanagari (Hindi), or Latin (English) for correct TTS selection |
| **Situation Summarisation** | Abstractive summarisation | Generates duty officer briefings from raw event streams |
| **AI Route GNN** | Graph Neural Network over road/LoRa topology | Assesses route viability incorporating SNR, flood imagery, and LoRa node battery status |

---

## 7. Security Model

| Control | Implementation |
|---|---|
| **Authentication** | Firebase Phone Auth (SMS OTP + auto-retrieval). Tokens exchanged for custom JWT signed with `PyJWT`. |
| **RBAC** | `Permission` enum (`SOS_VIEW`, `SOS_MANAGE`, `RESOURCE_MANAGE`, `ADMIN`). All sensitive endpoints require `Depends(require_permissions([...]))`. |
| **IVR Webhook Integrity** | Twilio request signature validation (`X-Twilio-Signature` header) on `/api/v1/voice/webhook`. |
| **Data Encryption** | Supabase enforces TLS 1.3 for all client connections. Row-Level Security (RLS) policies on citizen data tables. |
| **SOS Deduplication** | Idempotent ingestion — duplicate SOS packets (same `device_identifier` + `location_timestamp` within 60 s) are discarded, not double-counted. |
| **Rate Limiting** | FastAPI middleware (configurable); SOS submission endpoint is rate-limited per device identifier. |
| **Mesh Packet Integrity** | Each SOS packet carries a UUID. Relay nodes reject replay attacks by maintaining a per-session seen-packet log. |

> **Important:** The hackathon implementation uses a `MockExotelProvider` for IVR calls. Production deployment requires a real Twilio or Exotel account with verified sender IDs. The system must not claim official government operation unless authorised government integration exists.

---

## 8. Multilingual Architecture

PRANSETU is designed for India's linguistic diversity from the ground up.

| Language | Android | Web EOC | IVR TTS | Script |
|---|---|---|---|---|
| English | ✅ | ✅ | ✅ | Latin |
| Odia (ଓଡ଼ିଆ) | ✅ | ✅ | ✅ | Odia script |
| Hindi (हिन्दी) | ✅ | Partial | ✅ | Devanagari |

**Design principles:**
- Canonical database values (severity labels, event types, status codes) are always stored in **language-agnostic English**. Localised presentation text is resolved at the presentation layer, never in the database.
- Original citizen-language content (free-text SOS messages) is **preserved verbatim** and never overwritten by translation output.
- Android runtime language switching uses `LanguagePreferencesRepository` (DataStore) and forces an `Activity.recreate()` to reload the correct `strings.xml` locale.
- The localization architecture uses standard Android `strings.xml` resource qualifiers (`values-or/`, `values-hi/`) designed to scale to all 22 scheduled Indian languages and scripts with zero architectural changes.

---

## 9. Repository Structure

```
PRANSETU-SIH'26/
│
├── PRANSETU(android)/                    # Android Citizen Application
│   ├── android/PRANSETU/app/src/
│   │   └── main/java/com/pransetu/app/
│   │       ├── core/
│   │       │   ├── auth/                 # Firebase Phone Auth manager
│   │       │   ├── network/
│   │       │   │   ├── nearby/           # Google Nearby Connections mesh engine
│   │       │   │   ├── sync/             # Offline queue drain service
│   │       │   │   └── supabase/         # Supabase Kotlin client
│   │       │   ├── sos/                  # SOS repository & canonical data model
│   │       │   ├── data/                 # Room DB, UserProfileStore (DataStore)
│   │       │   ├── relay/                # TTL packet relay engine
│   │       │   ├── sensor/               # Barometric pressure sensor
│   │       │   ├── location/             # GPS location provider & observer
│   │       │   ├── battery/              # Battery monitor (Flow)
│   │       │   ├── dispatch/             # Shelter compass HUD & azimuth
│   │       │   ├── localization/         # Language preference store
│   │       │   └── tts/                  # Text-to-speech service
│   │       ├── feature/
│   │       │   ├── home/                 # HomeViewModel, HomeUiState, SOS UI
│   │       │   └── onboarding/           # OnboardingContract, phone auth flow
│   │       ├── navigation/               # Navigation graph (Compose)
│   │       └── ui/                       # Material 3 design tokens
│   ├── apk/                              # Pre-built APK artefacts
│   ├── docs/assets/                      # Logos and architecture diagrams
│   ├── AGENTS.md                         # AI-agent engineering rules
│   ├── CONTRIBUTING.md
│   ├── SECURITY.md
│   └── README.md                         # Android-specific documentation
│
└── PRANSETU(web)/                        # Web EOC Dashboard + Backend
    ├── src/
    │   ├── components/
    │   │   ├── CommandCenter.tsx          # KPI engine, triage gauge, event bus
    │   │   ├── MissionMap.tsx             # Full-canvas GIS mission map
    │   │   ├── SOSLogs.tsx                # SOS audit stream & CSV export
    │   │   ├── VoiceCampaigns.tsx         # IVR campaign management
    │   │   ├── Resources.tsx              # Shelter & fleet logistics
    │   │   ├── Support.tsx                # SOPs, contacts, diagnostics
    │   │   ├── map/                       # InteractiveEOCMap, layer controllers
    │   │   ├── modules/
    │   │   │   ├── AIRouteInspector.tsx   # GNN route viability analyser
    │   │   │   ├── DisasterDominoEffect.tsx # Cascade risk modeller
    │   │   │   ├── LiveWeatherWidget.tsx  # Environmental radar
    │   │   │   └── RegisteredCitizensWidget.tsx
    │   │   ├── dispatch/                  # RescueDispatchModal
    │   │   ├── alerts/                    # State emergency alert system
    │   │   └── voice/                     # IVR response UI components
    │   ├── context/
    │   │   └── EOCContext.tsx             # Global EOC state (signals, campaigns)
    │   ├── services/
    │   │   └── api.ts                     # apiFetch(), isBackendOffline(), back-off
    │   └── types/                         # TypeScript domain types
    │
    └── backend/
        ├── app/
        │   ├── api/
        │   │   ├── sos.py                 # SOS ingestion & acknowledgement
        │   │   ├── command_center.py      # KPI aggregation endpoint
        │   │   ├── voice_campaigns.py     # IVR broadcast & webhook handler
        │   │   ├── citizens.py            # Registered citizen registry
        │   │   ├── shelters.py            # Shelter network telemetry
        │   │   └── resources.py           # Fleet resource management
        │   ├── core/
        │   │   ├── db.py                  # Supabase client dependency injection
        │   │   ├── security.py            # JWT validation, require_permissions()
        │   │   └── rbac.py                # Permission enum
        │   └── services/
        │       └── exotel_provider.py     # IVR telephony abstraction layer
        ├── tests/                         # pytest async integration tests
        └── requirements.txt
```

---

## 10. Development Setup

### 10.1 Android Setup

**Prerequisites:** Android Studio Hedgehog (2023.1.1) or later · Android SDK 34 · JDK 17

```bash
# Clone the repository
git clone https://github.com/nirmalya-ghosh/PRANSETU-sih-26.git
cd "PRANSETU-SIH'26/PRANSETU(android)"

# Configure local properties
echo "sdk.dir=/path/to/android/sdk" > android/PRANSETU/local.properties
```

Create `android/PRANSETU/app/google-services.json` from your Firebase project console (Phone Auth must be enabled with your package `com.pransetu.app`).

```bash
# Build from CLI
cd android/PRANSETU
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug
```

> **Demo / offline testing:** Use phone number `9999999999` or enter OTP code `123456` to bypass Firebase SMS verification in development environments.

### 10.2 Web EOC Setup

**Prerequisites:** Node.js ≥ 20 · npm ≥ 10

```bash
cd "PRANSETU(web)"

# Install dependencies
npm install

# Configure environment
cp .env.example .env
# → Set VITE_SUPABASE_URL and VITE_SUPABASE_ANON_KEY

# Start development server (hot-reload)
npm run dev
# → http://localhost:5173

# Type-check
npx tsc --noEmit

# Lint
npm run lint

# Production build
npm run build
```

> The EOC dashboard functions in **seed data mode** when the backend is unreachable — all KPIs fall back to statically seeded demo values and the operator sees `"Seed data — API offline"` rather than an error state.

### 10.3 Backend Setup

**Prerequisites:** Python 3.12 · pip · (optional) PostgreSQL with PostGIS extension for local development

```bash
cd "PRANSETU(web)/backend"

# Create and activate virtual environment
python -m venv venv
source venv/bin/activate          # Linux/macOS
.\venv\Scripts\Activate.ps1       # Windows PowerShell

# Install dependencies
pip install -r requirements.txt

# Configure environment
cp env.example .env
# → Set: SUPABASE_URL, SUPABASE_SERVICE_KEY, JWT_SECRET, TWILIO_AUTH_TOKEN, etc.

# Run database migrations
python run_migration.py

# Start the API server (development)
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload

# Auto-generated API docs
# → http://localhost:8000/docs    (Swagger UI)
# → http://localhost:8000/redoc   (ReDoc)

# Run test suite
pytest tests/ -v
```

---

## 11. CI / CD & Deployment

| Stage | Tool | Trigger |
|---|---|---|
| **Lint (Web)** | oxlint | Every push |
| **Type-check (Web)** | TypeScript compiler (`tsc --noEmit`) | Every push |
| **Unit Tests (Backend)** | pytest | Every push |
| **Build (Web)** | Vite production build | PR merge to `main` |
| **Deploy (Web EOC)** | Vercel (automatic preview + production) | PR / merge to `main` |
| **Build (Android)** | Gradle CI workflow | Tag push |
| **APK Artefact** | GitHub Actions upload | Signed release tag |

Vercel configuration is defined in `PRANSETU(web)/vercel.json`. All SPA routes are rewritten to `index.html` to support client-side routing.

---

## 12. Development Philosophy

> **Critical functionality is considered complete only after build/test validation and, where applicable, physical-device or real-service validation.**

The canonical development cycle is:

```
Plan → Implement → Build → Test → Review → Physical Validation → Commit
```

- **No silent failures.** Every degraded-mode path surfaces a clear user-facing status. No spinner-forever states.
- **Offline-first, not offline-optional.** Cellular connectivity is a bonus, not a prerequisite. Every feature must define its offline behaviour before implementation begins.
- **Resilience over elegance.** A slightly more verbose implementation that handles the error path correctly is always preferred over a clean implementation that fails silently.
- **Data integrity over convenience.** Duplicate SOS ingestion is rejected at the database level (idempotency), not handled with a try/catch and a log line.
- **Human authority over AI.** The AI layer is advisory. Operators acknowledge and dispatch; algorithms score and flag. AI does not silently overwrite authoritative alerts or human decisions.

---

## 13. License

Developed for **Smart India Hackathon (SIH) 2026** — Disaster Management Initiative.  
See [LICENSE](PRANSETU(android)/LICENSE) for terms.

---

<p align="center">
  Built with 🧡 for the people of Odisha and every Indian who deserves to be heard — even when the networks go silent.
</p>
