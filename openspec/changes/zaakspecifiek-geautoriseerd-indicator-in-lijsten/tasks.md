## 1. Backend: expose the flag

- [x] 1.1 `RestTaakZoekObject`: add `isZaakspecifiekGeautoriseerd` (with `@get:JsonbProperty("isZaakspecifiekGeautoriseerd")`)
      and copy it from `TaakZoekObject` in `toRestTaakZoekObject`.
- [x] 1.2 `RestDocumentZoekObject`: same, copied from `DocumentZoekObject` in `toRestDocumentZoekObject`.
- [x] 1.3 `RestZaakKoppelenZoekObject`: same, copied from `RestZaakZoekObject` in `toRestZaakKoppelenZoekObject`.
- [x] 1.4 `RestZaakOverzicht`: add the field (`@get:` and `@set:JsonbProperty`, it is a `var` class). In
      `RestZaakOverzichtConverter.convert` and `convertForDisplay`, read `ZaakAutorisatieGegevens` once with
      `readZaakAutorisatieGegevens`, pass it to `readZaakRechten` and set the field from it.
- [x] 1.5 `RestZaakInformatieobject`: add the field and fill it the same way in
      `EnkelvoudigInformatieObjectRestService.toRestZaakInformatieobject`.
- [x] 1.6 `SearchService`: add a function that, for a given object type, index field and set of values, returns the
      values whose object is zaakspecifiek geautoriseerd, in one Solr query and without the user-specific filters.
- [x] 1.7 `RestSignaleringTaskSummary`: add the field (`@get:` and `@set:JsonbProperty`); give
      `toRestSignaleringTaakSummary` an `isZaakspecifiekGeautoriseerd` parameter; in
      `SignaleringService.listTakenSignaleringenPage` look up the listed task IDs with 1.6.
- [x] 1.8 `RestGerelateerdeZaak`: add the field and fill it in `RestGerelateerdeZaakConverter.convert` the same way
      as 1.4.
- [x] 1.9 `RestGekoppeldeZaakEnkelvoudigInformatieObject`: add the field; read the `ZaakAutorisatieGegevens` once per
      zaak in `EnkelvoudigInformatieObjectRestService` and pass its flag to `RestInformatieobjectConverter`.
- [x] 1.10 `RestDetachedDocument`: add the field; in `DetachedDocumentRestService` look up the zaaknummers of the page
      with 1.6 and pass the flag per row to `RestDetachedDocumentConverter`.
- [ ] 1.11 Regenerate the OpenAPI spec and check that the nine DTOs carry the new field.

## 2. Frontend: shared lock component

- [x] 2.1 Create the standalone `ZaakspecifiekGeautoriseerdIconComponent` (`zac-zaakspecifiek-geautoriseerd-icon`) in
      `shared/zaakspecifiek-geautoriseerd-icon/`, with one required signal input `isZaakspecifiekGeautoriseerd` and
      the template from the zaakdetailpagina; move the `.zaakspecifiek-geautoriseerd-icon` style into it.
- [x] 2.2 `zaak-details-card`: replace the inline lock markup with the component and remove the moved style.
- [x] 2.3 Add `isZaakspecifiekGeautoriseerd` to the hand-written `TaakZoekObject` and `DocumentZoekObject` types.

## 3. Frontend: lock in front of every listed zaaknummer

- [x] 3.1 Zoekresultaten: zaak result in `zoek-object-link`; the *hoort bij zaak* `zac-static-text` in
      `taak-zoek-object` and `document-zoek-object` (via `position="content-before"`).
- [x] 3.2 Werklijsten: `zaken-werkvoorraad`, `zaken-mijn`, `zaken-afgehandeld`, `taken-werkvoorraad`, `taken-mijn`.
- [x] 3.3 Dashboard cards: `zaken-card`, `zaak-waarschuwingen-card`, `zaak-zoeken-card`, `taken-card`,
      `taak-zoeken-card`.
- [x] 3.4 Detail pages: `zaak-verkort` (taak detailpagina) and the zaak cards in `informatie-object-view`.
- [x] 3.5 Persoon, bedrijf and BAG: `klant-zaken-tabel` and `bag-zaken-tabel`.
- [x] 3.6 Koppelen: `zaak-link` and `informatie-object-link`.
- [x] 3.7 Zaakdetailpagina: `zaak-details-gerelateerde-zaken-tab` and the zaak column of `zaak-documenten`.
- [x] 3.8 Ontkoppelde documenten: the zaak column of `ontkoppelde-documenten-list`.

## 4. Tests

- [x] 4.1 Make the existing backend unit tests work again with the changed constructors, signatures and calls (stubs
      and calls only; test names and logic unchanged).
- [x] 4.2 Unit tests in `SearchServiceTest` for the zaakspecifiek geautoriseerd lookup of 1.6.
- [ ] 4.3 Integration test: the 1.6 lookup finds a zaakspecifiek geautoriseerde zaak by zaaknummer and its taak by task
      ID, also for a user without the `zaakspecifiek_geautoriseerd` role.
- [x] 4.4 Frontend spec for `ZaakspecifiekGeautoriseerdIconComponent`: lock with title when `true`, nothing when
      `false`.
- [x] 4.5 Make the existing frontend specs work again where the new field breaks their test data.

## 5. Wrap-up

- [ ] 5.1 Ask the user to run `./gradlew spotlessApply detektApply detekt`, the backend tests, and `npm run lint` and
      `npm run test` in `src/main/app`, and to report the results.
