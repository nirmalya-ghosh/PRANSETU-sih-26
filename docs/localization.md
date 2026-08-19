# PRANSETU Localization Architecture

## Goal

PRANSETU is multilingual from day one.

### Initial Odisha rollout

- English (`en`)
- Odia (`or`)
- Hindi (`hi`)

The architecture must support expansion to all Indian languages and scripts without changing canonical data models.

## Principles

1. Never hard-code user-facing strings in business logic.
2. Use stable localization keys.
3. Store canonical status/severity codes independent of language.
4. Localize presentation at the client/UI boundary.
5. Persist user language preference.
6. Allow language switching without reinstalling.
7. Preserve original citizen-language content when translated.
8. AI translation must never overwrite the original.
9. Support locale-aware date, time and number formatting.
10. Architecturally support RTL scripts such as Urdu.
11. Critical emergency wording must be human-reviewed.
12. Voice/IVR flows must use the caller's selected/preferred language where supported.

## Example

Canonical:

```text
severity = CRITICAL
status = TRAPPED
```

Presentation:

```text
English: Critical / Trapped
Odia: localized equivalent
Hindi: localized equivalent
```

The database must never depend on translated display labels.

## AI Language Intelligence

AI may assist with:

- language detection
- translation assistance
- classification of multilingual reports
- multilingual summarization

For emergency operations, retain:

- original text
- detected language
- translated text, if produced
- model/provider
- model version
- confidence/quality metadata where available

Human operators must be able to inspect the original citizen message.
