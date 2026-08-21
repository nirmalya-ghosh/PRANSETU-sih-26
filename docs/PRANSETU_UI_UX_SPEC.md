# PRANSETU UI/UX Specification

## 1. Design Philosophy
- **Clarity over decoration:** Information must be immediately legible without cognitive strain.
- **Trust over visual excitement:** The app must feel calm, official, and sophisticated.
- **Speed over unnecessary interaction:** Emergency actions (like SOS) must be obvious and direct.
- **Accessibility by default:** Large touch targets, dynamic font scaling, high contrast, and screen-reader support are mandatory.
- **Honesty in State:** Never display a "success" or "delivered" state unless explicitly confirmed by the backend or local store.

## 2. Visual Identity
- The app portrays a serious government/public-safety identity.
- It is neither a flashy startup app nor a gaming interface.
- It uses deep, authoritative blues, clean layouts, and purposeful color coding for statuses.

## 3. Color System
- **Primary:** Deep Public-Service Blue (e.g., `#0F4C81`) - communicates trust and stability.
- **Secondary:** Calm Teal/Cyan (e.g., `#008080`) - for non-critical secondary actions.
- **Emergency (Error/Critical):** High-visibility Red (e.g., `#D32F2F`) - strictly reserved for SOS, critical alerts, and destructive actions.
- **Warning:** Amber (e.g., `#FBC02D`) - for offline states, missing permissions, or advisories.
- **Success:** Green (e.g., `#388E3C`) - exclusively for confirmed successful deliveries/acknowledgements.
- **Background:** True Black (`#000000`) or deep Gray (`#121212`) in dark mode (battery-saving priority); off-white (`#F8F9FA`) in light mode.
- **Surface:** Distinct elevations to separate cards (e.g., `#1E1E1E` for dark mode surfaces).

## 4. Typography
- Standard Android typography system (Material 3 `Typography` with `Roboto`).
- **Hierarchy:**
  - `Display`: For critical localized numbers (e.g., countdowns).
  - `TitleLarge`: Screen headers.
  - `TitleMedium`: Section headers.
  - `BodyLarge`: Standard reading text (Safety guides, alert descriptions).
  - `LabelMedium`: Small supporting text (timestamps).
- **Language Support:** Must seamlessly support English, Odia, Hindi, and scale up without clipping or forcing single-line constraints. Test at 130%, 150%, and 200% font scales.

## 5. Spacing
- Use a 4dp/8dp grid system.
- Standard screen padding: 16dp.
- Space between major sections: 24dp or 32dp.
- Space between related elements: 8dp.

## 6. Component System
- **PransetuTopBar:** Standardized `CenterAlignedTopAppBar` for primary screens, standard `TopAppBar` with back arrows for secondary screens.
- **EmergencySosButton:** Custom hold-to-activate component to prevent accidental presses.
- **SosProgressIndicator:** Circular progress that fills as the user holds the SOS button.
- **StatusTimeline:** Vertical timeline to show SOS delivery progression (Created -> Saved -> Relaying -> Delivered).
- **ConnectivityStatusCard & LocationStatusCard:** M3 Cards using icons (Wifi, WifiOff, LocationOn, LocationOff) instead of pure color dots.
- **AlertCard:** Warning-colored cards displaying disaster info.
- **PermissionExplanationCard:** Used in onboarding/settings to explain *why* a permission is needed.
- **LanguageSelector:** Standard `ListItem` rows with radio buttons.

## 7. Navigation
- **Architecture:** Bottom Navigation for primary destinations: Home, Alerts, Safety, More (Profile/Settings).
- SOS is always globally prominent, primarily living on the Home screen.
- Screen transitions should be fast and standard (e.g., slight crossfade or slide horizontal), avoiding excessive bouncing.

## 8. Home Screen
- **Welcome Section:** "Good morning, [Name]"
- **Status Area:** Current location area and location/connectivity status.
- **Active Alert (Dynamic):** If an alert is active, it appears prominently below the status area. If none, this space collapses to keep the UI calm.
- **SOS Zone:** The central interaction area. "PRESS AND HOLD TO SEND SOS".
- **Quick Actions:** Emergency Contacts, Safety Guide, Nearby Help.
- **Safety Today:** Relevant preparedness tips (bottom of the scrollable area).

## 9. SOS UX
- **Interaction:** The user must "Press and Hold" the SOS button for ~3 seconds.
- **Visuals:** A progress ring fills.
- **Haptics:** Pulsing vibration while holding, strong confirmation vibration upon success.
- **Cancellation:** Releasing before 3 seconds cancels the action immediately.

## 10. SOS State UI
- After SOS creation, the UI transitions to a dedicated **SOS Status Screen**.
- Displays a timeline:
  - ✓ SOS Created (Time)
  - ✓ Saved securely on device
  - ◌ Looking for connection / Transmitting via gateway
  - ○ Delivered to PRANSETU (Backend)
  - ○ Acknowledged by operator
- Will only show states verified by the local device state or backend response.

## 11. Alerts
- Alert screen lists active and past warnings.
- Each alert card shows: Severity Badge (INFO, ADVISORY, WARNING, CRITICAL), Title, Area, Issued/Updated time, Description, Instructions, Source.

## 12. Safety
- Dedicated tab.
- Categories: Before, During, After disaster.
- Topics: Cyclone, Flood, Earthquake, First Aid, etc.
- UI: Card grid or list with iconography. Content intended to be fetched from a CMS/Backend eventually.

## 13. Emergency Contacts
- Managed in the 'More' or 'Profile' tab.
- List of personal contacts and official authorities (populated by backend later).
- Options: Add, Edit, Remove.

## 14. Profile
- Shows: Name, Verified Phone, Preferred Language, Device Status.
- Kept minimal; no unnecessary data collection.

## 15. Settings
- Language selection (English, Odia, Hindi).
- Permissions status.
- Notifications, Privacy, About.

## 16. Onboarding
- Sequence: Welcome -> What PRANSETU does -> Language Selection -> Permission explanations (Location, Nearby Devices, Notifications) -> Phone Auth (UI only) -> Profile -> Ready.

## 17. Permissions UX
- Never request a permission without an explanatory screen first.
- E.g., "PRANSETU uses your location during emergencies so responders can understand where help is needed." -> [Allow] [Not Now].
- If denied, provide a permanent but calm persistent card in Settings/Home to re-enable.

## 18. Authentication Presentation
- Phone number input field.
- OTP entry screen.
- Currently, these will be UI mockups allowing the user to proceed to the main app, ready for backend wiring.

## 19. Offline UX
- Do NOT say "SOS impossible".
- Display an Amber banner: "Internet unavailable. SOS can still be stored and may be relayed."
- The SOS button remains visually enabled and functional.

## 20. Location UX
- Clear distinction between "CURRENT LOCATION" (recent GPS fix) and "LAST KNOWN LOCATION" (with a timestamp).
- States: Available, Unavailable, Permission Required, Acquiring...

## 21. Accessibility
- Minimum touch target: 48dp.
- Content descriptions on all functional icons.
- Merge semantics on complex cards (e.g., Status Card) so TalkBack reads it cleanly.
- Do not rely solely on color (use Icons + Text).

## 22. Localization
- Direct language switching support.
- Layouts must use `Modifier.weight()` and wrapping to handle Odia/Hindi text expansion without clipping.
- All strings must be extracted to `strings.xml`.

## 23. Responsive Layouts
- Elements adapt to screen width (e.g., SOS button uses `fillMaxWidth(0.6f)`).
- `verticalScroll` on main content to support landscape mode and small devices.

## 24. Animation/Motion
- Minimal and purposeful.
- Hold-to-SOS progress ring uses `animateFloatAsState`.
- Navigation transitions use standard native-feeling slides/fades.

## 25. Error States
- Actionable and plain language (e.g., "Unable to reach servers", not "Timeout Exception 500").
- Provide recovery buttons (e.g., "Retry", "Open Settings").

## 26. Loading States
- Determinate progress bars preferred.
- If waiting for GPS, show "Acquiring location..." but allow immediate SOS submission using Last Known Location if it takes too long.

## 27. Empty States
- E.g., "No active alerts in your area. You are safe."
- Friendly, illustrative, and clear.

## 28. Future Backend Integration Boundaries
- All UI components will consume Kotlin Data Classes (`UiState`) emitted by ViewModels.
- ViewModels will interface with Repositories.
- No Supabase or networking code will be placed inside UI Composables.
- The UI strictly responds to State and sends Intents.

## 29. Screen-by-Screen Implementation Plan
1. **Design System:** Implement Theme, Color, Type, and basic custom components (Buttons, TopBars).
2. **App Shell & Navigation:** Setup Bottom Navigation and Route structure (Home, Alerts, Safety, More).
3. **Home Screen (Redesign):** Implement Welcome, Status Dashboard, Hold-to-SOS button, Quick Actions.
4. **SOS Status Screen:** Implement the timeline UI for SOS states.
5. **Alerts & Safety Screens:** Implement list/card layouts for information consumption.
6. **Settings & Profile:** Implement lists and toggles.
7. **Onboarding Flow:** Implement the pager-based setup screens and permission explainers.
8. **Authentication Flow (UI):** Implement Login/OTP screens.
9. **Polish:** Ensure Dark Mode, Localization, Accessibility, and Offline/Error states are fully polished across all screens.
