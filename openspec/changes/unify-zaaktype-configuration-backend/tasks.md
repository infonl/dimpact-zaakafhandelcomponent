## 0. Baseline

- [ ] 0.1 On `main`, run `./gradlew generateOpenApiSpec` and copy `build/generated/openapi/META-INF/openapi/openapi.json`
      to the scratchpad as the contract baseline; verify the file exists.
- [ ] 0.2 Count the files in `src/main` that name `ZaaktypeCmmnConfiguration` or `ZaaktypeBpmnConfiguration`
      (`grep -rlwE 'ZaaktypeCmmnConfiguration|ZaaktypeBpmnConfiguration' src/main | wc -l`) and record the number in the A1 PR body.

## 1. PR A1: schema repair (branch `feature/PZ-12669-unify-zaaktype-configuration-backend`, base `main`)

- [ ] 1.1 Write `V100__repair_zaaktype_configuration_schema.sql` (design D2):
      - delete subclass rows without a base row
      - delete duplicate one-to-one children, keeping the highest id
      - add the PK on `zaaktype_bpmn_configuration(id)`
      - add FKs from both subclass tables to `zaaktype_configuration(id)` with ON DELETE CASCADE
      - set `configuration_type` NOT NULL
      - add UNIQUE on the FK column of betrokkene, BRP, and CMMN email parameters

      Verify that the itest stack starts and that Flyway reports V100 as applied.
- [ ] 1.2 Add an itest that reads the constraints from `information_schema` and asserts that the PK, FKs, NOT NULL,
      and UNIQUE constraints of V100 exist; verify with `./gradlew itest --tests "*SchemaTest*"`.
- [ ] 1.3 Add the shared engine fixture `listOf(CMMN, BPMN)` with configuration factories to `AdminFixtures.kt`.
      Switch `ZaaktypeHelperServiceTest` to it and verify that the test still passes.
- [ ] 1.4 Run `./gradlew spotlessApply detektApply detekt build` and the contract diff against the baseline;
      verify that both are clean.
- [ ] 1.5 Open the PR. Title: `fix(admin): repair the zaaktype configuration schema constraints`. Include the openspec change directory.
      The body ends with `Solves PZ-12669`.

## 2. PR A2: zaak settings to the base (branch `feature/PZ-12669-a2-zaak-settings-to-base`, base A1)

- [ ] 2.1 Write `V101__move_zaak_settings_to_zaaktype_configuration.sql` (design D3):
      - move both warning-window columns to the base
      - rename the email, zaakafzender, and mailtemplate parameter tables (drop the `cmmn_` infix) and re-point
        their FKs to `zaaktype_configuration(id)`
      - keep RESTRICT on the zaakafzender FK

      Verify on the itest stack that the existing CMMN data is still read.
- [ ] 2.2 Move the warning windows, the email parameters, the zaakafzenders, and the mailtemplate koppelingen from
      `ZaaktypeCmmnConfiguration` to `ZaaktypeConfiguration`. Rename the entities `ZaaktypeEmailParameters`,
      `ZaaktypeZaakafzenderParameters`, and `ZaaktypeMailtemplateParameters`. Verify with `./gradlew compileKotlin compileJava`.
- [ ] 2.3 Switch these readers to the generic `readZaaktypeConfiguration`:
      - `ZaakRestService.listZaakWarnings` and `listAfzendersVoorZaak`
      - `ZaakTaskDueDateEmailNotificationService`
      - `MailtemplateRESTService`
      - `MailTemplateKoppelingenService`
      - `BrpClientService`

      Verify with unit tests, parameterised by engine, for warnings, due-date signalering, afzenders, and BRP doelbinding.
- [ ] 2.4 Make the BPMN `POST` path preserve the moved settings. Verify with a unit test that a BPMN update keeps a
      stored warning window, and with an itest round trip.
- [ ] 2.5 Verify `./gradlew spotlessApply detektApply detekt build itest`, the contract diff, and a lower file count.
      Then open the PR `refactor(admin): move zaak settings to the engine-agnostic zaaktype configuration`, with body
      footer `Solves PZ-12669`.

## 3. PR A3: one configuration, a process binding, and a CMMN extension (branch `feature/PZ-12669-a3-split-configuration`, base A2)

- [ ] 3.1 Write `V102__split_zaaktype_configuration.sql` (design D4):
      - create `zaaktype_process_binding` and fill it from both subclass tables
      - rename `zaaktype_cmmn_configuration` to `zaaktype_cmmn_extension` and drop `id_case_definition`
      - add `zaaktype_configuration_id` to it
      - drop `zaaktype_bpmn_configuration`, the `configuration_type` column, and the ENUM type

      Verify on the itest stack that every seed configuration has the expected engine and key.
- [ ] 3.2 Replace the entities:
      - make `ZaaktypeConfiguration` concrete
      - add `ZaaktypeProcessBinding` with a `ProcessEngine` enum, and `ZaaktypeCmmnExtension` with the intake/afronden
        mail options and the humantask and usereventlistener children
      - delete `ZaaktypeCmmnConfiguration` and `ZaaktypeBpmnConfiguration`, and add the validity function to the entity

      Verify with entity unit tests for validity per engine.
- [ ] 3.3 Implement `ZaaktypeConfigurationService` (read, cached under the existing cache name) and
      `ZaaktypeConfigurationBeheerService` (store with bean validation, upsert by zaaktypeUuid, current-version
      queries, one productaanvraagtype check) with the contracts of design D5. Verify with unit tests,
      parameterised by engine, for every function.
- [ ] 3.4 Delete `ZaaktypeCmmnConfigurationService`, `ZaaktypeCmmnConfigurationBeheerService`,
      `ZaaktypeBpmnConfigurationService`, and `ZaaktypeBpmnConfigurationBeheerService`, together with their tests.
      Move the callers listed in proposal Impact to the new services. Verify with
      `./gradlew compileKotlin compileJava test`.
- [ ] 3.5 Make both REST resources and `RestZaaktypeConfigurationConverter` map their payloads onto the single
      entity and the binding:
      - `GET /zaakafhandelparameters/{uuid}` picks the shape from the binding engine, and uses CMMN when there is no binding
      - the BPMN list endpoint uses `listConfigurationsBoundTo(BPMN)`

      Verify with the existing REST unit tests and itests, unchanged.
- [ ] 3.6 Make `RestZaaktypeConverter`, `HealthCheckService`, and `ZaakRestService.isValidForZaakCreation` use
      `findConfiguration` and the entity validity. Verify with a unit test for "configuration without binding is not
      offered and not valid".
- [ ] 3.7 Make `ProductaanvraagService` select from `findCurrentConfigurationsByProductaanvraagtype`: newest wins,
      with a warning for more than one match. Verify with unit tests per engine and with the existing productaanvraag itests.
- [ ] 3.8 Add itests for the productaanvraagtype check:
      - a CMMN zaaktype blocks a BPMN zaaktype with another omschrijving
      - a new BPMN version keeps its own productaanvraagtype

      Verify that both pass.
- [ ] 3.9 Verify `./gradlew spotlessApply detektApply detekt build itest`, the contract diff, and a file count of zero.
      Then open the PR `refactor(admin): unify the CMMN and BPMN zaaktype configuration into one entity and one service layer`,
      with body footer `Solves PZ-12669`.

## 4. PR B1: process binding interface (branch `feature/PZ-12669-b1-process-binding`, base A3)

- [ ] 4.1 Add the `ProcessBinding` interface, `CmmnProcessBinding`, `BpmnProcessBinding`, and the `ProcessBindings`
      dispatcher over `Instance<ProcessBinding>` (design D6). Verify with unit tests that the dispatcher selects
      each adapter and fails for a configuration without a binding.
- [ ] 4.2 Add `BpmnService.deleteProcessInstance(zaakUuid)`, which also deletes the history and is a no-op without an instance.
      Verify with a unit test.
- [ ] 4.3 Replace the engine `when` in `ZaakRestService.startZaak`, `terminateZaak`, and `applyZaakUpdateSideEffects`, and the
      two start paths in `ProductaanvraagService`, with the dispatcher. Verify with the existing unit tests, which
      are now parameterised by engine.
- [ ] 4.4 Make the zaak-delete handler in `NotificationReceiver` call `delete` on every binding. Verify with a unit
      test and an itest that deletes a BPMN zaak and asserts that the process instance is gone.
- [ ] 4.5 Verify `./gradlew spotlessApply detektApply detekt build itest`, the contract diff, and that
      `grep -rn "ProcessEngine\.\(CMMN\|BPMN\)" src/main` shows hits only in the adapters and the entity. Then open the PR
      `refactor(flowable): start, terminate and delete zaak processes through one process binding interface`,
      with body footer `Solves PZ-12669`.

## 5. PR B2: resultaattype by omschrijving, expand step (branch `feature/PZ-12669-b2-resultaattype-omschrijving`, base B1)

- [ ] 5.1 Write `V103__add_resultaattype_omschrijving.sql`, which adds the two nullable omschrijving columns
      (design D7). Verify on the itest stack.
- [ ] 5.2 Write both columns on store: the converter resolves the omschrijving from the UUID through ZTC. Verify with
      converter unit tests.
- [ ] 5.3 Resolve the UUID per zaaktype version by omschrijving on read, with the stored UUID as fallback, in
      `terminateZaak`, `PlanItemsRestService.handleIntakeAfronden`, and the REST converters. Verify with unit tests
      for a match, a fallback, and a missing omschrijving.
- [ ] 5.4 Add `ResultaattypeOmschrijvingBackfill`, which runs on application start, is idempotent, and logs a
      summary of filled and unresolved rows. Verify with unit tests for success, a ZTC failure, and a second run.
- [ ] 5.5 Add a second version of one CMMN and one BPMN zaaktype to the Open Zaak seed data
      (`scripts/docker-compose/imports/openzaak-database/database/`). Rework
      `NotificationZaaktypeCompletionParametersTest` to publish that version, then verify that it passes for both engines.
- [ ] 5.6 Verify `./gradlew spotlessApply detektApply detekt build itest` and the contract diff. Then open the PR
      `feat(admin): reference resultaattypen by omschrijving in the zaaktype configuration`, with body footer `Solves PZ-12669`.

## 6. PR B3: configuration versioning (branch `feature/PZ-12669-b3-configuration-versioning`, base B2)

- [ ] 6.1 Rename `ZaaktypeHelperService` to `ZaaktypeConfigurationVersioning`, add
      `createNextVersion(previous, newZaaktype): ZaaktypeConfiguration`, and remove `updateZaakbeeindigGegevens`
      (design D8). Verify with unit tests, parameterised by engine.
- [ ] 6.2 Rewrite `ZaaktypeConfigurationBeheerService.upsertConfiguration` on top of `createNextVersion`. Verify
      with unit tests for: no previous, existing version, and a notification for an older version.
- [ ] 6.3 Add the reflection test that asserts that `createNextVersion` copies every non-identity property of
      `ZaaktypeConfiguration`. Verify that it fails when one copied line is removed, then restore the line.
- [ ] 6.4 Verify `./gradlew spotlessApply detektApply detekt build itest` and the contract diff. Then open the PR
      `refactor(admin): make zaaktype configuration versioning a pure function`, with body footer `Solves PZ-12669`.

## 7. PR B4: confirmation email fallback (branch `feature/PZ-12669-b4-confirmation-email-fallback`, base B3)

- [ ] 7.1 Make `template` and `from` on `SendConfirmationEmailDelegate` optional. Fall back to the configuration's
      email parameters for each missing value, and send nothing when no template results (design D9). Verify with unit
      tests for: process wins, configuration fallback, disabled, and no template.
- [ ] 7.2 Verify that the existing BPMN itest process definitions with the delegate still send their email
      (`./gradlew itest`).
- [ ] 7.3 Verify `./gradlew spotlessApply detektApply detekt build itest` and the contract diff. Then open the PR
      `feat(flowable): fall back to the zaaktype configuration for the BPMN confirmation email`, with body footer `Solves PZ-12669`.

## 8. Wrap-up

- [ ] 8.1 Run `openspec validate unify-zaaktype-configuration-backend --strict` and verify that it reports the change as valid.
- [ ] 8.2 Create a follow-up Jira ticket for the contract step of B2: drop the resultaattype UUID columns and the
      fallback. Verify that the ticket links PZ-12637.
