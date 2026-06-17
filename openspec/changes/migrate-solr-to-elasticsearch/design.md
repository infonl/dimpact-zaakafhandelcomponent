## Context

Search today runs on Apache Solr (SolrJ 9.10.1). A single core `zac` holds three document types (ZAAK, TAAK, DOCUMENT) via the `*ZoekObject` classes. The backend uses `Http2SolrClient` from two singletons:

- `IndexingService.kt` — single/batch indexing, delete, reindex, commit.
- `SearchService.kt` — builds `SolrQuery` with faceting (tagged-filter exclusions `{!ex=filter}`), multi-field sort with fallback, date-range and geo filtering, Dutch text analysis via `text`/`text_exact`/`text_rev` copy fields.

Schema is code-managed: `SolrDeployerService.java` applies versioned `SolrSchemaUpdate` migrations (`SolrSchemaV1`–`V7`) through the Solr Schema API, tracked via `schema_version_*` markers, triggering per-type reindex when a version requires it. Readiness uses `SolrPing` (`SolrReadinessHealthCheck.kt`). Config: `solr.url` (MicroProfile) and `SOLR_URL` (env). Deployment: a Solr container in `docker-compose.yaml` and Solr Operator / external-Solr in `charts/zac/values.yaml`. The REST surface (`/zoeken`, `/indexeren`, `/internal/indexeren`) and search-model enums (`ZoekVeld`, `FilterVeld`, `SorteerVeld`, `DatumVeld`) are the stable contract that must survive the migration.

## Goals / Non-Goals

**Goals:**
- Replace the Solr engine with Elasticsearch behind the unchanged `/zoeken` and `/indexeren` REST contracts.
- Preserve search behaviour: full-text + field-scoped search, independent facet filtering, multi-field sort with fallback, date-range and geo filtering, Dutch text analysis.
- Replace code-managed Solr schema versioning with an Elasticsearch mapping/index-template bootstrap plus a reindex-on-cutover strategy.
- Replace Solr provisioning (docker-compose, Helm) and config with Elasticsearch equivalents.

**Non-Goals:**
- No change to the frontend search UI or the `RestZoekParameters` / `RestZoekResultaat` shapes.
- No reuse/migration of existing Solr index data — cutover does a full reindex from source systems.
- No change to the `*ZoekObject` domain shapes or the search-model enums beyond field-name mapping.
- No introduction of new search features (e.g. autocomplete, learning-to-rank).

## Decisions

### Client library
Use the official Elasticsearch Java client (`co.elastic.clients:elasticsearch-java`) over the transport client or low-level REST client directly. Rationale: it is the supported, typed client with first-class query/aggregation DSL builders, mirroring how `SolrQuery` is built today. Alternative considered: OpenSearch client — rejected to match the proposal's target (Elasticsearch); the abstraction boundary (`SearchService`/`IndexingService`) keeps a later swap cheap.

### Mapping instead of versioned schema migrations
Replace `SolrSchemaV1..V7` + `SolrDeployerService` with a single declarative index mapping applied via an index template / create-index bootstrap on startup, idempotent if the index already exists. Field mapping:
- `text_nl` → `text` analyzer with the Dutch analyzer (stemming, stopwords).
- exact/whitespace copy fields (`text_exact`, `text_rev`) → `keyword` and/or analyzer sub-fields; the Solr catch-all `text` becomes either `copy_to` targets or a `multi_match` across source fields.
- `pdate` → `date`; `pint`/`plong` → `integer`/`long`; `pdouble` → `double`; booleans → `boolean`; `location` → `geo_point`; dynamic `zaak_betrokkene_*` → keyword multi-fields via a dynamic template.
Schema evolution after cutover is handled by editing the mapping/template and triggering a reindex, not by an in-app version ladder. Rationale: Elasticsearch mappings are largely additive; the bespoke version ladder was Solr-specific complexity that the index-template model removes. Alternative considered: porting the version ladder 1:1 — rejected as unnecessary machinery.

### Faceting with independent filtering
Map Solr tagged-filter exclusions to Elasticsearch `post_filter` plus per-facet filtered aggregations (each aggregation re-applies all selected filters except its own). Rationale: this reproduces the exact "a facet's own selection doesn't collapse its other options" behaviour. Drop zero-count buckets (`min_doc_count` semantics) and add a `missing` bucket where current behaviour requires it.

### Query construction layer
Keep `SearchService.zoek()` as the single query-build entry point; replace `SolrQuery` assembly with Elasticsearch `SearchRequest` builders, keeping the `ZoekVeld`/`FilterVeld`/`SorteerVeld`/`DatumVeld` → field-name mapping in one place. Default operator AND preserved via `multi_match`/`query_string` `operator: and`. Multi-field sort keeps the deterministic fallback (created, then id).

### Commit/refresh semantics
The `/indexeren/commit-pending-changes-to-search-index` endpoint maps to an Elasticsearch index `refresh`. Routine indexing relies on the default refresh interval; the explicit endpoint forces a refresh, matching the current Solr hard-commit behaviour.

### Readiness health check
Replace `SolrPing` with an Elasticsearch cluster-health / index-exists check in the readiness probe; UP only when the cluster is reachable and the index is available.

### Configuration & deployment
Replace `solr.url`/`SOLR_URL` with Elasticsearch connection config (URL, and credentials if secured). Replace the Solr container in `docker-compose.yaml` with an Elasticsearch service, and the Solr Operator / external-Solr block in `charts/zac/values.yaml` with an Elasticsearch deployment / external-endpoint config (e.g. ECK operator or external managed cluster). Remove `scripts/docker-compose/volume-data/solr-data/...` schema/config files.

## Risks / Trade-offs

- **Behavioural drift in relevance/analysis** (Dutch stemming, tokenisation differ between Lucene-via-Solr and Elasticsearch analyzers) → Mitigation: pin the Dutch analyzer config, add search integration tests asserting result sets/ordering for representative queries before cutover.
- **Facet-exclusion semantics mismatch** → Mitigation: implement per-facet filtered aggregations and cover with tests that select one facet value and assert other buckets remain visible.
- **Cutover requires full reindex; index empty until reindex completes** → Mitigation: run reindex per type as a controlled step on cutover; document expected duration; keep Solr available until reindex verified (parallel-run option).
- **Hidden coupling to Solr response fields in callers** → Mitigation: the `Explore` map shows the REST layer mediates everything; verify no caller reads Solr-specific shapes directly; preserve `RestZoekResultaat`.
- **Loss of schema-version audit trail** → Mitigation: track index/mapping version via an index alias or metadata field plus changelog; reindex is the migration mechanism.

## Migration Plan

1. Add Elasticsearch client dependency; introduce an `ElasticsearchClientFactory`/config alongside (not replacing yet) Solr.
2. Implement index bootstrap (mapping + template) and readiness check against Elasticsearch.
3. Re-implement `IndexingService` (index/delete/batch/reindex/commit) against Elasticsearch.
4. Re-implement `SearchService.zoek()` query/aggregation building against Elasticsearch, preserving the REST contract.
5. Stand up Elasticsearch in docker-compose and Helm; add config; keep Solr config behind a flag during transition.
6. Reindex all types into Elasticsearch; run integration tests comparing behaviour to Solr.
7. Cut over readiness/health and remove Solr: dependency, `solr/*` services and schema classes, `solr-data` config files, Solr docker-compose/Helm blocks, `solr.url`/`SOLR_URL`.

**Rollback:** until step 7, Solr remains provisioned and selectable via config — revert the engine flag and Solr continues serving. After step 7, rollback requires restoring the Solr provisioning and reverting the engine-specific code.

## Open Questions

- Target Elasticsearch deployment in production: managed/external cluster vs ECK operator in-cluster?
- Is a parallel-run (dual-write to Solr and Elasticsearch) required for verification, or is a maintenance-window cutover acceptable?
- Security: is the Elasticsearch endpoint secured (TLS + auth/API key), and where are credentials sourced?
- Single index with a `type` field (mirroring the current single core) vs one index per object type — confirm choice.
