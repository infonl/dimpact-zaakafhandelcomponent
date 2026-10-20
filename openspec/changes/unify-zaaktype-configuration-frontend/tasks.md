## 0. Baseline and stack

- [ ] 0.1 Read `PZ-12754-rfc-unify-zaaktype-configuration.md` from PZ-12754. Record every difference with design.md
      in this change before layer 5 starts, or record that there is none.
- [ ] 0.2 The bottom layer of the stack is the branch that holds this OpenSpec change
      (`PZ-12754-zaakafhandelparameters-za-ps-frontend-backend-api-code-voor-bpmn-en-cmmn-vereenvoudigen`).
      Verify that `openspec validate unify-zaaktype-configuration-frontend --strict` passes on it.
- [ ] 0.3 On `main`, run `./gradlew generateOpenApiSpec` and copy the generated `openapi.json` to the scratchpad as the
      contract baseline for layer 5. Record `wc -l` of `parameters-edit-cmmn.component.ts` and
      `parameters-edit-bpmn.component.ts` (1176 + 604 today) for the PR bodies.

## 1. Layer 1: shared beëindiging step (`feature/PZ-12754-step-beeindiging`)

- [ ] 1.1 Create `parameters-edit/steps/parameters-step-beeindiging` with a signal `input.required` for its `FormGroup`
      and its own query for zaakbeeindigredenen and resultaattypen (design D4). Verify `npm run lint` reports no
      `@Input`/`@Output`.
- [ ] 1.2 Replace the beëindiging markup and form code in `parameters-edit-cmmn` and `parameters-edit-bpmn` with the step
      component. Verify that both components render the step with the same fields as before.
- [ ] 1.3 Move `parameters-edit-cmmn.component.form-step-6.beeindiging.spec.ts` to a spec of the step component,
      parameterised by engine with `it.each`. Verify with `npm test` that it passes for CMMN and BPMN.
- [ ] 1.4 Run `./gradlew spotlessApply` and `npm test`, and open the PR. Title:
      `refactor(admin): share the zaak beëindiging step between the CMMN and BPMN screens`. The body ends with
      `Solves PZ-12754`.

## 2. Layer 2: shared koppelingen step (`feature/PZ-12754-step-koppelingen`)

- [ ] 2.1 Create `parameters-step-koppelingen` (betrokkene koppelingen and BRP doelbindingen). Restore the two
      `aria-label`s that the BPMN copy lost. Verify that every field has an accessible name with a
      Testing Library `getByRole` query in the spec.
- [ ] 2.2 Use the step in both edit components, and remove the duplicated form groups. Verify that the stored
      betrokkene and BRP values round-trip in both components with a spec per engine.
- [ ] 2.3 Move `form-step-7.koppelingen.spec.ts` to the step spec with `it.each` over the engines. Run
      `./gradlew spotlessApply` and `npm test`, and open the PR (`refactor(admin): …`, `Solves PZ-12754`).

## 3. Layer 3: one process model step (`feature/PZ-12754-step-proces-model`)

- [ ] 3.1 Create `parameters-step-proces-model`. It offers CMMN or BPMN and then a definition for an unsaved
      configuration, and offers only definitions of the bound engine for a saved one. Verify both cases with a spec.
- [ ] 3.2 Replace step 1 of both edit components and `parameters-select-process-model-method` with the step.
      Delete `parameters-select-process-model-method` and move its spec into the step spec. Verify `npm test`.
- [ ] 3.3 Run `./gradlew spotlessApply` and open the PR (`refactor(admin): …`, `Solves PZ-12754`).

## 4. Layer 4: shared algemeen step (`feature/PZ-12754-step-algemeen`)

- [ ] 4.1 Create `parameters-step-algemeen` with the groep/behandelaar cascade, productaanvraagtype, the two warning
      windows, and a definition slot (case definition or process definition). Hide the warning windows for BPMN
      in this layer, to keep it free of behaviour changes. Verify the groep cascade with a spec per engine.
- [ ] 4.2 Use the step in both edit components. Verify that `form-step-2.algemeen.spec.ts`, now moved to the step spec
      with `it.each`, passes for both engines.
- [ ] 4.3 Run `./gradlew spotlessApply` and `npm test`, and open the PR (`refactor(admin): …`, `Solves PZ-12754`).

## 5. Layer 5: one resource and one container (`feature/PZ-12754-one-resource`)

- [ ] 5.1 Backend: add `processEngine`, `RestCmmnConfiguration`, and `RestBpmnConfiguration` to
      `RestZaaktypeConfiguration`, and move the CMMN-only fields into the `cmmn` block (design D2). Update
      `RestZaaktypeConfigurationConverter`. Verify with a unit test per engine that a read has only its own block, and
      that an unbound configuration has none.
- [ ] 5.2 Backend: replace `PUT /zaakafhandelparameters` with `PUT /zaakafhandelparameters/{zaaktypeUUID}`. It rejects
      a block that does not match `processEngine`, both blocks at once, and a path key that does not match the
      payload. It requires `beheren` (design D3). Verify each rejection with a unit test.
- [ ] 5.3 Backend: delete `ZaaktypeBpmnConfigurationRestService`, `RestZaaktypeBpmnConfiguration`, and their unit
      tests. Verify `./gradlew build` and that `git grep zaaktype-bpmn-configuration -- src/main` finds nothing.
- [ ] 5.4 Itests: move `ZaaktypeBpmnConfigurationRestServiceTest`, `NotificationZaaktypeCompletionParametersTest`, and
      `ZacClient` to the one resource. Add one parameterised itest that round-trips the engine-agnostic settings for
      both engines, and one for "the BPMN resource is gone". Verify with `./gradlew itest`.
- [ ] 5.5 Run `./gradlew generateOpenApiSpec` and regenerate the frontend types. Diff the spec against the baseline of
      0.3, and verify that the only changes are those of design D2 and D3. Paste the summary into the PR body.
- [ ] 5.6 Frontend: write `toForm()` and `toRestConfiguration()` next to the container, with `it.each` specs over both
      engines. These include the "unopened step keeps its stored values" case for SmartDocuments and BRP. Verify
      with `npm test`.
- [ ] 5.7 Frontend: create the `parameters-edit` container (design D4, D5). It renders the stepper once for a saved
      configuration, and renders the process model step first for an unsaved one. Verify with specs: "saved BPMN
      shows no humantask step", "choose BPMN, go back to step 1, values kept".
- [ ] 5.8 Frontend: change the resolver to one `GET`. Use TanStack `queryOptions()` and `mutationOptions()` in
      `zaakafhandel-parameters.service.ts` for the read and the `PUT`, and point the `parameters/:uuid` route at the
      container. Verify the resolver spec and `npm test`.
- [ ] 5.9 Delete `parameters-edit-cmmn`, `parameters-edit-bpmn`, and `parameters-edit-shell`, after moving their
      remaining specs (humantasks, acties, mail, all-steps, selected-index-start) to the steps or the container.
      Verify `npm test`, `npm run lint`, and `ngc -p tsconfig.app.json`.
- [ ] 5.10 Run the e2e admin scenarios with the local Docker stack, and save one CMMN and one BPMN configuration
      through the screen. Verify that both reload with the stored values.
- [ ] 5.11 Run `./gradlew spotlessApply detektApply detekt build`, and open the PR. Title:
      `feat(admin)!: serve the zaaktype configuration of both engines from one REST resource and one screen`.
      The body records the old and new line counts and ends with `Solves PZ-12754`.

## 6. Layer 6: BPMN settings in the screen (`feature/PZ-12754-bpmn-settings`)

- [ ] 6.1 Show the two warning windows in the algemeen step for BPMN. Verify with a spec, and with an itest that a
      BPMN zaak with a stored window is listed by the zaak warnings endpoint.
- [ ] 6.2 Create `parameters-step-mail`. It shows the mailtemplate koppelingen and the zaakafzenders for both
      engines, and the intake/afronden mail and the confirmation email only for CMMN. Verify with `it.each` specs per
      engine, and move `form-step-5.mail.spec.ts` into it.
- [ ] 6.3 For BPMN, show a collapsed confirmation email section without fields, with the text that the BPMN process
      sends the confirmation of receipt. Add the text to the Dutch and English i18n files. Verify with a spec that
      no confirmation email field is in the DOM for BPMN.
- [ ] 6.4 Add itests: a BPMN zaak uses its mailtemplate koppeling in `GET /mailtemplates/{mail}/{zaakUUID}`, and
      lists its default zaakafzender. Verify with `./gradlew itest`.
- [ ] 6.5 Run `./gradlew spotlessApply` and `npm test`, and open the PR. Title:
      `feat(admin): let a beheerder set the warning windows and mail settings of a BPMN zaaktype`. The body ends with
      `Solves PZ-12754`.

## 7. Stack and archive

- [ ] 7.1 Link layers 0–6 as one GitHub stack with `gh stack init` / `gh stack submit --auto`. Verify the stack
      server-side: a no-op `gh pr edit --base` must fail with `part of a stack`.
- [ ] 7.2 After layer 6 merges, archive this change with `openspec archive unify-zaaktype-configuration-frontend`,
      and verify that `openspec/specs/zaaktype-configuration-admin-screen/spec.md` exists.
