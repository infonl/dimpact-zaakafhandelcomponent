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

### New Solr schema version; automatic reindex deferred to a later phase

Add a new `SolrSchemaVx` (next after the current highest version) that adds the three fields and the
copyField, following the existing versioning mechanism in `SolrDeployerService`/`SolrSchemaUpdate`. It
lists no zoekobject types in `getTeHerindexerenZoekObjectTypes()` yet: no zaak in production is
zaakspecifiek geautoriseerd at this point in the epic's rollout, so there is nothing yet for the new
field to correct, and triggering an automatic reindex of every zaak/taak/document would take a long time
(potentially days) on environments with a lot of data, for no observable benefit today. A later story in
this epic lists `ZAAK`, `TAAK`, and `DOCUMENT` in a subsequent schema version once the flag starts being
set, so previously-indexed documents get reindexed at that point. Until then, the field can be backfilled
manually on any environment where it is needed sooner.

### Filter construction: exclude flagged rows per zaaktype the user lacks the flag for

`SearchService` gains a second filter query, built alongside `getAllowedZaaktypenFilterQuery()`, from the
zaaktypen in `LoggedInUser.applicationRolesPerZaaktype` whose role set does *not* contain
`zaakspecifiek_geautoriseerd`. For that subset, it excludes rows where `zaaktypeOmschrijving` matches one of
those zaaktypen *and* `zaakspecifiekGeautoriseerd:true` — e.g.
`-((zaaktypeOmschrijving:"A" AND zaakspecifiekGeautoriseerd:true) OR (zaaktypeOmschrijving:"B" AND ...))`.
Zaaktypen where the user *does* hold the flag are left out of this exclusion entirely, so their
zaakspecifiek geautoriseerde zaken pass through unaffected. When every allowed zaaktype has the flag (or
there are no allowed zaaktypen), the filter query is omitted. If the user holds `zaakspecifiek_geautoriseerd`
as an overall role (not scoped to a specific zaaktype), the filter query is omitted entirely — mirroring
`UserInput`, where `user.rollen` for a zaaktype is `applicationRolesPerZaaktype[zaaktype] + overallRoles`, so
OPA already grants the flag for every zaaktype in that case. The `zaakspecifiek_geautoriseerd` role-name
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

- [No automatic reindex means every zaak/taak/document indexed before this deploy keeps the new field
  unset/false until it is next reindexed by its normal event-driven trigger or a later schema version] →
  Accepted: no zaak in production is zaakspecifiek geautoriseerd yet, so "not flagged" is also the correct
  value for all pre-existing data — there is nothing to backfill. A later story in this epic adds the
  reindex once the flag starts being set for real zaken.
- [A future automatic reindex (once triggered) is asynchronous and takes time proportional to the number
  of zaken/taken/documenten] → Same trade-off every prior `SolrSchemaUpdate` already accepts (e.g.
  `SolrSchemaV7`); deferring it here also avoids imposing a multi-day reindex on large environments before
  it is actually needed.
- [Denormalizing the flag means a zaakeigenschap change made directly in Open Zaak/ZGW after indexing is
  stale in Solr until the zaak is reindexed] → `NotificationReceiver` did not handle `Resource.ZAAKEIGENSCHAP`
  notificaties at all before this story, so this window would otherwise have been unbounded rather than
  short. Fixed as part of this story: a `zaakeigenschap` notificatie now reindexes the zaak and, via the new
  `IndexingService.addOrUpdateTakenForZaak(zaakUUID)`, both its open and its completed taken (a completed
  taak's flag can go stale just as easily as an open one), plus the zaak's documenten. This is a dedicated
  call rather than `addOrUpdateZaak(zaakUUID, inclusiefTaken = true)`, whose existing `inclusiefTaken`
  semantics (open taken only) stay unchanged for its other caller (the `zaak` update notificatie), so that
  caller does not start paying for a `HistoricTaskInstanceQuery` per notificatie. The flag is refreshed on
  all three row types within the same short, event-driven window as any other indexed zaak attribute.

## Migration Plan

Deploy-time only, no manual steps required for the schema change itself: the new `SolrSchemaVx` is picked up
by `SolrDeployerService` on next startup and applies the schema changes. It does not trigger a reindex —
`getTeHerindexerenZoekObjectTypes()` is empty for this version — so this deploy causes no reindex load and no
deploy-time downtime on any environment, regardless of the number of zaken, taken, or documenten. As noted
above, an environment that needs the field backfilled sooner than the later story can trigger a manual
reindex of `ZAAK`, `TAAK`, and `DOCUMENT`. A later story in this epic adds the
reindex, once zaken actually start being flagged zaakspecifiek geautoriseerd, following the same
`SolrDeployerService`/`ManagedExecutorService` background-reindex mechanism used by prior schema versions
(e.g. `SolrSchemaV7`). No rollback concerns beyond the existing schema-version mechanism (a later deploy
would need its own schema version to revert, same as any other `SolrSchemaUpdate`).
