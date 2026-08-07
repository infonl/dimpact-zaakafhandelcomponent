## Why

ZAC's application roles (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) currently rely on an implicit hierarchy: a user with a "higher" role is expected to also carry every "lower" role, and OPA policies only grant each permission to the one role that historically needed it, trusting the hierarchy to fan it out. This has two problems: it is easy to break silently (any role/user missing its inherited roles loses access nobody notices until it's needed), and it makes the "toekennen" (assign) group picker return groups that are only relevant for coordinators/recordmanagers/beheerders, because those functional roles also carry the `behandelaar` application role as part of the hierarchy convention (PZ-11245). Removing the hierarchy and making every permission grant explicit per role removes this hidden coupling without changing what any role is currently allowed to do.

## What Changes

- Flatten the application-role hierarchy: `rollen.rego` and the permission policy files (`zaak-rechten.rego`, `taak-rechten.rego`, `document-rechten.rego`, `werklijst-rechten.rego`, `notitie-rechten.rego`, `overige-rechten.rego`, `brp-rechten.rego`) explicitly grant each permission to every role that should have it (adding `coordinator`/`recordmanager`/`beheerder` to rules that previously relied on inheriting `raadpleger`'s or `behandelaar`'s grants), instead of relying on a role carrying multiple application roles at once.
- Flatten the local PABC test mapping (`scripts/docker-compose/imports/pabc-database/json-mapping/pabc-mapping-data.json`): each functional test role maps to exactly one application role instead of the current cumulative chain (e.g. `coordinator_domein_test_1` currently maps to `raadpleger`+`behandelaar`+`coordinator`; it will map to `coordinator` only).
- Update `docs/solution-architecture/accessControlPolicies.md`: remove the "lower-level roles" narrative (lines describing the inheritance chain) and make the permission matrix table explicit — every role that has a permission gets its own ✅, with no implied inheritance.
- Update `IdentityRestServiceTest.kt` expectations for `listBehandelaarGroupsForZaaktype(s)`: groups belonging to functional roles that are only `coordinator`/`recordmanager`/`beheerder` (and not `behandelaar`) SHALL NOT appear in the behandelaar-groups response. **BREAKING**: this narrows the set of groups returned by the existing `GET .../behandelaar-groups` and `POST .../behandelaar-groups` endpoints for any caller currently depending on the hierarchy-inflated result.
- No permission is added or removed for any role — every role keeps exactly the same effective access it has today; only how that access is granted changes (explicit instead of inherited).

## Capabilities

### New Capabilities
- `application-role-permission-matrix`: Documents and enforces that every ZAC application role (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) is granted its effective permissions explicitly in OPA policy, with no dependency on a user also carrying "lower" application roles.
- `behandelaar-groups-single-zaaktype`: Documents the hierarchy-free authorization behavior of the existing `GET /rest/identity/zaaktype/{zaaktypeDescription}/behandelaar-groups` endpoint (not previously covered by a spec).

### Modified Capabilities
- `behandelaar-groups-for-multiple-zaaktypes`: The "authorised for the `behandelaar` application role" requirement now explicitly excludes groups whose functional role maps only to `coordinator`, `recordmanager`, or `beheerder` (previously included due to hierarchy-inflated PABC test mappings).

## Impact

- **OPA policies**: `src/main/resources/policies/*.rego` (permission rules gain explicit role checks) and their tests `src/test/resources/policies/*_test.rego`.
- **Docs**: `docs/solution-architecture/accessControlPolicies.md`.
- **Local dev/test fixtures**: `scripts/docker-compose/imports/pabc-database/json-mapping/pabc-mapping-data.json`.
- **Integration tests**: `src/itest/kotlin/nl/info/zac/itest/IdentityRestServiceTest.kt` (and any other itest asserting on hierarchy-derived group/permission results, e.g. `ZaakRestServiceTest.kt`, `TaskRestServiceTest.kt` where a beheerder/coordinator/recordmanager test user relies on inherited read/write access).
- **Out of scope**: the actual PABC configuration on the INFO P:CT environment and municipality environments, and the release-notes entry instructing municipalities to remove the hierarchy in their own PABC setup — both are tracked separately in the "Applicatie Releases" story per PZ-11245's acceptance criteria. This change only touches ZAC's own repo (policies, docs, local dev fixtures, tests).
