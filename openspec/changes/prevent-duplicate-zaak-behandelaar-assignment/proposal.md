## Why

Calling `assignZaak` or `assignZaakToLoggedInUser` in quick succession causes multiple `Behandelaar` roles to be linked to a zaak in OpenZaak, violating ZAC's invariant of exactly one behandelaar per zaak. This is a concurrency defect that manifests regularly in normal UI usage (e.g., double-clicks or rapid navigation).

## What Changes

- Add idempotency protection to `ZaakService.assignZaak()` so that concurrent or near-concurrent calls for the same zaak do not result in multiple `Behandelaar` roles in OpenZaak.
- Before creating a new behandelaar role, explicitly delete all existing `Behandelaar` roles for that zaak (not just the one found at read time), eliminating duplicates left by a prior racy write.
- Introduce a per-zaak lock (using a fixed-size `ReentrantLock` array keyed by zaak UUID hash — standard `java.util.concurrent.locks`, no extra dependencies) at the service boundary so that concurrent assignment calls for the same zaak are serialized rather than interleaved.

## Capabilities

### New Capabilities

- `zaak-behandelaar-assignment-idempotency`: Ensures that assigning a behandelaar to a zaak is idempotent and concurrency-safe — exactly one `Behandelaar` role exists per zaak after any assignment call, regardless of concurrent invocations.

### Modified Capabilities

<!-- No existing spec-level capability requirements are changing. -->

## Impact

- `ZaakService` (`src/main/kotlin/nl/info/zac/zaak/ZaakService.kt`): assignment logic updated with locking and defensive role cleanup.
- `ZrcClientService` (`src/main/kotlin/nl/info/client/zgw/zrc/ZrcClientService.kt`): `updateRol`/`deleteRol` may be augmented with a cleanup step.
- No API contract changes — endpoints remain identical.
- No database migrations required.
- No new dependencies — uses standard `java.util.concurrent.locks.ReentrantLock`.
