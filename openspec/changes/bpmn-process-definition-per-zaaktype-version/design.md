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

The backend lists the UUIDs of the zaken of the zaaktype URL in Open Zaak with `listZakenUuids`, and checks whether
there is at least one; it needs no other data of the zaken. Alternatives:
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
technical error. The save asks Open Zaak once, before anything is stored, whether the zaaktype version has zaken,
and uses the answer both for the check and for `isProcessDefinitionChangeable` in its response.

The entity method that binds a configuration no longer rejects a change of engine: the check above decides. When it
binds to BPMN, it still removes the CMMN extension; otherwise it only replaces the binding and keeps the stored plan
item settings. The converter that applies a save to the configuration handles those settings. When the save binds to
another CMMN case definition, it clears the plan item settings of the previous case definition, also when the save
sends none, and stores only the settings of plan items that the new case definition has; rows of plan items that
both case definitions share are updated in place. On every bind to CMMN, it adds a default setting, without
toelichting, for each user event listener of the case definition that has no setting after the save, because listing
the actions of a zaak requires one per listener. Stored settings that the save leaves out keep their toelichting.
Because the converter reads the case definition before the store, a save with an unknown case definition stores
nothing; before this change, it stored the binding and then failed while building the response.

The zaken lookup runs before the transaction of the save, and the save builds its response after the commit. A
failed zaken lookup therefore stores nothing. The Catalogi reads of the response run after the commit, as they did
before this change, and are mostly cached. The save evicts the cached configuration before it commits, so a
remote call inside the transaction would give a concurrent read the time to cache the previous binding again, and
zaken created in the next hour would start the previous process. A zaak created between the lookup and the commit
makes the response report a changeable configuration once; the next save checks again.

### D5. The process model step offers every engine while the binding may change

While `isProcessDefinitionChangeable` is `true`, the step offers the CMMN case definitions and the BPMN process
definitions, as for an unbound configuration. When the beheerder chooses another engine, the screen shows the steps of
that engine, as it does for an unsaved configuration. When the beheerder chooses the stored process definition again,
the screen restores its stored task and action settings. Because the human tasks get their default reference tables
from the formulier definitions and the reference tables, the screen enables Save only once both have loaded; when one
of those reads fails, the screen reports that error and Save stays disabled.

### D6. The screen explains whether the process definition may change

When the process model step is locked, it shows one line under the list with the reason: the zaaktype version
already has zaken and a new version is needed for another process definition. When a bound configuration may still
change, the line instead says that the process definition locks once the first zaak exists, so the beheerder knows
the choice is not open for ever. An unbound configuration shows no line. The frontend derives both from
`isProcessDefinitionChangeable` and whether the configuration is bound, so the REST contract needs no reason field.
The general warning about effects on existing zaken stays.

Because the rule is per zaaktype version, and versions share their omschrijving, the step also names the version it
configures under the omschrijving by its validity: its begin geldigheid and, when set, einde geldigheid. These are the
dates a beheerder uses to change the process model of a zaaktype with zaken: the new version starts in the future and
the current version ends on that date. The identificatie and the versiedatum are left out, because ZAC does not use them
to tell versions apart. The screen already receives these fields with the zaaktype of the configuration.

## Risks / Trade-offs

- [A zaak is created between opening and saving the screen] → the save checks again and rejects the change. The
  screen then reads the configuration again and, when it is locked, shows the stored process definition, locked, with
  its explanation. Other unsaved settings are lost; that is rare and the message says nothing was saved.
- [A zaak is created between the check and the store of a save] → the check uses the single zaken lookup of the save,
  made just before it stores, which makes the window short but does not close it. A zaak created in that window keeps
  the process instance it started with; only new zaken use the new binding. For a CMMN zaak in that window, its plan
  item settings may no longer match its case definition, and after a change of engine, terminating it targets the
  engine of the configuration instead of the one it runs on. Routing termination by the running instance is a separate
  ticket.
- [A published zaaktype version is valid at once, so a zaak can be created with the copied process before the
  beheerder chooses another one] → the beheerder publishes the version with a future begin date, chooses the process,
  and lets the date take effect. This protects only zaken created by hand: the productaanvraag intake picks the most
  recently created configuration and ignores the begin date, so for a zaaktype with a productaanvraagtype the beheerder
  stops the intake first. Documented in the beheerder manual (task 2.7), not enforced.
- [Open Zaak is unavailable] → opening the configuration of a bound zaaktype, and saving any configuration, fails,
  as other Open Zaak calls of the screen already do.
- [The zaken of a zaaktype version are deleted in Open Zaak; ZAC cannot delete a zaak] → the version counts as new
  again and its process can change; no zaak runs on the old binding, so that is safe.
