## Why

PZ-11909 added the `zaakspecifiek_autorisatie_behandelaar` ZAC application role (PABC/Keycloak wiring only,
commit `5172e956b`) and the per-zaak `ZAAK_GEAUTORISEERD` indicator (`RestZaak.isZaakspecifiekGeautoriseerd`,
PZ-11952) as groundwork, but explicitly shipped **no enforcement**: commit `e98acd177` states "this new role
does not give the user any permissions yet". Today OPA has no notion at all of a "zaakspecifiek geautoriseerde
zaak" — any behandelaar/raadpleger/coordinator of the zaaktype can read, treat, and access the taken and
documenten of such a zaak, which defeats the purpose of marking it as specifically authorised. This change
implements the actual backend access control: only employees who hold `zaakspecifiek_autorisatie_behandelaar`
for the zaak's zaaktype may access a zaakspecifiek geautoriseerde zaak, its taken, and its documenten;
everyone else who does not already have unconditional access (i.e. `raadpleger`, `behandelaar`, and
`coordinator`) is denied, including via direct URL access. `recordmanager` and `beheerder` already have
unconditional access to every zaak today and this change leaves their rules untouched; formally specifying
and testing their access to a zaakspecifiek geautoriseerde zaak is left to a follow-up story.

## What Changes

- Add a `geautoriseerd` boolean to the OPA input for zaak, taak, and document policy checks
  (`ZaakData`/`TaakData`/`DocumentData` in `nl.info.zac.policy.input`), populated by looking up the zaak's
  `ZAAK_GEAUTORISEERD` zaakeigenschap (the same check `RestZaakConverter` already performs for the
  `isZaakspecifiekGeautoriseerd` indicator, extracted into a shared helper so both call sites stay in sync).
- Add a `zaakspecifiek_toegankelijk` gating rule to `zaak-rechten.rego`, `taak-rechten.rego`, and
  `document-rechten.rego`: true when the zaak/taak/document is not zaakspecifiek geautoriseerd, or the user
  holds `zaakspecifiek_autorisatie_behandelaar`. This gate is added only to the rule bodies that grant
  `raadpleger`, `behandelaar`, and/or `coordinator` a permission; the separate rule bodies that already grant
  `recordmanager`/`beheerder` a permission unconditionally are left untouched, so their access is unaffected
  by this change (splitting a combined rule body into a gated non-privileged branch and an untouched
  privileged branch where the two are not already separate today).
- Grant `zaakspecifiek_autorisatie_behandelaar` the same explicit rights as `behandelaar` everywhere
  `behandelaar` currently appears in a role set in these three policy files (per the acceptance criteria: this
  role's effective rights equal the normal behandelaar-and-raadpleger rights, granted explicitly and not via
  reliance on the user also separately holding those other roles, consistent with the existing no-hierarchy
  policy design).
- No REST-layer or frontend changes are required for the "direct URL access shows the generic insufficient
  rights message" acceptance criterion: `assertPolicy` already throws `PolicyException` → HTTP 403 → the
  frontend already renders the generic `msg.error.server.forbidden` ("U heeft helaas onvoldoende rechten om
  deze actie uit te voeren.") message for any policy denial, so a denied zaakspecifiek geautoriseerde zaak
  falls through the same existing path.
- Extend `docs/solution-architecture/accessControlPolicies.md`: add `zaakspecifiek_autorisatie_behandelaar` to
  the application roles table and to the permission matrix (as a new column, following the existing
  no-hierarchy documentation style), and document the zaakspecifiek-geautoriseerde-zaak access restriction.
- Add Rego unit tests (`zaak-rechten_test.rego`, new `taak-rechten_test.rego`/`document-rechten_test.rego`
  coverage) and backend integration tests in `ZaakRestServiceTest.kt` (and taak/document equivalents) using
  the existing `ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1` test user and `GROUP_ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAARS_TEST_1`
  test group fixtures, following the `HTTP_FORBIDDEN` pattern already used for other zaaktype-authorisation
  checks.

Out of scope (explicit in the ticket, follow-up PZ-11954): werklijsten and zoekresultaten (Solr-backed search)
are not restricted by this change. The `ZaakZoekObject`/`TaakZoekObject`/`DocumentZoekObject`-based rechten
lookups (used for search result rights display) are intentionally left with `geautoriseerd` defaulting to
`false`/absent, since those call sites are out of this story's scope.

## Capabilities

### New Capabilities
- `zaakspecifieke-autorisatie-toegang`: OPA-enforced access restriction that denies `raadpleger`,
  `behandelaar`, and `coordinator` access to a zaakspecifiek geautoriseerde zaak (and its taken/documenten)
  unless they also hold `zaakspecifiek_autorisatie_behandelaar` for that zaaktype, plus the explicit grant of
  behandelaar-equivalent rights to the new role. `recordmanager`/`beheerder` access to such a zaak is out of
  scope for this capability (follow-up story).

### Modified Capabilities
- `application-role-permission-matrix`: the documented/enforced set of ZAC application roles grows from 5
  (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) to include
  `zaakspecifiek_autorisatie_behandelaar`, and the "every role's grants are explicit, no inheritance" invariant
  now also covers this role's behandelaar-equivalent grants.

## Impact

- **Backend policy input**: `nl.info.zac.policy.input.ZaakData`, `TaakData`, `DocumentData` (new field), and
  `nl.info.zac.policy.PolicyService` (populates the new field for the direct-read call sites:
  `readZaakRechten(zaak, zaaktype, loggedInUser)`, `readDocumentRechten(enkelvoudigInformatieobject, lock, zaak)`,
  `readTaakRechten(taskInfo, zaaktypeOmschrijving)`).
- **OPA policies**: `src/main/resources/policies/zaak-rechten.rego`, `taak-rechten.rego`, `document-rechten.rego`,
  and their Rego unit tests under `src/test/resources/policies/`.
- **Shared helper**: extraction of the `ZAAK_GEAUTORISEERD` zaakeigenschap lookup currently duplicated in
  `RestZaakConverter` into a shared location reused by `PolicyService`.
- **Docs**: `docs/solution-architecture/accessControlPolicies.md`.
- **Tests**: backend unit tests (`PolicyServiceTest`), Rego unit tests, and `src/itest/kotlin/nl/info/zac/itest/ZaakRestServiceTest.kt`
  (plus taak/document itest equivalents), reusing the existing `ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAAR_1` /
  `GROUP_ZAAKSPECIFIEK_AUTORISATIE_BEHANDELAARS_TEST_1` fixtures.
- **Not impacted**: frontend code, Solr search/werklijst rechten, Keycloak/PABC configuration (already done
  in prior PZ-11909/PZ-11944 commits).
