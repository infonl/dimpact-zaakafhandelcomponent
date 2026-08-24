## Why

PZ-11909 added the `zaakspecifiek_geautoriseerd` ZAC application role (PABC/Keycloak wiring only, under its
original name `zaakspecifiek_autorisatie_behandelaar`, commit `5172e956b`) and the per-zaak
`ZAAK_GEAUTORISEERD` indicator (`RestZaak.isZaakspecifiekGeautoriseerd`, PZ-11952) as groundwork, but
explicitly shipped **no enforcement**: commit `e98acd177` states "this new role does not give the user any
permissions yet". Today OPA has no notion at all of a "zaakspecifiek geautoriseerde zaak" — any
behandelaar/raadpleger/coordinator of the zaaktype can read, treat, and access the taken and documenten of
such a zaak, which defeats the purpose of marking it as specifically authorised. This change implements the
actual backend access control: `zaakspecifiek_geautoriseerd` is a flag, not a rights-bearing role — a
medewerker who holds it for a zaaktype, *in addition to* a normal application role (`raadpleger`,
`behandelaar`, `coordinator`) for that same zaaktype, gets that normal role's rights extended to also cover
zaakspecifiek geautoriseerde zaken of that zaaktype. Holding the flag without also holding one of those
normal roles grants nothing. Everyone who does not hold the flag is denied access to a zaakspecifiek
geautoriseerde zaak, including via direct URL access, regardless of which normal role(s) they hold.
`recordmanager` and `beheerder` already have unconditional access to every zaak today and this change leaves
their rules untouched; formally specifying and testing their access to a zaakspecifiek geautoriseerde zaak
is left to a follow-up story.

## What Changes

- Add a `zaakspecifiekGeautoriseerd` boolean to the OPA input for zaak, taak, and document policy checks
  (`ZaakData`/`TaakData`/`DocumentData` in `nl.info.zac.policy.input`), populated by looking up the zaak's
  `ZAAK_GEAUTORISEERD` zaakeigenschap (the same check `RestZaakConverter` already performs for the
  `isZaakspecifiekGeautoriseerd` indicator, extracted into a shared helper so both call sites stay in sync).
- Rename the `zaakspecifiekAutorisatieBehandelaar` Rego role constant (`rollen.rego`) and its underlying role
  string to `zaakspecifiekGeautoriseerd` / `zaakspecifiek_geautoriseerd`, and rename the corresponding PABC
  application role (`scripts/docker-compose/imports/pabc-database/json-mapping/pabc-mapping-data.json`) to
  match, so the role PABC hands out and the role OPA checks for stay the same string. Also extend that
  functional role's PABC mapping to grant `behandelaar` in addition to `zaakspecifiek_geautoriseerd`, so it
  is self-sufficient under the flag model instead of depending on a separate, unrelated group membership
  for its `behandelaar` grant.
- Add a `zaak_allowed` gating rule to `zaak-rechten.rego`, `taak-rechten.rego`, and `document-rechten.rego`:
  true when the zaak/taak/document is not zaakspecifiek geautoriseerd, or the user holds
  `zaakspecifiek_geautoriseerd`. This gate is added only to the rule bodies that grant `raadpleger`,
  `behandelaar`, and/or `coordinator` a permission; the separate rule bodies that already grant
  `recordmanager`/`beheerder` a permission unconditionally are left untouched, so their access is unaffected
  by this change (splitting a combined rule body into a gated non-privileged branch and an untouched
  privileged branch where the two are not already separate today).
- `zaakspecifiek_geautoriseerd` is **not** added to any `some role in {...}` set: it grants no permission by
  itself. A medewerker's actual rights on a zaakspecifiek geautoriseerde zaak still come entirely from
  whichever normal role (`raadpleger`, `behandelaar`, `coordinator`) they separately hold for that zaaktype —
  the flag only decides, via `zaak_allowed`, whether that normal role's rights are allowed to apply to a
  zaakspecifiek geautoriseerde zaak at all.
- No REST-layer or frontend changes are required for the "direct URL access shows the generic insufficient
  rights message" acceptance criterion: `assertPolicy` already throws `PolicyException` → HTTP 403 → the
  frontend already renders the generic `msg.error.server.forbidden` ("U heeft helaas onvoldoende rechten om
  deze actie uit te voeren.") message for any policy denial, so a denied zaakspecifiek geautoriseerde zaak
  falls through the same existing path.
- Extend `docs/solution-architecture/accessControlPolicies.md`: add `zaakspecifiek_geautoriseerd` to the
  application roles table, and add a note explaining the flag mechanism. Unlike a normal role, it does **not**
  get its own column in the permission matrix, since it grants no permission on its own.
- Add Rego unit tests (`zaak-rechten_test.rego`, `taak-rechten_test.rego`, `document-rechten_test.rego`)
  covering: the flag alone grants nothing, a normal role alone (without the flag) is denied on a
  zaakspecifiek geautoriseerde zaak, and a normal role combined with the flag is allowed. Backend
  integration tests in `ZaakRestServiceTest.kt` (and taak/document equivalents) reuse the existing
  `ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1` test user and `GROUP_ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAARS_TEST_1`
  test group fixtures (which already hold both a normal `behandelaar` role and the flag for the relevant
  zaaktype), following the `HTTP_FORBIDDEN` pattern already used for other zaaktype-authorisation checks.

Out of scope (explicit in the ticket, follow-up PZ-11954): werklijsten and zoekresultaten (Solr-backed search)
are not restricted by this change. The `ZaakZoekObject`/`TaakZoekObject`/`DocumentZoekObject`-based rechten
lookups (used for search result rights display) are intentionally left with `zaakspecifiekGeautoriseerd`
defaulting to `false`/absent, since those call sites are out of this story's scope.

## Capabilities

### New Capabilities
- `zaakspecifieke-autorisatie-toegang`: OPA-enforced access restriction that denies `raadpleger`,
  `behandelaar`, and `coordinator` access to a zaakspecifiek geautoriseerde zaak (and its taken/documenten)
  unless the user also holds the `zaakspecifiek_geautoriseerd` flag for that zaaktype; the flag itself grants
  no rights and is inert without one of those normal roles. `recordmanager`/`beheerder` access to such a
  zaak is out of scope for this capability (follow-up story).

## Impact

- **Backend policy input**: `nl.info.zac.policy.input.ZaakData`, `TaakData`, `DocumentData` (new field), and
  `nl.info.zac.policy.PolicyService` (populates the new field for the direct-read call sites:
  `readZaakRechten(zaak, zaaktype, loggedInUser)`, `readDocumentRechten(enkelvoudigInformatieobject, lock, zaak)`,
  `readTaakRechten(taskInfo, zaaktypeOmschrijving)`).
- **OPA policies**: `src/main/resources/policies/rollen.rego`, `zaak-rechten.rego`, `taak-rechten.rego`,
  `document-rechten.rego`, and their Rego unit tests under `src/test/resources/policies/`.
- **PABC seed data**: `scripts/docker-compose/imports/pabc-database/json-mapping/pabc-mapping-data.json`
  (rename the `zaakspecifiek_autorisatie_behandelaar` application role to `zaakspecifiek_geautoriseerd`).
  Keycloak's functional role/group names (e.g. `zaakspecifiek_autorisatie_behandelaar_test_1`) are left
  unchanged — they are free-text labels, not the ZAC application role string OPA checks.
- **Shared helper**: extraction of the `ZAAK_GEAUTORISEERD` zaakeigenschap lookup currently duplicated in
  `RestZaakConverter` into a shared location reused by `PolicyService`.
- **Docs**: `docs/solution-architecture/accessControlPolicies.md`.
- **Tests**: backend unit tests (`PolicyServiceTest`), Rego unit tests, and
  `src/itest/kotlin/nl/info/zac/itest/ZaakRestServiceTest.kt` (plus taak/document itest equivalents), reusing
  the existing `ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1` / `GROUP_ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAARS_TEST_1`
  fixtures.
- **Not impacted**: frontend code, Solr search/werklijst rechten, Keycloak configuration (functional
  role/group naming is unaffected).
