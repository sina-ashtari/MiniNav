# Local protocol v1

`GET http://127.0.0.1:<port>/v1/state`, header `Authorization: Bearer <32 lowercase hex digits>`.

Bridge code: `MN1:<1024..65535>:<128-bit token>`. Settings accepts only this grammar; the URL cannot be overridden. Android persists a random token and port in private preferences with backup disabled. Port conflict selects another local port; the owner must paste the updated code. Key rotation is available only with the bridge stopped.

Server: loopback binding, HTTP/1.1, GET only, exact path, no request bodies or transfer encoding, duplicate headers rejected, 8 KiB header cap, 1.5 s socket read timeout and two-second total header deadline, two workers/four queued clients, connection close and `Cache-Control: no-store`. Never record headers or request payloads. The endpoint is not reachable on Wi-Fi interfaces.

Every response has `protocolVersion: 1`, monotonically changing `sequence` within the Android process, wall-clock `servedAt`, strings `sessionId`, `instructionId`, `destination`, `street`, `turnDistanceText`, `remainingText`, canonical uppercase `maneuver`, `state` (`idle`, `navigating`, `rerouting`, `arrived`, `error`) and `source` (`maps`, `waze`, null).

`theme` keys: `background`, `destination`, `arrow`, `distance`, `street`, `duration`; each is `#RRGGBB`. `screenMode` is retained for protocol-v1 compatibility and is always emitted as `always_on`; the watch still releases its display locks for stale guidance and the low-battery cutoff. `vibrationEnabled`: boolean. `vibrationMap`: all 19 uppercase maneuver keys mapped to `NONE`, `SHORT_LIGHT`, `SHORT_STRONG`, `TWO_SHORT`, `LONG`, `EXTRA_LONG`, or `STRONG_REMINDER`.

`vibrationPreview` is null or `{ nonce, preset, expiresAt }`. It expires in ten seconds. Compare `expiresAt` against **phone** `servedAt`, not the watch clock. The watch records nonces (last 32) before playing; duplicate commands are silent. Tests do not alter navigation or instruction identity. Concurrent maneuvers/tests can interrupt a firmware vibration; no vibration queue is promised.

The Zepp side service has one outstanding poll at a time, nominally every second. It forwards changed sequences and a full-state heartbeat at least every five seconds when healthy. The watch acknowledges via ZML request/response with its last instruction ID and preview nonce. No mutable HTTP ACK endpoint is exposed. The Android preview can remain in HTTP state until expiry: watch deduplication supplies one-shot behavior.

After twelve seconds without a valid watch update, the last card is marked disconnected. The screen returns to normal timeout. Reconnecting does not replay an acknowledged instruction. Cached navigation and acknowledgement identifiers live only for the watch application's lifetime; crash-safe exactly-once semantics are impossible without additional persistent acknowledgement design.

Android source arbitration locks to the first parsed ongoing notification in auto mode. Active notification removal has a five-second grace period. A new update cancels that removal. Destination carries over within the session; manual destination is used only when no source destination was found. Stopping/ending a session clears the manual label and in-memory raw diagnostic text.

No arbitrary host, control endpoint, police/camera signal, telemetry or cloud service exists in the protocol. The Zepp application itself is third-party software with its own privacy practices.
