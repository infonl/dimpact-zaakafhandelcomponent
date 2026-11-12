# Tasks

## 1. Backend rule

- [ ] 1.1 Add `isProcessDefinitionChangeable` and `checkProcessDefinitionMayChange` to `ZaaktypeConfigurationBeheerService`, with the zaken of the zaaktype URL from Open Zaak (design D1, D2), and verify with unit tests in `ZaaktypeConfigurationBeheerServiceTest` for: unbound, BPMN without zaken, BPMN with a zaak, CMMN, the same binding again (no Open Zaak call), and a zaaktype version without configuration
- [ ] 1.2 Call `checkProcessDefinitionMayChange` in `PUT /zaakafhandelparameters/{zaaktypeUUID}` before the configuration is converted and stored (design D4), and verify with a unit test in `ZaaktypeConfigurationRestServiceTest` that a rejected change stores nothing
- [ ] 1.3 Add `isProcessDefinitionChangeable` to `RestZaaktypeConfiguration` with `@get:`/`@set:JsonbProperty`, and fill it in `RestZaaktypeConfigurationConverter` only with the related data (design D3); verify with `RestZaakafhandelParametersConverterTest` and by checking that `./gradlew generateOpenApiSpec` lists the field
- [ ] 1.4 Add an integration test that binds a BPMN zaaktype version to another process definition before its first zaak, and that a save with another process definition is rejected after a zaak of that version exists; verify with `./gradlew itest`

## 2. Admin screen

- [ ] 2.1 Lock the process model step in `zaaktype-configuration-form.ts` only when a bound configuration has `isProcessDefinitionChangeable` false, and verify with `zaaktype-configuration-form.spec.ts` for a locked and an unlocked BPMN configuration
- [ ] 2.2 Offer only the process definitions of the bound engine in the process model step (design D5), and verify with `parameters-step-proces-model.component.spec.ts` that a BPMN configuration offers only BPMN process definitions and can choose another one
- [ ] 2.3 Verify with `parameters-edit.component.spec.ts` that a changeable BPMN configuration stores the other BPMN process definition that the beheerder chooses, and that the tests of the locked step follow the backend after saving
- [ ] 2.4 Show the explanation of a locked process model step, with the BPMN or CMMN reason, in `nl.json` and `en.json` (design D6), and verify with `parameters-step-proces-model.component.spec.ts` that a locked BPMN step, a locked CMMN step and an unlocked step show the right text or none

## 3. Checks

- [ ] 3.1 Verify that the new frontend tests fail when the lock, the filter or the explanation is undone, and pass with the change
- [ ] 3.2 Run `./gradlew spotlessApply detekt`, `npm run lint` and `npx tsc --project .` in `src/main/app`, and verify that they report no new issues
- [ ] 3.3 In the local stack, publish a new version of a BPMN zaaktype, choose another process before the first zaak, create a zaak and verify that it starts the chosen process; then verify that the process model step is locked

## Workflow follow-up

- Discuss this change with Sander before implementing or merging it.
- Archive `unify-zaaktype-configuration-frontend` first, then this change.
