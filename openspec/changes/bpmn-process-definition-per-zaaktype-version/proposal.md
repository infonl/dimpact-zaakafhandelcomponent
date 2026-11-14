# Proposal

## Why

Since the CMMN and BPMN admin screens were merged (PZ-12754), a beheerder can no longer choose another BPMN process
for a zaaktype: the process model step locks as soon as a process definition is bound. Before the merge, only the
choice between CMMN and BPMN was locked. A new zaaktype version takes over the binding of its previous version, so
the lock also holds on every new version, and a beheerder has to create a whole new zaaktype to use another process
(PZ-13372).

The reporter agreed in PZ-13372 that another BPMN process is chosen on a new version of the zaaktype, not at any
moment. The resulting rule, "until the first zaak of that zaaktype version", is proposed in comment 100687 and awaits
confirmation. "A new version that was never saved" cannot be detected, because ZAC stores the
configuration of a new zaaktype version itself when Open Notificaties reports its publication. This change therefore
defines a new version as a zaaktype version without zaken.

## What Changes

- A beheerder can bind a BPMN zaaktype version to another BPMN process definition until the first zaak of that
  zaaktype version exists. After that, its process definition is locked.
- The process definition of a CMMN zaaktype version stays locked once bound, because the running zaken of the
  zaaktype version read the plan item settings of its case definition.
- A bound configuration still never changes engine. The process model step of a BPMN zaaktype version offers only
  BPMN process definitions.
- The backend decides whether the process definition may change. The REST configuration of one zaaktype version
  tells the screen, and the backend rejects a save that changes a locked process definition.
- A zaak keeps the process definition it started with, so existing zaken are not affected.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `zaaktype-process-binding`: a bound configuration may change its definition only when it is bound to BPMN and its
  zaaktype version has no zaken; a CMMN binding never changes its definition.
- `zaaktype-configuration-admin-screen`: the process model step of a saved configuration lets the beheerder choose
  another process definition of the same engine while the backend allows it, instead of always locking it. This
  capability is added by the open change `unify-zaaktype-configuration-frontend`, so this change is archived after
  that one.

## Impact

- Backend: `ZaaktypeConfigurationBeheerService`, `ZaaktypeConfigurationRestService` (`PUT
  /zaakafhandelparameters/{zaaktypeUUID}`), `RestZaaktypeConfigurationConverter` and `RestZaaktypeConfiguration` (new
  field `isProcessDefinitionChangeable`), and the error codes `msg.error.process.engine.cannot.change`, `msg.error.process.definition.cannot.change.cmmn` and
  `msg.error.process.definition.cannot.change`.
- Open Zaak: an extra call, `GET /zaken?zaaktype=<url>`, when a beheerder opens or saves the configuration of a BPMN
  zaaktype version, a second one when the save changes its process definition, and one more when a failed save of a
  changeable configuration reads it again. No other endpoint makes this
  call: not the list of all zaaktype configurations, nor the zaaktype of a zaak or the zaaktypen to create a zaak with.
- Frontend: `zaaktype-configuration-form.ts` and the process model step of `parameters-edit`.
- Test-data scripts: unaffected, they store the same process definition again.
