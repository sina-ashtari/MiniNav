# Balance 2 XT experimental target

The main MiniNav app now includes a `balance2xt` target for deviceSource
10486016, 10486017 and 10486019, as listed in the installed official Zeus
device catalog. These are 480-by-480 round displays. The API 4.2 minimum
is unchanged; the current catalog lists API 5.0 with backward compatibility.

The existing 466-pixel layout is centered with a 7-pixel offset on this device;
the background fills all 480 pixels. Active 2 rendering is unchanged.
The Android companion and bridge code do not need changes.

From `E:\MiniNav\ZEPP App`, generate a physical-device test QR:

```powershell
zeus.cmd preview --target "Amazfit Balance 2 XT"
```

This is test-only compatibility, not a claim of physical-device validation.
Installation, background service, connectivity, fonts, screen behavior and
vibration still need checking on the actual Balance 2 XT. It does not fix the
unresolved Zepp unzip error. Public v1 compatibility remains Active 2 Round.
