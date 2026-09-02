## Why

[PZ-11909](https://dimpact.atlassian.net/browse/PZ-11909) added the `zaakspecifiek_geautoriseerd`
application role and made single-zaak/taak/document read and handle rights respect it: an employee
without the flag for a zaaktype cannot open a zaakspecifiek geautoriseerde zaak of that zaaktype, even
via a direct URL. However, that story deliberately left werklijsten and zoekresultaten untouched
(`zaakspecifieke-autorisatie-toegang`'s "Werklijsten and zoekresultaten are not restricted by this
capability" requirement), because those are served from the Solr search index rather than a live ZGW
call. As a result, an employee without the flag currently cannot open a flagged zaak directly, but can
still find it, and its taken and documenten, in worklists and search results — the case simply doesn't
say so.

[PZ-11954](https://dimpact.atlassian.net/browse/PZ-11954) closes that gap: worklists and search results
must apply the same flag+role rule as single-resource access, so an employee without
`zaakspecifiek_geautoriseerd` for a zaaktype sees no trace of that zaaktype's flagged zaken (or their
taken/documenten) in werklijsten or zoekresultaten, while an employee who does hold the flag sees them
exactly as they would see any other zaak of that zaaktype. No visual indicator is added in this story
(a follow-up story covers that); this story is about visibility and rights only.

## What Changes

- Index the zaak's `ZAAK_GEAUTORISEERD` eigenschap into Solr as a new boolean field on the `ZaakZoekObject`,
  `TaakZoekObject`, and `DocumentZoekObject` documents (denormalized at index time from
  `ZrcClientService.isZaakspecifiekGeautoriseerd`, mirroring how `PolicyService` already reads it for
  single-resource rechten), via a new Solr schema version. This version does not trigger a reindex of
  existing data: no zaak in production is zaakspecifiek geautoriseerd yet, so there is nothing to backfill,
  and reindexing all zaken/taken/documenten upfront could take a long time on large environments. A later
  story in this epic adds the reindex once the flag starts being set for real zaken.
- Extend `SearchService.search()`'s existing allowed-zaaktypen filter query so that, for a zaaktype where
  the logged-in user holds an application role but not `zaakspecifiek_geautoriseerd`, zaakspecifiek
  geautoriseerde zaken (and their taken/documenten) of that zaaktype are excluded from the Solr result set
  entirely — not merely shown with reduced rights. Zaaktypen where the user does hold the flag are
  unaffected: their zaakspecifiek geautoriseerde zaken remain in the result set exactly as before.
- Fix `PolicyService.readZaakRechtenForZaakZoekObject`, `readDocumentRechten(DocumentZoekObject)`, and
  `readTaakRechten(TaakZoekObject)`, which currently hardcode or default `zaakspecifiekGeautoriseerd` to
  `false`, to instead read the new indexed field, so that rechten computed for a werklijst/zoekresultaat
  row are consistent with the OPA policy already enforced for the same resource's detail view.
- Extend integration tests to cover: a flagged zaak (and its taken/documenten) is absent from worklist and
  search results for a user without the flag for that zaaktype, and present with correct rechten for a
  user who holds it.

## Capabilities

### New Capabilities
- `zaakspecifiek-geautoriseerde-zoekindex`: indexes the zaakspecifiek-geautoriseerd flag into Solr and
  filters it out of werklijst/zoekresultaat queries for users who lack the flag for the zaak's zaaktype.

### Modified Capabilities
- `zaakspecifieke-autorisatie-toegang`: replaces the "Werklijsten and zoekresultaten are not restricted by
  this capability" requirement — rechten computed from a `ZaakZoekObject`, `TaakZoekObject`, or
  `DocumentZoekObject` must now reflect the same flag+role rule as the single-resource rechten in this
  capability, instead of always assuming `zaakspecifiekGeautoriseerd = false`.

## Impact

- `src/main/kotlin/nl/info/zac/search/model/zoekobject/{ZaakZoekObject,TaakZoekObject,DocumentZoekObject}.kt`
  — new indexed boolean field.
- `src/main/kotlin/nl/info/zac/search/converter/{ZaakZoekObjectConverter,TaakZoekObjectConverter,DocumentZoekObjectConverter}.kt`
  — populate the new field at index time.
- `src/main/kotlin/nl/info/zac/solr/schema/` — new `SolrSchemaVx` adding the field, without triggering a
  reindex (deferred to a later phase of this epic).
- `src/main/kotlin/nl/info/zac/search/SearchService.kt` — new filter query alongside
  `getAllowedZaaktypenFilterQuery()`.
- `src/main/kotlin/nl/info/zac/policy/PolicyService.kt` — the three zoekobject-based `readXxxRechten`
  overloads.
- `src/main/kotlin/nl/info/zac/notification/NotificationReceiver.kt` — handle the `zaakeigenschap`
  notificatie, previously not handled at all, so a `ZAAK_GEAUTORISEERD` change reindexes the zaak, its
  open taken, and its documenten instead of only becoming visible on their next unrelated reindex.
- `src/itest/kotlin/nl/info/zac/itest/` — extended integration tests (likely alongside the existing
  `TaskRestServiceZaakspecifiekAutorisatieTest.kt` / `EnkelvoudigInformatieObjectRestServiceZaakspecifiekAutorisatieTest.kt`
  pattern, plus a search/werklijst-focused test).
- No frontend or OPA rego changes: the rego `zaak_allowed`/`taak_allowed`/`document_allowed` guards already
  do the right thing once fed the real flag value.
- `net.atos.zac.csv.CsvService` is not changed. It reflects over every `ZoekObject` bean property not
  listed in its `uitzonderingen` exception list, so the new field automatically gains a
  `zaakspecifiekGeautoriseerd` "Ja"/"Nee" column in the zaken/taken/documenten CSV export, shifting the
  position of every column after it. This is intended: see design.md - Risks / Trade-offs.
