## Context

`GET informatieobjecten/informatieobject/{uuid}` in `EnkelvoudigInformatieObjectRestService.kt` accepts an optional `@QueryParam("zaak") zaakUUID: UUID?`. It passes the resolved `Zaak?` into `RestInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject, zaak)`, which passes it into `PolicyService.readDocumentRechten(...)`. The OPA policy `document-rechten.rego` uses `zaak_open` and `zaaktype` — both `null`/`false` when no zaak is supplied — to gate `vergrendelen`, `ondertekenen`, and (for `behandelaar`/`coordinator`) `wijzigen`/`verwijderen`/etc. Only `recordmanager`/`beheerder` retain full rights without a zaak.

Today, three different frontend routes reach the same `InformatieObjectViewComponent`:
- `/informatie-objecten/:uuid`
- `/informatie-objecten/:uuid/:zaakUuid`
- `/informatie-objecten/:uuid/:versie/:zaakUuid`

Only routes carrying `:zaakUuid` resolve a zaak (via `ZaakUuidResolver`) and pass it to the second, rights-bearing call to `readEnkelvoudigInformatieobject`. Every call site that links to this page (`zaak-documenten`, search results, inbox lists) has to know and pass the correct zaak UUID, which is redundant (the document's zaak is derivable from the document itself) and lets a caller pass an arbitrary/incorrect zaak UUID that the backend accepts uncritically.

`ZrcClientService.listZaakinformatieobjecten(informatieobject: EnkelvoudigInformatieObject): List<ZaakInformatieObject>` already exists and returns all zaken linked to a document. `EnkelvoudigInformatieObjectRestService.listZaakInformatieobjecten` already uses it (for the "linked zaken" list shown on the document detail page), and `DocumentZoekObjectConverter` already uses `.firstOrNull()` on the same call for Solr indexing — so "pick the first, there's normally exactly one" is an established pattern in this codebase, just not yet applied to rights resolution.

## Goals / Non-Goals

**Goals:**
- Document rights (and thus action-button visibility) are identical no matter which page the user opened the document from.
- The backend, not the client, determines which zaak is authoritative for a document's rights.
- Exactly one URL shape exists for the document detail page: `/informatie-objecten/:uuid` (and `/informatie-objecten/:uuid/:versie` for a specific version).

**Non-Goals:**
- Changing how rights are computed once a zaak is known (the `PolicyService`/OPA rules themselves are unchanged).
- Changing behavior for documents that are genuinely not linked to any zaak (inbox, detached/"ontkoppelde" documents) — these continue to compute rights with `zaak = null`, which is correct since there is no zaak to resolve.
- Handling documents linked to multiple zaken as a first-class case — this is not possible via ZAC today and only theoretically possible via direct Open Zaak manipulation; we degrade gracefully (first zaak + warning) rather than design new UX for it.

## Decisions

**Resolve the zaak server-side inside `readEnkelvoudigInformatieobject`, reusing the existing `listZaakinformatieobjecten` lookup.**
Replace the `zaakUUID: UUID?` query param with an internal lookup:
```kotlin
@GET
@Path("informatieobject/{uuid}")
fun readEnkelvoudigInformatieobject(
    @PathParam("uuid") uuid: UUID
): RestEnkelvoudigInformatieobject =
    uuid
        .let(drcClientService::readEnkelvoudigInformatieobject)
        .let { enkelvoudigInformatieObject ->
            zrcClientService.listZaakinformatieobjecten(enkelvoudigInformatieObject)
                .also { zaakInformatieobjecten ->
                    if (zaakInformatieobjecten.size > 1) {
                        logger.warn {
                            "Enkelvoudig informatieobject '$uuid' is linked to " +
                                "${zaakInformatieobjecten.size} zaken; using the first one for rights resolution."
                        }
                    }
                }
                .firstOrNull()
                ?.let { zrcClientService.readZaak(it.zaakUUID) }
                .let { zaak -> restInformatieobjectConverter.convertToREST(enkelvoudigInformatieObject, zaak) }
        }
```
Alternative considered: have the frontend call the existing `informatieobject/{uuid}/zaakinformatieobjecten` endpoint first and then pass the resolved zaak UUID back as a query param. Rejected — this keeps the client in charge of supplying the zaak (still spoofable, still two round-trips) and doesn't fix the "wrong URL, wrong buttons" problem, since the client would still need to know to do this extra step.

**Drop the `:uuid/:zaakUuid` and `:uuid/:versie/:zaakUuid` routes entirely; keep only `:uuid` and `:uuid/:versie`.**
Since the backend now resolves the zaak itself, the frontend has no remaining use for a zaak UUID in the URL or in `InformatieObjectViewComponent`. `ZaakUuidResolver` becomes unused for this route and its `data.zaak` binding is removed; `this.zaak` in the component is instead populated from `RestEnkelvoudigInformatieobject`'s existing zaak-derived fields returned by the single backend call (or left unset when the document truly has no zaak), removing the need for the separate `loadZaakInformatieobjecten()` → `loadZaak()` two-step and the stale-menu bug where `toevoegenActies()` ran before `this.zaak` was set.
Alternative considered: keep the routes but stop reading `:zaakUuid` (redirect/normalize them to the `:uuid` form). Rejected — the proposal explicitly calls for a single canonical URL, and keeping dead route definitions around invites the same bug to resurface later.

**Update every call site that currently builds a two-UUID link to build a one-UUID link.**
This includes `zaak-documenten.component.html`/`.ts` (remove `getZaakUuidVanInformatieObject`), and confirms other call sites (search results, inbox, ontkoppelde documenten) already only pass one UUID and need no change — they will now also get correct rights, for free, once the backend resolves the zaak itself.

## Risks / Trade-offs

- **[Risk]** The backend now performs an extra `listZaakinformatieobjecten` + `readZaak` call on every document-read, even for documents that previously supplied the zaak UUID directly (saving a round trip). → **Mitigation**: this mirrors the cost already paid by `listZaakInformatieobjecten`/`DocumentZoekObjectConverter`; the endpoint is not on a hot path, and correctness (consistent rights) outweighs the marginal extra ZGW API call.
- **[Risk]** A document linked to multiple zaken (only possible via direct Open Zaak API use, not through ZAC) will silently pick "the first" zaak for rights purposes, which may not be the zaak the user is currently viewing. → **Mitigation**: log a warning (as already asked for), matching existing precedent (`DocumentZoekObjectConverter`); no ZAC-created data can trigger this today.
- **[Risk]** Removing the `:uuid/:zaakUuid` routes is a breaking change for any bookmarked/shared URLs of that shape. → **Mitigation**: Angular will fail to match the old route and fall through to the wildcard/not-found handling; acceptable since this is an internal case-management tool, not a public-facing URL scheme, and the proposal explicitly calls for URL unification.

## Migration Plan

1. Backend: remove the `zaak` query param and switch to server-side resolution (with unit test updates).
2. Frontend: update `InformatieObjectenService.readEnkelvoudigInformatieobject` to drop the `zaakUuid` argument, update `InformatieObjectViewComponent` to stop relying on a resolved `zaak` route-data value, remove the two zaak-carrying routes and `ZaakUuidResolver` usage for this feature, update `zaak-documenten` to link with a single UUID.
3. Manual verification by the user across affected entry points (zaak-documenten, search results, inbox).
4. Update/remove backend unit tests and any frontend specs affected by the signature/route changes.

No data migration or rollback concerns — this is a stateless behavior/routing change with no persisted data model impact. Rollback is a straightforward revert of the commit(s).

## Open Questions

None — the approach directly follows the existing `listZaakinformatieobjecten`/`firstOrNull()` + warning-log pattern already used elsewhere in the codebase, per the proposal's technical direction.