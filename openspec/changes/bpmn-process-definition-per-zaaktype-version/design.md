# Design

## Context

- The binding of a zaaktype configuration holds the engine and the definition **key**, not a version of the process
  definition. A new zaak starts the newest Flowable version of that key and keeps it for its whole life.
- Every zaaktype version in Open Zaak has its own URL, and every zaak refers to exactly one of them. When Open
  Notificaties reports a published zaaktype version, ZAC copies the configuration of the previous version with the
  same omschrijving, including its binding, and stores it at once.
- The entity method that binds a configuration rejects a change of engine and accepts a new key. It removes the CMMN
  extension when it binds to BPMN. Since the merge of the admin screens, only the frontend locks the process model
  step, for every saved configuration.
- The open change `unify-zaaktype-configuration-frontend` adds the capability `zaaktype-configuration-admin-screen`
  and the current text of the binding requirement. This change modifies both, so it is archived after that change.

## Goals / Non-Goals

**Goals:**
- One place, in the backend, decides whether the engine and the definition of a zaaktype version may change.
- The admin screen follows that decision and cannot be bypassed through the REST API.

**Non-Goals:**
- Migrating running zaken to another process definition, engine or newer version of their process definition.
- Linking zaaktype versions by `identificatie` instead of `omschrijving`.
- Re-uploading the forms of a new process definition version.

## Decisions

### D1. "A new zaaktype version" means a zaaktype version without zaken

A version that was never saved by a beheerder cannot be detected, because ZAC stores it itself. Alternatives:
- *A flag set when a beheerder first saves the configuration*: needs a database migration, and blocks a beheerder
  who saves another setting before choosing the process.
- *Always allow a change for BPMN*: matches the screens before the merge, but the reporter and the team chose a new
  version as the moment to change (PZ-13372).

A zaaktype version without zaken is what matters for the behaviour: no zaak runs on the binding yet. The same holds
for both engines, so the rule covers a change of CMMN case definition and a change of engine as well: without zaken,
no zaak reads the plan item settings or runs a process instance of the old binding.

### D2. Open Zaak is the source for "has zaken"

The backend lists the zaken of the zaaktype URL in Open Zaak and checks whether there is at least one. Alternatives:
- *Solr*: faster, but lags behind when a notification is missed, which would unlock a configuration that has zaken.
- *Flowable process variables*: only know zaken that ZAC started.
- *`isProcessDefinitionInUse` of the BPMN service*: checks a key across all zaaktypen, not one zaaktype version.

### D3. The backend exposes the decision as `isProcessDefinitionChangeable`

`RestZaaktypeConfiguration` gets `isProcessDefinitionChangeable`. Only `GET` and `PUT
/zaakafhandelparameters/{zaaktypeUUID}` fill it in. Every other conversion of a configuration leaves it `false`, such
as the list of all configurations, the zaaktype of a zaak and the zaaktypen to create a zaak with, so those make no
Open Zaak call and do not fail when Open Zaak cannot list zaken. It is `true` for an unbound configuration, and for a
bound configuration of either engine while the zaaktype version has no zaken. The frontend locks the process model
step when a bound configuration has `isProcessDefinitionChangeable` set to `false`.

Alternative: a separate endpoint. Rejected, because the screen needs the value together with the configuration and
would otherwise make a second request on every open.

### D4. The save checks the same rule

`PUT /zaakafhandelparameters/{zaaktypeUUID}` rejects a binding with another engine or key when the zaaktype version
has zaken, before anything is stored. The validation error has the error code
`msg.error.process.definition.cannot.change`, so the beheerder sees a translated message instead of a generic
technical error. The check makes no Open Zaak call when the binding stays the same; the response of the save still
makes one, to fill in `isProcessDefinitionChangeable`.

The entity method that binds a configuration no longer rejects a change of engine: the check above decides. When it
binds to BPMN, it still removes the CMMN extension. When it binds to another CMMN case definition, it first clears the
plan item settings of the previous case definition, also when the save sends none, and stores only the settings of
plan items that the new case definition has.

The save looks up `isProcessDefinitionChangeable` for its response before the transaction commits, so a failed Open
Zaak call stores nothing instead of reporting an error for a configuration that did change.

### D5. The process model step offers every engine while the binding may change

While `isProcessDefinitionChangeable` is `true`, the step offers the CMMN case definitions and the BPMN process
definitions, as for an unbound configuration. When the beheerder chooses another engine, the screen shows the steps of
that engine, as it does for an unsaved configuration.

### D6. The screen explains a locked process definition

When the process model step is locked, it shows one line under the list with the reason: the zaaktype version
already has zaken and a new version is needed for another process definition. The frontend derives it from
`isProcessDefinitionChangeable`, so the REST contract needs no reason field. The general warning about effects on
existing zaken stays.

## Risks / Trade-offs

- [A zaak is created between opening and saving the screen] → the save checks again and rejects the change. The
  screen then reads the configuration again and, when it is locked, shows the stored process definition, locked, with
  its explanation. Other unsaved settings are lost; that is rare and the message says nothing was saved.
- [A zaak is created between the check and the store of a save] → the save checks again just before it stores, which
  makes the window short but does not close it. A zaak created in that window keeps the process instance it started
  with; only new zaken use the new binding. For a CMMN zaak in that window, its plan item settings may no longer
  match its case definition, and after a change of engine, terminating it targets the engine of the configuration
  instead of the one it runs on. Routing termination by the running instance is a separate ticket.
- [A published zaaktype version is valid at once, so a zaak can be created with the copied process before the
  beheerder chooses another one] → the beheerder publishes the version with a future begin date, chooses the process,
  and lets the date take effect. This protects only zaken created by hand: the productaanvraag intake picks the most
  recently created configuration and ignores the begin date, so for a zaaktype with a productaanvraagtype the beheerder
  stops the intake first. Documented in the PR, not enforced.
- [Open Zaak is unavailable] → opening and saving the configuration of a bound zaaktype fails, as other Open Zaak
  calls of the screen already do.
- [The zaken of a zaaktype version are deleted in Open Zaak; ZAC cannot delete a zaak] → the version counts as new
  again and its process can change; no zaak runs on the old binding, so that is safe.
