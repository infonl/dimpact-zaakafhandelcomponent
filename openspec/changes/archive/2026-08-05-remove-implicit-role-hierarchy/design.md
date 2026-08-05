## Context

ZAC's OPA policies (`src/main/resources/policies/*.rego`) grant most permissions to exactly one role, e.g. `zaak-rechten.rego`'s `lezen` only checks `raadpleger.rol in user.rollen`. This works today only because the PABC test/prod mapping data gives every "higher" role user the `rol` markers of every "lower" role too (`coordinator_domein_test_1` maps to `raadpleger`+`behandelaar`+`coordinator`, etc. — see `scripts/docker-compose/imports/pabc-database/json-mapping/pabc-mapping-data.json`). The hierarchy order, per `docs/solution-architecture/accessControlPolicies.md`, is:

```
raadpleger  <  behandelaar  <  coordinator  <  recordmanager  <  beheerder
```

(each role needs every role to its left). `brp_zoeken` also has a documented dependency on `raadpleger`, but it is a separate, one-off relationship — `brp-rechten.rego` never references `raadpleger` at all, only `brpZoeken.rol in user.overallRoles` — so it is not part of the 5-role chain and is unaffected by this change (see Non-Goals).

The assign-group endpoints (`IdentityService.listActiveGroupsForBehandelaarRoleAndZaaktype(s)`, exposed via `IdentityRestService`) query PABC for groups authorised for the `behandelaar` application role. Because of the mapping-data hierarchy, a functional role like `coordinator_domein_test_1` also carries the `behandelaar` application role today, so its groups incorrectly show up as "behandelaar-authorised" in the assign picker.

## Goals / Non-Goals

**Goals:**
- Every permission currently granted to a role only via inheritance is granted to that role explicitly in Rego, with identical effective access before and after.
- The PABC test mapping data (`pabc-mapping-data.json`) maps each functional test role to exactly one application role, so `behandelaar-groups` endpoints only return groups whose functional role is actually `behandelaar`.
- `accessControlPolicies.md` reflects reality with no inheritance narrative.
- Integration tests assert the flattened (non-inherited) behavior.

**Non-Goals:**
- Changing what any role is currently allowed to do (no permission additions/removals in net effect).
- Touching the `brp_zoeken` ↔ `raadpleger` relationship — it isn't enforced in Rego and isn't part of the 5-role chain.
- Changing the PABC configuration on the INFO P:CT environment or any municipality's PABC setup, and writing the release-notes entry for municipalities — tracked separately in the "Applicatie Releases" story per PZ-11245.
- Adding a "test as behandelaar" impersonation feature for beheerders — explicitly out of scope per PZ-11245.

## Decisions

### 1. Mechanical rule for flattening Rego rules

For every permission rule in the 7 policy files (`zaak-rechten.rego`, `taak-rechten.rego`, `document-rechten.rego`, `werklijst-rechten.rego`, `notitie-rechten.rego`, `overige-rechten.rego`; `brp-rechten.rego` is unaffected), if a clause currently grants the permission to role `X` under condition `C`, add the same clause (same `C`) for every role that sits to the right of `X` in the chain above, since those roles used to satisfy `X.rol in user.rollen` by inheritance:

| Role granted today | Roles that inherited it (need an explicit clause added) |
|---|---|
| `raadpleger` | `behandelaar`, `coordinator`, `recordmanager`, `beheerder` |
| `behandelaar` | `coordinator`, `recordmanager`, `beheerder` |
| `coordinator` | `recordmanager`, `beheerder` |
| `recordmanager` | `beheerder` |
| `beheerder` | (none, top of chain) |

Worked example (`zaak-rechten.rego`), matching the AC's own examples:
- `lezen` (today: `raadpleger` only) → add explicit clauses for `behandelaar`, `coordinator`, `recordmanager`, `beheerder`.
- `wijzigen` clause 1 (today: `behandelaar`, condition `zaak.open`) → add `coordinator` under the same condition (it inherited `behandelaar`); `recordmanager`/`beheerder` already/newly covered by clause 2.
- `wijzigen` clause 2 (today: `recordmanager`, no condition) → add `beheerder` (it inherited `recordmanager`).
- `heropenen` (today: `recordmanager` only) → add `beheerder`.
- `bekijken_zaakdata` (today: `beheerder` only) → no change, `beheerder` is the top of the chain.

`taak-rechten.rego` has no `coordinator`/`recordmanager`/`beheerder` references at all today, so all four of its permissions gain 3 new explicit clauses each (mirroring whichever role — `raadpleger` or `behandelaar` — they're keyed on).

**Alternative considered**: keep a helper like `inherits(role)` in `rollen.rego` and call it instead of writing out every role explicitly. Rejected because the whole point of PZ-11245 is to make every grant explicit and auditable in the policy + doc table — a hierarchy helper just re-introduces the implicit hierarchy one layer down.

### 2. Flatten PABC mapping data to one application role per functional role

In `pabc-mapping-data.json`, remove every mapping entry that isn't the functional role's own single application role (e.g. `coordinator_domein_test_1` keeps only its `coordinator` mapping, drops `raadpleger` and `behandelaar`). This is what actually removes the hierarchy for the local dev/test environment and is what makes the flattened Rego grants from Decision 1 observable in `itest`.

### 3. Update `accessControlPolicies.md` table to be fully explicit

Remove the "lower-level roles" narrative (the bullet list explaining inheritance). Add a ✅ to every cell that a role now has an explicit Rego grant for, per Decision 1 — e.g. the `lezen` row gets ✅ under `raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`, but **not** `brp_zoeken` (unaffected relationship, Non-Goals). Cells that already had a direct, non-inherited grant (e.g. `recordmanager`'s current `wijzigen (en afgehandeld)` ✅) are unchanged.

### 4. Integration test updates follow directly from Decision 2

With flattened PABC mappings, the existing `IdentityRestServiceTest` expectations for `GET .../behandelaar-groups` and `POST .../behandelaar-groups` change in a predictable, mechanical way — the test doubles as the regression check for Decision 2:
- Single-zaaktype test (currently expects behandelaar + beheerder + coordinator + recordmanager groups for zaaktype test 2): after flattening, only groups whose functional role is actually `behandelaar` remain — `GROUP_BEHANDELAARS_TEST_1` and `GROUP_BEHANDELAARS_LONG_NAME_TEST`. `GROUP_BEHEERDERS_ELK_DOMEIN`, `GROUP_COORDINATORS_TEST_1`, `GROUP_RECORDMANAGERS_TEST_1` must be removed from the expected response.
- Multi-zaaktype test (currently expects `GROUP_BEHEERDERS_ELK_DOMEIN` as the one group authorised as `behandelaar` for both zaaktypes, because that functional role today has no domain restriction and inherits `behandelaar`): after flattening, `beheerder_elk_domein` no longer carries the `behandelaar` application role, so the expected result becomes an empty list — no group is authorised as `behandelaar` for both zaaktypes.
- A new test scenario should assert the actual goal of PZ-11245 directly: a group whose functional role is `coordinator`-only (or `recordmanager`-only, or `beheerder`-only) does not appear in either `behandelaar-groups` response, even though those users can still do everything they could before (verified separately via existing/updated zaak- and taak-rechten Rego unit tests, not via the group-picker endpoints).
- `ZaakRestServiceTest.kt`/`TaskRestServiceTest.kt` (and any other itest with a `COORDINATOR_*`/`RECORDMANAGER_*`/`BEHEERDER_*` test user performing a zaak/taak read or write) need a pass to confirm they still succeed post-flattening — they should, since Decision 1 preserves effective permissions, but they are the safety net if a clause is missed.

## Risks / Trade-offs

- **[Risk] A permission grant is missed in Decision 1, silently narrowing a role's access.** → Mitigation: the `*_test.rego` unit tests already assert access per role; extend them for every permission touched (add tests for the newly-explicit `coordinator`/`recordmanager`/`beheerder` clauses), and run the full `itest` suite (which exercises `COORDINATOR_1`, `RECORDMANAGER_1`, `BEHEERDER_1` test users against real zaak/taak actions) before merging.
- **[Risk] `accessControlPolicies.md` and the actual Rego drift again immediately after this change**, since nothing enforces the two stay in sync. → Mitigation: out of scope for this change (no tooling change proposed); existing convention is a code-review reminder (comment header in every `.rego` file), unchanged by this proposal.
- **[Trade-off] The Rego files get longer/more repetitive** (every permission now lists up to 5 explicit role clauses instead of 1-2). This is an intentional trade: explicit and auditable over compact and implicit.

