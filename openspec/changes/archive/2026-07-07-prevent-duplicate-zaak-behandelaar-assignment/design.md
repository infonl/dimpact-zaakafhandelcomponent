# Design: Prevent Duplicate Zaak Behandelaar Assignment

## Context

`ZaakService.assignZaak()` must ensure that exactly one `Behandelaar` role (with `betrokkeneType=MEDEWERKER`) exists per zaak in OpenZaak after any assignment call. The current implementation is a read-modify-write sequence against the OpenZaak REST API with no concurrency control:

1. `listRollen(zaak)` — read current roles
2. Mutate list (add new role)
3. `updateRollen(zaak, rollen, toelichting)` — reads current roles **again**, then deletes/creates delta

When two HTTP requests reach `assignZaak` for the same zaak UUID concurrently (e.g., double-click in the browser), both can read an empty or stale list at step 1, both proceed through step 3, and each creates its own MEDEWERKER role in OpenZaak before the other's deletes run — leaving two Behandelaar roles.

`ZaakService` is a CDI `@ApplicationScoped` bean (implied by `@AllOpen` + no scope annotation → WildFly defaults to dependent; however it is effectively shared via injection). There are no existing distributed-lock or optimistic-version mechanisms for roles.

## Goals / Non-Goals

**Goals:**
- Ensure at most one MEDEWERKER Behandelaar role per zaak after any `assignZaak` or `assignZaakToLoggedInUser` call.
- Serialize concurrent assignment calls for the **same zaak UUID** within a JVM instance.
- Clean up pre-existing duplicate Behandelaar roles defensively on every assignment.
- No new runtime dependencies.

**Non-Goals:**
- Multi-pod / multi-JVM serialization (addressed in Open Questions).
- Fixing races in batch operations (`assignZaken`) — those are inherently async and already fire-and-forget.
- Adding retry logic for OpenZaak API failures.

## Decisions

### Decision 1: Per-zaak striped lock in `ZaakService`

Use a fixed-size array of `ReentrantLock` (standard `java.util.concurrent.locks`) indexed by `uuid.hashCode() % size`. This is the standard-Java equivalent of Guava's `Striped` — same O(1) lookup, same bounded memory, no extra dependency.

```kotlin
private val zaakAssignmentLocks = Array(64) { ReentrantLock() }

private fun lockForZaak(uuid: UUID) =
    zaakAssignmentLocks[Math.floorMod(uuid.hashCode(), zaakAssignmentLocks.size)]

fun assignZaak(zaak: Zaak, groupId: String, userName: String?, reason: String?) {
    lockForZaak(zaak.uuid).withLock {
        // existing logic
    }
}
```

`withLock` is a Kotlin stdlib extension on `java.util.concurrent.locks.Lock` (`kotlin.concurrent.withLock`) — it handles `lock()`/try-finally/`unlock()` automatically. No extra dependencies beyond stdlib.

**Alternatives considered:**

| Alternative | Why rejected |
|---|---|
| Guava `Striped.lazyWeakLock(64)` | Equivalent semantics but adds Guava API surface; prefer standard JDK |
| `ConcurrentHashMap<UUID, ReentrantLock>` | Unbounded growth; needs cleanup logic; more complex |
| `synchronized(zaak.uuid.toString().intern())` | String intern pool is a global memory leak; not idiomatic |
| Jakarta EE `@Lock(WRITE)` on the bean | Serializes ALL zaak assignments across all UUIDs — too coarse, creates unnecessary contention |
| Infinispan distributed lock | Correct for multi-pod, but adds complexity; not needed to fix the reported symptom (browser double-click hits same pod via sticky sessions) |
| Optimistic locking via OpenZaak ETag | OpenZaak does not expose ETags on rol resources in the current integration |

Fixed array of 64 locks: bounded memory, no GC pressure, no cleanup logic. False-contention probability is negligible for typical zaak assignment volumes.

### Decision 2: Defensive all-Behandelaar-roles cleanup in `assignUser`

Before creating a new MEDEWERKER role, explicitly delete **all** existing MEDEWERKER roles (not just the one found at read time). This handles:

- Pre-existing duplicates from prior races (belt-and-suspenders even when locking prevents future races).
- The case where `updateRol` added a second role due to an earlier bug.

Change `ZrcClientService.updateRol` to **not** keep existing MEDEWERKER roles in the desired list when a new MEDEWERKER role is being assigned. Instead, within `ZaakService.assignUser`, replace the single `updateRol` call with:
1. `deleteRol(zaak, BetrokkeneTypeEnum.MEDEWERKER, reason)` — purge all existing
2. `createRol(bepaalRolMedewerker(user, zaak), reason)` — create exactly one

This removes the read-modify-write dependency on stale state for the medewerker assignment step specifically.

**Alternative considered:** Modify `updateRollen` to tolerate duplicates. Rejected because `updateRollen` compares by `equalBetrokkeneRol` (betrokkeneType + roltype equality), so two roles with different betrokkene but same type would confuse the delta logic.

## Risks / Trade-offs

| Risk | Mitigation |
|---|---|
| Striped lock does not prevent races across multiple JVM/pod instances | Document clearly; accept for now since browser double-clicks go to the same pod. Revisit if ZAC is horizontally scaled with session affinity disabled. |
| `deleteRol` + `createRol` is not atomic — a crash between the two leaves the zaak with no Behandelaar | Acceptable: the assignment endpoint is idempotent; the caller can retry. Better than leaving duplicates. |
| Lock held during multiple OpenZaak HTTP calls may increase p99 latency under high concurrency | Zoek-operations for the same zaak UUID are already rare events; 64 stripes further reduce contention. |
| Fixed lock array causes false contention when two different zaak UUIDs hash to the same slot | Probability is 1/64 per pair; assignment is infrequent so this is acceptable. Increase array size if needed. |

## Migration Plan

1. Deploy new `ZaakService` (with lock + defensive cleanup) — no data migration needed.
2. Any duplicate Behandelaar roles already in OpenZaak are **not** automatically cleaned up by the deploy. A one-time ZAC admin action (or OpenZaak admin query) can detect and purge duplicates: `SELECT zaak_url, COUNT(*) FROM roles WHERE roltype = 'behandelaar' AND betrokkeneType = 'medewerker' GROUP BY zaak_url HAVING COUNT(*) > 1`. The next assignment call for each affected zaak will clean duplicates defensively.

## Open Questions

1. **Multi-pod**: Should we add an Infinispan-based distributed lock for environments where ZAC runs with `replicas > 1` and no sticky sessions? Recommend deferring until load-testing confirms it is needed.
2. **Audit trail**: Should the defensive delete of duplicate roles emit an audit log warning? Recommend yes — log at `WARNING` level whenever more than one MEDEWERKER role is found and purged.
