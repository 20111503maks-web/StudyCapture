# ADR 0001 — v0.1 AI is free, on-device and extractive

Status: accepted (2026-10-22)

## Context

The owner asked for "free AI". The product requirements also say:

- notes are private, never used to train public models, images are not sent anywhere without consent;
- API keys must never be stored in the Android app;
- AI must not present unverified claims as the content of the user's material.

Options considered:

| Option | Cost | Privacy | Needs server | Verdict |
|---|---|---|---|---|
| Free tier of a cloud LLM called from the app | free with limits | free tiers may use inputs to improve models; key would sit in the APK | no (but key leaks) | rejected |
| Cloud LLM through our own gateway | paid per call + hosting | good with consent | yes | later (Phase 2+) |
| On-device LLM (e.g. Gemma via LiteRT-LM / MediaPipe) | free | excellent | no | later: 0.5–1.5 GB download, slow on older phones, still can hallucinate |
| **Deterministic on-device engine** (`core/understanding`) | free | excellent | no | **chosen for v0.1** |

## Decision

v0.1 ships `OnDeviceStudyAssistant`, built on `core/understanding`:

- summaries are **extractive**: they select sentences, definitions and formulas from the pages and quote them verbatim with a `SourceRef`;
- flashcards use templates for the question ("What is X?") and **verbatim material** for the answer;
- subject suggestions come from a keyword lexicon plus the user's own past choices.

The app declares no `INTERNET` permission at all.

## Consequences

- Zero cost, works offline, no hallucinated facts.
- "Explain / Simplify / Give example" are **not** in v0.1 — they need a generative model. They are not faked.
- `StudyAssistant` is the provider seam: an on-device LLM or a consented cloud gateway can implement it later. Generated text must then carry `Provenance.AI_EXPLANATION` or `AI_EXTRA` and the UI must label it.
