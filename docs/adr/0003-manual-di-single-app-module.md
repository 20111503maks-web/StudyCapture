# ADR 0003 — Manual DI and one Android module for v0.1

Status: accepted (2026-10-22)

## Context

The plan called for Hilt and per-feature Gradle modules. Each adds build configuration
(KSP processors, plugin compatibility with AGP 9 built-in Kotlin) that could not be compiled
locally while v0.1 was written (the Android SDK host was unreachable from the dev machine).

## Decision

- Dependencies are wired by hand in `AppContainer`; ViewModels are created with
  `viewModelFactory { initializer { … } }` via `appViewModel {}`.
- Android code lives in one `:app` module, organized by package
  (`data`, `ocr`, `processing`, `ai`, `ui/<feature>`).
- All logic that does not need Android lives in pure Kotlin modules:
  `:core:model`, `:core:srs`, `:core:understanding` — these are the code a Windows
  (Compose Desktop) client will reuse.

## Consequences

- Fewer moving parts; swapping a provider is one line in `AppContainer`.
- When screens grow, `ui/<feature>` packages can be lifted into `:feature:*` modules
  without changing their code. Hilt can be introduced at the same time.
