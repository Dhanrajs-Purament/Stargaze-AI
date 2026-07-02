# StarGaze AI — Google Play Data Safety (answer sheet)

> Use this to fill the Play Console "Data safety" form. Answers reflect the v1 (no-login,
> offline-first, cloud-AI-opt-in) build. Re-verify if you add analytics, ads, or accounts.

## Does your app collect or share any of the required user data types?

**Default build (cloud AI OFF):** No personal data is collected or shared.
**If cloud AI is enabled by the user:** limited data is *processed* (sent to our AI proxy) as below.

## Data types

| Category | Collected? | Shared? | Processed ephemerally? | Required? | Purpose |
|---|---|---|---|---|---|
| Location (approx/precise) | Only if cloud AI enabled | Sent to AI proxy only then | Yes | Optional | App functionality (compute sky / answer location-aware questions) |
| App activity (AI questions) | Only if cloud AI enabled | Sent to AI proxy/provider | Yes | Optional | App functionality (generate answer) |
| Camera | No (on-device only, not collected) | No | N/A | Optional | AR background only |
| Personal identifiers, contacts, financials, photos, messages | No | No | — | — | — |
| Advertising ID | No | No | — | — | — |

## Security & handling declarations

- **Data encrypted in transit:** Yes (HTTPS/TLS, cleartext disabled).
- **Users can request deletion:** Yes — account-less; clear app data/uninstall removes on-device
  data. For cloud-AI data, contact privacy@hacklet.in.
- **Data collection optional:** Yes — cloud AI is opt-in and off by default.
- **Independent security review:** Not yet performed; planned before public launch.
- **Committed to Play Families policy:** No (general audience, 13+). Change if you ship a Kids edition.

## Permissions justification (for Play listing)

- `ACCESS_FINE/COARSE_LOCATION` — compute the local sky; optional, app works with a default location.
- `CAMERA` — live AR background; optional, app works in map mode without it.
- `INTERNET` — optional cloud AI and on-device model download.
- `VIBRATE` — haptic feedback.
