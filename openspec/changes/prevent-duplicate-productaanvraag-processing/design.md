## Context

`NotificationReceiver.notificatieReceive` (`src/main/kotlin/nl/info/zac/notification/NotificationReceiver.kt`) handles Notificaties API callbacks from Open Notificaties (NRC) synchronously, on the HTTP request thread, and only returns `204 No Content` after every handler (signaleringen, productaanvraag, indexing, inbox documents, Flowable data, zaaktype, websockets) has run. Per the VNG Notificaties API standard, NRC delivery is at-least-once: if ZAC does not acknowledge in time, NRC resends the identical notification.

`ProductaanvraagService.handleProductaanvraag(productaanvraagObjectUUID: UUID)` has no idempotency guard. Its call chain (`handleProductaanvraagDimpact` → `processProductaanvraagWithCmmnZaaktype`/`processProductaanvraagWithBpmnZaaktype` → `createZaak` → `cmmnService.startCase`/`bpmnService.startProcess`) unconditionally creates a new zaak and starts a case/process every time it runs. A redelivered notification therefore produces duplicate zaken in production.

ZAC can run as multiple pods behind a load balancer, so a redelivered notification is not guaranteed to hit the same JVM instance as the original. A comparable duplicate-write bug was previously fixed in `ZaakService.assignZaak` (see `openspec/changes/archive/2026-07-07-prevent-duplicate-zaak-behandelaar-assignment/`) using an in-process striped `ReentrantLock` array; that design explicitly scoped out multi-pod correctness because the triggering scenario (rapid double-clicks) reliably hit the same pod via sticky sessions. That assumption does not hold for NRC redelivery, so the same approach is not sufficient here — the lock must be visible across pods, i.e. persisted in the database.

The `inbox_productaanvraag` table already has a `UNIQUE` constraint on the productaanvraag object UUID (`src/main/resources/schemas/V50__inbox_productaanvraag.sql`), but this only guards the "no CMMN/BPMN mapping configured" fallback path, not the actual zaak-creation paths.

## Goals / Non-Goals

**Goals:**
- Guarantee at most one zaak (and CMMN case / BPMN process) is created per productaanvraag object, regardless of how many times ZAC receives a notification for it.
- Make the guard resilient across multiple ZAC pods (persisted in PostgreSQL, not in-process).
- Self-heal if a claimed productaanvraag is never marked done (e.g. ZAC crashes mid-processing), without requiring manual database intervention for the common case.
- Keep the change minimal and scoped to the productaanvraag handling path.

**Non-Goals:**
- Reducing the frequency of NRC redeliveries (e.g. by making notification handling asynchronous so ZAC acknowledges faster). This is a valid complementary follow-up but is not required to fix the reported duplicate-zaak bug, and is a materially larger change (touches error handling/observability for all notification handlers).
- Building a generic distributed-locking facility for reuse elsewhere in the codebase. This design solves the productaanvraag case specifically; other handlers do not need it today.
- Bounding or backing off indefinite retries of a productaanvraag that keeps failing (not crashing) during processing. Documented as an accepted limitation, not solved here.
- Deduplicating on the raw notification payload. The Notificaties API does not provide a unique message/delivery identifier, so payload-level dedup is not viable; this design dedupes on the productaanvraag object's own stable UUID instead.

## Decisions

### Decision: Persist the claim in a new dedicated table, not by reusing `inbox_productaanvraag`

`inbox_productaanvraag` is a functionally distinct concept (a persisted record of a productaanvraag that had no zaaktype mapping). Reusing it for dedup would conflate two unrelated purposes and would not cover the CMMN/BPMN paths, which never write to that table. A new table `verwerkte_productaanvragen` (`productaanvraag_object_uuid UUID PRIMARY KEY, status VARCHAR(20) NOT NULL, gestart_op TIMESTAMP NOT NULL`) is added instead, via `V97__verwerkte_productaanvragen.sql`.

Alternative considered: add a `verwerkt` boolean column to an existing table. Rejected — no existing table is keyed by productaanvraag object UUID in a way that fits both the CMMN and BPMN paths.

### Decision: Atomic claim via a single upsert statement, not read-then-write

The claim is a single `INSERT ... ON CONFLICT ... DO UPDATE ... WHERE ... RETURNING` statement. PostgreSQL's own uniqueness/row-locking semantics make the claim atomic across concurrent transactions/pods without any application-level locking code. A read-then-write pattern (`SELECT` to check, then `INSERT`/`UPDATE`) would reintroduce exactly the race this change is meant to close, since two concurrent requests could both pass the `SELECT` check before either writes.

Alternative considered: JPA pessimistic locking (`LockModeType.PESSIMISTIC_WRITE` / `SELECT ... FOR UPDATE`). Rejected as unnecessary complexity — no existing code in this codebase uses JPA pessimistic locking, and a single upsert achieves the same atomicity with simpler code and one round trip.

Alternative considered: in-process `ReentrantLock` (the `ZaakService.assignZaak` precedent). Rejected — does not hold across pods (see Context).

### Decision: Claim commits in its own transaction (`REQUIRES_NEW`)

The claim-insert must be visible to a concurrent overlapping request as soon as it is committed, independent of whatever transactional scope wraps the rest of `handleProductaanvraag`'s processing (which is mostly external ZGW/Objects/CMMN/BPMN API calls, not a single local DB transaction). Running the claim in `@Transactional(REQUIRES_NEW)` ensures it commits immediately, so a second, near-simultaneous request reliably observes it.

### Decision: TTL-based reclaim of stale `IN_PROGRESS` claims (10 minutes)

If ZAC crashes or is restarted after claiming a productaanvraag but before marking it `DONE`, that productaanvraag must not be permanently stuck unprocessed. The claim query allows reclaiming a row whose status is `IN_PROGRESS` and whose `gestart_op` is older than 10 minutes, treating it as abandoned and re-attempting processing.

Trade-off: if `handleProductaanvraagDimpact` fails with a normal (non-crash) exception rather than throwing before completion, the row is left `IN_PROGRESS` and will be retried indefinitely on every notification redelivery once the TTL has elapsed, with no backoff or retry cap. This is an accepted limitation for this iteration (see Risks).

Alternative considered: no TTL, stuck rows require manual cleanup. Rejected — introduces an operational burden (an on-call runbook / alert) for what should be a self-healing case.

10 minutes was chosen as comfortably longer than the expected end-to-end duration of `handleProductaanvraagDimpact` (zaak creation + CMMN/BPMN start + role assignment + document pairing + email) under normal conditions, while still being short enough that a genuinely abandoned productaanvraag recovers within one operator-visible SLA window. This value can be tuned later without a schema change.

### Decision: Guard scoped to `handleProductaanvraag` only

Only the productaanvraag handling path is wrapped. The other handlers invoked from `notificatieReceive` (`handleSignaleringen`, `handleIndexing`, `handleInboxDocuments`, `handleFlowableProcessData`, `handleZaaktype`, `handleWebsockets`) are idempotent-safe under redelivery already (upserts, or resending harmless websocket/signalering events), so adding a persisted-claim guard there would be unneeded scope and complexity.

## Risks / Trade-offs

- **[Risk]** A permanently failing (not crashing) productaanvraag is retried indefinitely once its `IN_PROGRESS` claim goes stale, with no backoff or cap, potentially generating repeated warning logs and repeated partial side effects on each retry. → **Mitigation**: none in this iteration; flagged as a documented follow-up. Existing per-step exception handling in `ProductaanvraagService` already logs failures at `WARNING` level, so operators have visibility.
- **[Risk]** If processing succeeds but the final `UPDATE status = 'DONE'` fails to commit (e.g. a crash in the narrow window after zaak creation but before the status update), the productaanvraag will be reclaimed and reprocessed after the TTL, creating a duplicate zaak — the exact bug this change fixes, in a much narrower window. → **Mitigation**: accepted as a residual risk; the window is small (a single local `UPDATE` statement) compared to the original bug (full external API chain). Could be closed later by recording the created zaak UUID and checking for its existence before reprocessing, but that is out of scope for this first iteration.
- **[Risk]** Choosing 10 minutes as the TTL is a fixed value with no measured basis. If real-world `handleProductaanvraagDimpact` durations regularly exceed it under load, a still-in-flight (not crashed) request's claim could be reclaimed by a redelivery, causing a duplicate. → **Mitigation**: 10 minutes is intentionally generous relative to expected processing time; monitor and tune via a follow-up if needed.

## Migration Plan

1. Add Flyway migration `V97__verwerkte_productaanvragen.sql` creating the `verwerkte_productaanvragen` table. This is additive and backward-compatible; no existing data or behavior is affected until the code change is deployed alongside it.
2. Deploy the repository and the `ProductaanvraagService.handleProductaanvraag` guard in the same release as the migration (the guard requires the table to exist).
3. No backfill needed — the table starts empty and only tracks productaanvragen received after deployment. Productaanvragen already processed before this change remain unaffected (no retroactive claim rows).
4. Rollback: reverting the code change is safe without reverting the migration (the table would simply go unused). Reverting the migration itself would require dropping the table, which is safe since no other part of the system depends on it.

## Open Questions

- Should the 10-minute TTL be a configurable property (e.g. via `@ConfigProperty`) instead of a hardcoded constant, to allow tuning per environment without a code change? Leaning yes, but deferred to implementation-time judgment since it doesn't affect the core design.
- Should there be a scheduled cleanup job that deletes old `DONE` rows to keep the table small, or is unbounded growth acceptable given ZAC's productaanvraag volume? Left as a follow-up; the table is small per row and unlikely to be a concern in the near term.
