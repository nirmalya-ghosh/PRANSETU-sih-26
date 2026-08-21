# PRANSETU MASTER SPECIFICATION

## Project Identity
PRANSETU is a disaster-management and emergency-response platform designed initially for Odisha and later scalable across all of India. It is a complete disaster communication, emergency response, intelligence, coordination, and public-safety ecosystem.

## Core Vision
A person should be able to trigger an emergency SOS even when their phone has no normal cellular/internet connectivity. The SOS should be capable of being transferred from one device to another using appropriate peer-to-peer technologies (Bluetooth, Wi-Fi Direct, Nearby Connections, etc.).
This is a **STORE-AND-FORWARD / MULTI-HOP** emergency communication concept.
The implementation must eventually be REAL and FUNCTIONAL. No fake network transmissions.

## Government Integration Representation
PRANSETU is a prototype of how this system could operate from the government/emergency-response side. Use abstraction layers so that future official government APIs or emergency infrastructure can replace prototype/demo providers.

## Android Application
The citizen/emergency-user side. Core future functionality includes:
1. Emergency SOS, One-tap SOS, GPS/Last-known location, Timestamp.
2. User/device identification, Emergency category/description.
3. Offline SOS persistence, Store-and-forward, Peer-to-peer SOS transfer, Multi-hop relay.
4. Bluetooth/Nearby/Wi-Fi communication, Message deduplication, Message ID, TTL, Hop count, Retry, Delivery acknowledgement.
5. Gateway discovery, Internet delivery when connectivity returns, Emergency status tracking.
6. Disaster alerts, Safety information, Multilingual UI, Accessibility.
7. AI-assisted emergency support, Automated emergency voice-call workflow.

## Critical Language Requirement
Language switching must be direct (e.g., English -> Odia, Hindi -> Odia) without assuming English as the intermediate language. Canonical data must remain language-independent.
Future languages: Odia, Hindi, Bengali, Assamese, Telugu, Tamil, Kannada, Malayalam, Marathi, Gujarati, Punjabi, Urdu, etc.
No hard-coded user-facing text. Localization keys only.

## SOS System
An SOS packet contains structured data: unique ID, originating device ID, timestamp, location, accuracy, emergency category, priority, TTL, hop count, protocol/version, delivery state, integrity/security information.

## Offline-First Requirement
Offline functionality is a PRIMARY requirement.
- No Internet: SOS initiated, persisted locally, attempt offline transport, prevent duplicate transmission, track delivery state.
- Internet available: Deliver stored SOS through backend gateway.

## Multi-Hop / Device Relay
Device A → Device B → Device C → Internet gateway.
Considerations: Message ID, duplicate detection, TTL, hop count, retry, acknowledgement, expiration, storage limits, battery, privacy, security, malicious/replayed packets, unreliable peer connections.

## Backend
Must receive SOS messages from Internet-connected Android devices and offline gateway devices when they regain connectivity.
Support: Authentication, authorization, SOS ingestion, validation, deduplication, persistence, event processing, location processing, emergency prioritization, audit logs, government integration abstraction, notification services, analytics, AI services.
Do NOT hard-code Android to one backend. Keep repository interfaces replaceable.

## Web Platform
24/7 web platform / command centre representing the emergency-response/command side. Deployable as a production-style service.
Features: Government dashboard, Live SOS map, Incoming SOS feed, Priority, Location visualization, Disaster/Risk zones, Rescue status, Incident management, Resource allocation, Event timeline, AI insights, Multilingual support, Audit trail, System/Communication status.

## AI / Intelligence
Assists human operators; does NOT make uncontrolled life-critical decisions.
Functionality: Disaster consequence prediction, propagation analysis, risk scoring, hotspot detection, SOS prioritization, duplicate detection, resource prioritization, incident clustering, multilingual assistance, operator assistance, summarization.

## Disaster Domino Effect
Model consequences of disasters (e.g., Earthquake → infrastructure damage → road blockage → hospital accessibility reduction). The web platform should eventually visualize these relationships.

## Automated Voice Call
Future workflow where an emergency system initiates an automated call in the appropriate language, user responds, responses are interpreted, follow-up questions asked, and information is structured for the backend. Must distinguish demo/API from actual government infrastructure.

## Security
Consider: Authentication, authorization, encryption, secure transport, packet integrity, replay protection, device identity, least privilege, data minimization, secure storage, auditability, API security, abuse prevention.
No API keys in source code. No secrets exposed on GitHub.

## Android UI/UX
- Serious emergency/public-safety application look and feel.
- Extremely clear, calm, trustworthy, fast, accessible, minimal cognitive load.
- Usable under stress, large touch targets, strong visual hierarchy, clear status.
- Minimal unnecessary animations, high contrast, readable typography, localization-aware.
- Prominent SOS action, but prevent accidental activation.
- Clear truthful feedback on SOS state (stored locally, relayed, reached gateway, received by backend, acknowledged).

## UX Under Disaster Conditions
Design for users who may be: frightened, injured, in darkness, under poor network conditions, using one hand, using low-end devices, elderly, visually impaired, unfamiliar with technology, using regional languages.

## Final Rule
PRANSETU must be treated as a serious emergency-response system. Correctness, reliability, security, accessibility, offline operation, truthful delivery status, and real-world usability are more important than superficial feature count.
