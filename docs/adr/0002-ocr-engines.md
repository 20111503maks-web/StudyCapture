# ADR 0002 — OCR: ML Kit for Latin, Tesseract for Cyrillic

Status: accepted (2026-10-22)

## Context

ML Kit Text Recognition v2 is fast and accurate on-device, but supports Latin, Chinese,
Devanagari, Japanese and Korean scripts only — **no Cyrillic**. Many users write in Russian.

## Decision

- `MlKitLatinOcr` (bundled model, offline) always runs.
- When "Russian (Cyrillic) text" is on (default when the phone language is ru/uk/be/kk/bg/sr),
  `TesseractOcr` (Tesseract4Android 4.9.0, Apache-2.0, `rus+eng` tessdata_fast) also reads the page.
  If ≥30 % of its letters are Cyrillic, its result is used; otherwise ML Kit's.
- Language data (≈8 MB) ships inside the APK, so no download is needed.
- Edge detection, perspective correction and cleanup come from the ML Kit Document Scanner
  (Google Play services). Gallery imports get rotation and contrast only — the UI says so.

## Consequences

- Russian pages take roughly twice as long (two engines).
- Handwriting and formulas are recognized poorly by both engines; low-confidence lines are
  marked "Not sure — please check" and are excluded from summaries and flashcards.
- Phones without Google Play services cannot use the scanner; "Import photos" still works.
- Later: a consented cloud vision model can implement `OcrEngine` for handwriting and formulas.
