## Context

PZ-11909 introduced two pieces of groundwork with no enforcement behind them yet:
- The `zaakspecifiek_geautoriseerd` ZAC application role (PABC/Keycloak, commit `5172e956b` / `e98acd177`,
  originally named `zaakspecifiek_autorisatie_behandelaar`), granted per zaaktype, deliberately shipped with
  no permissions attached.
- The `ZAAK_GEAUTORISEERD` zaakeigenschap convention: an Open Zaak zaakeigenschap named exactly
  `ZAAK_GEAUTORISEERD` with value `"true"` marks an individual zaak as "zaakspecifiek geautoriseerd".
  `RestZaakConverter.toRestZaak` (`src/main/kotlin/nl/info/zac/app/zaak/converter/RestZaakConverter.kt:71-73,114-116`)
  already reads this via `zrcClientService.listZaakeigenschappen(zaak.uuid)` to compute the purely
  informational `RestZaak.isZaakspecifiekGeautoriseerd` field (lock icon in the frontend).
  A zaak can only carry this zaakeigenschap if its zaaktype defines the corresponding eigenschap
  definitie (reflected in `RestZaaktypeConfiguration.zaakspecifiekAutoriseerbaar`) — the ZGW API rejects
  setting a zaakeigenschap on a zaak for an eigenschap the zaaktype does not define. This change therefore
  does not need to separately cross-check `zaakspecifiekAutoriseerbaar`: checking the zaak's own
  zaakeigenschap value is already sufficient, since the ZGW spec itself guards against a zaak of a
  non-`zaakspecifiekAutoriseerbaar` zaaktype ever carrying this eigenschap.

Access control in ZAC is enforced in OPA (Rego), not in Kotlin: every zaak/taak/document REST read or
mutation builds a `ZaakInput`/`TaakInput`/`DocumentInput` (`nl.info.zac.policy.input`), sends it to OPA's
`zaak_rechten`/`taak_rechten`/`document_rechten` rule sets (`src/main/resources/policies/*.rego`), and
`assertPolicy(...)` (`PolicyService.kt:255-259`) throws `PolicyException` → HTTP 403 when a right is `false`.
The three policy files each follow the same shape: a `zaaktype_allowed` gate, then one rule per permission of
the form `some role in {…}; role.rol in user.rollen` (optionally combined with zaak/taak/document state such
as `zaak.open`). A companion capability, `application-role-permission-matrix`, already established the
project's convention that every role's grants must be explicit — no role may rely on also holding a "lower"
role.

None of `ZaakData`, `TaakData`, or `DocumentData` currently carry any "is this zaakspecifiek geautoriseerd"
signal, so today OPA cannot distinguish a zaakspecifiek geautoriseerde zaak from any other zaak of the same
zaaktype: every application role, including `recordmanager` and `beheerder`, has unconditional access to it,
its taken, and its documenten.

**Corrections from earlier versions of this design**:
1. An earlier iteration treated `zaakspecifiek_geautoriseerd` (then still named
   `zaakspecifiek_autorisatie_behandelaar`) as a rights-bearing role in its own right, granted the same
   explicit permissions as `behandelaar`. That is not the intended model. `zaakspecifiek_geautoriseerd` is a
   flag: it carries no permissions of its own. A medewerker's rights on a zaakspecifiek geautoriseerde zaak
   come entirely from whichever other application role they separately hold for that zaaktype — the flag
   only decides whether that role's rights are allowed to apply to a zaakspecifiek geautoriseerde zaak at
   all. Holding the flag without also holding another application role grants nothing.
2. A later iteration then left `recordmanager` and `beheerder` deliberately ungated, reasoning that their
   existing unconditional access should be untouched and that formally restricting them was a follow-up
   story's concern. That is also not the intended model: `recordmanager` and `beheerder` are treated exactly
   like every other application role — they too need the flag to access a zaakspecifiek geautoriseerde zaak.
   There is no privileged role that bypasses the restriction.

## Goals / Non-Goals

**Goals:**
- A medewerker who holds any application role (`raadpleger`, `behandelaar`, `coordinator`, `recordmanager`,
  or `beheerder`) for a zaaktype, and who *also* holds `zaakspecifiek_geautoriseerd` for that same zaaktype,
  gets that role's rights extended to also cover zaakspecifiek geautoriseerde zaken of that zaaktype — in
  addition to the non-geautoriseerde zaken they already covered.
- A medewerker who holds an application role but *not* `zaakspecifiek_geautoriseerd` for a zaaktype is
  denied on a zaakspecifiek geautoriseerde zaak of that zaaktype — including via direct URL / deep-link
  access to the zaak, a taak, or a document — regardless of which role(s) they hold. This applies uniformly
  to `recordmanager` and `beheerder` as well; neither is exempt.
- A medewerker who holds only `zaakspecifiek_geautoriseerd`, without any other application role, for a
  zaaktype has no rights at all on zaken of that zaaktype — the flag is inert on its own, exactly as if the
  medewerker held no application role for that zaaktype.
- A denied user sees the same generic "onvoldoende rechten" message ZAC already shows for any other policy
  denial — no new user-facing error path.

**Non-Goals:**
- Werklijsten and Solr-backed zoekresultaten (`ZaakZoekObject`/`TaakZoekObject`/`DocumentZoekObject` rechten
  lookups) are explicitly out of scope — the ticket defers this to follow-up PZ-11954. Those call sites keep
  `zaakspecifiekGeautoriseerd` defaulting to "not zaakspecifiekGeautoriseerd", i.e. unrestricted, for now.
- Being able to mark a zaak as zaakspecifiek geautoriseerd from within ZAC (currently only possible directly
  in Open Zaak) is a separate follow-up story, not part of this change.
- No changes to Keycloak functional role/group naming (aside from the one itest fixture user's group
  membership, see Decision 3), or to how `LoggedInUser.applicationRolesPerZaaktype` is populated — that
  plumbing already exists and already produces the role correctly. The PABC *application role name* does
  change (see Decision 3), but this requires no change to Keycloak itself.

## Decisions

### 1. Add one `zaakspecifiekGeautoriseerd` boolean field per policy input, sourced from the existing zaakeigenschap check

Add `zaakspecifiekGeautoriseerd: Boolean` to `ZaakData`, `TaakData`, and `DocumentData` (`nl.info.zac.policy.input`).
`PolicyService` populates it for the three call sites that back an actual single-resource read/mutation:
- `readZaakRechten(zaak, zaaktype, loggedInUser)` — looks up the zaak's own zaakeigenschappen.
- `readTaakRechten(taskInfo, zaaktypeOmschrijving)` — resolves the zaak UUID via
  `TaakVariabelenService.readZaakUUID(taskInfo)` (already used elsewhere for other purposes), then the same
  zaakeigenschap lookup.
- `readDocumentRechten(enkelvoudigInformatieobject, lock, zaak)` — uses the already-passed `zaak: Zaak?`
  parameter (`false`/absent when `zaak` is `null`, i.e. a document not linked to any zaak — unchanged from
  today).

The two search-object-based overloads (`readZaakRechtenForZaakZoekObject`, `readTaakRechten(taakZoekObject)`,
`readDocumentRechten(enkelvoudigInformatieobject: DocumentZoekObject)`) leave `zaakspecifiekGeautoriseerd` at
its default `false`, consistent with the Non-Goals above.

**Shared lookup helper**: extract the zaakeigenschap check duplicated between `RestZaakConverter` and
`PolicyService` into one shared function next to the existing `ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD` /
`ZAAKEIGENSCHAP_WAARDE_GEAUTORISEERD` constants, so the "what counts as zaakspecifiek geautoriseerd" rule is
defined exactly once.

### 2. Gate every rule body uniformly with one shared `zaak_allowed` rule — no role is exempt

In each of `zaak-rechten.rego`, `taak-rechten.rego`, `document-rechten.rego`, add:
```rego
default zaak_allowed := false
zaak_allowed if {
    not zaak.zaakspecifiekGeautoriseerd   # or taak.zaakspecifiekGeautoriseerd / document.zaakspecifiekGeautoriseerd
}
zaak_allowed if {
    zaakspecifiekGeautoriseerd.rol in user.rollen
}
```
This gate is added as an extra condition to **every** rule body in these three files, with no exception:
the bodies that grant `recordmanager`/`beheerder` a permission get the gate too, exactly like the bodies
that grant `raadpleger`, `behandelaar`, or `coordinator`. Role sets themselves are **not** changed (see
Decision 3) — only the gate condition is added.

Where a permission already has two separate `if` bodies today (e.g. `wijzigen`: one body for
`{behandelaar, coordinator}` + `zaak.open`, a second body for `{recordmanager, beheerder}` unconditionally),
both bodies get the gate:
```rego
default wijzigen := false
wijzigen if {
    zaaktype_allowed
    zaak.open
    zaak_allowed
    some role in {behandelaar, coordinator}
    role.rol in user.rollen
}
wijzigen if {
    zaaktype_allowed
    zaak_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}
```
Where a permission today has a single body combining every role (e.g. `lezen`:
`{raadpleger, behandelaar, coordinator, recordmanager, beheerder}`), the gate is simply added to that one
body — no split is needed, because there is no longer a privileged subset of roles to carve out:
```rego
default lezen := false
lezen if {
    zaaktype_allowed
    zaak_allowed
    some role in {raadpleger, behandelaar, coordinator, recordmanager, beheerder}
    role.rol in user.rollen
}
```
Rules whose only roles are `recordmanager`/`beheerder` (`heropenen`, `bekijken_zaakdata`, `brondatum_zetten`)
also get the gate added to their one body.

**Alternative considered**: gate only the rule bodies that grant `raadpleger`/`behandelaar`/`coordinator`,
leaving the `recordmanager`/`beheerder` bodies untouched and unconditional. Rejected: `recordmanager` and
`beheerder` must be treated exactly like every other application role — no role is exempt from needing the
flag on a zaakspecifiek geautoriseerde zaak. (This was in fact an earlier iteration of this design; see the
Context section.)

**Alternative considered**: only add the gate to `lezen`/`behandelen` (the two rights explicitly called out
in the acceptance criteria). Rejected: the ticket is explicit that unauthorised employees may not see or
treat any right on such a zaak/taak/document ("mogen geen … kunnen zien of behandelen"), and leaving e.g.
`wijzigen` or `toevoegen_document` ungated would let a plain behandelaar mutate a zaak they cannot even read.
Gating every rule body is also simpler to reason about and test than tracking which rules are gated.

### 3. `zaakspecifiek_geautoriseerd` is a flag, not a rights-bearing role — do not add it to any role set, and rename/extend the PABC application role mappings to match

Do **not** add `zaakspecifiekGeautoriseerd` to any `some role in {...}` set in any of the three policy files.
The flag exists purely as the second branch of the `zaak_allowed` gate (Decision 2). A medewerker's actual
rights on a zaakspecifiek geautoriseerde zaak come entirely from whichever other application role they
separately hold for the zaaktype — matching the acceptance criteria: "wanneer een medewerker naast zijn
applicatierol ook `zaakspecifiek_geautoriseerd` heeft, gelden de rechten van die rol ook voor zaakspecifiek
geautoriseerde zaken van dat zaaktype." Combined with Decision 2, this gives exactly the intended matrix,
for every application role including `recordmanager` and `beheerder`:

| Holds `zaakspecifiek_geautoriseerd`? | Holds another application role (e.g. `behandelaar`, `beheerder`)? | Rights on a geautoriseerde zaak |
|---|---|---|
| No | No | none (unchanged from today) |
| No | Yes | none — `zaak_allowed` fails, denied |
| Yes | No | none — the flag alone is in no role set |
| Yes | Yes | that role's full rights — `zaak_allowed` passes, and the role is checked as usual |

Rename the Rego role constant in `rollen.rego` from `zaakspecifiekAutorisatieBehandelaar` (role string
`zaakspecifiek_autorisatie_behandelaar`) to `zaakspecifiekGeautoriseerd` (role string
`zaakspecifiek_geautoriseerd`), and rename the corresponding PABC application role in
`scripts/docker-compose/imports/pabc-database/json-mapping/pabc-mapping-data.json` (the `applicationRoles[]`
entry's `name` field) to match, so the role string PABC hands out to a logged-in user and the role string
OPA checks for stay identical end-to-end. Keycloak's functional role and group names (e.g.
`zaakspecifiek_autorisatie_behandelaar_test_1`) are left as-is: they are independent, free-text labels and do
not need to match the ZAC application role string.

Additionally, extend two PABC mappings so the reference test setup reflects the "every role needs the flag"
model instead of relying on incidental, unrelated group memberships:
- The "zaakspecifiek geautoriseerd" functional role's mapping (which already grants
  `zaakspecifiek_geautoriseerd` for domain `domein_test_1`) gets a second mapping entry that also grants it
  `behandelaar` for that same domain, so this one functional role is self-sufficient. The itest fixture
  user's membership of the unrelated `/behandelaars-test-1` Keycloak group is removed at the same time, so
  the itest suite genuinely exercises this mapping for the `behandelaar` grant.
- The `beheerder_elk_domein` functional role's mapping (which already grants `beheerder` globally, with
  `isAllEntityTypes: true` and no domain restriction) gets a second mapping entry that also grants
  `zaakspecifiek_geautoriseerd`, globally in the same way. Without this, the reference `BEHEERDER_1` fixture
  would lose access to zaakspecifiek geautoriseerde zaken under the newly-uniform gate — which would be a
  reference-setup regression, not an intended behaviour change (a real-world beheerder is expected to
  retain broad access; this change does not ask PABC administrators to reconsider that).

**Alternative considered (an earlier iteration of this design)**: explicitly grant `zaakspecifiek_geautoriseerd`
the same rights as `behandelaar` by adding it to every role set `behandelaar` appears in. Superseded: the
actual intended behaviour is that the flag extends whichever role the medewerker already holds, not that it
independently grants a fixed set of behandelaar-equivalent rights of its own. Under that design, a
medewerker holding only the flag (no other role) would incorrectly gain full behandelaar rights on
geautoriseerde zaken; under the corrected design they correctly gain nothing.

### 4. No REST/frontend change for the "generic error on direct URL access" criterion

`assertPolicy(zaakRechten.lezen)` (and the equivalent for taak/document reads) already throws
`PolicyException`, mapped to HTTP 403, which the frontend already renders as the generic
`msg.error.server.forbidden` message. This is identical to what already happens today for, e.g., a wrong
zaaktype. No new code path is needed; only the OPA decision changes from `true` to `false` for the newly
denied cases.

## Risks / Trade-offs

- [Extra ZGW call per zaak/taak/document rechten evaluation] → `readZaakRechten`, `readTaakRechten`, and
  `readDocumentRechten` each gain one more `zrcClientService.listZaakeigenschappen` call (already paid by
  `RestZaakConverter` on the zaak-detail path, but new on the taak/document paths and on zaak actions other
  than the detail read). Mitigated by scope: this is the same trade-off the ticket already accepts for the
  zaak detail page today, and zaakeigenschappen lists are small; no caching is introduced in this change since
  the ticket does not call for a performance goal.
- [Werklijsten/zoekresultaten remain unrestricted] → explicitly accepted, tracked by follow-up PZ-11954; the
  spec deltas in this change note the boundary so a future contributor does not mistake the omission for a
  bug.
- [Gating `recordmanager`/`beheerder` too could lock out a real beheerder/recordmanager who is not also
  granted the flag] → mitigated for the reference PABC setup by Decision 3's `beheerder_elk_domein` mapping
  addition; for `recordmanager` no equivalent addition was made (not requested), so a `recordmanager` fixture
  without the flag is, correctly, denied on a zaakspecifiek geautoriseerde zaak under this design — this is
  the intended behaviour, not an oversight, but a production PABC rollout needs to consciously decide which
  recordmanager/beheerder functional roles should also carry the flag.
- [Renaming the PABC application role could desynchronise PABC and OPA if applied only on one side] →
  mitigated by making the rename part of this same change: the Rego role string and the PABC
  `applicationRoles[].name` value are updated together, and the itest suite (which authenticates as a real
  Keycloak/PABC user and inspects the resulting `application roles per zaaktype` log line) exercises the
  full round trip end to end.

## Migration Plan

No data migration. Rollout is a normal backend deploy: the new Rego rules, the renamed role, and the
`zaakspecifiekGeautoriseerd` input field ship together, are covered by `opa test` in CI, and take effect
immediately since OPA policies are deployed fresh on ZAC startup. The PABC seed data rename and mapping
additions only affect the Docker Compose / INFO test environment's PABC database import; a production PABC
environment would need the equivalent application role renamed, and would need to consciously decide which
recordmanager/beheerder functional roles should also carry the flag, as a separate, environment-specific
configuration step (out of scope for this code change). No existing zaken need be touched — a zaak only
becomes subject to the new restriction if it already carries the `ZAAK_GEAUTORISEERD` zaakeigenschap, which
today exists on exactly the one zaak created for INFO testomgeving testing (per the PZ-11909 Jira ticket)
and any Docker Compose test fixture zaak set up the same way.

## Open Questions

None — the acceptance criteria and existing groundwork fully determine the behaviour; the werklijsten/
zoekresultaten boundary is explicitly deferred by the ticket itself, not an open decision for this change.
