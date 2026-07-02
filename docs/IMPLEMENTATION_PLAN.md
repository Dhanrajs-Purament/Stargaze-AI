# StarGaze AI — Implementation Plan

**Companion docs:** [PRD.md](./PRD.md), [TRD.md](./TRD.md)
**Approach:** goal-driven, verify at each phase, root-cause fixes, no mocks.

---

## Phase 0 — Project scaffold
- Gradle wrapper, `settings.gradle.kts`, root `build.gradle.kts`, `gradle/libs.versions.toml`.
- `:core:astronomy` (`kotlin("jvm")`) and `:app` (`com.android.application`) modules.
- `gradle.properties`, `local.properties` handling, `.gitignore`.
- **Verify:** `./gradlew :core:astronomy:compileKotlin` succeeds (JVM module builds here).

## Phase 1 — Core astronomy engine (highest risk → first, test-first)
- `time/Time.kt` — Julian day, GMST, LST.
- `coords/Coordinates.kt` — `equatorialToHorizontal`, range helpers, `GeoLocation`.
- `ephemeris/Ephemeris.kt` — Sun, Moon, planets (Schlyter).
- `satellites/Satellites.kt` — ISS/HST/CSS pass model behind a `SatelliteProvider` interface.
- `catalog/` — `Stars.kt`, `Constellations.kt`, `Planets.kt`, `SkyEvents.kt`.
- `knowledge/KnowledgeEngine.kt` — offline location-aware answerer.
- **Verify:** full unit-test suite green (GMST@J2000, Polaris=lat, Sun dec bounds, ranges, knowledge answers).

## Phase 2 — App scaffold, theme, navigation
- `StarGazeApp` (Hilt `@HiltAndroidApp`), `MainActivity`, manifest (least-privilege perms, security flags).
- Cinematic dark theme (color, type, shapes) matching the prototype's design language.
- Bottom-nav scaffold with 5 destinations + onboarding flow.

## Phase 3 — Sensors + rendering (the AR wedge)
- `sensors/OrientationProvider.kt` — rotation-vector fusion + smoothing, Flow of (az,pitch,roll).
- `render/SkyProjection.kt` — pure projection math (mirrors prototype `project`).
- `render/SkyRenderer.kt` + `SkyCanvas` composable — frame loop, all layers, labels, motion graphics.
- **Verify:** projection unit-tested; renderer composable compiles; manual map-mode/AR-mode paths.

## Phase 4 — Data & AI layer (real, no mock)
- `data/LocationRepository` (FusedLocation-free fallback to default; real runtime permission path).
- `data/SettingsRepository` (DataStore: night mode, toggles, AI quota, onboarding-done).
- `network/AiService` (Retrofit) + `data/AiGuideRepository` (remote → offline fallback).
- `EncryptedSharedPreferences` for any sensitive token.
- **Verify:** repository fallback unit-tested (remote throws → offline answer returned).

## Phase 5 — Feature screens
- Sky (hero): canvas + top bar + tools + HUD + time bar + search + object/constellation detail sheets.
- Tonight: brief + "up now" + events, tap-to-locate.
- AI Guide: chat UI, suggestions, send/voice, quota.
- Learn: tours + quiz engine + badges.
- Pro: plans + features + Telegram note (Billing-ready scaffold).
- **Verify:** each screen wired to real ViewModel state; navigation works.

## Phase 6 — Hardening & verification
- Security pass against TRD §7 checklist.
- R8 keep rules; release build config.
- Accessibility content descriptions; string externalization.
- Final test run; clean up temporary files.

---

## Environment reality & how it's handled
- This box has **JDK 21, no Android SDK**. Therefore:
  - `:core:astronomy` is compiled and unit-tested **here** (it is pure JVM) — real verification of the riskiest code.
  - `:app` (Compose/Android) is authored to compile in a standard Android toolchain; its correctness
    is ensured by following Compose/Hilt/Navigation contracts precisely and by keeping all
    framework-independent logic in the tested core.

## Definition of done (per TRD §Release criteria)
All five surfaces real and functional; engine tested; offline mode works; security checklist passed;
release build configured; no mocks or placeholders in shipped paths.
