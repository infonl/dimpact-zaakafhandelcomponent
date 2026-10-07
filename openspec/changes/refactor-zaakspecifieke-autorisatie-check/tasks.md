## 1. One derivation and one check

- [x] 1.1 Add `isGeautoriseerdeMedewerker(userId: String)` to `ZaakAutorisatieGegevens`. It returns
      `isZaakspecifiekGeautoriseerd && userId in geautoriseerdeMedewerkers`.
- [x] 1.2 Move the derivation from `ReindexSupportService.zaakAutorisatieGegevens` (the `Zaak` and `UUID` variants and
      the private body) into `ZaakspecifiekeAutorisatieService.readZaakAutorisatieGegevens(zaak)` and
      `readZaakAutorisatieGegevens(zaakUuid)`. Let `ReindexSupportService` delegate to them and keep its memoising
      lookup.

## 2. `PolicyService`

- [x] 2.1 Give `readZaakRechten(zaak, zaaktype, loggedInUser)`, `readDocumentRechten(eio, lock, zaak)` and
      `readTaakRechten(taskInfo, zaaktypeOmschrijving)` an optional
      `zaakAutorisatieGegevens: ZaakAutorisatieGegevens` parameter that defaults to reading it. Derive
      `zaakspecifiekGeautoriseerd` and `loggedInUserIsGeautoriseerdeMedewerker` from it, and remove the private
      `Zaak.isGeautoriseerdeMedewerkerOf`. For a document without a zaak, keep today's behaviour: not zsa and not a
      geautoriseerde medewerker.
- [x] 2.2 In the three zoekobject overloads, build a `ZaakAutorisatieGegevens` from the zoekobject's own fields and
      use `isGeautoriseerdeMedewerker`.
- [x] 2.3 Let the convenience overloads (`readZaakRechten(zaak, loggedInUser)`, `readDocumentRechten(eio, zaak)`,
      `readTaakRechten(taskInfo)`) pass the parameter through where a caller needs it.

## 3. Reuse on the read paths

- [x] 3.1 Give `RestZaakConverter.toRestZaak` an optional `zaakAutorisatieGegevens` parameter that defaults to
      reading it, and use its flag for `isZaakspecifiekGeautoriseerd`.
- [x] 3.2 In `ZaakRestService.readZaak` and `readZaakById`, read the data once and pass it to `readZaakRechten` and
      `toRestZaak`.
- [x] 3.3 In `ZaakRestService.updateZaak`, read the data once before the writes for `readZaakRechten` and
      `isAlreadyZaakspecifiekGeautoriseerd`. Leave the final `toRestZaak` on its default, so that the response is
      built from data read after the writes.
- [x] 3.4 In `TaskRestService.listTasksForZaak`, read the data once and pass it to `readZaakRechten` and to a new
      `RestTaskConverter.convert(tasks, zaakAutorisatieGegevens)`, which passes it to `readTaakRechten`.
- [x] 3.5 In `EnkelvoudigInformatieObjectRestService.listEnkelvoudigInformatieobjectenVoorZaak`, read the data once
      and pass the already-read zaak and the data to `RestInformatieobjectConverter.convertToREST`, so the zaak and
      its zsa marking are not read again for every document.

## 4. Tests (start only after the developer's explicit OK)

- [ ] 4.1 `ZaakAutorisatieGegevens`: `isGeautoriseerdeMedewerker` covers a zsa zaak with and without the user, and a
      zaak that is not zsa, for which the medewerkers are not read.
- [ ] 4.2 `ZaakspecifiekeAutorisatieServiceTest`: `readZaakAutorisatieGegevens` reads the rollen only when
      `geautoriseerdeMedewerkers` is used on a zsa zaak. Move or adapt the existing `ReindexSupportService` tests.
- [ ] 4.3 `PolicyServiceTest`: with and without passed-in data, the OPA input is identical to today, and passed-in
      data causes no zsa call.
- [ ] 4.4 `ZaakRestServiceTest`, `TaskRestServiceTest` and `EnkelvoudigInformatieObjectRestServiceTest`: the zsa
      marking is read once per request on the listed paths, and `updateZaak` returns the marking read after the
      write.
- [x] 4.5 Adjust the setup (constructors and stubs, not the assertions) of the existing tests on the changed paths:
      `ZaakRestServiceTest` (opening and updating a zaak), `TaskRestServiceTest` (listing the taken of a zaak) and
      `EnkelvoudigInformatieObjectRestServiceTest` (listing the documenten of a zaak).

## 5. Wrap-up

- [ ] 5.1 Ask the developer to run `./gradlew spotlessApply detektApply`, `./gradlew detekt`, the unit tests and the
      integration tests. The integration tests are the main safety net for "no behaviour change".
- [ ] 5.2 Run `openspec validate refactor-zaakspecifieke-autorisatie-check`.
