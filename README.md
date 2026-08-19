# PRANSETU

## Resilient Emergency Communication & Disaster Intelligence Network

> Bridging every life to help, even when networks fail.

PRANSETU is an Odisha-first disaster-management platform designed to preserve citizen-to-government communication when conventional connectivity is degraded. The architecture is intended to scale from Odisha to India.

## Core System

### Android Citizen App

- One-tap emergency SOS
- Real device location and accuracy/timestamp
- Last-known-location fallback when current connectivity is unavailable
- Local-first SOS persistence
- Store-and-forward delivery
- Nearby-device relay
- Gateway synchronization
- Delivery acknowledgement and status tracking
- Multilingual UI

### Emergency Operations Centre Web Platform

- Live incident map
- SOS monitoring
- Incident details and status
- Rescue prioritization
- Shelter/safe-place visibility
- Safe-place verification workflows
- Resource and response tracking
- Voice/IVR event monitoring
- Analytics and situation summaries
- Multilingual operator/citizen-facing views where applicable

### Backend

- FastAPI APIs
- Supabase PostgreSQL
- PostGIS geospatial operations
- Idempotent SOS ingestion
- Synchronization and acknowledgement
- IVR webhook processing
- Integration adapters
- Audit/event records

### Voice/IVR

Automated call -> language selection/recognized preference -> localized questions -> DTMF responses -> secure webhook -> backend -> database -> EOC.

The hackathon implementation can use a real telephony provider/API. It must not claim official government operation unless authorized government integration exists.

### AI / PRANSETU Intelligence

- Multilingual language detection and translation assistance
- Incident classification
- SOS clustering
- Rescue priority scoring
- Vulnerability-aware triage
- Cascading/"domino effect" risk analysis
- Situation summarization

AI assists operators; it does not silently replace authoritative alerts or human emergency decisions.

## Offline SOS

Target architecture:

```text
Phone A
   |
   v
Phone B
   |
   v
Phone C
   |
   v
Internet-capable Gateway
   |
   v
PRANSETU Backend
   |
   v
EOC
```

Each relay stores and forwards a bounded SOS packet. Packets use unique IDs, TTL/hop limits, deduplication and acknowledgement.

## Multilingual India-Ready Design

Initial Odisha languages:

- English
- Odia
- Hindi

The localization architecture is designed to expand to all Indian languages and scripts. Canonical database values remain language-independent; presentation text is localized separately. Original citizen-language content is preserved when translation is used.

## Repository

```text
PRANSETU-sih-26/
├── android/       # Citizen Android application
├── backend/       # FastAPI services and integrations
├── web/            # EOC web application
├── ai/             # PRANSETU Intelligence components
├── docs/           # Architecture, protocols and operational documentation
├── scripts/        # Development/testing/deployment helpers
├── .github/        # CI and contribution automation
├── AGENTS.md       # AI-agent and engineering rules
├── README.md
├── SECURITY.md
└── CONTRIBUTING.md
```

## Development Philosophy

Critical functionality is considered complete only after build/test validation and, where applicable, physical-device or real-service validation.

**Plan -> Implement -> Build -> Test -> Review -> Physical Validation -> Commit**
