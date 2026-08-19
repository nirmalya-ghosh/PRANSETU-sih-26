# PRANSETU IVR

## Target Flow

Automated call -> language-aware greeting -> emergency/safety prompts -> DTMF -> secure webhook -> backend -> EOC.

## Initial Languages

- English
- Odia
- Hindi

## Requirements

- Real telephony provider/API for integration testing
- Verified webhook authenticity
- Idempotent event processing
- Explicit call/session ID
- Original responses preserved
- Language preference preserved
- Failure/retry handling

The hackathon implementation must clearly distinguish third-party telephony infrastructure from any future authorized government deployment.
