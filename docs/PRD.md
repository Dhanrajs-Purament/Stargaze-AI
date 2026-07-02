# StarGaze AI — Product Requirements Document (PRD)

**Document status:** v1.0 (living)
**Owner:** Founder / Product
**Last updated:** 2026-06-07

---

## 1. One-line thesis

SkyView is a beloved but frozen-in-2014 AR stargazing app with 40M+ downloads and almost no
meaningful updates. StarGaze AI is the **AI-native, beautifully-redesigned sky companion** that
closes that gap: point your phone at the sky and a conversational AI guide identifies, explains,
teaches, and personalizes — beautiful enough for a beginner, deep enough for a hobbyist.

## 2. Problem statement

The dominant beginner AR sky app (SkyView) is loved for its concept but criticised for:

- **Shallow catalog** — "missing stars" complaints (e.g. Mira in Cetus).
- **Dated UI/UX** — design language is firmly 2014.
- **Janky AR tracking** — users disable AR because it lags.
- **Sensor/compass issues** — fails near magnetic phone cases.
- **No depth, no personalization, no learning path, no AI.**

These complaints are the roadmap. No incumbent (SkySafari, Stellarium, Star Walk, Sky Tonight)
owns a genuinely conversational, personalized **AI sky tutor**. That is the white space.

## 3. Goals & non-goals

### Goals (v1)
1. Deliver the "point at the sky → instant identification" magic moment with **smoother** AR than SkyView.
2. Ship a **conversational AI Sky Guide** that gives real, location-aware answers (online LLM + offline knowledge engine).
3. Provide a personalized **"Tonight for you"** brief.
4. Provide **Learn** (guided tours, gamified quizzes, badges).
5. Offline-first core catalog (bright stars, planets, constellations, ISS/satellites).
6. Establish the **freemium + subscription + lifetime** monetization scaffold (Pro screen).

### Non-goals (v1)
- Full Gaia 1.7B-star streaming catalog (Pro/v2).
- Telescope hardware control (v2+).
- Community feed / social graph (v2+).
- Astrophotography planner (Pro/v2).
- The Web/Telegram surface is served by the **existing HTML/JS prototype**, not rebuilt natively.

## 4. Target users / personas

| Persona | Need | v1 fit |
|---|---|---|
| **Curious beginner / family** | Zero learning curve, "what is that?" | Core AR + AI Guide |
| **Student / educator** | Learn astronomy, quizzes, curriculum hooks | Learn tab, quiz, AI explanations |
| **Hobbyist astronomer** | Depth, accuracy, "what's up tonight" | Tonight brief, deep object cards |
| **Cosmic-curious** | Light, delightful exploration | Beautiful UI, tours |

Primary launch geography: **India-first + global**. Default location Mumbai (19.07°N, 72.88°E)
until the user grants location permission.

## 5. v1 feature scope (the five surfaces)

The app is organised around five bottom-nav destinations, mirroring the validated prototype.

### 5.1 Sky (the hero)
- Live AR sky map: camera/sensor-driven or drag-to-explore.
- Real astronomy: stars, planets, Sun, Moon, constellations, satellites (ISS/HST/CSS).
- Sensor fusion (rotation vector) with **smoothing** to fix the "lag/jank" complaint.
- Toggles: constellation lines, labels, satellite tracking, time-travel slider, recenter.
- Night (red) mode for dark adaptation.
- Tap any object → detail card (facts, altitude/azimuth, "point me to it", "ask the AI").
- Search across all objects.
- Motion graphics: twinkle, meteors, Milky Way band, selection reticle.

### 5.2 Tonight
- Personalized sky brief for the user's location and local time.
- "Up right now" list (planets, bright stars, satellites overhead) — tap to locate on the map.
- Sky quality score.
- Upcoming sky events (ISS passes, full moon, meteor showers, oppositions, eclipses).

### 5.3 AI Guide
- Conversational chat. Ask "what should I look at tonight?", "why is Mars red?", "where is the ISS?".
- **Two real layers (no mock):**
  - **Online:** real LLM via secure HTTPS API (key never in the binary; server-proxied).
  - **Offline:** deterministic local knowledge engine that computes live alt/az for any catalog
    object and answers from a structured astronomy knowledge base.
- Suggested prompts, voice-input affordance, location-aware answers.
- Free tier: limited questions/day; Pro: unlimited.

### 5.4 Learn
- Guided tours ("Your First Night", "Catch the ISS", "Spot All the Planets", "Reading Orion").
- Daily quiz with scoring, streaks, haptics.
- Badges (earned/locked).

### 5.5 Pro (paywall)
- Plans: Annual (anchor), Monthly, Lifetime — localized USD/INR.
- Feature list: unlimited AI, deep catalog, satellite alerts, all tours/quizzes,
  astrophotography planner, ad-free/offline.
- Telegram Stars note for the mini-app surface.
- v1 is a non-charging scaffold wired for Play Billing integration.

## 6. Onboarding
Three-step: brand splash → value props → location permission (with skip-to-default).
First-run magic moment: "point at the Moon."

## 7. Monetization (product decision, evidence-based)
- **Do NOT** do pure one-time purchase or ads-as-primary.
- **DO** freemium + subscription (primary) + lifetime one-time (anti-subscription segment, big in India) + Telegram Stars (mini-app).
- Suggested pricing: Pro Monthly $3.99 / ₹149; Annual $19.99–24.99 / ₹599; Lifetime $44.99 / ₹1,799.

## 8. Success metrics
| Metric | Target (90 days post-launch) |
|---|---|
| D1 / D7 retention | 35% / 15% |
| AR "magic moment" completion in onboarding | >70% |
| AI Guide messages per WAU | >3 |
| Free→Pro conversion | 2–4% |
| Crash-free sessions | >99.5% |
| Median sky-frame rate | ≥55 fps on mid-range Android |

## 9. Quality, accessibility, localization
- 60fps target; graceful map-mode fallback when sensors are poor/absent.
- Accessibility: content descriptions, dynamic type, sufficient contrast, large tap targets.
- Localization-ready (English first; Hindi + regional planned). All strings externalized.

## 10. Security & privacy (product-level)
- Location used only to compute the local sky; never sold; on-device by default.
- No third-party tracking SDKs in v1.
- No secrets in the app binary. LLM access is server-proxied.
- Full technical controls in the TRD §Security.

## 11. Risks & mitigations
| Risk | Mitigation |
|---|---|
| AR jank on cheap Android | Rotation-vector fusion + low-pass smoothing + map-mode fallback |
| AI cost per user | Offline knowledge engine answers most queries; rate-limit free tier; cache |
| Incumbent copies AI | Move fast; own India + education + Telegram niches |
| Trademark (SkyView®) | Original brand, original assets, original code |

## 12. Release criteria (v1 "done")
- All five surfaces functional with real data (no placeholders/mocks).
- Astronomy engine unit-tested against known ephemeris values.
- Offline mode fully usable with no network.
- Security checklist (TRD §Security) passed.
- App builds release (R8/minify) and runs crash-free through the core flows.
