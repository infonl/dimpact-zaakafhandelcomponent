## 1. Backend

- [ ] 1.1 Add `ERROR_CODE_MEDEWERKER_ALREADY_ZAAKSPECIFIEK_GEAUTORISEERD` and
      `ERROR_CODE_ZAAK_NOT_ZAAKSPECIFIEK_GEAUTORISEERD` with their exceptions.
- [ ] 1.2 `ZaakspecifiekeAutorisatieService.listKandidaten(zaak, groepId)`: the users of a `behandelaar` groep for
      the zaaktype minus `geautoriseerdeMedewerkerIds` and members of `zaakspecifiek_geautoriseerd` groepen.
- [ ] 1.3 `ZaakspecifiekeAutorisatieService.addZaakspecifiekGeautoriseerdeMedewerker(zaak, groepId, medewerkerId)`:
      validate, grant under the lock with the taakbehandelaar toelichting, reindex.
- [ ] 1.4 `ZaakRestService`: `GET zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/kandidaten` and
      `POST zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers`, both asserting `wijzigen`.
- [ ] 1.5 Unit tests for 1.2-1.4; integration test that adds a medewerker and checks access, the rol, the
      betrokkenen and the candidates.
- [ ] 1.6 Regenerate the OpenAPI spec.

## 2. Frontend

- [ ] 2.1 `ZakenService`: `listZaakspecifiekGeautoriseerdeMedewerkerKandidaten` and
      `addZaakspecifiekGeautoriseerdeMedewerker`.
- [ ] 2.2 *Medewerker toevoegen* menu item under *Koppelingen* in `zaak-view-menu.builder.ts`.
- [ ] 2.3 Side panel component with groep and medewerker selects; snackbar and close on success.
- [ ] 2.4 i18n texts in `nl.json` and `en.json`, including the two error codes.
- [ ] 2.5 Specs for the menu builder and the panel.

## 3. Wrap-up

- [ ] 3.1 Update `docs/solution-architecture/accessControlPolicies.md`.
- [ ] 3.2 Run the linters and tests.
