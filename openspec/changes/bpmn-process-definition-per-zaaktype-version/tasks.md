# Tasks

## 1. Backend rule

- [ ] 1.1 Add `isProcessDefinitionChangeable` and `checkProcessDefinitionMayChange` to `ZaaktypeConfigurationBeheerService`, with the zaken of the zaaktype URL from Open Zaak (design D1, D2), and verify with unit tests in `ZaaktypeConfigurationBeheerServiceTest` for: unbound, BPMN and CMMN without zaken, BPMN and CMMN with a zaak, another engine with and without zaken, the same binding again (no Open Zaak call), and a zaaktype version without configuration
- [ ] 1.2 Call `checkProcessDefinitionMayChange` in `PUT /zaakafhandelparameters/{zaaktypeUUID}` before the configuration is converted and stored (design D4), and verify with a unit test in `ZaaktypeConfigurationRestServiceTest` that a rejected change stores nothing
- [ ] 1.3 Add `isProcessDefinitionChangeable` to `RestZaaktypeConfiguration` with `@get:`/`@set:JsonbProperty`, and fill it in only in `GET` and `PUT /zaakafhandelparameters/{zaaktypeUUID}` (design D3); verify with `ZaaktypeConfigurationRestServiceTest`, and with `RestZaakafhandelParametersConverterTest` that the conversion of a configuration makes no Open Zaak call
- [ ] 1.4 Let `ZaaktypeConfiguration.bindTo` accept a change of engine, remove the CMMN extension when it binds to BPMN, and keep only the plan item settings of the new case definition when it binds to another CMMN case definition (design D4); verify with the unit tests of `ZaaktypeConfiguration` and `RestZaaktypeConfigurationConverterTest`
- [ ] 1.5 Add an integration test that, before the first zaak of a zaaktype version, binds a BPMN configuration to another process definition, a CMMN configuration to another case definition, and a configuration to the other engine, and that a save with another process definition or engine is rejected after a zaak of that version exists; list it in `src/itest/itest-shards.txt` and verify with `./gradlew itest`
- [ ] 1.6 Reject a change with the error code `msg.error.process.definition.cannot.change` and its nl/en message (design D4), and verify with `ZaaktypeConfigurationBeheerServiceTest`

## 2. Admin screen

- [ ] 2.1 Lock the process model step in `zaaktype-configuration-form.ts` only when a bound configuration has `isProcessDefinitionChangeable` false, and verify with `zaaktype-configuration-form.spec.ts` for a locked and an unlocked configuration of each engine
- [ ] 2.2 Offer the process definitions of every engine in the process model step while the binding may change (design D5), and verify with `parameters-step-proces-model.component.spec.ts` that a changeable BPMN and a changeable CMMN configuration offer both engines and can choose a process definition of the other engine
- [ ] 2.3 Verify with `parameters-edit.component.spec.ts` that a changeable configuration stores the other process definition, of the same or the other engine, that the beheerder chooses, that the steps follow the chosen engine, and that the tests of the locked step follow the backend after saving
- [ ] 2.4 Show the explanation of a locked process model step in `nl.json` and `en.json` (design D6), and verify with `parameters-step-proces-model.component.spec.ts` that a locked step shows the text and an unlocked step none
- [ ] 2.5 After a rejected save, read the configuration again and show the locked process model step with its explanation when the backend no longer allows a change (risk: a zaak created between opening and saving), and verify with `parameters-edit.component.spec.ts` for a locked and a still changeable result

## 3. Checks

- [ ] 3.1 Verify that the new frontend tests fail when the lock, the list of both engines or the explanation is undone, and pass with the change
- [ ] 3.2 Run `./gradlew spotlessApply detekt`, `npm run lint` and `npx tsc --project .` in `src/main/app`, and verify that they report no new issues
- [ ] 3.3 In the local stack, publish a new version of a BPMN zaaktype, choose another BPMN process before the first zaak, create a zaak and verify that it starts the chosen process; then verify that the process model step is locked
- [ ] 3.4 In the local stack, publish a new version of a CMMN zaaktype, switch it to a BPMN process before the first zaak, create a zaak and verify that it starts the BPMN process; repeat from BPMN to CMMN
- [ ] 3.5 Run `scripts/test-data/configure-zaakafhandelparameters.py` against a zaaktype version without zaken and one with zaken, and verify that the first is bound to CMMN and the second is rejected with the error code

## Workflow follow-up

- Discuss this change with Sander before implementing or merging it.
- Archive `unify-zaaktype-configuration-frontend` first, then this change.
