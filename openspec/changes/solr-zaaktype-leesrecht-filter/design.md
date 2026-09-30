## Context

`SearchService.search` adds two filter queries derived from the `LoggedInUser`:

- `getAllowedZaaktypenFilterQuery`: an `OR` over `zaaktypeOmschrijving` for every key of
  `applicationRolesPerZaaktype`, i.e. every zaaktype the user holds *any* role for. An empty set yields a
  filter on a non-existing zaaktype, so no results.
- `getZaakspecifiekGeautoriseerdFilterQuery`: excludes zaakspecifiek geautoriseerde rows for zaaktypen
  without the `zaakspecifiek_geautoriseerd` flag.

OPA (`zaak-rechten.rego`, `taak-rechten.rego`, `document-rechten.rego`) grants `lezen` only when the roles
passed as `user.rollen` contain one of `raadpleger`, `behandelaar`, `coordinator`, `recordmanager`,
`beheerder`. `UserInput` computes `user.rollen` for a zaaktype as
`applicationRolesPerZaaktype[zaaktype] + overallRoles`. The Solr filter ignores both the role names and the
overall roles, which is the mismatch this change removes. See proposal.md - Why.

In the Koppelen flow, `ZaakKoppelenRestService` evaluates OPA rights for every search hit and
`gerelateerdNotLinkableReason` returns `NOT_AUTHORISED_TO_LEZEN` when `lezen` is false; `linkZaak` asserts
`canBeRelatedTo`, which today is defined as "no reason".

## Goals / Non-Goals

**Goals:**
- Solr admits a zaaktype only when the roles OPA would see for it contain a read role.
- Remove the now-unreachable `NOT_AUTHORISED_TO_LEZEN` reason end to end, while keeping the server-side read
  check in `linkZaak`.

**Non-Goals:**
- Admitting zaaktypen the user has no per-zaaktype roles for at all, even when they hold an overall read
  role. OPA would grant `lezen` there, but Solr hides those today and the ticket requires that users with a
  read role see the same zaken as now. System users (`FUNCTIONEEL_GEBRUIKER`, `PRODUCTAANVRAAG_GEBRUIKER`)
  have no per-zaaktype roles and are unaffected.
- Changing the zaakspecifiek geautoriseerd filter or any OPA policy.
- Changing the werklijst-level `zaken_taken` policy.

## Decisions

### Filter on read roles in `getAllowedZaaktypenFilterQuery`
Replace `applicationRolesPerZaaktype.keys` with the keys whose role set, united with `overallRoles`,
intersects the read roles. Keep the empty-set fallback to the non-existing zaaktype.

Alternatives considered:
- *Post-filter the Solr results through OPA*: correct by construction, but breaks paging, counts and facets,
  and costs one OPA call per hit.
- *Index read-role information in Solr*: roles are per user; nothing to index.

### Define the read roles once in Kotlin
Add a named set of read role names next to the existing role constants in `PabcClient.kt`
(`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`), with the same "must match
`rollen.rego`" note the other constants carry. The rego files stay the source of truth for OPA; the Kotlin
set only mirrors the `lezen` rule for query building. A unit test pins the set so that a change to one side
shows up in review.

Alternative: ask OPA which zaaktypen are readable. No such policy exists and it adds a remote call per
search for a value derivable locally.

### Remove `NOT_AUTHORISED_TO_LEZEN`, keep the read check in `canBeRelatedTo`
- Drop the enum value and the `!to.lezen` branch from `gerelateerdNotLinkableReason`.
- Redefine `canBeRelatedTo` as `to.lezen && gerelateerdNotLinkableReason(to) == null` so `linkZaak` still
  refuses an unreadable target.
- Regenerate the OpenAPI spec and frontend types; remove the i18n keys in `nl.json` and `en.json` and the
  value from the reason list in `zaak-link.component.spec.ts`. That spec already uses Testing Library, so
  touching it needs no migration.

A hit that slips through anyway (e.g. an index row whose zaaktype roles changed mid-session) is then listed
without a reason but refused on link with the generic policy error. Acceptable: it requires a PABC change
during the session.

## Risks / Trade-offs

- [Kotlin read-role set drifts from `rollen.rego` / `lezen` rules] → constant documented as mirroring the
  rego rule; unit test on the set; policy documentation (`docs/solution-architecture/accessControlPolicies.md`)
  mentions the search filter.
- [Users relying on a misconfiguration lose search visibility] → intended; they could not open those zaken.
- [REST enum value removal] → only consumed by the ZAC frontend, regenerated in the same change.

## Migration Plan

No data or index migration: the filter is computed per query. Rollback is a plain revert.
