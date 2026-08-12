## Why

The `readZaak` (`GET /zaken/zaak/{uuid}`) and `readZaakById` (`GET /zaken/zaak/id/{identificatie}`)
endpoints in `ZaakRestService` build their response via `RestZaakConverter.toRestZaak`, which
currently performs two sets of redundant backend calls on every invocation:

1. `groep`, `behandelaar`, and `initiatorIdentificatie` are each resolved via a separate
   `zgwApiService.find*RoleForZaak(zaak)` call, and each of those independently issues its own
   filtered `zrcClientService.listRollen(RolListParameters(...))` HTTP call to the Open Zaak ZRC
   API — three uncached HTTP round trips fetching what is, at the API level, the same "roles for
   this zaak" collection, just filtered differently.
2. `zaakVariabelenService.readZaakdata(zaak.uuid)` and
   `zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.uuid)` are both called, and both
   internally resolve via the same `findVariables(zaakUuid)` Flowable CMMN/BPMN runtime+historic
   query — so that query runs twice per request for data that is already available from the first
   call.

Both endpoints are on the critical path for opening a zaak in the frontend, so removing this
redundant I/O reduces per-request latency with no behavior change.

## What Changes

- Resolve `groep`, `behandelaar`, and `initiatorIdentificatie` from a single, unfiltered
  `zrcClientService.listRollen(zaak: Zaak): List<Rol<*>>` call, filtering the result in-memory by
  roltype omschrijving generiek (`BEHANDELAAR` / `INITIATOR`) and `betrokkeneType`
  (`ORGANISATORISCHE_EENHEID` / `MEDEWERKER`) instead of issuing three separate server-side
  filtered list calls.
- Derive `heeftOntvangstbevestigingVerstuurd` from the `zaakData` map already fetched via
  `readZaakdata(zaak.uuid)` (key `ZaakVariabelenService.VAR_ONTVANGSTBEVESTIGING_VERSTUURD`)
  instead of calling `findOntvangstbevestigingVerstuurd(zaak.uuid)` separately.
- No change to the `RestZaak` response shape, field values, or any other endpoint behavior.

## Capabilities

### New Capabilities

- `zaak-detail-read-efficiency`: non-functional requirements constraining how many backend calls
  `readZaak` / `readZaakById` are allowed to make to resolve zaak roles and zaak variables, so the
  duplicate-call regression this change fixes cannot silently creep back in.

### Modified Capabilities

None. The data returned by `readZaak` and `readZaakById` is unchanged — this only constrains the
internal call pattern used to produce that (unchanged) response.

## Impact

- `nl.info.client.zgw.shared.ZgwApiService`: `findGroepForZaak`, `findBehandelaarMedewerkerRoleForZaak`,
  `findInitiatorRoleForZaak` (and their shared private helper `findBehandelaarRoleForZaak`).
- `nl.info.zac.app.zaak.converter.RestZaakConverter.toRestZaak`.
- No changes to `ZaakRestService`, the `RestZaak` API contract, or the Angular frontend.
- Explicitly out of scope: the N+1 `gerelateerdeZaken` / `besluiten` / `zaakSpecificContactDetails`
  call paths, and the unused `RestZaak` fields (`bronorganisatie`, `verantwoordelijkeOrganisatie`,
  `kenmerken`, `isDeelzaak`, `isHoofdzaak`, `isVerlengd`) — these are tracked as potential future
  follow-ups, not part of this change.
