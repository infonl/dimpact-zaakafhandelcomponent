## 1. Backend: create a zaak linked to another zaak

- [x] 1.1 Add `RestZaakAanmaakKoppeling(zaakUuid, relatieType)` and the optional field `koppeling` to
      `RestZaakAanmaakGegevens`; run `./gradlew generateOpenApiSpec` and verify the OpenAPI spec has the optional field.
- [x] 1.2 Add a link check for a new zaak next to the rules in `ZaakLinkData.kt` (rights, open zaak, `DEELZAAK` rules,
      only `GERELATEERD` and `DEELZAAK` allowed); verify with unit tests in `ZaakLinkDataTest`.
- [x] 1.3 Move `addGerelateerdeZaak` and the hoofdzaak reindex and `ScreenEvent` out of `ZaakKoppelenRestService` into a
      shared service; verify `ZaakKoppelenRestServiceTest` still passes.
- [x] 1.4 In `ZaakRestService.createZaak`, run the check before the zaak is created, create the zaak with `hoofdzaak`
      or `gerelateerdeZaken` set, and for `GERELATEERD` patch the current zaak with the explanation *Aangemaakt als
      gerelateerde zaak*; verify with unit tests in `ZaakRestServiceTest` for each scenario of the spec
      requirement "Creating a zaak with a link checks the link before the zaak is created", including that no zaak is
      created when a check fails.
- [x] 1.5 Add `koppelen` to `RestZaakRechten`, filled from `ZaakRechten.canKoppelen`; verify with the converter test.
- [x] 1.6 Integration test: create a gerelateerde zaak and a deelzaak from an existing zaak, then read both zaken and
      check `gerelateerdeZaken` on both sides and the history of both zaken; verify with `./gradlew itest`.
- [x] 1.7 Run `./gradlew spotlessApply detektApply detekt test` and verify it passes.

## 2. Frontend: Nieuwe zaak aanmaken tab in Zaak koppelen

- [ ] 2.1 Regenerate the frontend types from the OpenAPI spec and verify `RestZaakAanmaakGegevens.koppeling` and
      `RestZaakRechten.koppelen` exist.
- [ ] 2.2 Give `zac-zaak-create` the signal input `koppeling` and the outputs `zaakCreated` and `cancelled`: filter the
      zaaktypen for `DEELZAAK`, clear a zaaktype that is no longer offered, show the initiator read-only (or the
      "not copied" hint), send `koppeling`, emit instead of navigating; verify with specs in
      `zaak-create.component.spec.ts`, and verify the existing page specs still pass.
- [ ] 2.3 Wrap `zac-zaak-link` in a `mat-tab-group` with *Bestaande zaak koppelen* and *Nieuwe zaak aanmaken*; add the
      *Soort koppeling* select (disabled *Deelzaak* on a deelzaak), the relation text, the empty zaaktype text, and
      the embedded `zac-zaak-create`; show the tab only for an open zaak with `rechten.koppelen` and at least one
      creatable zaaktype; verify with specs in `zaak-link.component.spec.ts`.
- [ ] 2.4 On `zaakCreated`, show `msg.zaak.aangemaakt-en-gekoppeld`, emit `zaakLinked` and close; on `cancelled`,
      close; verify in the `zaak-link` spec and that `zaak-view` reloads the zaak.
- [ ] 2.5 Add the i18n keys to `nl.json` and `en.json` (tab labels, relation options *Gerelateerde zaak* and *Deelzaak*,
      relation texts, empty zaaktype text, initiator hint, success message, history labels `hoofdzaak` and
      `gerelateerdeZaken`) and fix the Dutch `en.json` value of `zaak.koppelen.link.type.deelzaak`; verify no missing
      keys in the specs.
- [ ] 2.6 Run `npm run lint`, `npm test` and `./gradlew spotlessApply` and verify they pass.
