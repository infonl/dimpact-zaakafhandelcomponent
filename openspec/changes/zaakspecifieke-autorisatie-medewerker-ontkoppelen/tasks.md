## 1. Backend

- [ ] 1.1 Add `ERROR_CODE_MEDEWERKER_CANNOT_BE_ONTKOPPELD`, `ERROR_CODE_MEDEWERKER_NOT_ZAAKSPECIFIEK_GEAUTORISEERD`
      and `ERROR_CODE_MEDEWERKER_CANNOT_ONTKOPPEL_THEMSELVES` with their exceptions.
- [ ] 1.2 `ZaakspecifiekeAutorisatieService.removeZaakspecifiekGeautoriseerdeMedewerker(zaak, zaakAutorisatieGegevens, medewerkerId, reden)`:
      refuse a zaak that is not zaakspecifiek geautoriseerd and ontkoppelen yourself; then, under the lock, read
      the rollen and open taken, refuse the zaakbehandelaar, a behandelaar of an open taak and a medewerker
      without a ZGM rol, delete every ZGM rol of the medewerker with the reden, reindex the zaak, its taken and
      its documenten, and commit Solr.
- [ ] 1.3 `isOntkoppelbaar` on `RestGeautoriseerdeMedewerker`, worked out by the service. The list shows a
      medewerker with several ZGM rollen once.
- [ ] 1.4 `DELETE zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/{medewerkerId}` in
      `ZaakspecifiekeAutorisatieRestService`, asserting `wijzigen`.
- [ ] 1.5 Unit tests for 1.1-1.4 and 1.7: the refusals, deleting every rol, listing a medewerker once, the
      reindex and commit under the lock, and taak assignment under the same lock.
- [ ] 1.6 Integration test: ontkoppel an added medewerker and check the rol is gone, access is refused, the
      medewerker no longer finds the zaak, its taak and its document through `zoeken/list` (the endpoint
      behind the werkvoorraden and zoekresultaten; koppelen zaken uses the same `SearchService` filter), the
      zaakhistorie line with the medewerker's name and the reden, and that the zaakbehandelaar and a blank
      reden are refused.
- [ ] 1.7 Assign an existing taak under the per-zaak lock, so ontkoppelen cannot run between granting the rol
      and assigning the taak.

## 2. Frontend

- [ ] 2.1 `ZakenService.deleteZaakspecifiekGeautoriseerdeMedewerker`.
- [ ] 2.2 `ZaakDialogService.openOntkoppelZaakspecifiekGeautoriseerdeMedewerker`.
- [ ] 2.3 Action column in the *Medewerkers* tab, only when the employee has `wijzigen`, with the action on the
      rows that are `isOntkoppelbaar`; snackbar and refresh on success.
- [ ] 2.4 i18n texts in `nl.json` and `en.json`, including the three new error codes.
- [ ] 2.5 Specs for the tab, including an employee without `wijzigen` who sees no action, for the dialog
      configuration (required, multiline reden) and for the `DELETE` request (path and `{ reden }` body) and
      what it refreshes.

## 3. Wrap-up

- [ ] 3.1 Update the gebruikershandleiding and `docs/solution-architecture/accessControlPolicies.md`.
- [ ] 3.2 Run the linters and tests.
