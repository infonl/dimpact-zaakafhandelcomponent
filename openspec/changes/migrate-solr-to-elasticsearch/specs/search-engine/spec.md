## ADDED Requirements

### Requirement: Search index bootstrap

The system SHALL provision an Elasticsearch index for zaken, taken and documenten on startup, applying a mapping that covers all indexed fields (Dutch-analysed text, exact/keyword variants, dates, integers/longs, doubles, booleans, geo-point location, multi-valued `betrokkene` fields, and catch-all/copy fields). The system SHALL wait for the search backend to become available before completing bootstrap, and SHALL be idempotent across restarts.

#### Scenario: Index created on first startup

- **WHEN** the application starts and the configured Elasticsearch index does not yet exist
- **THEN** the system creates the index with the full field mapping and reports readiness only after the index is available

#### Scenario: Index already exists

- **WHEN** the application starts and the index already exists with the expected mapping
- **THEN** the system completes bootstrap without recreating or clearing the index

#### Scenario: Search backend not yet available

- **WHEN** the search backend is unreachable at startup
- **THEN** the system retries until the backend becomes available rather than failing permanently

### Requirement: Document indexing

The system SHALL index zaak, taak and document objects into Elasticsearch, supporting indexing of a single object, a batch of objects, and removal of an object from the index. The system SHALL support an explicit commit/refresh so that just-indexed changes become searchable on demand.

#### Scenario: Index a single object

- **WHEN** a zaak, taak or document is created or updated
- **THEN** the corresponding document is added or replaced in the index keyed by its UUID

#### Scenario: Remove an object from the index

- **WHEN** a zaak, taak or document is deleted
- **THEN** its document is removed from the index and no longer appears in search results

#### Scenario: Commit pending changes

- **WHEN** a client calls the commit-pending-changes endpoint
- **THEN** the system refreshes the index so all pending indexed changes are immediately searchable

### Requirement: Reindexing by object type

The system SHALL support a full reindex per object type (ZAAK, TAAK, DOCUMENT) so that all objects of a type can be rebuilt in the index, for use after a mapping change or on cutover.

#### Scenario: Reindex a type

- **WHEN** an operator triggers reindexing for a given object type
- **THEN** the system reindexes all objects of that type in batches and the index reflects the current source data

### Requirement: Full-text and field-scoped search

The system SHALL execute search queries over the `/zoeken` REST contract, supporting full-text search across a catch-all field and field-scoped search via the existing `ZoekVeld` fields, using AND as the default operator between terms. The request and response shape (`RestZoekParameters` in, `RestZoekResultaat` of items + facets + pagination out) SHALL be preserved.

#### Scenario: Full-text search across all fields

- **WHEN** a client searches with the `ALLE` field and a query string
- **THEN** the system returns matching zaken, taken and documenten ranked by relevance, with pagination metadata

#### Scenario: Field-scoped search

- **WHEN** a client searches on a specific field such as `ZAAK_IDENTIFICATIE`
- **THEN** only documents matching the term in that field are returned

### Requirement: Faceted search with independent facet filtering

The system SHALL return facet counts for the configured `FilterVeld` fields and SHALL apply selected facet filters such that each facet's available values are computed independently of that facet's own active filter (equivalent to Solr's tagged-filter exclusion behaviour). Facet values with a zero count SHALL be omitted, and a "missing value" facet bucket SHALL be supported where the existing behaviour requires it.

#### Scenario: Facet counts returned with results

- **WHEN** a client performs a search
- **THEN** the response includes, for each configured filter field, the available values with their result counts

#### Scenario: Selecting a facet value filters results

- **WHEN** a client selects one or more values for a filter field
- **THEN** results are restricted to documents matching the selected values, while that field's other selectable values and counts remain visible

### Requirement: Sorting, date-range and geo filtering

The system SHALL support multi-field sorting over the `SorteerVeld` fields with a deterministic fallback ordering, date-range filtering over the `DatumVeld` fields, and geographic filtering on the zaak location.

#### Scenario: Sort by a chosen field

- **WHEN** a client requests sorting on a `SorteerVeld` with a direction
- **THEN** results are ordered by that field, with a stable fallback ordering for ties

#### Scenario: Date-range filter

- **WHEN** a client supplies a from/to range on a `DatumVeld`
- **THEN** only documents whose date falls within the range are returned

### Requirement: Search backend readiness health check

The system SHALL expose a readiness health check that reports the search backend as UP only when the Elasticsearch cluster/index is reachable and healthy, and DOWN otherwise.

#### Scenario: Backend healthy

- **WHEN** the readiness probe runs and Elasticsearch is reachable and the index is available
- **THEN** the health check reports UP

#### Scenario: Backend unavailable

- **WHEN** the readiness probe runs and Elasticsearch is unreachable
- **THEN** the health check reports DOWN
