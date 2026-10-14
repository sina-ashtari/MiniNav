# Contributing to MiniNav

Thanks for contributing. Please discuss substantial changes in an issue before writing code.

## Workflow

1. Create an issue, then branch from the latest `main` using `feature/`, `fix/`, or `docs/`.
2. Keep each pull request focused and include tests when behavior changes.
3. Run the checks that apply to your change:

   ```powershell
   Set-Location 'Android App'; .\gradlew.bat testDebugUnitTest
   Set-Location '..\ZEPP App'; npm.cmd ci; npm.cmd test
   ```

4. Open a pull request against `main`, complete the template, and address review comments.

## Safety and privacy

Do not commit signing keys, `.env` files, connection codes, notification captures, or personal route data. Follow the limitations and physical-device checks in [`docs/RELEASE.md`](docs/RELEASE.md).
