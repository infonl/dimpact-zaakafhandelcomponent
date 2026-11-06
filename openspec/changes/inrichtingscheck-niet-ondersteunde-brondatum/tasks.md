## 1. OpenSpec change (PR 1)

- [ ] 1.1 Add this change: proposal, specs, design and tasks.

## 2. change (PR 2)

- [ ] 2.1 Add `resultaattypesMetNietOndersteundeBrondatum` to `ZaaktypeInrichtingscheck`; fill it in
      `HealthCheckService` for `ander_datumkenmerk` and `zaakobject`; require it empty in `isValide`.
- [ ] 2.2 Expose it in `RestZaaktypeInrichtingscheck` and its converter; regenerate the OpenAPI spec and frontend types.
- [ ] 2.3 Show the error in `inrichtingscheck.component.html` (category label and detail text) with texts in
      `nl.json` and `en.json`.
- [ ] 2.4 Tests: `HealthCheckServiceTest` (each enum value, several resultaattypes, supported values, invalid result),
      `HealthCheckRestServiceTest` itest if affected, and the component spec.
- [ ] 2.5 Handleiding: state that ZAC does not support *Ander datumkenmerk* and *Zaakobject*, and add the check to the
      list of checked parts.
- [ ] 2.6 Run `./gradlew spotlessApply detekt`, the backend tests and `npm run lint` and `npm test` in `src/main/app`.
