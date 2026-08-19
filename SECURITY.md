# PRANSETU Security Baseline

## Never commit

- API keys
- service-role keys
- telephony credentials
- database passwords
- signing keys
- government credentials
- private certificates

## SOS security

- Use authenticated server APIs.
- Validate all incoming packets.
- Enforce idempotency.
- Protect against replay/duplicate delivery.
- Minimize personal data in relay packets.
- Never trust client-provided authority roles.
- Audit critical state changes.

## Privacy

Location and contact information are sensitive operational data. Collect and expose only what is required for emergency response. The UI must clearly distinguish current location from last-known location.

## Government integration

Government-system integration must use an authorized API, gateway or documented interface. Do not scrape or impersonate government systems.
