# Installation probe — deviceSource 10092803

Temporary diagnostic, not the navigation app. One native text page, no phone-side
service, settings, background service, permissions, or network traffic at runtime.
Uses the installed Zeus compiler and MiniNav's existing 124-by-124 icon.
The app ID intentionally matches MiniNav's working development ID:
installing this probe may replace an existing MiniNav installation.

From PowerShell:

```powershell
Set-Location 'E:\MiniNav\diagnostics\zepp-install-probe'
node '../../ZEPP App/scripts/zeus.cjs' preview --target 'Amazfit Active 2 (Round)' --legacy-zpk-name
```

Preview uploads the diagnostic package to Zepp and prints a QR code. Scan in
Zepp Developer Mode / Mini Program. No Android MiniNav bridge is needed.

- If installed, open it and check for `Install OK`. This narrows the problem
  toward differences in the full MiniNav package; it does not identify which one.
- If the same unzip error occurs, the navigation implementation is not required
  to reproduce it. Next investigate the shared compiler/package format, download
  path and Zepp installer. A changed-network retry can test the download path,
  but failure alone does not prove a network problem.

Keep the original MiniNav source and package untouched. To return to MiniNav,
generate its preview from `E:\MiniNav\ZEPP App` and install that QR instead.
