# PRANSETU Android Specification

## Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Coroutines/Flow
- Android Location APIs
- Supported nearby-device transport, validated on physical devices

## Features

### Home

- PRANSETU branding
- large SOS action
- network status
- location status
- pending SOS count
- last-known-location age
- language selector

### SOS

- unique SOS ID
- real location if available
- last-known location fallback
- timestamp and accuracy
- severity
- people count
- medical assistance flag
- local persistence before transmission
- explicit delivery state

### Offline

The app must open and create/store an SOS without internet. Critical data must survive process death and device restart subject to Android storage guarantees.

### Localization

Initial languages: English, Odia, Hindi.

Architecture must support additional Indian languages and scripts, locale-aware formatting, and RTL layouts.

### Diagnostics

Development-only diagnostics should expose safe operational information such as:

- connectivity state
- location freshness
- queued SOS count
- relay state
- last synchronization attempt
- acknowledgement state

Do not expose secrets.
