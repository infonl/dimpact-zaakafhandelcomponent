## 1. Backend

- [ ] 1.1 Add `ERROR_CODE_MEDEWERKER_CANNOT_BE_ONTKOPPELD` and
      `ERROR_CODE_MEDEWERKER_NOT_ZAAKSPECIFIEK_GEAUTORISEERD` with their exceptions.
- [ ] 1.2 `ZaakspecifiekeAutorisatieService.removeZaakspecifiekGeautoriseerdeMedewerker(zaak, zaakAutorisatieGegevens, medewerkerId, reden)`:
      validate, delete every ZGM rol of the medewerker under the lock with the reden, reindex.
- [ ] 1.3 `isOntkoppelbaar` on `RestGeautoriseerdeMedewerker`, worked out by the service.
- [ ] 1.4 `DELETE zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/{medewerkerId}` in
      `ZaakspecifiekeAutorisatieRestService`, asserting `wijzigen`.
- [ ] 1.5 Unit tests for 1.1-1.4.
- [ ] 1.6 Integration test: ontkoppel an added medewerker and check the rol is gone, access is refused, the
      medewerker no longer finds the zaak through `zoeken/list`, the zaakhistorie, and that the zaakbehandelaar
      is refused.

## 2. Frontend

- [ ] 2.1 `ZakenService.deleteZaakspecifiekGeautoriseerdeMedewerker`.
- [ ] 2.2 `ZaakDialogService.openOntkoppelZaakspecifiekGeautoriseerdeMedewerker`.
- [ ] 2.3 Action column in the *Medewerkers* tab; snackbar and refresh on success.
- [ ] 2.4 i18n texts in `nl.json` and `en.json`, including the two new error codes.
- [ ] 2.5 Specs for the tab.

## 3. Wrap-up

- [ ] 3.1 Update the gebruikershandleiding and `docs/solution-architecture/accessControlPolicies.md`.
- [ ] 3.2 Run the linters and tests.
