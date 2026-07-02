# StarGaze AI — Attributions & Licenses

This app builds on open astronomical data, open-source libraries, and an open AI model. We are
grateful to these projects and comply with their licenses. Replace/extend as the data sources evolve.

## Astronomical data

- **Star positions & magnitudes** — derived from the Hipparcos/Tycho and HYG catalog traditions
  (public domain / permissive). Bundled subset curated for offline use.
- **Planetary & lunar positions** — computed using the low-precision algorithms of **Paul Schlyter,
  "How to compute planetary positions"** (used by permission of the freely published method).
- **Deep-sky / future deep catalog** — ESA **Gaia** data (Gaia DR2/DR3) under ESA's data terms,
  when the Pro deep-catalog tier is enabled.
- **Satellite elements** — orbital data conventions from **CelesTrak** TLEs (when the live satellite
  source is enabled). Attribution to CelesTrak / Dr. T.S. Kelso.

> Action item: confirm and pin the exact catalog files and their license texts you ship, and include
> each upstream license in the app's open-source notices screen.

## On-device AI model

- **Google Gemma 3n** (E2B / E4B), run via **MediaPipe LLM Inference / Google AI Edge**.
  Use is subject to the **Gemma Terms of Use** and the **Gemma Prohibited Use Policy**
  (https://ai.google.dev/gemma/terms). The model is downloaded to the user's device on demand; it is
  not bundled in the APK. Display the required Gemma attribution and comply with the prohibited-use
  policy (no generating disallowed content).

## Cloud AI

- The optional cloud tier calls a third-party large language model through our first-party proxy.
  The provider's acceptable-use and data policies apply to data sent when the user opts in.

## Key open-source libraries

- Kotlin, Kotlin Coroutines, kotlinx.serialization — Apache 2.0 (JetBrains)
- AndroidX (Core, Lifecycle, Activity, Navigation, DataStore, Security-Crypto), Jetpack Compose,
  Material 3 — Apache 2.0 (Google)
- Hilt / Dagger — Apache 2.0 (Google)
- Retrofit, OkHttp — Apache 2.0 (Square)
- MediaPipe Tasks GenAI — Apache 2.0 (Google)
- desugar_jdk_libs — GPL-2.0-with-Classpath-Exception (Google)

> Action item: generate a complete open-source license report (e.g., via a licenses Gradle plugin)
> and surface it in an in-app "Open-source licenses" screen before release.
