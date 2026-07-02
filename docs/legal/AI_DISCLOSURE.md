# StarGaze AI — AI Disclosure & Transparency

**Effective date:** 2026-06-07

This document explains how StarGaze AI uses artificial intelligence, in plain language, in line with
transparency expectations (including the EU AI Act's transparency principles and app-store GenAI
policies).

## You are interacting with AI

The "AI Sky Guide" is an automated system. When you chat with it, you are not talking to a human.
Answers are generated automatically and **may be incomplete or wrong**. They are for education and
general information only and are not professional, medical, legal, financial, navigational, or safety
advice.

## How the AI generates answers (three tiers, most-private first)

1. **On-device knowledge engine.** Deterministic answers computed locally from open astronomical
   data (positions, visibility, facts). No data leaves your device.
2. **On-device AI model (optional).** If you download it, a local language model (**Google Gemma 3n**)
   runs entirely on your phone. Your questions are processed locally and are **not** transmitted.
   Subject to the Gemma Terms of Use and Prohibited Use Policy (see `ATTRIBUTIONS.md`).
3. **Cloud AI (opt-in, off by default).** Only if you explicitly enable it, your question text and
   coarse location/time are sent over HTTPS to our AI proxy, which calls a third-party large language
   model. You can disable this at any time in Settings.

## What we do to use AI responsibly

- **Privacy by default:** the most private tier that can answer is used first; cloud AI requires opt-in.
- **No training on your data by us:** we do not use your questions to build user profiles.
- **Safety framing:** the guide presents itself as an educational tool and discourages reliance for
  safety-critical decisions.
- **Human oversight:** you remain in control — you choose what to ask and whether to use cloud AI.

## Limitations

AI models can "hallucinate" (state confident but incorrect information), can reflect biases in their
training data, and do not have real-time awareness beyond the data and computations the app provides.
Always verify important information with an authoritative source.

## Your controls

- Turn cloud AI on/off in Settings (off by default).
- Download or remove the on-device AI model at any time.
- Use the app fully offline with the on-device tiers only.
