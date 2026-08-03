## Why

In production, Open Notificaties (NRC) sometimes redelivers the exact same `productaanvraag` object-creation notification because ZAC did not return `200 OK` before NRC's delivery timeout elapsed. Per the VNG Notificaties API standard, NRC guarantees **at-least-once** delivery, not exactly-once — redelivery of an identical notification is expected, standard behavior, not a bug in NRC. `NotificationReceiver.handleProductaanvraag` has no idempotency guard, so each redelivered notification runs the full `ProductaanvraagService.handleProductaanvraag` chain again, creating a second (or third) zaak, CMMN case/BPMN process, role assignment, and confirmation email for the same real-world productaanvraag. This is a significant data-quality issue for municipalities using ZAC and needs a fix now.

## What Changes

- Add a new database table (`verwerkte_productaanvragen`) that persistently records, per productaanvraag object UUID, whether that productaanvraag has already been claimed/processed.
- Add a repository providing an atomic claim operation (DB-level upsert) that a caller uses to determine whether it is safe to process a given productaanvraag object UUID, or whether it should skip because another call already claimed it (in progress or done).
- Wrap `ProductaanvraagService.handleProductaanvraag` so that it claims the productaanvraag object UUID before doing any zaak-creation work, skips processing entirely if the claim is rejected, and marks the record `DONE` after successful processing.
- A stale claim (marked in-progress but never completed, e.g. due to a crash) is automatically reclaimable after a fixed TTL (10 minutes), so a genuinely lost productaanvraag is not stuck forever without requiring manual intervention.
- Scope is deliberately narrow: only the productaanvraag handling path gets this guard. Other notification handlers (`handleSignaleringen`, `handleIndexing`, `handleInboxDocuments`, `handleFlowableProcessData`, `handleZaaktype`, `handleWebsockets`) are unaffected — they already tolerate redelivery safely.
- Explicitly out of scope: an in-memory/JVM-local lock (rejected — ZAC can run multiple pods behind a load balancer, so a redelivered notification can land on a different pod than the one still processing the original; only a DB-persisted claim is correct here). Also out of scope: switching notification handling to asynchronous processing to reduce the frequency of NRC timeouts/retries in the first place — this change makes redelivery *safe*, it does not attempt to make redelivery *less likely*.

## Capabilities

### New Capabilities
- `productaanvraag-notification-idempotency`: guarantees that a given productaanvraag object (identified by its Objects API UUID) results in at most one zaak/case/process being created, no matter how many times ZAC receives a notification for it.

### Modified Capabilities
_None — no existing spec's requirements change; this introduces new behavior around a previously unspecified area._

## Impact

- **Affected code**: `nl.info.zac.productaanvraag.ProductaanvraagService` (guard added around `handleProductaanvraag`), a new repository class in `nl.info.zac.productaanvraag`, `nl.info.zac.notification.NotificationReceiver` (no functional change expected, but touches the call site).
- **Database**: new Flyway migration `V97__verwerkte_productaanvragen.sql` adding table `verwerkte_productaanvragen` under `src/main/resources/schemas/`.
- **External systems**: none — no change to the Notificaties API contract, ZAC continues to respond the same way (`204 No Content`) to NRC; only internal handling of duplicate deliveries changes.
- **Tests**: new unit tests for the repository's claim/TTL-reclaim logic and for `ProductaanvraagService.handleProductaanvraag`'s skip-on-duplicate-claim behavior.
