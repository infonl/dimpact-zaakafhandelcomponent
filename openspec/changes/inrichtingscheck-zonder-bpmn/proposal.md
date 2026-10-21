## Why

[PZ-11837](https://dimpact.atlassian.net/browse/PZ-11837): most inrichtingschecks, such as the statustypes
*Intake*, *In behandeling*, *Heropend* and *Afgerond*, only apply to CMMN. A zaaktype with BPMN in its
zaakafhandelparameters therefore always shows false errors, so a beheerder cannot rely on the error list.

## What Changes

- **The inrichtingscheck skips BPMN zaaktypes**: a zaaktype whose zaakafhandelparameters choose BPMN is neither
  checked nor shown. Zaaktypes with CMMN, or without zaakafhandelparameters, are checked as before.
- **The synchronise button goes**: the card *Zaaktype catalogus synchronisatie* is removed from the
  inrichtingscheck page. **BREAKING** for the REST contract: `DELETE /rest/health-check/ztc-cache` and
  `GET /rest/health-check/ztc-cache` are removed. The admin endpoint that clears all caches stays.
- **The handleiding** drops the synchronisation section, says BPMN zaaktypes are not checked or shown, and
  explains that ZAC fetches changed data once Open Zaak sends a notification about the changed zaaktype, and
  how a beheerder clears the cached catalogue data by hand.

## Capabilities

### New Capabilities

- `zaaktype-inrichtingscheck`: which zaaktypes the inrichtingscheck checks and shows, and that it offers no
  manual synchronisation.

### Modified Capabilities

None.

## Impact

- **Backend**: `HealthCheckRestService` filters BPMN zaaktypes and loses the two `ztc-cache` endpoints.
- **Frontend**: the synchronisation card, its service calls and i18n texts are removed.
- **Docs**: `docs/manuals/inrichting-zaakafhandelcomponent/inrichting-zaakafhandelcomponent.md`.
- **Database, Solr, OPA, Open Zaak**: no change.
