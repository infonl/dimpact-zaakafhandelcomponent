## 1. Database migration

- [ ] 1.1 Add `src/main/resources/schemas/V97__verwerkte_productaanvragen.sql` creating table `verwerkte_productaanvragen` with columns `productaanvraag_object_uuid UUID PRIMARY KEY`, `status VARCHAR(20) NOT NULL`, `gestart_op TIMESTAMP NOT NULL` (include SPDX header per project convention for `.sql`/schema files if applicable, otherwise follow the style of neighboring `V9x__*.sql` files).

## 2. Persistence layer

- [ ] 2.1 Add an `AllOpen` JPA entity class (e.g. `VerwerkteProductaanvraag`) mapping to `verwerkte_productaanvragen`, following the repo convention of `lateinit var` for non-nullable columns.
- [ ] 2.2 Add `ProductaanvraagVerwerkingRepository` (or similar `XxxRepository` name) in `nl.info.zac.productaanvraag` implementing an atomic `claim(productaanvraagObjectUUID: UUID): Boolean` function using a native upsert query (`INSERT ... ON CONFLICT ... DO UPDATE ... WHERE ... RETURNING`) with a staleness window for reclaiming abandoned `IN_PROGRESS` rows, injected via `@ConfigProperty(name = "PRODUCTAANVRAAG_CLAIM_TIMEOUT_MINUTES", defaultValue = "10")` (`Instance<Long>` or equivalent, per the `ConfigurationService`/`BrpConfiguration` convention) rather than hardcoded.
- [ ] 2.3 Annotate the claim function with `@Transactional(REQUIRES_NEW)` so it commits independently of the caller's transaction scope.
- [ ] 2.4 Add a `markDone(productaanvraagObjectUUID: UUID)` function (or equivalent) on the repository, annotated `@Transactional(REQUIRED)`, to update the row's status after successful processing.

## 3. Configuration

- [ ] 3.1 Document `PRODUCTAANVRAAG_CLAIM_TIMEOUT_MINUTES` (default `10`) in `.env.example`, alongside a short comment explaining its purpose.
- [ ] 3.2 Add a `productaanvraag.claimTimeoutMinutes` value (default `10`) to `charts/zac/values.yaml` and render it as `PRODUCTAANVRAAG_CLAIM_TIMEOUT_MINUTES` in `charts/zac/templates/config.yaml`, following the existing `brpApi`/`BRP_*` pattern.

- [ ] 3.3 Extend `docs/solution-architecture/productRequestSupport.md` with the claim record: when it is created, when it is set to `DONE`, and how the staleness timeout allows reprocessing.

## 4. Guard integration

- [ ] 4.1 In `ProductaanvraagService.handleProductaanvraag`, call the repository's claim function before doing any work; if the claim is rejected, log at `INFO` level and return without processing.
- [ ] 4.2 Call `markDone` inline, immediately after the zaak's case/process has been started — **not** after the full `handleProductaanvraagDimpact` chain completes:
  - In `startZaakWithCmmnProcess`, immediately after `cmmnService.startCase(...)` returns, before group/employee assignment, document pairing, betrokkene/contact linking, and the confirmation email.
  - In `processProductaanvraagWithBpmnZaaktype`, immediately after `bpmnService.startProcess(...)` returns (already the last statement in that function).
  - Do not rely on catching an exception around the whole call chain to decide whether to call `markDone`: `processProductaanvraagWithCmmnZaaktype` already swallows exceptions from `startZaakWithCmmnProcess` into a `WARNING` log without rethrowing, so nothing observable propagates to `handleProductaanvraagDimpact`/`handleProductaanvraag` for the CMMN path.
- [ ] 4.3 Verify no other notification handler in `NotificationReceiver` is affected by this change (should require no code changes to `handleSignaleringen`, `handleIndexing`, `handleInboxDocuments`, `handleFlowableProcessData`, `handleZaaktype`, `handleWebsockets`).

## 5. Tests

- [ ] 5.1 Add Kotest `BehaviorSpec` unit tests for the repository's claim logic: fresh UUID claims successfully; already-`DONE` UUID is rejected; fresh `IN_PROGRESS` UUID is rejected; stale `IN_PROGRESS` UUID (older than the configured staleness window) is reclaimed successfully; a non-default configured staleness window is honored. Use `afterEach { checkUnnecessaryStub() }` per project convention.
- [ ] 5.2 Add Kotest `BehaviorSpec` unit tests for `ProductaanvraagService.handleProductaanvraag`: a rejected claim results in no calls to zaak-creation/CMMN/BPMN collaborators; a successful claim proceeds with existing behavior and marks the record done right after `cmmnService.startCase`/`bpmnService.startProcess` (verify `markDone` is called even if a later step, e.g. role assignment or the confirmation email, throws — for the CMMN path this also verifies the pre-existing swallow-to-warning behavior still applies).
- [ ] 5.3 If integration test coverage exists for `NotificationReceiver` (TestContainers-based), add or extend a test that sends the same productaanvraag notification twice and asserts only one zaak is created.

## 6. Verification and cleanup

- [ ] 6.1 Run `./gradlew spotlessApply detektApply` and fix any findings.
- [ ] 6.2 Run `./gradlew test --tests "nl.info.zac.productaanvraag.*"` (and any relevant `NotificationReceiver`/repository test classes) to confirm the new tests pass.
- [ ] 6.3 Manually verify with the `verify` skill or local docker-compose stack: send the same productaanvraag notification payload twice to the `notificaties` endpoint and confirm only one zaak is created in OpenZaak.
- [ ] 6.4 Verify Helm chart renders correctly: `helm template charts/zac` (or equivalent) shows `PRODUCTAANVRAAG_CLAIM_TIMEOUT_MINUTES` in the rendered ConfigMap with the default value, and with an overridden value when set.
