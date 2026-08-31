## Why

Reindexing (`internal/indexeren/herindexeren/{type}`, `IndexingAdminRestService.kt`) is slow on
environments with lots of data. Investigation found redundant per-zaak calls to Open Zaak's
"list rollen" operation and fully sequential, single-threaded conversion of each page. These are
independent, low-risk fixes that cut the number of Open Zaak round trips and shorten wall-clock
reindex time without changing the indexed data.

(A related correctness bug — document reindexing re-fetching page 1 forever instead of the given
page — was already fixed separately in commit 7fa1c8826 and is out of scope here.)

## What Changes

- In `ZaakZoekObjectConverter.convert`, fetch a zaak's roles ("rollen") from Open Zaak at most
  once per zaak and derive the initiator, group ("groep"), behandelaar, and other betrokkenen
  fields from that single result set, instead of each field issuing its own `listRollen` call.
- Convert the objects within a reindex page concurrently (bounded parallelism) instead of
  sequentially, so the per-page wall-clock time is bounded by the slowest single conversion
  rather than the sum of all of them.
- Raise `TAKEN_MAX_RESULTS` (task reindex page size) to reduce the number of pages fetched for
  large task backlogs.

## Capabilities

### New Capabilities
- `solr-reindexing-performance`: reindexing behavior for correct, complete pagination and for
  bounding the number of external ZGW API calls issued per indexed object.

### Modified Capabilities
(none — no existing spec covers reindexing)

## Impact

- `src/main/kotlin/nl/info/zac/search/IndexingService.kt`: add bounded-parallel conversion within
  `indexeerDirect`, raise `TAKEN_MAX_RESULTS`.
- `src/main/kotlin/nl/info/zac/search/converter/ZaakZoekObjectConverter.kt`: fetch and reuse a
  single `listRollen` result per zaak.
- No API contract, request/response shape, or indexed Solr document content changes — behavior
  observable to ZAC's frontend and to Solr search results is unchanged; only the number of Open
  Zaak calls and reindex duration change.
- No database migration or configuration changes required.
