# MiniNav privacy policy — publication draft

Effective date: **fill before publication**. Publisher and contact: **owner must supply legal/publisher name and contact address**.

MiniNav displays navigation notifications from Google Maps or Waze on paired Amazfit Active Edge and Active 2 Round watches. It does not provide its own route planning or collect GPS measurements.

## Information processed

With your explicit Android notification-access permission, MiniNav reads current ongoing Maps/Waze navigation notifications, including instruction text, street/destination, distance, time and maneuver images. Other notification packages are filtered out before processing. Android grants broader access than MiniNav uses.

Navigation content is kept in memory for the current session. MiniNav does not save navigation history. Theme, vibration, display and source preferences, and the local connection token/port are stored on the phone. The Zepp settings page stores the connection code locally in Zepp's settings storage. Watch navigation and recent acknowledgement identifiers are held in memory.

## Transfers and sharing

Current guidance travels to the Zepp app on the same phone through an authenticated localhost endpoint, then through Zepp's paired-watch Bluetooth channel. MiniNav does not send navigation to a developer server and includes no accounts, ads, analytics or crash uploader. Maps, Waze and Zepp are separate products governed by their own policies; this policy does not describe or control their independent processing.

Diagnostics are created only when you choose an Android share recipient, as a one-shot in-memory payload; MiniNav does not automatically write reports or images. The default report excludes notification text, locations, credentials, original bitmaps and device identifiers. It contains only source/version and display metadata, field types, parser/classifier outcomes and a normalized small-icon mask. Including notification text or masks derived from large, expanded or picture icons requires a separate confirmation, because those images can expose map or lane details. Review every opt-in report before sharing. Copying a bridge code places a secret on the clipboard temporarily; never send it to another person.

## Controls, deletion and security

You can stop the bridge, revoke notification access in Android settings, rotate its connection key, forget the code in Zepp, or uninstall the apps. App preferences are removed by clearing Android app storage or uninstalling. Clear the MiniNav settings in Zepp separately. The Android app disables platform backups. A random 128-bit token protects its read-only localhost endpoint; no network interface other than loopback is bound. This does not protect against a compromised/rooted phone or access to your unlocked device.

## Safety and changes

MiniNav is a supplementary display, not a safety-critical navigation system. Notifications may be late, ambiguous or unavailable. Set up and customize the app while stationary. Publisher contact and policy updates will appear here before release.
