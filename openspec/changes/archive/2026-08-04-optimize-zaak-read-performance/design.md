## Context

`RestZaakConverter.toRestZaak` is invoked by `ZaakRestService.readZaak` and `readZaakById` on
every zaak-detail page load. It currently resolves `groep`, `behandelaar`, and
`initiatorIdentificatie` via three independent `ZgwApiService` lookups
(`findGroepForZaak`, `findBehandelaarMedewerkerRoleForZaak`, `findInitiatorRoleForZaak`), each of
which calls the private `findBehandelaarRoleForZaak` pattern or its own inline logic, and each of
which issues its own filtered `zrcClientService.listRollen(RolListParameters(...))` HTTP call to
the Open Zaak ZRC API. Separately, it calls `zaakVariabelenService.readZaakdata(zaak.uuid)` and
`zaakVariabelenService.findOntvangstbevestigingVerstuurd(zaak.uuid)`, both of which resolve via
the same underlying `findVariables(zaakUuid)` Flowable CMMN/BPMN runtime+historic query.

`ZrcClientService` already exposes an unfiltered `listRollen(zaak: Zaak): List<Rol<*>>`
(`ZrcClientService.kt:165-166`), used elsewhere (e.g. `ZaakService.listBetrokkenenforZaak`), that
returns all roles for a zaak in a single call.

## Goals / Non-Goals

**Goals:**
- Reduce `readZaak` / `readZaakById` to one ZRC role-list call and one process-variables query,
  down from three and two respectively.
- Keep `RestZaak` output byte-for-byte identical to today for the same underlying zaak state.
- Keep the change localized to `ZgwApiService` and `RestZaakConverter`.

**Non-Goals:**
- Caching `listRollen`, `readStatus`, or any other ZGW client call.
- Addressing the `gerelateerdeZaken` / `besluiten` / `zaakSpecificContactDetails` N+1 call paths.
- Changing the `RestZaak` API contract or removing any (used or unused) fields.
- Changing behavior for any other caller of `findGroepForZaak`,
  `findBehandelaarMedewerkerRoleForZaak`, or `findInitiatorRoleForZaak` beyond the call-count
  reduction (their return types and semantics stay the same).

## Decisions

**Fetch all roles once, filter in-memory, in `ZgwApiService`.**
Add a private helper in `ZgwApiService` that fetches `zrcClientService.listRollen(zaak)` once and
reuse it across `findGroepForZaak`, `findBehandelaarMedewerkerRoleForZaak`, and
`findInitiatorRoleForZaak` when all three are needed for the same zaak within one request. Since
these three methods are public API on `ZgwApiService` used independently elsewhere too, the
simplest correct approach is:
- Introduce a new method, e.g. `ZgwApiService.findRolesForZaak(zaak: Zaak): List<Rol<*>>` (thin
  wrapper delegating to `zrcClientService.listRollen(zaak)`), or resolve directly in
  `RestZaakConverter` via `zrcClientService.listRollen(zaak)` and use existing roltype-matching
  logic (roltype omschrijving generiek `BEHANDELAAR`/`INITIATOR`, `betrokkeneType`
  `ORGANISATORISCHE_EENHEID`/`MEDEWERKER`) already present in `ZgwApiService`'s private
  `findBehandelaarRoleForZaak`.
- Preferred: keep the role-type-matching logic where it already lives (`ZgwApiService`), but let
  `RestZaakConverter.toRestZaak` fetch the full role list once and pass it into the three
  existing finder methods (overloaded to accept a pre-fetched `List<Rol<*>>` instead of always
  calling `zrcClientService.listRollen` themselves). This avoids duplicating roltype-matching
  logic in the converter and keeps `ZgwApiService`'s existing single-role finder methods usable
  standalone (backward compatible) by other callers that don't have a pre-fetched list.
- Alternative considered: cache `listRollen` results per-request in `ZrcClientService` (e.g. a
  request-scoped cache). Rejected — adds request-scope plumbing/complexity for a benefit that a
  simple "fetch once, pass down" refactor achieves with less risk.

**Derive confirmation-of-receipt flag from the already-fetched zaak data map.**
`readZaakdata(zaak.uuid)` returns a `Map<String, Any>` that already includes
`ZaakVariabelenService.VAR_ONTVANGSTBEVESTIGING_VERSTUURD` when set. Replace the separate
`findOntvangstbevestigingVerstuurd(zaak.uuid)` call in `RestZaakConverter.toRestZaak` with a
direct read from that map: `(zaakData[VAR_ONTVANGSTBEVESTIGING_VERSTUURD] as? Boolean) ?: false`.
`findOntvangstbevestigingVerstuurd` itself is left in place (unused call site removed, method
kept since other callers may use it — verify during implementation and remove only if it becomes
dead code).

## Risks / Trade-offs

- [Risk] Roltype-matching logic duplicated or diverges between the three finder methods and a
  new pre-fetched-list variant → Mitigation: reuse the exact same matching logic
  (`ztcClientService.findRoltypen` + omschrijving/betrokkeneType filter) by overloading the
  existing private helper rather than rewriting it in the converter.
- [Risk] `findOntvangstbevestigingVerstuurd` and `readZaakdata` could theoretically diverge if the
  process variable were ever stored with a different type/casing than expected in the map →
  Mitigation: both currently read the exact same underlying variable name and go through the same
  `findVariables` call, so behavior is identical; add a unit test asserting parity.
- [Trade-off] The pre-fetched-list overloads add a small amount of API surface to `ZgwApiService`;
  accepted since it keeps the fix minimal and localized rather than restructuring the service.

## Migration Plan

No data migration. This is a pure code-level refactor of internal call patterns behind an
unchanged API contract. Roll out as a normal deploy; rollback is a plain revert if any regression
in role/variable resolution is observed.

## Open Questions

- Should the pre-fetched-roles parameter be added directly to the existing
  `findGroepForZaak`/`findBehandelaarMedewerkerRoleForZaak`/`findInitiatorRoleForZaak` signatures
  (as an optional parameter) or exposed as new overloads? Leaning toward optional parameter with a
  default that preserves current single-call behavior for other callers, decided during
  implementation.
