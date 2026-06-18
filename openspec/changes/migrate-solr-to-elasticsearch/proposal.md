## Why

The application currently relies on Apache Solr (SolrJ 9.10.1) for full-text search, faceting and indexing of zaken, taken and documenten. Migrating the search backend to Elasticsearch consolidates on a search engine with broader ecosystem support, managed-service availability and a richer query/aggregation API, while removing the bespoke Solr schema-versioning machinery. This change replaces the search infrastructure without changing the search behaviour exposed to end users.

## What Changes

- Replace the SolrJ client and `Http2SolrClient` usage with an Elasticsearch client for all indexing and query operations.
- Replace the Solr `zac` core and its `managed-schema.xml` / `solrconfig.xml` with three separate Elasticsearch indices — one per object type (`zaak`, `taak`, `document`) — each with its own index mapping/template covering the relevant fields (Dutch text analysis, dates, booleans, geo-point, dynamic `betrokkene` fields, copy/catch-all fields).
- Replace the versioned `SolrSchemaUpdate` migration machinery (`SolrSchemaV1`–`V7`, `SolrDeployerService`, `SolrSchemaUpdateHelper`, `FieldType`) with per-index Elasticsearch index-template / mapping bootstrap and reindex strategy.
- Re-implement faceted search (with per-facet exclusions), multi-field sorting, date-range filtering, geo filtering and full-text matching using Elasticsearch aggregations and query DSL — querying across the three indices (multi-index search) so a single `/zoeken` call returns mixed zaak/taak/document results — preserving the existing `/zoeken` REST contract and response shape (`RestZoekResultaat`: items + facets + pagination).
- Replace the Solr readiness health check (`SolrPing`) with an Elasticsearch cluster/index health check.
- Update deployment: replace the Solr container in `docker-compose.yaml`, the Solr Operator / external-Solr config in the Helm chart (`charts/zac/values.yaml`), and related provisioning with Elasticsearch equivalents.
- Replace `solr.url` / `SOLR_URL` configuration with Elasticsearch connection configuration.
- **BREAKING** (operational): existing Solr cores/data are not reused; a full reindex of zaken, taken and documenten is required on cutover.

## Capabilities

### New Capabilities
- `search-engine`: Indexing and full-text/faceted search of zaken, taken and documenten — covering per-type index bootstrap (one index each for zaak, taak, document), document indexing/reindexing, multi-index search queries with faceting, sorting, date-range and geo filtering, the `/zoeken` and `/indexeren` REST behaviour, and search backend readiness health checks. This captures the behaviour that must be preserved while the underlying engine moves from Solr to Elasticsearch.

### Modified Capabilities
<!-- None: no existing spec describes search behaviour; the search-engine capability is new. -->

## Impact

- **Dependencies**: remove `org.apache.solr:solr-solrj` from `gradle/libs.versions.toml` and `build.gradle.kts`; add an Elasticsearch client dependency.
- **Backend code**: `nl/info/zac/search/IndexingService.kt`, `nl/info/zac/search/SearchService.kt`, `nl/info/zac/solr/SolrUtil.kt`, `nl/info/zac/health/SolrReadinessHealthCheck.kt`, `net/atos/zac/solr/SolrDeployerService.java`, `net/atos/zac/solr/SolrSchemaUpdateHelper.java`, `net/atos/zac/solr/FieldType.java`, `net/atos/zac/solr/schema/SolrSchemaV1..V7.java`.
- **REST layer (unchanged contract)**: `nl/info/zac/app/search/SearchRestService.kt` (`/zoeken`), `IndexingRestService.kt` (`/indexeren`), `IndexingAdminRestService.kt` (`/internal/indexeren`).
- **Search model (reused)**: `ZoekVeld`, `FilterVeld`, `SorteerVeld`, `DatumVeld`, `ZoekObjectType` and the `*ZoekObject` documents.
- **Schema/config files**: remove `scripts/docker-compose/volume-data/solr-data/...` (`managed-schema.xml`, `solrconfig.xml`); add per-type Elasticsearch index templates/mappings (zaak, taak, document).
- **Deployment**: `docker-compose.yaml`, `charts/zac/values.yaml`, init containers / core-creation logic, CI.
- **Configuration**: `solr.url` / `SOLR_URL` replaced by Elasticsearch connection config.
- **Frontend**: no contract change expected — `/zoeken` request/response shape is preserved.
