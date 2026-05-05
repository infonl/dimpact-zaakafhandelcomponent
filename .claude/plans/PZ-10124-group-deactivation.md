# PZ-10124 — Group Deactivation

Als beheerder wil ik een groep kunnen deactiveren zodat deze groep door ZAC wordt gezien als inactieve groep en niet meer geselecteerd kan worden binnen ZAC (op specifieke uitzonderingen na).

## Key decisions

- Keycloak group attribute `active` with value `"false"` → inactive. Absent or any other value → active.
- Both Keycloak-direct and PABC `GroupRepresentation.toGroup()` read `active` and `email` directly from the `attributes` map.
- Backend list endpoints carry the correct `active` flag — no filtering yet (comes in Stage 4).
- The only place `active` needs to surface to the frontend for detail display is on the `RestGroup` embedded in `RestZaak.groep` and `RestTask.groep` (already full objects resolved via live Keycloak lookup).
- When a zaak/taak is already assigned to an (now) inactive group, the user may keep it or switch to an active one — but cannot pick a different inactive group.
- Stage 2 and Stage 4 **must ship together** — filtering without the frontend reassignment special case breaks the reassignment form.

---

## Stage 1 — Backend: Add `active` to Group model and REST response

> Additive only. No filtering yet. Safe to deploy with no Keycloak attribute set — all groups default to active.

### Checklist

- [x] `Group.kt` — add `val active: Boolean = true`
- [x] `RestGroup.kt` — add `val active: Boolean`
- [x] Keycloak `GroupRepresentation.toGroup()` extension — read `attributes["active"]?.firstOrNull() == "false"` evaluates to `false`, all other cases (absent, `"true"`, anything else) evaluate to `true`
- [x] PABC `GroupRepresentation.toGroup()` extension — reads `active` and `email` from the PABC `attributes` map (same logic as Keycloak path; corrected in Stage 2)
- [x] `Group.toRestGroup()` converter — pass `active` through
- [x] Unit test (`GroupTest.kt`): `toGroup()` with attribute absent → `active = true`
- [x] Unit test: `toGroup()` with `active = "true"` → `active = true`
- [x] Unit test: `toGroup()` with `active = "false"` → `active = false`
- [x] Update `IdentityFixtures.kt` (`identity/model`) to include `active` field in fixture group objects
- [x] Integration test (`IdentityRestServiceTest.kt`): `GET /rest/identity/groups` response includes `active` field on each group
- [x] `RestZaak.groep` and `RestTask.groep` now carry `active` in the API response (verify via OpenAPI spec or manual call)
- [x] Backend unit and integration tests pass (`./gradlew build`)

---

## Stage 2 — Backend: Set `active` flag correctly on all group list endpoints ✅ merged

> No filtering yet — filtering ships together with Stage 4. This stage ensures the `active` flag is correctly populated on every group list endpoint, including the PABC path.

### Checklist

- [x] `Group.kt` — add `GROUP_ATTRIBUTE_ACTIVE` and `GROUP_ATTRIBUTE_EMAIL` constants; both Keycloak and PABC `toGroup()` read `active` and `email` directly from the `attributes` map
- [x] PABC `GroupRepresentation.toGroup()` — reads `active` and `email` from attributes (same logic as Keycloak path; no extra Keycloak call needed)
- [x] `IdentityService.kt` — `listGroupsForBehandelaarRoleAndZaaktypeUuid` → renamed to `listActiveGroupsForBehandelaarRoleAndZaaktypeUuid`; `listGroupsForBehandelaarRoleAndZaaktype` → renamed to `listActiveGroupsForBehandelaarRoleAndZaaktype` (no filtering yet — "active" refers to intended final behaviour)
- [x] `IdentityService.kt` — `listInactiveGroupNames()` removed (no longer needed now that PABC reads `active` from attributes)
- [x] Unit test (`GroupTest.kt`): PABC `toGroup()` with no attributes → `active = true`, `email = null`
- [x] Unit test: PABC `toGroup()` with `active = "false"` and `email` → `active = false`, correct email
- [x] Unit tests (`IdentityServiceTest.kt`): PABC path — all groups returned with correct `active` flag
- [x] Integration test (`IdentityRestServiceTest.kt`): `GET /rest/identity/groups` response includes correct `active` field per group
- [x] Backend unit and integration tests pass

---

## Stage 3 — Frontend: visual warning on detail pages ✅ done

> After Stage 1 is merged and deployed. Only shows warning for explicitly inactive groups — no UI change for active groups.

### Checklist

- [x] `./gradlew generateOpenApiSpec` — regenerate backend OpenAPI spec
- [x] `cd src/main/app && npm run generate-api-types` — regenerate frontend TS types; `RestGroup` has `active?: boolean`
- [x] Zaak detail page (`zaak-view.component.html`) — `<em>(inactief)</em>` rendered via `@if (zaak.groep?.active === false)` inside `zac-static-text` ng-content
- [x] Taak detail page (`taak-view.component.html`) — same pattern for `taak?.groep?.active === false`
- [x] Spec test (`zaak-view.component.spec.ts`): `(inactief)` renders when `groep.active === false`
- [x] Spec test: absent when `groep.active === true`
- [x] Spec test: no crash when `groep` is `null`
- [x] Spec test (`taak-view.component.spec.ts`): same three cases — uses `ReplaySubject` pattern inside main describe (no extra `TestBed.configureTestingModule`)
- [x] `npm test` passes (25 tests in taak-view, 38+ in zaak-view)

---

## Stage 4 — Backend filter + Frontend: Reassignment special case (zaak + taak) ✅ done

> **Must ship together with the backend filter** — deploying the filter alone removes the currently-assigned inactive group from the dropdown, breaking the reassignment form. Both changes are complete.

### Backend filter checklist

- [x] `IdentityRestService.kt` — `.filter { it.active }` on `listGroups()` (`GET /rest/identity/groups`)
- [x] `IdentityRestService.kt` — `.filter { it.active }` on `listBehandelaarGroupsForZaaktype()` (`GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups`)
- [x] `IdentityRestService.kt` — `.filter { it.active }` on deprecated `listBehandelaarGroupsForZaaktypeUuid()` (`GET /rest/identity/groups/behandelaar/zaaktype/{zaaktypeUuid}`)
- [x] Integration test (`IdentityRestServiceTest.kt`): inactive group absent from `GET /rest/identity/groups` response (`shouldNotContain`)

### Frontend checklist

- [x] `zaak-details-wijzigen` — `map` pipe prepends current group when not found in API response (handles any reason for absence, not just `active` flag — same pattern as `taak-edit`)
- [x] `zaak-details-wijzigen` — `groupDisplayValue` function appends translated `(inactief)` suffix when `group.active === false`; uses `TranslateService.instant()`
- [x] `zaak-details-wijzigen` template — `[optionDisplayValue]="groupDisplayValue"` wired to `zac-select`
- [x] `taak-edit` — already had correct prepend logic (`groups.unshift(taskGroup)` when group missing from list) — no change needed
- [x] Spec test (`zaak-details-wijzigen.component.spec.ts`): current group prepended when missing from list
- [x] Spec test: no prepend when group already in list
- [x] Spec test: no prepend when `zaak.groep` is `undefined`
- [x] Spec test: `groupDisplayValue` appends `(inactief)` for inactive group
- [x] Spec test: `groupDisplayValue` returns just `naam` for active group
- [x] `npm test` passes (22 tests in zaak-details-wijzigen)
- [x] `medewerker-groep.component.ts` (legacy Atos form builder) — **skipped**: component is on its way out; `taak-edit` covers the active path

---

## Finalise

- [ ] Delete this MD file

---

## Acceptance criteria cross-check

| Criterion                                                                             | Stage                                          |
| ------------------------------------------------------------------------------------- | ---------------------------------------------- |
| Keycloak `active=false` attribute → group is inactive; absent or other value → active | 1                                              |
| Zaak/taak assignment dropdowns only show active groups                                | 4 (backend filter + frontend ship together)    |
| Exception: currently-assigned inactive group remains selectable (keep current)        | 4                                              |
| Cannot assign to a _different_ inactive group                                         | 4 (free — backend list has no inactive groups) |
| Visual indicator on zaakdetail + taakdetail for inactive assigned group               | 3                                              |
| Zaak aanmaken — only active groups selectable                                         | 4 (backend filtered)                           |
| Bulk distribution (verdelen) — only active groups shown                               | 4 (backend filtered)                           |
| BPMN human task forms — only active groups shown                                      | 4 (backend filtered)                           |
| Group authorisations (PABC) unaffected                                                | — (no change to auth flow)                     |
| Fetching users for a group unaffected                                                 | — (no change to user lookup)                   |
