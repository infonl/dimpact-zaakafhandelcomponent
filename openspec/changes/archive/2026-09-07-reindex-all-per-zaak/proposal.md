## Why

`IndexingService.reindexAll` reindexes `ZAAK`, `TAAK`, and `DOCUMENT` as three independent passes. Every
taak belongs to exactly one zaak (`TaakVariabelenService.readZaakUUID` requires a zaak UUID process
variable on every task), and every indexed document belongs to exactly one zaak (a document with no linked
zaak is explicitly skipped by `DocumentZoekObjectConverter.convert`, never indexed). Yet
`TaakZoekObjectConverter.convert` and `DocumentZoekObjectConverter.convert` each call
`ZrcClientService.readZaak` again for that same zaak, independently of the `ZAAK` pass that already
retrieved it and independently of every other taak/document of that same zaak. On an environment with many
taken and documenten per zaak, this multiplies the number of ZGW "read zaak" calls issued during a full
reindex far beyond the number of zaken that actually exist, making `reindexAll` slower than it needs to be.

## What Changes

- When `reindexAll` (or `reindexAllAsync`) is asked to reindex `ZAAK` together with `TAAK` and/or `DOCUMENT`,
  drive the combined pass from the zaak listing: for each page of zaken, index the zaak, then index its open
  taken and its linked documenten, reusing the `Zaak` already retrieved for that page instead of having the
  taak/document conversion read it again.
- Add a converter entry point to `TaakZoekObjectConverter` and `DocumentZoekObjectConverter` that accepts an
  already-retrieved `Zaak`, so the zaak-driven pass can skip the redundant `ZrcClientService.readZaak` call.
  The existing entry points (read the zaak by UUID themselves) are kept for standalone per-type reindexing.
- Documents with no linked zaak at all ("orphans") exist in the DRC today and are deliberately enumerated
  and skipped (counted as "skipped", not silently dropped) by the current global document pass. Since a
  zaak-driven pass never visits a document unless it is reachable from some zaak, add a supplementary
  orphan sweep that runs after the zaak-driven pass and enumerates the DRC's full informatieobject listing
  (as today), skipping only the documents already reindexed via a zaak, so orphan documents keep being
  found, indexed as skipped, and counted exactly as they are today.
- When only `TAAK` and/or `DOCUMENT` are requested without `ZAAK` (e.g. reindexing a single object type on
  its own via the internal REST endpoint), keep today's independent, type-scoped paging behavior unchanged.
- Preserve the existing per-object-type "Reindexing started"/"Reindexing finished" log lines, Solr document
  counts, and reindexed/skipped/error totals for `ZAAK`, `TAAK`, and `DOCUMENT` individually, even though they
  are now produced from a combined pass (plus the orphan sweep for `DOCUMENT`) rather than three fully
  independent ones.

## Capabilities

### New Capabilities
(none)

### Modified Capabilities
- `solr-reindexing-performance`: adds a requirement that a combined reindex of `ZAAK` with `TAAK` and/or
  `DOCUMENT` retrieves each zaak at most once and reuses it for that zaak's taken and documenten, instead of
  each taak/document conversion retrieving the zaak again; and a requirement that documents with no linked
  zaak are still found and accounted for by an orphan sweep after the zaak-driven pass.

## Impact

- `src/main/kotlin/nl/info/zac/search/IndexingService.kt`: `reindexAll`, `reindexAllZaken`,
  `reindexAllTaken`, `reindexAllInformatieobjecten`, and the private page-reindexing helpers they call.
- `src/main/kotlin/nl/info/zac/search/converter/TaakZoekObjectConverter.kt` and
  `DocumentZoekObjectConverter.kt`: new `convert` overload taking a pre-fetched `Zaak`.
- No REST API, Solr schema, or observable log-message changes; only how the underlying data is fetched
  during a combined reindex changes.
