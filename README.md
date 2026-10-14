# MiniNav

MiniNav brings turn-by-turn navigation notifications from an Android phone to a Zepp OS watch. It comprises a local-only Android bridge and a Zepp mini app for **Amazfit Active Edge (360 × 360)**, **Active 2 Round (466 × 466)**, and the existing **480-pixel round** target (API 3.5).

> Development implementation only — it has not yet been device-certified or prepared for store release.

## Repository layout

- [`Android App`](Android%20App): Kotlin/Compose Android companion (Android 8+, target API 36) with a user-started foreground service and bounded, authenticated, read-only localhost bridge.
- [`ZEPP App`](ZEPP%20App): Zepp page, background AppService, phone-side polling service, and one-time bridge settings. It uses the official ZML MessageBuilder transport and firmware vibration scenes.
- [`docs`](docs): protocol, privacy, release notes, and the physical-device acceptance checklist.
- [`diagnostics`](diagnostics): small development probes used to validate Zepp tooling and target behavior; not part of the shipping app.

## Requirements

- Android: JDK 17–22 and Android SDK 36.
- Watch app: Node.js 22+ and a Zepp developer setup capable of installing development packages.
- A physical Android phone and supported watch for end-to-end validation. The emulator and unit tests cannot certify notification parsing, watch lifecycle behavior, or vibration timing.

## Build and test

Android: set `JAVA_HOME` and `ANDROID_HOME`, then run:

```powershell
Set-Location 'Android App'
.\gradlew.bat testDebugUnitTest assembleDebug bundleRelease
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. Release AAB: `app/build/outputs/bundle/release/app-release.aab`. Release builds are **unsigned** unless the publisher signing environment variables in `docs/RELEASE.md` are supplied. Debug APKs use Android's development key, not a store key.

Watch:

```powershell
Set-Location 'ZEPP App'
npm.cmd ci
npm.cmd test
npm.cmd run build
```

Zeus downloads its official device/compiler resources as needed. `scripts/zeus.cjs` fixes local CLI alias resolution and suppresses its version check and CLI telemetry. Runtime app code has no analytics. Build packages appear in `dist`.

## First trip

1. Install the Android debug APK; install the watch package using Zepp developer preview/internal distribution.
2. Open Android MiniNav, read the disclosure and enable notification access. Start the bridge.
3. Copy the connection code. In the Zepp app, open MiniNav's settings and paste the code. It is a local secret; don't share it. The clipboard is cleared after one minute while the Android process remains alive.
4. Open MiniNav on the watch and allow its background-service permission. Start a route in Maps or Waze.
5. Configure colors, screen mode and vibration patterns in Android. **Test** sends a short-lived, one-shot watch command. Master mute also mutes tests.
6. Stop the Android bridge after the trip. Idle/ended navigation auto-stops it after two minutes. The watch service exits after two idle minutes or five minutes without the phone bridge.

Use the English/Persian demo buttons only while stationary; Clear ends their synthetic session. Demo notifications never become saved history. Android's connection label confirms **Zepp's phone-side polling**, not Bluetooth delivery to the watch.

## Known limits / release gates

- Notification formats are not public navigation APIs. Parsing fixtures are representative, not recorded from this owner's devices. Real notification samples and icon validation are required for both apps/languages. Unrecognized guidance remains unknown; unsupported icon shapes must not be advertised as supported.
- Instruction identity uses session, maneuver, instruction text stripped of distance/ETA, and street. Distinct successive left turns on different streets work. Two turns with completely identical exposed instruction/street cannot reliably be separated without a route-step identifier the source does not provide. No distance-reset guessing is used.
- Vibration acknowledgements and current navigation are held in watch RAM across page recreation and BLE reconnects. A terminated/restarted watch app loses that RAM; exactly-once delivery across a process crash is not claimed. No route content is written to disk.
- Local HTTP access from the installed Zepp Android version, background execution, Persian shaping, watch lifecycle/shared App context and vibration timing require the physical test checklist. Firmware and Android battery policies can interrupt delivery. Never rely solely on the watch for driving decisions.
- The watch uses firmware Noto Sans. Persian glyph shaping/bidi and line clipping must be checked on Active Edge and Active 2 Round; arrow geometry is never mirrored for RTL.
- The Zepp app ID `20001`, vendor and Android package name are development placeholders. Replace them with owner-approved identifiers before distribution. No store upload or public release is performed.
- Police/speed-camera alerts, accounts, backend services, navigation history, analytics, ads and arbitrary vibration sequencing are intentionally excluded.

See [plan coverage and remaining gaps](docs/IMPLEMENTATION-STATUS.md), [release and acceptance checklist](docs/RELEASE.md) and [protocol](docs/PROTOCOL.md).

## Repository hygiene

The repository intentionally excludes dependency directories, build output, Android SDK paths, local IDE/tool state, signing material, environment files, logs, and generated diagnostic packages. Keep credentials and release keys out of commits.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the branch, test, pull-request, and privacy expectations.
