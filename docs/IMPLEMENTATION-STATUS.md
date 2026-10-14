# Plan coverage and remaining gates

## Implemented in the API-3.5 compatibility update (2026-10-12)

- One shared Zepp package now declares Active Edge sources `8388864`/`8388865` (360 round), Active 2 and the existing 480-round target at API 3.5. The static watch test rejects the known API-4-only timer and `reload` option.
- The App Service uses one maintenance function from incoming state heartbeats and `Time.onPerMinute`; it preserves the two-minute idle and five-minute dead-bridge limits. Whether minute callbacks remain reliable with the screen off, and whether an API-3 alarm fallback is necessary, remains a hardware gate.
- The watch page is ratio-based and circle-chord constrained for 360/466/480 displays. It prioritizes distance, physical arrow, street and ETA, marks stale data prominently, keeps relaunch enabled for active routes, and releases both display locks below 15% battery or when inactive/stale.
- Parser matching is anchored to instruction fields, normalizes Persian/Arabic variants and uses a bounded lead-in/compound rule. It now rejects street/destination-like false positives in unit coverage.
- Notification exports remain one-shot user shares. The redacted report excludes navigation text and credentials; raw text requires confirmation. Real fixture/mask capture, redaction review and the coverage gate are still required before claiming source-icon reliability.

The repository contains both app implementations and local build/test commands. Builds are development artifacts, not a declaration of tested Active 2 compatibility.

## Verified locally (2026-10-11)

- Android: 18 JVM tests passed; debug/release APK and release AAB builds succeeded. Lint: zero errors, 15 warnings (dependency/target-version and KTX suggestions). API 36 is retained as requested.
- Watch: 11 Node tests passed, including mocked service/page lifecycle and side-service transport; Zeus ZAB compilation succeeded.
- Android debug APK signature verified successfully. Release AAB verified as unsigned: publisher signing inputs are still required.
- No physical-watch acceptance results are claimed. Tests exercise synthetic notification/icon fixtures and mocked Zepp APIs, not real firmware.

Implemented: native notification listener for the two source packages; English/Persian direction/digit parsing across title, text, expanded text and text lines; conservative icon templates; source locking/removal grace; in-memory manual destination; local authenticated bounded HTTP bridge; preference persistence; color controls/contrast warnings/live preview; both display modes; all 19 maneuver mappings and seven presets; master mute, reset and preview commands; Zepp code settings, polling, heartbeat/ACK, service cache, arrow UI and stale indication; redacted/opt-in raw diagnostics; draft privacy and store-review documentation.

Follow-up fixes include physical maneuver shapes in Android preview, instruction text in expanded notification lines, approach-distance-prefix normalization to avoid duplicate vibration when “In 120 m” disappears, ending sessions when their notification becomes non-ongoing, small-icon fallback, discarded in-flight responses after bridge-key changes, object-response size limits and a total HTTP header deadline.

## Not complete / cannot truthfully guarantee yet

- Real Maps/Waze notification fixtures and icon shapes on the owner's devices: synthetic icon templates do not cover every source icon. In particular keep/exit/roundabout shapes require verified distinguishable source samples; these maneuvers are available through textual parsing, and unmatched bitmaps remain UNKNOWN.
- Consecutive steps with identical exposed maneuver/instruction/street cannot be uniquely identified from notifications alone. Distinct streets or instructions work; distance/ETA changes deliberately do not create IDs.
- Vibration dedup survives page redraw/reopen and reconnect while the watch service lives, not an entire application crash/restart. Navigation history is not persisted.
- Physical Active 2 tests: localhost access through Zepp, background BLE, phone locked/battery saver, Persian shaping and circular text, motor perceptibility, timeout restoration, two-second delivery and the 60-minute trip remain pending. Node mocks and JVM tests do not establish these results.
- Owner-approved package IDs, Zepp app ID, publisher/support details, privacy URL and signing keys are absent. APK debug signing works; release APK/AAB are unsigned without owner environment variables. No public/closed-store upload has been performed.

See RELEASE.md for the owner inputs and full physical acceptance checklist. Police and speed-camera alerts are intentionally outside v1, not unfinished features.
