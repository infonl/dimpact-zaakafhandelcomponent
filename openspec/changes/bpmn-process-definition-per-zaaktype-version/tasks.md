# Tasks

## 1. Backend rule

- [x] 1.1 Add `isProcessDefinitionChangeable` and `checkProcessDefinitionMayChange` to `ZaaktypeConfigurationBeheerService`, with the zaken of the zaaktype URL from Open Zaak (design D1, D2), and verify with unit tests in `ZaaktypeConfigurationBeheerServiceTest` for: unbound, BPMN without zaken, BPMN with a zaak, CMMN, the same binding again (no Open Zaak call), and a zaaktype version without configuration
- [x] 1.2 Call `checkProcessDefinitionMayChange` in `PUT /zaakafhandelparameters/{zaaktypeUUID}` before the configuration is converted and stored (design D4), and verify with a unit test in `ZaaktypeConfigurationRestServiceTest` that a rejected change stores nothing
- [x] 1.3 Add `isProcessDefinitionChangeable` to `RestZaaktypeConfiguration` with `@get:`/`@set:JsonbProperty`, and fill it in only in `GET` and `PUT /zaakafhandelparameters/{zaaktypeUUID}` (design D3); verify with `ZaaktypeConfigurationRestServiceTest`, and with `RestZaakafhandelParametersConverterTest` that the conversion of a configuration makes no Open Zaak call
- [ ] 1.4 Add an integration test that binds a BPMN zaaktype version to another process definition before its first zaak, and that a save with another process definition is rejected after a zaak of that version exists, and that a configuration cannot switch engine; list it in `src/itest/itest-shards.txt` and verify with `./gradlew itest`
- [x] 1.5 Reject a change with an error code per reason and its nl/en message (design D4), and verify with `ZaaktypeConfigurationBeheerServiceTest`

## 2. Admin screen

- [x] 2.1 Lock the process model step in `zaaktype-configuration-form.ts` only when a bound configuration has `isProcessDefinitionChangeable` false, and verify with `zaaktype-configuration-form.spec.ts` for a locked and an unlocked BPMN configuration
- [x] 2.2 Offer only the process definitions of the bound engine in the process model step (design D5), and verify with `parameters-step-proces-model.component.spec.ts` that a BPMN configuration offers only BPMN process definitions and can choose another one
- [x] 2.3 Verify with `parameters-edit.component.spec.ts` that a changeable BPMN configuration stores the other BPMN process definition that the beheerder chooses, and that the tests of the locked step follow the backend after saving
- [x] 2.4 Show the explanation of a locked process model step, with the BPMN or CMMN reason, in `nl.json` and `en.json` (design D6), and verify with `parameters-step-proces-model.component.spec.ts` that a locked BPMN step, a locked CMMN step and an unlocked step show the right text or none
- [x] 2.5 After a rejected save, read the configuration again and show the locked process model step with its explanation when the backend no longer allows a change (risk: a zaak created between opening and saving), and verify with `parameters-edit.component.spec.ts` for a locked and a still changeable result

## 3. Checks

- [x] 3.1 Verify that the new frontend tests fail when the lock, the filter or the explanation is undone, and pass with the change
- [ ] 3.2 Run `./gradlew spotlessApply detekt`, `npm run lint` and `npx tsc --project .` in `src/main/app`, and verify that they report no new issues
- [x] 3.3 In the local stack, publish a new version of a BPMN zaaktype, choose another process before the first zaak, create a zaak and verify that it starts the chosen process; then verify that the process model step is locked

## Workflow follow-up

- Discuss this change with Sander before implementing or merging it.
- Archive `unify-zaaktype-configuration-frontend` first, then this change.
