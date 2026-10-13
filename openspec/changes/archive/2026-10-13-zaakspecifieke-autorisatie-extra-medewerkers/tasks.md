## 1. Backend

- [x] 1.1 Add `ERROR_CODE_ZAAK_NOT_ZAAKSPECIFIEK_GEAUTORISEERD`, `ERROR_CODE_MEDEWERKER_ALREADY_ZAAKSPECIFIEK_GEAUTORISEERD`
      and `ERROR_CODE_GROUP_NOT_BEHANDELAAR_FOR_ZAAKTYPE` with their exceptions.
- [x] 1.2 `ZaakspecifiekeAutorisatieService.listZaakspecifiekGeautoriseerdeMedewerkerKandidaten(zaak, zaakType, groepId)`:
      the users of a `behandelaar` groep for the zaaktype minus `geautoriseerdeMedewerkerIds` and members of
      `zaakspecifiek_geautoriseerd` groepen.
- [x] 1.3 `ZaakspecifiekeAutorisatieService.addZaakspecifiekGeautoriseerdeMedewerker(zaak, zaakType, groepId, medewerkerId)`:
      validate, grant under the lock with the shared toelichting, reindex.
- [x] 1.4 New `ZaakspecifiekeAutorisatieRestService`: `GET zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/kandidaten`
      and `POST zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers`, both asserting `wijzigen`.
- [x] 1.5 Unit tests for 1.1-1.4.
- [x] 1.6 Integration test: add a medewerker and check the rol, access, that the medewerker finds the zaak, its
      taak and its document through `zoeken/list` (the endpoint behind the werkvoorraden and zoekresultaten), the
      zaakhistorie, the betrokkenen, the candidates, a refused duplicate, a request without groep, and that the
      added medewerker can add another one.

## 2. Frontend

- [x] 2.1 `ZakenService`: `listZaakspecifiekGeautoriseerdeMedewerkerKandidaten` and
      `addZaakspecifiekGeautoriseerdeMedewerker`.
- [x] 2.2 *Medewerker toevoegen* menu item under *Koppelingen* in `zaak-view-menu.builder.ts`.
- [x] 2.3 Side panel component with groep and medewerker selects; snackbar and close on success.
- [x] 2.4 i18n texts in `nl.json` and `en.json`, including the three new error codes.
- [x] 2.5 Specs for the menu builder and the panel.

## 3. Wrap-up

- [x] 3.1 Update `docs/solution-architecture/accessControlPolicies.md`.
- [x] 3.2 Run the linters and tests.
