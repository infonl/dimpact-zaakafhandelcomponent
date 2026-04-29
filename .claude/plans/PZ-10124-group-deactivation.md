# PZ-10124 — Group Deactivation

Als beheerder wil ik een groep kunnen deactiveren zodat deze groep door ZAC wordt gezien als inactieve groep en niet meer geselecteerd kan worden binnen ZAC (op specifieke uitzonderingen na).

## Key decisions

- Keycloak group attribute `active` with value `"false"` → inactive. Absent or any other value → active.
- Backend list endpoints filter out inactive groups → frontend dropdowns only ever contain active groups, no `active` field needed on list items.
- The only place `active` needs to surface to the frontend is on the `RestGroup` embedded in `RestZaak.groep` and `RestTask.groep` (already full objects resolved via live Keycloak lookup).
- When a zaak/taak is already assigned to an (now) inactive group, the user may keep it or switch to an active one — but cannot pick a different inactive group.

---

## Stage 1 — Backend: Add `active` to Group model and REST response

> Additive only. No filtering yet. Safe to deploy with no Keycloak attribute set — all groups default to active.

### Checklist

- [x] `Group.kt` — add `val active: Boolean = true`
- [x] `RestGroup.kt` — add `val active: Boolean`
- [x] Keycloak `GroupRepresentation.toGroup()` extension — read `attributes["active"]?.firstOrNull() == "false"` evaluates to `false`, all other cases (absent, `"true"`, anything else) evaluate to `true`
- [x] PABC `GroupRepresentation.toGroup()` extension — always `active = true` (active flag is Keycloak-only; PABC does not carry this attribute)
- [x] `Group.toRestGroup()` converter — pass `active` through
- [x] Unit test (`GroupTest.kt`): `toGroup()` with attribute absent → `active = true`
- [x] Unit test: `toGroup()` with `active = "true"` → `active = true`
- [x] Unit test: `toGroup()` with `active = "false"` → `active = false`
- [x] Update `IdentityFixtures.kt` (`identity/model`) to include `active` field in fixture group objects
- [x] Integration test (`IdentityRestServiceTest.kt`): `GET /rest/identity/groups` response includes `active` field on each group
- [x] `RestZaak.groep` and `RestTask.groep` now carry `active` in the API response (verify via OpenAPI spec or manual call)
- [x] Backend unit and integration tests pass (`./gradlew build`)

---

## Stage 2 — Backend: Filter inactive groups from list endpoints

> Filter at the REST layer. Safe to deploy before any Keycloak group is marked inactive — filter is a no-op until then.

### Checklist

- [ ] `IdentityRestService.kt` — add `.filter { it.active }` on `GET /rest/identity/groups` (used by bulk distribution dialogs)
- [ ] `IdentityRestService.kt` — add `.filter { it.active }` on `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups`
- [ ] `IdentityRestService.kt` — add `.filter { it.active }` on deprecated `GET /rest/identity/groups/behandelaar/zaaktype/{zaaktypeUuid}`
- [ ] Unit test (`IdentityRestServiceTest.kt` unit level, with MockK): endpoint returns only active groups when list contains a mix
- [ ] Unit test: endpoint returns all groups when all are active (no regression)
- [ ] Integration test (`IdentityRestServiceTest.kt` itest): `GET /rest/identity/groups` with a Keycloak group marked `active=false` — group is absent from the response
- [ ] Integration test: `GET /rest/identity/groups` with no `active` attribute set — group is present in the response
- [ ] Backend unit and integration tests pass (`./gradlew build`)

---

## Stage 3 — Frontend: Regenerate types + visual warning on detail pages

> After Stage 1 is merged and deployed. Only shows warning for explicitly inactive groups — no UI change for active groups.

### Checklist

- [ ] `./gradlew generateOpenApiSpec` — regenerate backend OpenAPI spec
- [ ] `cd src/main/app && npm run generate-api-types` — regenerate frontend TS types; confirm `RestGroup` now has `active: boolean`
- [ ] Zaak detail page — add visual indicator (icon + tooltip, per UX design) next to group name when `zaak.groep.active === false`
- [ ] Taak detail page — same indicator when `taak.groep.active === false`
- [ ] Spec test (`zaak-detail.component.spec.ts`): warning renders when `groep.active === false`
- [ ] Spec test: warning is absent when `groep.active === true`
- [ ] Spec test: no error/crash when `groep` is `null`
- [ ] Spec test (`taak-detail.component.spec.ts` or equivalent): same three cases
- [ ] `npm run lint` passes
- [ ] `npm test` passes

---

## Stage 4 — Frontend: Reassignment special case (zaak + taak)

> The currently-assigned group is inactive — user can keep it or switch to active, but cannot pick a different inactive group. Since backend list only contains active groups, the last constraint is automatically satisfied.

### Checklist

- [ ] `zaak-details-wijzigen` — if `zaak.groep?.active === false`, prepend the current group to the fetched active groups list so it appears as a selectable "keep current" option
- [ ] `zaak-details-wijzigen` — ensure the prepended inactive group is visually distinguishable (label/badge) from the active options in the dropdown
- [ ] `taak-edit` — same prepend logic when `taak.groep?.active === false`
- [ ] `taak-edit` — same visual distinction in the dropdown
- [ ] Spec test (`zaak-details-wijzigen.component.spec.ts`): when current group is inactive, it appears in the dropdown alongside active groups
- [ ] Spec test (`zaak-details-wijzigen.component.spec.ts`): when current group is active, only active groups appear in the dropdown (no duplicates)
- [ ] Spec test (`zaak-details-wijzigen.component.spec.ts`): inactive group in dropdown is visually distinguishable (e.g. label contains indicator)
- [ ] Spec test (`taak-edit.component.spec.ts`): same three cases
- [ ] Manual smoke test: mark a Keycloak group as `active=false`, open a zaak assigned to it — verify warning shown and dropdown contains the inactive group + all active groups only
- [ ] `npm run lint` passes
- [ ] `npm test` passes

---

## Finalise

- [ ] Delete this MD files

---

## Acceptance criteria cross-check

| Criterion                                                                             | Stage                                          |
| ------------------------------------------------------------------------------------- | ---------------------------------------------- |
| Keycloak `active=false` attribute → group is inactive; absent or other value → active | 1                                              |
| Zaak/taak assignment dropdowns only show active groups                                | 2                                              |
| Exception: currently-assigned inactive group remains selectable (keep current)        | 4                                              |
| Cannot assign to a _different_ inactive group                                         | 4 (free — backend list has no inactive groups) |
| Visual indicator on zaakdetail + taakdetail for inactive assigned group               | 3                                              |
| Zaak aanmaken — only active groups selectable                                         | 2 (backend filtered)                           |
| Bulk distribution (verdelen) — only active groups shown                               | 2 (backend filtered)                           |
| BPMN human task forms — only active groups shown                                      | 2 (backend filtered)                           |
| Group authorisations (PABC) unaffected                                                | — (no change to auth flow)                     |
| Fetching users for a group unaffected                                                 | — (no change to user lookup)                   |
