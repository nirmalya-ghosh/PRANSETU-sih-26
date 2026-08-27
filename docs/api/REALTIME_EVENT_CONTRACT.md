# PRANSETU Real-Time Event & API Contract

**Version:** 1.0.0  
**Protocol:** PRANSETU-CANONICAL-REALTIME-v1  
**Status:** AUTHORITATIVE SPECIFICATION

---

## 1. Architectural Philosophy

The PRANSETU ecosystem operates on an **event-driven, offline-first, authoritative-backend architecture**. 

```
Android Client ──(PostgREST / Mesh Relay)──► Supabase / Backend ──(Realtime Event Bus)──► Authorized Web Clients (EOC)
                                                  │
                                                  ▼
                                       Postgres Persistence & Triggers
                                                  │
                                                  ▼
                                      Authoritative Status Sync
                                                  │
                                                  ▼
                                            Android Client
```

- **Client ↛ Client Direct Mutation**: Android and Web never directly communicate or mutate each other's state.
- **Backend as Single Source of Truth**: All operational state (delivery state, acknowledgments, dispatching) is validated and persisted on the backend before being broadcast.
- **No Faked Delivery**: Clients never claim "Delivered" or "Acknowledged" without cryptographic or server confirmation.

---

## 2. Universal Event Envelope

Every operational event in the PRANSETU platform conforms to the following schema:

```json
{
  "event_id": "UUID string (client or server generated)",
  "event_type": "STRING enum (e.g., 'SOS_CREATED', 'SOS_BACKEND_RECEIVED')",
  "event_version": 1,
  "occurred_at": "ISO 8601 UTC Timestamp (e.g., '2026-08-27T17:30:00.000Z')",
  "server_received_at": "ISO 8601 UTC Timestamp (auto-populated by server)",
  "user_id": "STRING | null (Authenticated User ID or hashed identifier)",
  "device_id": "STRING (Unique Hardware/App Node identifier)",
  "session_id": "STRING | null (Client session identifier)",
  "sos_id": "STRING | null (Target SOS UUID)",
  "incident_id": "STRING | null (Associated Incident Cluster ID)",
  "campaign_id": "STRING | null (Associated IVR Voice Campaign ID)",
  "source": "STRING ('android' | 'web_eoc' | 'backend' | 'ivr_telephony')",
  "sequence": "INTEGER (Monotonically increasing sequence number from server)",
  "payload": "JSON object with domain-specific payload"
}
```

---

## 3. Operational Event Taxonomy

### 3.1 Application Lifecycle Events
- **`APPLICATION_STARTED`**: Client application initialized.
  - Payload: `{"app_version": "1.0.0", "battery_percent": 88, "os_version": "Android 14"}`
- **`APPLICATION_READY`**: Local storage, permissions, and background daemons verified.
  - Payload: `{"is_mesh_capable": true, "location_available": true}`

### 3.2 User & Device State Events
- **`USER_SIGNED_IN`**: Citizen or operator authenticated.
  - Payload: `{"auth_provider": "google", "role": "citizen"}`
- **`USER_SIGNED_OUT`**: Active session ended.
- **`LANGUAGE_CHANGED`**: Language preference updated.
  - Payload: `{"language_code": "or"}` (e.g., 'or' = Odia, 'hi' = Hindi, 'en' = English)
- **`NETWORK_STATUS_CHANGED`**: Connectivity state transitioned.
  - Payload: `{"network_type": "CELLULAR_4G" | "WIFI" | "OFFLINE"}`
- **`LOCATION_STATUS_CHANGED`**: Location fix state changed.
  - Payload: `{"has_fix": true, "accuracy_m": 5.2}`

### 3.3 SOS Distress Lifecycle Events
- **`SOS_CREATED`**: Citizen triggered an emergency SOS.
  - Payload: `{"latitude": 20.2961, "longitude": 85.8245, "people_count": 2, "medical_required": true, "severity": "CRITICAL"}`
- **`SOS_SAVED_LOCALLY`**: SOS encrypted and stored in local Room DB.
  - Payload: `{"storage_engine": "ROOM_SQLITE"}`
- **`SOS_SEARCHING_FOR_RELAY`**: App scanning for BLE/Wi-Fi peer mesh nodes.
  - Payload: `{"in_range_peers": 3}`
- **`SOS_RELAY_STARTED`**: Distress packet transmitted to adjacent mesh node.
  - Payload: `{"relay_node": "Node_Pixel_4A", "hop_count": 1}`
- **`SOS_GATEWAY_FOUND`**: Packet reached an internet-connected peer node.
  - Payload: `{"gateway_id": "GW_Node_Galaxy_S23"}`
- **`SOS_UPLOAD_STARTED`**: PostgREST HTTP transmission initiated.
  - Payload: `{"transport": "HTTPS_POSTGREST"}`
- **`SOS_BACKEND_RECEIVED`**: Backend validated and persisted SOS record in PostgreSQL.
  - Payload: `{"assigned_eoc": "SEOC_ODISHA", "postgis_point": "POINT(85.8245 20.2961)"}`
- **`SOS_OPERATOR_ACKNOWLEDGED`**: Authorized EOC Operator acknowledged the distress signal.
  - Payload: `{"operator_id": "usr_op_992", "operator_role": "OPERATOR"}`
- **`SOS_DISPATCHED`**: Rescue team assigned and dispatched to the location.
  - Payload: `{"team_id": "RES-T1", "team_name": "NDRF-Alpha (Battalion 03)", "eta_minutes": 14}`
- **`SOS_RESOLVED`**: Mission completed; citizen confirmed rescued/safe.
  - Payload: `{"resolution": "RESCUED_AND_VERIFIED_SAFE"}`

### 3.4 Public Safety & Authority Broadcasts
- **`DISASTER_ALERT_CREATED`**: High-priority public safety warning published.
  - Payload: `{"alert_type": "CYCLONE", "severity": "RED_CRITICAL", "affected_area": "Puri Coastal Belt", "title": "Category 4 Cyclone Warning"}`
- **`DISASTER_ALERT_CANCELLED`**: Warning de-escalated / all-clear issued.
  - Payload: `{"alert_id": "ALT-CYC-20260827-A9B1", "reason": "Cyclone moved north-east away from coastal zone"}`

### 3.5 AI Hotspot & IVR Telephony Events
- **`HOTSPOT_DETECTED`**: Domino AI detected a spatial-temporal cluster of distress signals.
  - Payload: `{"hotspot_id": "HOT-042", "center_lat": 20.296, "center_lng": 85.824, "sos_count": 14, "confidence": 0.94}`
- **`VOICE_CAMPAIGN_STARTED`**: Outbound automated voice wellness campaign launched.
  - Payload: `{"campaign_id": "CMP-2026-08", "target_audience": "Cuttack Lowlands", "target_count": 8500}`
- **`CALL_ANSWERED`**: Citizen picked up the IVR AI phone call.
  - Payload: `{"call_id": "call_twilio_0991", "citizen_phone": "+91******84"}`
- **`ASSESSMENT_UPDATED`**: Voice AI analyzed citizen's spoken response.
  - Payload: `{"triage_state": "TRAPPED_ROOFTOP", "medical_urgency": "HIGH", "food_water_needed": true}`

---

## 4. Idempotency & Delivery Guarantees

1. **Client Deduplication**: Every event must carry a unique `event_id` (UUID v4).
2. **Server Upsert**: All event ingestion endpoints and database triggers check for primary key or unique index conflicts:
   ```sql
   ON CONFLICT (event_id) DO NOTHING;
   ```
3. **Sequence Ordering**: The server assigns a monotonically increasing `sequence` number. Web and Android clients use `sequence` and `occurred_at` to guarantee correct state transitions without backwards regression.

---

## 5. Security & Role-Based Access Control (RLS)

| Role | Permissions & Data Clearance |
| :--- | :--- |
| **CITIZEN** | Can insert their own SOS events; can view own SOS status and public disaster alerts. Cannot access other citizens' records or raw device streams. |
| **OPERATOR** | Can view active SOS events with masked phone numbers; can acknowledge SOS, view live maps, and track dispatch status. |
| **RESPONDER**| Can view exact coordinates of assigned incidents and navigate to distress locations. |
| **ADMINISTRATOR** / **DMO** | Full administrative access, alert publishing, unmasked audit access, and campaign management. |

---

## 6. Realtime Reconnection & Gap Recovery

If a client temporarily disconnects from the Supabase Realtime WebSocket:
1. Client catches `DISCONNECT` / `ERROR`.
2. Initiates reconnection with exponential backoff (1s, 2s, 4s, max 15s).
3. Upon reconnect, fetches missed events:
   ```sql
   SELECT * FROM realtime_events 
   WHERE sequence > :lastReceivedSequence 
   ORDER BY sequence ASC;
   ```
4. Reconciles in-memory state and resumes real-time stream subscription.
