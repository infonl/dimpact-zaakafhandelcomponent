## 1. Dependencies and client setup

- [ ] 1.1 Add the Elasticsearch Java client (`co.elastic.clients:elasticsearch-java`) to `gradle/libs.versions.toml` and wire it in `build.gradle.kts`
- [ ] 1.2 Add Elasticsearch connection configuration (URL, optional TLS/credentials/API key) and an `ElasticsearchClientFactory`/producer alongside the existing Solr config (do not remove Solr yet)
- [ ] 1.3 Introduce an engine-selection flag/config so Solr and Elasticsearch can coexist during transition

## 2. Index mapping and bootstrap

- [ ] 2.1 Define the Elasticsearch index mapping/template covering all `*ZoekObject` fields: Dutch-analysed text, keyword/exact variants, dates, integer/long/double, boolean, `geo_point` location, and a dynamic template for `zaak_betrokkene_*`
- [ ] 2.2 Map the Solr catch-all behaviour (`text`/`text_exact`/`text_rev`) to `copy_to` targets or `multi_match` source fields
- [ ] 2.3 Implement idempotent index bootstrap on startup (create index/template if absent, leave intact if present), replacing `SolrDeployerService` and the `SolrSchemaV1..V7` ladder
- [ ] 2.4 Implement startup wait/retry until Elasticsearch is reachable before completing bootstrap

## 3. Indexing service

- [ ] 3.1 Re-implement single-object index/update in `IndexingService` against Elasticsearch, keyed by UUID
- [ ] 3.2 Re-implement batch indexing using the bulk API
- [ ] 3.3 Re-implement object deletion from the index
- [ ] 3.4 Re-implement per-type reindex (ZAAK, TAAK, DOCUMENT) in batches
- [ ] 3.5 Map `/indexeren/commit-pending-changes-to-search-index` to an index `refresh`
- [ ] 3.6 Port/replace `SolrUtil.kt` query-escaping helpers with Elasticsearch-appropriate equivalents (or remove if handled by the typed client)

## 4. Search service

- [ ] 4.1 Re-implement `SearchService.zoek()` to build an Elasticsearch `SearchRequest`, preserving the `RestZoekParameters` → `RestZoekResultaat` contract
- [ ] 4.2 Implement full-text (`ALLE`) and field-scoped search via `ZoekVeld` mapping with AND default operator
- [ ] 4.3 Implement faceting over `FilterVeld` using `post_filter` + per-facet filtered aggregations (independent facet filtering), dropping zero-count buckets and supporting the `missing` bucket where required
- [ ] 4.4 Implement multi-field sorting over `SorteerVeld` with deterministic fallback (created, then id)
- [ ] 4.5 Implement date-range filtering over `DatumVeld`
- [ ] 4.6 Implement geo filtering on the zaak location (`geo_point`)
- [ ] 4.7 Keep the `ZoekVeld`/`FilterVeld`/`SorteerVeld`/`DatumVeld` → field-name mapping in one place

## 5. Health check

- [ ] 5.1 Replace `SolrReadinessHealthCheck` (`SolrPing`) with an Elasticsearch cluster-health / index-availability readiness check reporting UP/DOWN

## 6. Deployment and configuration

- [ ] 6.1 Replace the Solr container in `docker-compose.yaml` with an Elasticsearch service and volume
- [ ] 6.2 Replace the Solr Operator / external-Solr block in `charts/zac/values.yaml` with Elasticsearch deployment / external-endpoint config and update init containers
- [ ] 6.3 Remove `scripts/docker-compose/volume-data/solr-data/...` (`managed-schema.xml`, `solrconfig.xml`, core data)
- [ ] 6.4 Update CI configs that reference Solr to use Elasticsearch
- [ ] 6.5 Replace `solr.url` / `SOLR_URL` configuration usages with Elasticsearch connection config

## 7. Testing and verification

- [ ] 7.1 Add/adapt unit tests for `IndexingService` and `SearchService` against Elasticsearch (testcontainers)
- [ ] 7.2 Add integration tests asserting result sets and ordering for representative queries, including Dutch stemming/analysis
- [ ] 7.3 Add tests for independent facet filtering (selecting a facet value keeps that field's other buckets visible)
- [ ] 7.4 Add tests for date-range, geo filtering, sorting fallback, and the commit/refresh endpoint
- [ ] 7.5 Verify the `/zoeken` and `/indexeren` REST contracts are unchanged (request/response shape, frontend unaffected)

## 8. Cutover and cleanup

- [ ] 8.1 Run a full reindex of ZAAK, TAAK and DOCUMENT into Elasticsearch and verify counts/behaviour against Solr
- [ ] 8.2 Switch readiness/health and live traffic to Elasticsearch via the engine flag
- [ ] 8.3 Remove the Solr code: `nl/info/zac/solr/SolrUtil.kt`, `net/atos/zac/solr/SolrDeployerService.java`, `SolrSchemaUpdateHelper.java`, `FieldType.java`, `SolrSchemaV1..V7.java`, and `SolrReadinessHealthCheck.kt`
- [ ] 8.4 Remove the `org.apache.solr:solr-solrj` dependency and the engine-selection flag once cutover is confirmed
