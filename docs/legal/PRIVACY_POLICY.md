# StarGaze AI — Privacy Policy

**Effective date:** 2026-06-07
**Last updated:** 2026-06-07

> This is a review-ready template prepared by the engineering team. Have it reviewed by qualified
> legal counsel for your operating jurisdictions before publication. Replace the bracketed
> placeholders (company name, contact email, governing jurisdiction) before release.

StarGaze AI ("the app", "we", "us") is an augmented-reality stargazing companion published by
**Hacklet Pvt Ltd**. This policy explains what data the app handles, why, and the
choices you have. We designed the app to be **offline-first and privacy-respecting**: by default,
your data stays on your device.

## 1. Summary (the short version)

- The app works **without an account** — there is no sign-up and no login.
- **Location** is used only on your device to compute what is in your sky. It is **not** sent to us
  by default and is never sold.
- **Camera** is used only to show the live AR background. Frames are processed on-device and are
  **not** recorded, stored, or transmitted.
- The **AI Sky Guide** answers most questions **entirely on your device**. Sending a question to the
  optional **cloud AI** happens **only if you explicitly turn it on**.
- We do **not** include third-party advertising or tracking SDKs.

## 2. Data we process and why

| Data | Purpose | Leaves your device? | Legal basis (GDPR) |
|---|---|---|---|
| Approximate/precise location | Compute the horizon, alt/az of objects, "Tonight" brief | No, unless cloud AI is enabled and a location-dependent question is asked | Consent / legitimate interest |
| Camera frames | Live AR background only | No — never stored or transmitted | Consent |
| App settings (night mode, toggles, AI usage count, Pro status) | Remember your preferences; enforce free-tier limits | No — stored locally (DataStore) | Legitimate interest |
| AI questions you type | Answer your question | Only to our AI proxy **if you enable cloud AI**; otherwise processed on-device | Consent |
| On-device AI model files | Run the offline AI | Downloaded **to** your device from Google's model host; your questions are **not** sent anywhere when using on-device AI | Consent |

We do **not** collect your name, email, contacts, photos, advertising identifiers, or behavioural
profiles.

## 3. The AI Sky Guide — how it handles your data

The AI works in tiers, most-private first:

1. **On-device knowledge engine** — deterministic answers computed locally. No data leaves the device.
2. **On-device AI model (optional download)** — a local language model (Google Gemma 3n) runs on
   your phone. Your questions are processed locally and are **not** transmitted.
3. **Cloud AI (opt-in, off by default)** — only if you turn it on, your question text and your
   coarse location/time are sent over an encrypted (HTTPS) connection to our AI proxy, which queries
   a third-party language model to generate an answer. We do not use your questions to identify you.
   You can turn this off at any time in Settings.

The AI can be wrong. It is for **education and general information only** and is **not** professional,
medical, legal, financial, or safety advice.

## 4. Sharing

We do not sell your data. We share data only:
- with our **AI model provider** (only the question/context you send when cloud AI is enabled), and
- where required by law.

## 5. Retention

- On-device data (settings, model files) stays until you clear app data or uninstall.
- Cloud AI requests are processed transiently; see our proxy/provider terms for their retention.

## 6. Your rights

Depending on where you live (e.g., **GDPR** in the EU, **India DPDP Act 2023**, **CCPA** in
California) you may have rights to access, correct, delete, or port your data, and to withdraw
consent. Because the app is account-less and stores data on your device, you can exercise most of
these directly by clearing app data or uninstalling. For cloud-AI data, contact us at
**privacy@hacklet.in**.

## 7. Children

The app is intended for users **aged 13 and older**. We do not knowingly collect personal data from
children under 13 (or the minimum age in your jurisdiction). Cloud AI is off by default. If you
believe a child has provided personal data, contact us and we will delete it. See `CHILDREN_POLICY.md`.

## 8. Security

We apply industry-standard protections: HTTPS-only networking, no secrets stored in the app,
encrypted storage for any sensitive token, least-privilege permissions, and no third-party trackers.
See our security documentation for details.

## 9. Changes

We will update this policy as the app evolves and will revise the "Last updated" date. Material
changes will be surfaced in-app.

## 10. Contact

**Hacklet Pvt Ltd** — **privacy@hacklet.in** — **Hacklet Pvt Ltd, Thane, Maharashtra, India**
