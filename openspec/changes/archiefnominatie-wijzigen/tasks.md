## 1. Backend

- [ ] 1.1 `wijzigen_archief` in `zaak-rechten.rego` with its policy tests.
- [ ] 1.2 `canWijzigenArchief` in `ZaakRechten` and `RestZaakRechten`.
- [ ] 1.3 `ZaakArchiefRestService` with `PUT /zaken/zaak/{uuid}/archiefnominatie` and `RestZaakArchiefnominatieGegevens`.
- [ ] 1.4 Unit tests for 1.2 and 1.3.
- [ ] 1.5 Integration test: a recordmanager changes the archiefnominatie of an afgehandelde zaak; a behandelaar
      is refused.
- [ ] 1.6 Rights matrix in `docs/solution-architecture/accessControlPolicies.md`.

## 2. Frontend (after design PZ-13224)

- [ ] 2.1 *Wijzigen archiefgegevens* in the zaak action menu, only with `canWijzigenArchief`.
- [ ] 2.2 Pop-up with the action *Wijzigen archiefnominatie*: current value, new value, mandatory reden.
- [ ] 2.3 i18n texts and specs.

## 3. Wrap-up

- [ ] 3.1 Update the gebruikershandleiding.
- [ ] 3.2 Add the spike findings and recommendations to PZ-9845.
