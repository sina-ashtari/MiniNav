# Notification fixture coverage gate

Capture only while stationary or by a passenger. Before committing a fixture, the owner reviews it, replaces streets and destinations with stable placeholders, removes device identifiers, and confirms no original export or bitmap is included. The fixture leak test must reject unexpected words, URLs, coordinates and credential-shaped values.

| Maneuver | Maps English | Maps Persian | Waze English | Waze Persian |
|---|---|---|---|---|
| Straight | not yet captured | not yet captured | not yet captured | not yet captured |
| Slight / ordinary / sharp / keep left/right | not yet captured | not yet captured | not yet captured | not yet captured |
| U-turn left/right | not yet captured | not yet captured | not yet captured | not yet captured |
| Roundabout left/right | not yet captured | not yet captured | not yet captured | not yet captured |
| Exit left/right | not yet captured | not yet captured | not yet captured | not yet captured |
| Merge, rerouting, arrival, unknown | not yet captured | not yet captured | not yet captured | not yet captured |

Record each cell as `text`, `icon`, `both`, `not exposed by source`, or `not yet captured`; include light/dark mode plus source version, locale, Android API, density and candidate slot. Parser/template changes are not ready for release until this table and its sanitized fixtures are reviewed.
