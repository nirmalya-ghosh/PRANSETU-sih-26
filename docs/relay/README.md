# PRANSETU Offline Relay

## Target

A citizen without internet can persist an SOS and, when a supported nearby transport is available, forward it through participating PRANSETU devices until an internet-capable gateway is reached.

```text
A -> B -> C -> Gateway -> Backend -> EOC
```

## Required Properties

- store-and-forward
- unique SOS ID
- TTL
- hop limit
- deduplication
- integrity validation
- acknowledgement
- bounded retry
- persistence across app/process restart
- privacy-aware relay metadata

## Transport

Transport selection must be based on current Android capabilities and physical-device validation. Do not assume Bluetooth/Wi-Fi can provide arbitrary mesh behavior or work when the operating system disables required radios/permissions.

## Milestones

1. discovery
2. connection
3. small test packet
4. one-hop SOS
5. deduplication
6. multi-hop
7. gateway
8. failure/recovery tests
