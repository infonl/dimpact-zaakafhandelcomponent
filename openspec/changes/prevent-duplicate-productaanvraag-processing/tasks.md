## 1. Database migration

- [ ] 1.1 Add `src/main/resources/schemas/V97__verwerkte_productaanvragen.sql` creating table `verwerkte_productaanvragen` with columns `productaanvraag_object_uuid UUID PRIMARY KEY`, `status VARCHAR(20) NOT NULL`, `gestart_op TIMESTAMP NOT NULL` (include SPDX header per project convention for `.sql`/schema files if applicable, otherwise follow the style of neighboring `V9x__*.sql` files).

## 2. Persistence layer

- [ ] 2.1 Add an `AllOpen` JPA entity class (e.g. `VerwerkteProductaanvraag`) mapping to `verwerkte_productaanvragen`, following the repo convention of `lateinit var` for non-nullable columns.
- [ ] 2.2 Add `ProductaanvraagVerwerkingRepository` (or similar `XxxRepository` name) in `nl.info.zac.productaanvraag` implementing an atomic `claim(productaanvraagObjectUUID: UUID): Boolean` function using a native upsert query (`INSERT ... ON CONFLICT ... DO UPDATE ... WHERE ... RETURNING`) with a staleness window constant (e.g. 10 minutes) for reclaiming abandoned `IN_PROGRESS` rows.
- [ ] 2.3 Annotate the claim function with `@Transactional(REQUIRES_NEW)` so it commits independently of the caller's transaction scope.
- [ ] 2.4 Add a `markDone(productaanvraagObjectUUID: UUID)` function (or equivalent) on the repository, annotated `@Transactional(REQUIRED)`, to update the row's status after successful processing.

## 3. Guard integration

- [ ] 3.1 In `ProductaanvraagService.handleProductaanvraag`, call the repository's claim function before doing any work; if the claim is rejected, log at `INFO` level and return without processing.
- [ ] 3.2 On successful completion of `handleProductaanvraagDimpact`, call `markDone`.
- [ ] 3.3 Verify no other notification handler in `NotificationReceiver` is affected by this change (should require no code changes to `handleSignaleringen`, `handleIndexing`, `handleInboxDocuments`, `handleFlowableProcessData`, `handleZaaktype`, `handleWebsockets`).

## 4. Tests

- [ ] 4.1 Add Kotest `BehaviorSpec` unit tests for the repository's claim logic: fresh UUID claims successfully; already-`DONE` UUID is rejected; fresh `IN_PROGRESS` UUID is rejected; stale `IN_PROGRESS` UUID (older than the staleness window) is reclaimed successfully. Use `afterEach { checkUnnecessaryStub() }` per project convention.
- [ ] 4.2 Add Kotest `BehaviorSpec` unit tests for `ProductaanvraagService.handleProductaanvraag`: a rejected claim results in no calls to zaak-creation/CMMN/BPMN collaborators; a successful claim proceeds with existing behavior and marks the record done afterward.
- [ ] 4.3 If integration test coverage exists for `NotificationReceiver` (TestContainers-based), add or extend a test that sends the same productaanvraag notification twice and asserts only one zaak is created.

## 5. Verification and cleanup

- [ ] 5.1 Run `./gradlew spotlessApply detektApply` and fix any findings.
- [ ] 5.2 Run `./gradlew test --tests "nl.info.zac.productaanvraag.*"` (and any relevant `NotificationReceiver`/repository test classes) to confirm the new tests pass.
- [ ] 5.3 Manually verify with the `verify` skill or local docker-compose stack: send the same productaanvraag notification payload twice to the `notificaties` endpoint and confirm only one zaak is created in OpenZaak.
