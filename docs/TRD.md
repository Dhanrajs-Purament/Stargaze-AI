# StarGaze AI — Technical Requirements Document (TRD)

**Document status:** v1.0
**Last updated:** 2026-06-07
**Companion docs:** [PRD.md](./PRD.md), [IMPLEMENTATION_PLAN.md](./IMPLEMENTATION_PLAN.md)

---

## 1. Platform decision: Native Kotlin + Jetpack Compose (not Flutter)

### Decision
Build the flagship Android app in **native Kotlin + Jetpack Compose**. Serve the Web + Telegram
mini-app surfaces with the **existing HTML/JS prototype** (already a working PWA / TG web build).

### Rationale
The app's competitive wedge is **smooth, low-latency AR** and **60fps custom sky rendering** — the
exact thing the incumbent does badly. For a sensor-fusion + custom-renderer product, the decision
factors favour native:

- **Sensor latency:** native `SensorManager` (`TYPE_ROTATION_VECTOR` / `TYPE_GAME_ROTATION_VECTOR`)
  gives the lowest-latency, highest-frequency orientation stream. Flutter routes high-frequency
  sensor data across a platform channel, adding jitter/latency — directly the "janky AR" failure mode.
- **Camera passthrough:** first-class CameraX vs plugin bridge.
- **Custom 60fps canvas:** Compose Canvas draws on the native pipeline with no per-frame bridge crossing.
- **OS integration:** Play Billing, widgets, permissions, haptics — all first-party.
- **UI/UX polish:** Compose is fully mature for the cinematic glassmorphism UI in the prototype;
  this column is ~a tie with Flutter, so it is not a reason to pay the AR latency tax.

The single column Flutter wins — multi-platform — is already covered by the existing web prototype,
so choosing native costs us nothing there.

### Consequence
Best-possible AR UX on Android; zero rewrite for Web/Telegram; aligns with the installed Android skills.

## 2. Tech stack

| Layer | Choice | Version |
|---|---|---|
| Language | Kotlin | 1.9.24 |
| UI | Jetpack Compose (BOM) | 2024.06.00 |
| Min / Compile / Target SDK | 24 / 34 / 34 | — |
| JVM target | 17 | — |
| Async | Coroutines + Flow | 1.8.x |
| DI | Hilt | 2.51.1 |
| Navigation | Navigation-Compose | 2.7.x |
| Local prefs | DataStore (Preferences) | 1.1.x |
| Encrypted storage | `androidx.security:security-crypto` (EncryptedSharedPreferences) | 1.1.0-alpha06 |
| Networking | Retrofit + OkHttp + kotlinx-serialization | 2.11 / 4.12 / 1.6.x |
| Sensors | `SensorManager` rotation vector | platform |
| Build | AGP + Gradle wrapper | 8.5.x / 8.9 |
| Tests | JUnit4, kotlinx-coroutines-test, Turbine, Compose UI test | — |

## 3. Module architecture

Offline-first, unidirectional data flow, reactive Flow streams, testable by interface (NowInAndroid-aligned),
kept pragmatically lean (no convention-plugin sprawl for a v1 of this size).

```
StarGaze-AI/
├── settings.gradle.kts
├── build.gradle.kts                      # root, plugin aliases (apply false)
├── gradle/libs.versions.toml             # version catalog (single source of truth)
├── core/
│   └── astronomy/                        # PURE kotlin("jvm") — no Android deps
│       ├── time   (Julian, GMST, LST)
│       ├── coords (RA/Dec → Alt/Az, projection-independent)
│       ├── ephemeris (Schlyter Sun/Moon/planets)
│       ├── satellites (orbital pass model)
│       ├── catalog (stars, constellations, planets, events)
│       └── knowledge (offline AI knowledge engine)
└── app/                                  # Android application (Compose)
    ├── di            (Hilt modules)
    ├── data          (location, settings, AI repository)
    ├── network       (Retrofit AI service, interceptors)
    ├── sensors       (orientation fusion + smoothing)
    ├── render        (SkyProjection, SkyRenderer on Compose Canvas)
    ├── ui/theme      (cinematic dark theme, type, color)
    ├── ui/sky        (Sky screen + ViewModel)
    ├── ui/tonight    (Tonight screen + ViewModel)
    ├── ui/guide      (AI Guide screen + ViewModel)
    ├── ui/learn      (Learn + Quiz screen + ViewModel)
    ├── ui/pro        (Pro/paywall screen)
    ├── ui/onboarding (3-step onboarding)
    └── MainActivity / StarGazeApp / navigation
```

**Why `:core:astronomy` is pure JVM:** the math is the highest-risk, most-testable part. Keeping it
free of the Android framework means it compiles and unit-tests in CI/locally without an emulator,
and is reusable. It is the source of truth for all celestial computation.

## 4. Astronomy engine (correctness is the product)

Ported and hardened from the validated JS prototype, with named constants and types.

- **Time:** `julianDay`, `gmst` (Greenwich Mean Sidereal Time), `localSiderealTime(lon)`.
- **Coordinate transform:** equatorial (RA/Dec) → horizontal (Alt/Az) via hour angle and observer latitude.
- **Ephemeris (Paul Schlyter model):** Sun, Moon (with perturbation terms kept minimal/correct),
  Mercury–Saturn via Keplerian elements + heliocentric→geocentric→equatorial chain.
- **Satellites:** deterministic, plausible pass model (alt/az over orbital phase) for ISS/HST/CSS;
  designed so a real **SGP4 + live TLE** source can replace the provider without touching callers.
- **Catalog:** 70+ bright stars `[name, RA°, Dec°, mag, constellation, blurb]`, 16 constellation
  figures (line pairs), 7 solar-system bodies, curated sky events.
- **Knowledge engine:** deterministic offline answerer — resolves object names, computes live
  visibility, returns structured facts. This is the **real** offline AI layer, not a stub.

### Verification
Unit tests assert engine outputs against independently-known values:
- GMST at J2000 epoch.
- Polaris altitude ≈ observer latitude (it sits at the pole).
- Circumpolar/never-set logic for high-dec stars at given latitude.
- Sun declination within seasonal bounds; equinox/solstice sanity.
- Round-trip stability and `[0,360)` / `[-90,90]` range invariants.

## 5. Rendering & sensor fusion

- **Projection:** gnomonic/stereographic-style projection of (az,alt) onto screen given view
  center (az,alt) and FOV; identical math to the prototype `project()`.
- **Renderer:** Compose `Canvas` (`drawWithCache`/`Canvas`) driven by a `withFrameNanos` loop in the
  ViewModel/holder; redraws star/planet/satellite/constellation layers, labels with collision
  avoidance, twinkle, meteors, Milky Way, reticle, compass.
- **Sensor fusion:** `TYPE_ROTATION_VECTOR` → rotation matrix → remap for the device pointing
  direction → azimuth/pitch. **Low-pass / slerp smoothing** on the orientation quaternion to remove
  jitter (the explicit fix for SkyView's jank). Map-mode fallback when sensor unavailable/unreliable.
- **Performance:** cache horizontal coordinates and recompute on a throttle (≈1.2s) since star
  positions change slowly; only the camera transform updates per frame.

## 6. AI layer (real, layered — no mock)

```
AiGuideRepository
 ├── RemoteAiDataSource   (Retrofit → HTTPS LLM proxy; streaming/JSON)
 └── OfflineKnowledgeEngine (core:astronomy.knowledge; deterministic, location-aware)
```

Resolution order:
1. If network available **and** a backend base URL is configured → call remote LLM proxy.
2. On any failure (offline, timeout, error, or no URL configured) → **fall back to the offline
   knowledge engine**, which still gives a real, computed, location-aware answer.

This guarantees the feature works fully offline and never shows a fake/canned "I can't" — it is a
genuine two-tier system. The free-tier daily question cap is enforced client-side via DataStore and
is intended to be re-validated server-side when the backend lands.

## 7. Security (highest priority — defence in depth)

| Threat | Control |
|---|---|
| Secret leakage | **No API keys in the app.** LLM calls go to a first-party HTTPS proxy that holds the key server-side. Any client tokens stored in **EncryptedSharedPreferences** (AES-256, Android Keystore). |
| MITM / network tampering | HTTPS-only; `usesCleartextTraffic=false`; Network Security Config forbids cleartext; certificate/timeout config on OkHttp; optional certificate pinning hook. |
| Injection / malformed responses | All network input treated as untrusted: kotlinx-serialization with `ignoreUnknownKeys`, explicit validation, bounded sizes, no `eval`-like dynamic execution anywhere. |
| Prompt-side abuse | User text length-capped and sanitized before send; responses rendered as text (no HTML injection into native views). |
| Reverse engineering | R8/minify + resource shrinking in release; no debug logging of sensitive data in release (logging interceptor gated to debug builds). |
| Over-permissioning | Least privilege: `INTERNET`, `ACCESS_COARSE/FINE_LOCATION` (runtime, optional), `CAMERA` (optional for AR), `VIBRATE`. No storage, contacts, or background-location. `allowBackup=false` to avoid leaking app data via ADB backup. |
| Data at rest | DataStore for non-sensitive prefs; EncryptedSharedPreferences for any sensitive value. Location kept on-device. |
| Tapjacking / exported components | Only `MainActivity` exported (launcher); `android:exported` explicit everywhere; no exported providers/receivers. |
| Supply chain | Pinned dependency versions via version catalog; reputable libraries only. |
| Input at boundaries | Location lat/lon clamped to valid ranges; time offsets bounded; all catalog indices bounds-checked. |

Build-config flag `AI_PROXY_BASE_URL` is injected at build time (empty by default → app runs fully
offline). It is **not** a secret; the secret lives only on the proxy.

## 8. Testing strategy
- **Unit (JVM, runs here):** `core:astronomy` — time, coordinates, ephemeris, knowledge engine.
- **Repository:** AI repository fallback logic (remote failure → offline) with fakes (test doubles, no mocking lib).
- **UI:** Compose UI tests for navigation and key screens (run on device/emulator in CI).
- **Verification loop:** after each change, compile + run the relevant test suite; fix root cause before proceeding.

## 9. Build & CI notes
- Gradle wrapper committed; `libs.versions.toml` is the single version source.
- Release build: `isMinifyEnabled=true`, `isShrinkResources=true`, ProGuard/R8 keep rules for
  serialization models and Compose.
- This environment has **JDK 21 but no Android SDK**, so `:core:astronomy` (pure JVM) is fully
  compiled and tested here; the Android `:app` module is authored to compile against SDK 34 in a
  standard Android toolchain (Android Studio / CI with the SDK installed).
