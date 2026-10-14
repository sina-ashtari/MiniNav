# Release checklist — not approved for public distribution

## Owner inputs

- Confirm Android application ID (build property `-Pmininav.applicationId=...`), Zepp app ID/vendor, app-store publisher name, support contact and hosted privacy-policy URL.
- Supply your own release signing configuration locally: `MININAV_KEYSTORE` (absolute path), `MININAV_STORE_PASSWORD`, `MININAV_KEY_ALIAS`, `MININAV_KEY_PASSWORD`. Never commit secrets or paste them into diagnostics/chat. Run `gradlew assembleRelease bundleRelease`; keep the key and recovery copies securely.
- Replace the development Zepp `appId` and `vender` in `ZEPP App/app.json`, run the watch tests/build and use the official developer preview/internal distribution flow. Do not publish the placeholder ID.
- Validate Android release APK with `apksigner verify`; verify AAB signing and upload only through an owner-authorized store workflow. This repository does not perform store uploads.

## Play listing / Data Safety draft

Title: **MiniNav**. Short description: **Minimal local Waze and Maps guidance on Amazfit Active Edge and Active 2 Round.**

Full description: MiniNav forwards supported navigation notification instructions from Google Maps and Waze to your paired Active 2 Round. Customize six display colors, screen behavior and a vibration preset for each maneuver. Requires Android notification access, the Zepp companion app and the MiniNav watch app. Start the bridge and open the watch app for each trip. No MiniNav account, ads or developer cloud service. Guidance availability depends on source notifications and device background policies. Not affiliated with Google, Waze or Amazfit/Zepp. No police or speed-camera alerts.

Data Safety **draft for owner review, not a submitted declaration**: no developer collection or sharing off-device is intended; no accounts or deletion-request backend. Navigation/location-related text is processed locally and sent to the paired device. Explicit user diagnostic exports may contain location text if opted in. Review current Play definitions for device-to-device transfer and user-initiated sharing before selecting questionnaire answers. Do not claim control over Zepp/Maps/Waze data practices. Host the completed privacy policy and show the in-app disclosure before notification permission.

Foreground service: `specialUse`, permission `FOREGROUND_SERVICE_SPECIAL_USE`. Manifest subtype describes the local authenticated navigation bridge. Proposed declaration: “The user taps Start bridge to relay ongoing navigation notifications to a paired watch through the Zepp app. A visible ongoing notification provides a Stop action. The service stops when requested or two minutes after navigation ends. Interrupting the service breaks active turn guidance.” Record a real-device video of start, notification permission disclosure, incoming navigation, watch updates and stop. Approval of `specialUse` is a Play review gate, not guaranteed by this implementation.

## Zepp review metadata draft

Category: Utilities / Navigation. Devices: Active Edge (360 round), Active 2 Round (466 round) and the existing 480-round target, API 3.5. Permission: `device:os.bg_service`, needed to receive updates/vibrate when the page sleeps. Explain battery use, two-minute idle and five-minute disconnected service limits. Supply paired Android APK installation instructions, privacy/support URL, real watch screenshots and an English/Persian demo. Disclose the one-time local bridge-code setup and that the Android companion must remain running. No server credentials or real routes in review screenshots.

## Physical acceptance matrix (all pending)

Test with owner hardware, record phone model/Android, Zepp version, watch firmware and Maps/Waze versions. Test while stationary or with a passenger; do not operate setup controls while driving.

| Test | Pass criteria | Result |
|---|---|---|
| Local bridge from real Zepp | Authenticated loopback fetch succeeds with correct code, fails with wrong code | Pending |
| Both sources / both languages | Real notification text and icons map correctly; ambiguous cases remain unknown. Text parser is English/Persian only; other locales must be icon-only. | Pending |
| All 19 maneuver presets | Each row, defaults, reset, mute and one-shot Test behave correctly | Pending |
| Identity | Duplicate, ETA, distance, theme and heartbeat changes remain silent; distinct next instructions vibrate once | Pending |
| Sleep and raise | AppService continues; current card appears without replay when raised/reopened | Pending |
| Display | On both watches, maximum page bright time and wrist-drop pause work independently; timeout/wrist behavior restores on idle, stale, low battery and exit; wrist raise relaunches an active route | Pending |
| Persian | Correct shaping/bidi, physical arrow direction unchanged, readable two-line long street names | Pending |
| Circular layout | No clipped distance/arrow/street/ETA inside the inscribed safe circle on 360, 466 and 480 round screens | Pending |
| Phone locked / Zepp background | Normal delivery within two seconds | Pending |
| Battery saver / permission revoked | No crash, honest stale/disconnected state, permission recovery | Pending |
| Temporary Bluetooth loss | Last card marked stale; cached acknowledged instruction not replayed | Pending |
| 60-minute trip | At equal brightness, record battery at 0/30/60 minutes for always-on and normal-timeout modes; use drain/hour + 5 points (minimum 15%) as the final cutoff | Pending |
| App Service maintenance | With the screen off, verify seven minutes of minute callbacks, idle exit in 2–3 minutes and dead-bridge exit in 5–6 minutes; use the API-3 alarm fallback only if this fails | Pending |
| Restart behavior | Verify reboot, power mode, language change, app update, BLE loss and Zepp restart. API-3.5 restart behavior without `reload` is not assumed. | Pending |
| Port conflict / key rotation | New code required and old credentials fail | Pending |
| Session end | Notification removal grace works; no saved route content; auto-stop works | Pending |
| Network capture | MiniNav runtime traffic stays local/paired; no MiniNav cloud endpoint | Pending |

Public rollout requires these results, corrected real-world parser fixtures, signed owner builds, policy review and internal/closed testing first.

## Primary implementation references

- [Zepp device catalog](https://docs.zepp.com/docs/v2/reference/related-resources/device-list/)
- [Zepp architecture and ZML](https://docs.zepp.com/docs/guides/architecture/arc/)
- [AppService capabilities and restrictions](https://docs.zepp.com/docs/guides/framework/device/app-service/)
- [Firmware vibrator scenes](https://docs.zepp.com/docs/reference/device-app-api/newAPI/sensor/Vibrator/)
- [System background timer](https://docs.zepp.com/docs/reference/device-app-api/newAPI/timer/createSysTimer/)
- [Zepp system Noto Sans](https://docs.zepp.com/docs/designs/visual/font/)
- [Android notification listener](https://developer.android.com/reference/android/service/notification/NotificationListenerService)
- [Android special-use foreground service](https://developer.android.com/develop/background-work/services/fgs/service-types#special-use)
