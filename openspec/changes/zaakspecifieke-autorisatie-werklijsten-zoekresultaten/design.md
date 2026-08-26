## Context

See proposal.md - Why. Two existing mechanisms this design builds on:

- `ZrcClientService.isZaakspecifiekGeautoriseerd(zaakUUID)` (a live ZGW `listZaakeigenschappen` call) is
  the single source of truth for "is this zaak zaakspecifiek geautoriseerd", already used by
  `PolicyService.readZaakRechten`/`readDocumentRechten`/`readTaakRechten(TaskInfo)` for single-resource
  rechten.
- Werklijsten and zoekresultaten are both served by the *same* Apache Solr-backed pipeline
  (`SearchRestService` → `SearchService.search()`), not by a separate JPA/database query. `SearchService`
  already builds one allowed-zaaktypen filter query (`getAllowedZaaktypenFilterQuery()`) from
  `LoggedInUser.applicationRolesPerZaaktype.keys` and adds it to every Solr query. The three zoekobject
  types (`ZaakZoekObject`, `TaakZoekObject`, `DocumentZoekObject`) each carry their own prefixed
  `<type>_zaaktypeOmschrijving` field, `copyField`-merged in the Solr schema into one shared
  `zaaktypeOmschrijving` field so that one filter query works across all three types.

## Goals / Non-Goals

**Goals:**
- Exclude zaakspecifiek geautoriseerde zaken/taken/documenten from werklijst and zoekresultaat queries for
  users who lack `zaakspecifiek_geautoriseerd` for the zaak's zaaktype, without a per-row live ZGW call.
- Make the three zoekobject-based `PolicyService.readXxxRechten` overloads compute the same result as their
  single-resource counterparts.

**Non-Goals:**
- No visual indicator on a zaakspecifiek geautoriseerde row (explicitly deferred to a follow-up story per
  the Jira acceptance criteria).
- No change to the OPA rego policies themselves (`zaak-rechten.rego` etc. already do the right thing once
  fed a correct `zaakspecifiekGeautoriseerd` input; only the Kotlin callers feed it incorrectly today).
- No change to `werklijst-rechten.rego` (the coarse "can this user see any werklijst at all" check) —
  out of scope, unaffected by per-zaak flag filtering.

## Decisions

### Denormalize the flag into Solr at index time, not query time

Each `ZaakZoekObjectConverter`/`TaakZoekObjectConverter`/`DocumentZoekObjectConverter` already builds its
zoekobject from a `Zaak` it has fully loaded (all three call `zrcClientService.readZaak(...)` or already
hold the `Zaak`), so calling `zrcClientService.isZaakspecifiekGeautoriseerd(zaak.uuid)` there costs one
extra ZGW round-trip per index write — the same call `PolicyService` already makes per single-resource
rechten read, just moved to index time. Alternative considered: keep resolving the flag live at query/row
time in `PolicyService` (as the single-resource overloads do). Rejected: it cannot answer the visibility
question this story requires (excluding rows from a Solr result set needs the flag to be a queryable field,
not something resolved after the fact per row) and would mean N extra ZGW calls per search response
instead of one per index write.

### Field naming: per-type prefixed fields + copyField into a shared field, mirroring `zaaktypeOmschrijving`

Add `zaak_zaakspecifiekGeautoriseerd`, `taak_zaakspecifiekGeautoriseerd`, and
`informatieobject_zaakspecifiekGeautoriseerd` (boolean) to the three zoekobjecten, each `copyField`-merged
into one shared `zaakspecifiekGeautoriseerd` Solr field — the same pattern already used for
`zaaktypeOmschrijving`. This lets `SearchService` build one filter expression that works across `ZAAK`,
`TAAK`, and `DOCUMENT` result rows, consistent with how the existing allowed-zaaktypen filter already
works across all three.

### New Solr schema version, reindexing all three zoekobject types

Add a new `SolrSchemaVx` (next after the current highest version) that adds the three fields, the
copyField, and lists `ZAAK`, `TAAK`, and `DOCUMENT` in `getTeHerindexerenZoekObjectTypes()` — following the
existing versioning/reindex mechanism in `SolrDeployerService`/`SolrSchemaUpdate`. Every previously-indexed
document is reindexed once on deploy so the new field reflects each zaak's actual eigenschap value.

### Filter construction: exclude flagged rows per zaaktype the user lacks the flag for

`SearchService` gains a second filter query, built alongside `getAllowedZaaktypenFilterQuery()`, from the
zaaktypen in `LoggedInUser.applicationRolesPerZaaktype` whose role set does *not* contain
`zaakspecifiek_geautoriseerd`. For that subset, it excludes rows where `zaaktypeOmschrijving` matches one of
those zaaktypen *and* `zaakspecifiekGeautoriseerd:true` — e.g.
`-((zaaktypeOmschrijving:"A" AND zaakspecifiekGeautoriseerd:true) OR (zaaktypeOmschrijving:"B" AND ...))`.
Zaaktypen where the user *does* hold the flag are left out of this exclusion entirely, so their
zaakspecifiek geautoriseerde zaken pass through unaffected. When every allowed zaaktype has the flag (or
there are no allowed zaaktypen), the filter query is omitted. The `zaakspecifiek_geautoriseerd` role-name
string is currently only a literal inside `rollen.rego`; this design adds a matching Kotlin constant
(alongside the existing `ROLE_NAME_BRP_ZOEKEN`-style constants) so the two don't drift independently.

### Fix the three `PolicyService` zoekobject-based rechten methods to read the new field

`readZaakRechtenForZaakZoekObject`, `readDocumentRechten(DocumentZoekObject)`, and
`readTaakRechten(TaakZoekObject)` currently hardcode/default `zaakspecifiekGeautoriseerd = false`. Each
zoekobject gains a `isZaakspecifiekGeautoriseerd: Boolean` property (backed by the new Solr field), and
`PolicyService` reads it instead — no extra ZGW call needed, since the value is already on the row returned
by Solr. This makes row-level rechten consistent with the OPA policy already enforced for the equivalent
single-resource lookup, closing the gap the removed `zaakspecifieke-autorisatie-toegang` requirement used
to describe.

## Risks / Trade-offs

- [Reindex is asynchronous and takes time proportional to the number of zaken/taken/documenten] → Same
  trade-off every prior `SolrSchemaUpdate` already accepts (e.g. `SolrSchemaV7`); until reindexing
  completes, the new field is absent/default on not-yet-reindexed documents.
- [A document not yet reindexed defaults the new field to unset/false, which the filter treats as "not
  flagged" — briefly under-restrictive rather than over-restrictive during the reindex window] → Accepted:
  identical in kind to the existing single-resource path being eventually consistent with ZGW; the reindex
  is triggered automatically and immediately on deploy, and this is a short, one-time transitional window,
  not steady-state behavior. No mitigation beyond what `SolrDeployerService` already provides.
- [Denormalizing the flag means a zaakeigenschap change made directly in Open Zaak/ZGW after indexing is
  stale in Solr until the zaak is reindexed] → Existing, accepted behavior of the search index in general
  (every other zaak field already has this property); the zaak's own event-driven reindex triggers
  (`notificaties`) already keep it acceptably fresh, same as any other indexed zaak attribute.

## Migration Plan

Deploy-time only, no manual steps: the new `SolrSchemaVx` is picked up by `SolrDeployerService` on next
startup, applies the schema changes, and triggers a full reindex of `ZAAK`, `TAAK`, and `DOCUMENT`. No
rollback concerns beyond the existing schema-version mechanism (a later deploy would need its own schema
version to revert, same as any other `SolrSchemaUpdate`).

`SolrDeployerService.onStartup` submits the reindex to a `ManagedExecutorService` and returns without
waiting for it, and `IndexingService.reindex()` pages through zaken/taken/documenten on that background
thread — so this schema bump does not delay WildFly readiness and causes no deploy-time downtime, on an
environment with any number of zaken, taken, or documenten; existing (pre-flag) behaviour simply continues
for each row until its turn in the reindex is reached.
