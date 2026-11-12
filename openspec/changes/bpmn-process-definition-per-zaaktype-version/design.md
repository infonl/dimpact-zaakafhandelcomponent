# Design

## Context

- The binding of a zaaktype configuration holds the engine and the definition **key**, not a version of the process
  definition. A new zaak starts the newest Flowable version of that key and keeps it for its whole life.
- Every zaaktype version in Open Zaak has its own URL, and every zaak refers to exactly one of them. When Open
  Notificaties reports a published zaaktype version, ZAC copies the configuration of the previous version with the
  same omschrijving, including its binding, and stores it at once.
- The entity method that binds a configuration already rejects a change of engine and accepts a new key. Since the
  merge of the admin screens, only the frontend locks the process model step, for every saved configuration.
- The open change `unify-zaaktype-configuration-frontend` adds the capability `zaaktype-configuration-admin-screen`
  and the current text of the binding requirement. This change modifies both, so it is archived after that change.

## Goals / Non-Goals

**Goals:**
- One place, in the backend, decides whether the process definition of a zaaktype version may change.
- The admin screen follows that decision and cannot be bypassed through the REST API.

**Non-Goals:**
- Changing the case definition of a CMMN zaaktype version, also when it has no zaken.
- Migrating running zaken to another process definition or to a newer version of their process definition.
- Linking zaaktype versions by `identificatie` instead of `omschrijving`.
- Re-uploading the forms of a new process definition version.

## Decisions

### D1. "A new zaaktype version" means a zaaktype version without zaken

A version that was never saved by a beheerder cannot be detected, because ZAC stores it itself. Alternatives:
- *A flag set when a beheerder first saves the configuration*: needs a database migration, and blocks a beheerder
  who saves another setting before choosing the process.
- *Always allow a change for BPMN*: matches the screens before the merge, but the reporter and the team chose a new
  version as the moment to change (PZ-13372).

A zaaktype version without zaken is what matters for the behaviour: no zaak runs on the binding yet.

### D2. Open Zaak is the source for "has zaken"

The backend lists the zaken of the zaaktype URL in Open Zaak and checks whether there is at least one. Alternatives:
- *Solr*: faster, but lags behind when a notification is missed, which would unlock a configuration that has zaken.
- *Flowable process variables*: only know zaken that ZAC started.
- *`isProcessDefinitionInUse` of the BPMN service*: checks a key across all zaaktypen, not one zaaktype version.

### D3. The backend exposes the decision as `isProcessDefinitionChangeable`

`RestZaaktypeConfiguration` gets `isProcessDefinitionChangeable`. It is filled in only when one configuration is
read with its related data, so `GET /zaakafhandelparameters` makes no Open Zaak call per zaaktype. It is `true` for
an unbound configuration, `false` for CMMN, and for BPMN `true` while the zaaktype version has no zaken. The frontend
locks the process model step when a bound configuration has `isProcessDefinitionChangeable` set to `false`.

Alternative: a separate endpoint. Rejected, because the screen needs the value together with the configuration and
would otherwise make a second request on every open.

### D4. The save checks the same rule

`PUT /zaakafhandelparameters/{zaaktypeUUID}` rejects a binding with another engine or key when the stored
configuration does not allow it, with a validation error, before anything is stored. Saving the same binding again
makes no Open Zaak call, so the test-data scripts and repeated saves are unaffected.

### D5. The process model step offers only the engine of the binding

The step gets the engine of the stored binding and filters the list of process definitions on it. A beheerder of a
BPMN configuration therefore cannot choose a CMMN case definition; the backend rejects a change of engine anyway.

### D6. The screen explains a locked process definition

When the process model step is locked, it shows one line under the list with the reason: for BPMN, that the zaaktype
version already has zaken and a new version is needed; for CMMN, that its process definition cannot change. The
frontend derives the reason from the engine of the binding and `isProcessDefinitionChangeable`, so the REST contract
needs no reason field. The general warning about effects on existing zaken stays.

## Risks / Trade-offs

- [A zaak is created between opening and saving the screen] → the save checks again and rejects the change.
- [A published zaaktype version is valid at once, so a zaak can be created with the copied process before the
  beheerder chooses another one] → the beheerder publishes the version with a future begin date, chooses the process,
  and lets the date take effect. Documented in the PR, not enforced.
- [Open Zaak is unavailable] → opening and saving the configuration of a BPMN zaaktype fails, as other Open Zaak
  calls of the screen already do.
- [The zaken of a zaaktype version are deleted] → the version counts as new again and its process can change; no
  zaak runs on the old binding, so that is safe.
