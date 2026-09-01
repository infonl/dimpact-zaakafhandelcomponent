## 1. Database migration

- [x] 1.1 Add `src/main/resources/schemas/V97__verwerkte_productaanvraag.sql` creating table `verwerkte_productaanvraag` with columns `uuid_productaanvraag_object UUID PRIMARY KEY`, `status VARCHAR(20) NOT NULL`, `gestart_op TIMESTAMP NOT NULL`. Table and column names follow the singular naming of the neighbouring `inbox_productaanvraag` table rather than the plural name used in the proposal.

## 2. Persistence layer

- [x] 2.1 No JPA entity is added: both repository functions are native statements, so an entity would be unused code.
- [x] 2.2 Add `ProductaanvraagClaimRepository` in `nl.info.zac.productaanvraag` implementing an atomic `claim(productaanvraagObjectUUID: UUID): Boolean` using a native upsert query (`INSERT ... ON CONFLICT ... DO UPDATE ... WHERE`) with a staleness window for reclaiming abandoned `IN_PROGRESS` rows, injected via `@ConfigProperty(name = "PRODUCTAANVRAAG_CLAIM_TIMEOUT_MINUTES", defaultValue = "10")`. Timestamps and the staleness comparison are evaluated by PostgreSQL (`now()`), so the result does not depend on the clock of the ZAC instance handling the notification.
- [x] 2.3 Annotate the claim function with `@Transactional(REQUIRES_NEW)` so it commits independently of the caller's transaction scope.
- [x] 2.4 Add a `markDone(productaanvraagObjectUUID: UUID)` function on the repository, annotated `@Transactional(REQUIRED)`, to update the row's status after successful processing.

## 3. Configuration

- [x] 3.1 Document `PRODUCTAANVRAAG_CLAIM_TIMEOUT_MINUTES` (default `10`) in `.env.example`, alongside a short comment explaining its purpose.
- [x] 3.2 Add a `productaanvraag.claimTimeoutMinutes` value (default `10`) to `charts/zac/values.yaml` and render it as `PRODUCTAANVRAAG_CLAIM_TIMEOUT_MINUTES` in `charts/zac/templates/config.yaml`.
- [x] 3.3 Extend `docs/solution-architecture/productRequestSupport.md` with the claim record: when it is created, when it is set to `DONE`, and how the staleness timeout allows reprocessing.

## 4. Guard integration

- [x] 4.1 In `ProductaanvraagService.handleProductaanvraag`, call the repository's claim function before doing any work; if the claim is rejected, log at `INFO` level and return without processing.
- [x] 4.2 Call `markDone` inline, immediately after the zaak's case/process has been started — **not** after the full `handleProductaanvraagDimpact` chain completes:
  - In `startZaakWithCmmnProcess`, immediately after `cmmnService.startCase(...)` returns, before group/employee assignment, document pairing, betrokkene/contact linking, and the confirmation email.
  - In `processProductaanvraagWithBpmnZaaktype`, immediately after `bpmnService.startProcess(...)` returns (already the last statement in that function).
  - Also after `registreerInbox(...)`: a productaanvraag without a CMMN or BPMN mapping reaches a terminal state without creating a zaak, so leaving its claim `IN_PROGRESS` would make every redelivery after the staleness window retry it forever against the unique constraint on `inbox_productaanvraag`. This call site is not mentioned in the proposal.
  - Do not rely on catching an exception around the whole call chain to decide whether to call `markDone`: `processProductaanvraagWithCmmnZaaktype` already swallows exceptions from `startZaakWithCmmnProcess` into a `WARNING` log without rethrowing.
- [x] 4.3 Verify no other notification handler in `NotificationReceiver` is affected by this change (no code changes were needed to `handleSignaleringen`, `handleIndexing`, `handleInboxDocuments`, `handleFlowableProcessData`, `handleZaaktype`, `handleWebsockets`).

## 5. Tests

- [x] 5.1 Add Kotest `BehaviorSpec` unit tests for the repository: a claim succeeds when the statement affects a row and is rejected when it does not, the configured staleness window is passed to the query, and `markDone` sets the status to `DONE`. Which rows the statement affects is decided by PostgreSQL and is covered by the integration test rather than by these unit tests.
- [x] 5.2 Add a Kotest `BehaviorSpec` unit test for `ProductaanvraagService.handleProductaanvraag`: a rejected claim results in no calls to zaak-creation/CMMN/BPMN/inbox collaborators.
- [x] 5.3 Extend `NotificationProductaanvraagCmmnTest` with a test that sends an already-handled productaanvraag notification a second time and asserts that ZAC still responds successfully and that no second acknowledgement of receipt email is sent.
- [x] 5.4 Add `NotificationProductaanvraagIdempotencyTest`, which asserts the claim behaviour against real PostgreSQL by reading and seeding the `verwerkte_productaanvraag` table through `psql` in the `zac-database` container: simultaneous delivery of the same notification, take-over of a claim that outlived the staleness period, a claim held within that period being left untouched, and a released claim never being taken again. This task is not mentioned in the proposal.

- [x] 5.5 Add a Kotest `BehaviorSpec` unit test asserting that `registreerInbox` creates the inbox productaanvraag before removing its documents from the inbox, and reorder `registreerInbox` accordingly. The documents were deleted in their own committed transactions before the inbox productaanvraag was persisted, so a failure to persist it - for example on the unique constraint over `uuid_aanvraagdocument` - deleted the aanvraag documents without anything referring to them, and the claim then kept retrying that same failure after every staleness period. This task is not mentioned in the proposal.

## 6. Verification and cleanup

- [x] 6.1 Run `./gradlew spotlessApply detektApply` and fix any findings.
- [x] 6.2 Run `./gradlew test` to confirm the new and existing tests pass.
- [ ] 6.3 Manually verify with the local docker-compose stack: send the same productaanvraag notification payload twice to the `notificaties` endpoint and confirm only one zaak is created in OpenZaak.
- [ ] 6.4 Verify Helm chart renders correctly: `helm template charts/zac` shows `PRODUCTAANVRAAG_CLAIM_TIMEOUT_MINUTES` in the rendered ConfigMap with the default value, and with an overridden value when set. Could not be run locally: the chart's `opentelemetry-collector` and `solr-operator` dependencies require `helm repo add` first.
