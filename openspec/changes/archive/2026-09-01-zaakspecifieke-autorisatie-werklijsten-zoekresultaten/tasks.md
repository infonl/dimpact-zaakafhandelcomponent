## 1. Solr schema and indexed fields

- [x] 1.1 Add a Kotlin constant for the `zaakspecifiek_geautoriseerd` role name (mirroring the existing
      `ROLE_NAME_BRP_ZOEKEN`-style constants) and verify `rollen.rego`'s `"zaakspecifiek_geautoriseerd"`
      literal and the new Kotlin constant have the same value (unit test or code comment cross-reference).
- [x] 1.2 Add `isZaakspecifiekGeautoriseerd: Boolean` (`@Field`-annotated) to `ZaakZoekObject`,
      `TaakZoekObject`, and `DocumentZoekObject`, and populate it in `ZaakZoekObjectConverter`,
      `TaakZoekObjectConverter`, and `DocumentZoekObjectConverter` via
      `zrcClientService.isZaakspecifiekGeautoriseerd(zaak.uuid)`; verify with converter unit tests asserting
      the field is `true`/`false` for a flagged/unflagged zaak.
- [x] 1.3 Add a new `SolrSchemaVx` (next version after the current highest) that adds the three prefixed
      boolean fields and a `copyField` from each into a shared `zaakspecifiekGeautoriseerd` field (mirroring
      the existing `zaaktypeOmschrijving` copyField pattern). Leave `getTeHerindexerenZoekObjectTypes()`
      empty for this version: an automated reindex of existing zaken/taken/documenten is **deliberately not
      done in this PR**, because it can cause load/duration problems on real environments; the reindex is
      to be triggered **manually**, per environment, at a later time — see design.md for the accepted gap
      this leaves (existing flagged zaken stay visible until reindexed) and the correction to the earlier,
      incorrect "no zaak in production is zaakspecifiek geautoriseerd yet" justification. Verify with a
      `SolrSchemaVx` unit test matching the existing `SolrSchemaV*` test pattern.
- [x] 1.4 Handle the `zaakeigenschap` notificatie in `NotificationReceiver` (previously unhandled, falling
      through to `else -> {}`): on a `zaakeigenschap` change, reindex the zaak (via
      `addOrUpdateZaak(zaakUUID, inclusiefTaken = false)`), both its open and completed taken (new
      `IndexingService.addOrUpdateTakenForZaak`, kept separate from `addOrUpdateZaak`'s `inclusiefTaken`
      flag so the unrelated `zaak` update notificatie does not start paying for a
      `HistoricTaskInstanceQuery` per notificatie), and the zaak's documenten (new
      `IndexingService.addOrUpdateInformatieobjectenForZaak`), so a `ZAAK_GEAUTORISEERD` change is reflected
      on all three row types, not only whichever row a later, unrelated reindex happens to touch; verify with
      `NotificationReceiverTest`/`IndexingServiceTest` cases and an itest relying on the notificatie alone
      (no manual `/internal/indexeren/herindexeren/{type}` call).

## 2. Search-time filtering

- [x] 2.1 Add a filter-query builder in `SearchService` (alongside `getAllowedZaaktypenFilterQuery()`) that
      excludes zaakspecifiek geautoriseerde rows for zaaktypen in `applicationRolesPerZaaktype` whose role
      set lacks `zaakspecifiek_geautoriseerd`, and wire it into `search()`; verify with a `SearchServiceTest`
      asserting the built `SolrQuery` contains the expected exclusion filter query for a mixed
      flagged/unflagged zaaktype role set, and no such filter when every allowed zaaktype has the flag.
- [x] 2.2 Verify via an integration test (extending the existing Solr-backed itest setup) that a
      zaakspecifiek geautoriseerde zaak, its taak, and its document are absent from `search()` results for a
      user without the flag for that zaaktype, and present for a user who holds it.

## 3. Rechten computed for werklijst/zoekresultaat rows

- [x] 3.1 Update `PolicyService.readZaakRechtenForZaakZoekObject`, `readDocumentRechten(DocumentZoekObject)`,
      and `readTaakRechten(TaakZoekObject)` to set `zaakspecifiekGeautoriseerd` from the zoekobject's new
      `isZaakspecifiekGeautoriseerd` field instead of hardcoding/defaulting to `false`; verify with
      `PolicyServiceTest` cases asserting the computed rechten for a flagged `ZaakZoekObject`/
      `TaakZoekObject`/`DocumentZoekObject` match the rechten for the equivalent single-resource lookup,
      for both a user with and without `zaakspecifiek_geautoriseerd`.

## 4. Integration test coverage

- [x] 4.1 Extend `TaskRestServiceZaakspecifiekAutorisatieTest.kt` and
      `EnkelvoudigInformatieObjectRestServiceZaakspecifiekAutorisatieTest.kt` (or add a sibling test) to
      cover the werklijst/zoekresultaat visibility and rechten behaviour end to end via the REST search
      endpoint, and verify `./gradlew itest --info` passes.

## 5. Documentation

- [x] 5.1 Update `docs/solution-architecture/accessControlPolicies.md` if its description of
      `zaakspecifiek_geautoriseerd` still states werklijsten/zoekresultaten are unaffected, and verify the
      wording matches the new behaviour.

## 6. OpenSpec

- [x] 6.1 Run `openspec validate zaakspecifieke-autorisatie-werklijsten-zoekresultaten --strict` and confirm
      it passes before archiving.
