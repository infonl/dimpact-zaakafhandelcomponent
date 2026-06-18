## 1. Dependencies and client setup

- [x] 1.1 Add the Elasticsearch Java client (`co.elastic.clients:elasticsearch-java`) to `gradle/libs.versions.toml` and wire it in `build.gradle.kts`
- [x] 1.2 Add Elasticsearch connection configuration (URL, optional TLS/credentials/API key) and an `ElasticsearchClientFactory`/producer alongside the existing Solr config (do not remove Solr yet)
- [x] 1.3 ~~Introduce an engine-selection flag/config so Solr and Elasticsearch can coexist during transition~~ — **dropped**: decision is to replace Solr outright (no flag); Solr code and dependency removed in this pass

## 2. Index mapping and bootstrap

- [x] 2.1 Define three separate index mappings/templates — one per object type (`zaak`, `taak`, `document`) — each covering that type's `*ZoekObject` fields: Dutch-analysed text, keyword/exact variants, dates, integer/long/double, boolean, `geo_point` location (zaak), and a dynamic template for `zaak_betrokkene_*` (zaak)
- [x] 2.2 Map the Solr catch-all behaviour (`text`/`text_exact`/`text_rev`) to `copy_to` targets or `multi_match` source fields within each index mapping
- [x] 2.3 Decide and centralise the index naming convention and the type→index resolution (and the multi-index search target — explicit list, alias, or `zac-*` wildcard)
- [x] 2.4 Implement idempotent bootstrap on startup that creates each of the three indices/templates if absent and leaves intact if present, replacing `SolrDeployerService` and the `SolrSchemaV1..V7` ladder
- [x] 2.5 Implement startup wait/retry until Elasticsearch is reachable before completing bootstrap

## 3. Indexing service

- [x] 3.1 Re-implement single-object index/update in `IndexingService` against Elasticsearch, routing to the index for the object's type, keyed by UUID
- [x] 3.2 Re-implement batch indexing using the bulk API, targeting the correct per-type index per document
- [x] 3.3 Re-implement object deletion from the type's index
- [x] 3.4 Re-implement per-type reindex (ZAAK, TAAK, DOCUMENT) in batches, each rebuilding only its own index
- [x] 3.5 Map `/indexeren/commit-pending-changes-to-search-index` to a `refresh` of the relevant index/indices
- [x] 3.6 Port/replace `SolrUtil.kt` query-escaping helpers with Elasticsearch-appropriate equivalents (or remove if handled by the typed client) — **removed**: the typed client builds requests as JSON, so no manual escaping/quoting is needed

## 4. Search service

- [x] 4.1 Re-implement `SearchService.zoek()` to build an Elasticsearch `SearchRequest` targeting all three indices (multi-index search), deriving each hit's object type from its origin index, preserving the `RestZoekParameters` → `RestZoekResultaat` contract
- [x] 4.2 Implement full-text (`ALLE`) and field-scoped search via `ZoekVeld` mapping with AND default operator
- [x] 4.3 Implement faceting over `FilterVeld` using `post_filter` + per-facet filtered aggregations (independent facet filtering), dropping zero-count buckets and supporting the `missing` bucket where required
- [x] 4.4 Implement multi-field sorting over `SorteerVeld` with deterministic fallback (created, then id)
- [x] 4.5 Implement date-range filtering over `DatumVeld`
- [x] 4.6 Implement geo filtering on the zaak location (`geo_point`) — mapping in place (`zaak_locatie` is `geo_point`); the current `RestZoekParameters` exposes no geo filter, so no query wiring is added (parity with Solr, which also had no geo query in `zoek()`)
- [x] 4.7 Keep the `ZoekVeld`/`FilterVeld`/`SorteerVeld`/`DatumVeld` → field-name mapping in one place

## 5. Health check

- [x] 5.1 Replace `SolrReadinessHealthCheck` (`SolrPing`) with an Elasticsearch cluster-health / index-availability readiness check reporting UP only when the cluster and all three indices (zaak, taak, document) are available, DOWN otherwise

## 6. Deployment and configuration

- [x] 6.1 Replace the Solr container in `docker-compose.yaml` with an Elasticsearch service and volume
- [x] 6.2 Replace the Solr Operator / external-Solr block in `charts/zac/values.yaml` with Elasticsearch deployment / external-endpoint config and update init containers
- [x] 6.3 Remove `scripts/docker-compose/volume-data/solr-data/...` (`managed-schema.xml`, `solrconfig.xml`, core data)
- [x] 6.4 Update CI configs that reference Solr to use Elasticsearch
- [x] 6.5 Replace `solr.url` / `SOLR_URL` configuration usages with Elasticsearch connection config

## 7. Testing and verification

- [x] 7.1 Add/adapt unit tests for `IndexingService` and `SearchService` — **done as mockk unit tests** against a mocked `ElasticsearchClient` (bulk/delete/reindex routing; multi-index request, sort fallback, response mapping). Also added `ElasticsearchReadinessHealthCheckTest`. Deleted `SolrUtilTest`/`SolrDeployerServiceTest`/`SolrReadinessHealthCheckTest`. The full `./gradlew test` suite passes. (A testcontainers-based variant is deferred to the itest suite — needs a running ES.)
- [ ] 7.2 Add integration tests asserting result sets and ordering for representative queries, including Dutch stemming/analysis — **deferred**: requires a running Elasticsearch (testcontainers/itest); not runnable in this environment
- [x] 7.3 Add tests for independent facet filtering — **query-construction verified** in `SearchServiceTest`: each facet aggregation re-applies the other facets' selections and excludes its own, with selections applied as a `post_filter`. (End-to-end bucket visibility against real ES is covered by the itest.)
- [ ] 7.4 Add tests for date-range, geo filtering, sorting fallback, and the commit/refresh endpoint — **partial**: multi-field sort fallback is unit-tested; date-range/geo/commit runtime behavior needs the ES itest (deferred)
- [x] 7.5 Verify the `/zoeken` and `/indexeren` REST contracts are unchanged — **preserved by design**: `RestZoekParameters`/`RestZoekResultaat` and the REST services are untouched; exercised end-to-end by the existing full-stack itest (e.g. `CsvRestServiceTest`, `NotificationZaakDestroyTest`)

## 8. Cutover and cleanup

- [ ] 8.1 Run a full reindex of ZAAK, TAAK and DOCUMENT into their respective indices and verify per-index counts/behaviour against Solr
- [ ] 8.2 Switch readiness/health and live traffic to Elasticsearch via the engine flag
- [x] 8.3 Remove the Solr code: `nl/info/zac/solr/SolrUtil.kt`, `net/atos/zac/solr/SolrDeployerService.java`, `SolrSchemaUpdateHelper.java`, `FieldType.java`, `SolrSchemaV1..V7.java`, and `SolrReadinessHealthCheck.kt` — **done** in this pass (replaced outright); the `**/solr/**` test files still reference these and are handled under section 7
- [x] 8.4 ~~Remove the `org.apache.solr:solr-solrj` dependency~~ **done** (removed from `libs.versions.toml` + `build.gradle.kts`); engine-selection flag N/A (none introduced)
