## 1. OpenSpec change (PR 1)

- [ ] 1.1 Add this change: proposal, specs, design and tasks.

## 2. BPMN zaaktypes hidden from the inrichtingscheck (PR 2)

- [ ] 2.1 `HealthCheckRestService.listZaaktypeInrichtingschecks` skips every zaaktype bound to BPMN, found with
      `ZaaktypeConfigurationService.findConfiguration`, before it builds a check.
- [ ] 2.2 Unit tests: a BPMN zaaktype is not returned and not read from Open Zaak; CMMN and unconfigured
      zaaktypes are still checked; concept and expired zaaktypes stay excluded.
- [ ] 2.3 Handleiding: add that zaaktypes with BPMN in the zaakafhandelparameters are not checked or shown.

## 3. Synchronise button removed (PR 3)

- [ ] 3.1 Remove `DELETE /rest/health-check/ztc-cache` and `GET /rest/health-check/ztc-cache` and their tests;
      regenerate the OpenAPI spec and frontend types.
- [ ] 3.2 Remove the *Zaaktype catalogus synchronisatie* card, its service calls and its i18n texts in
      `nl.json` and `en.json`; update the specs.
- [ ] 3.3 Handleiding: remove the *Zaaktypecatalogus synchronisatie* section and replace "synchroniseer de
      zaaktypecatalogus" with: ZAC fetches the changed data once Open Zaak sends a notification about the
      changed zaaktype, and a beheerder can clear the cached catalogue data with the admin endpoint.

## 4. Wrap-up

- [ ] 4.1 Run the linters and tests.
