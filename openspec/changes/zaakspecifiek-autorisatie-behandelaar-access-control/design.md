## Context

PZ-11909 introduced two pieces of groundwork with no enforcement behind them yet:
- The `zaakspecifiek_autorisatie_behandelaar` ZAC application role (PABC/Keycloak, commit `5172e956b`
  / `e98acd177`), granted per zaaktype, deliberately shipped with no permissions attached.
- The `ZAAK_GEAUTORISEERD` zaakeigenschap convention: an Open Zaak zaakeigenschap named exactly
  `ZAAK_GEAUTORISEERD` with value `"true"` marks an individual zaak as "zaakspecifiek geautoriseerd".
  `RestZaakConverter.toRestZaak` (`src/main/kotlin/nl/info/zac/app/zaak/converter/RestZaakConverter.kt:71-73,114-116`)
  already reads this via `zrcClientService.listZaakeigenschappen(zaak.uuid)` to compute the purely
  informational `RestZaak.isZaakspecifiekGeautoriseerd` field (lock icon in the frontend).

Access control in ZAC is enforced in OPA (Rego), not in Kotlin: every zaak/taak/document REST read or
mutation builds a `ZaakInput`/`TaakInput`/`DocumentInput` (`nl.info.zac.policy.input`), sends it to OPA's
`zaak_rechten`/`taak_rechten`/`document_rechten` rule sets (`src/main/resources/policies/*.rego`), and
`assertPolicy(...)` (`PolicyService.kt:255-259`) throws `PolicyException` → HTTP 403 when a right is `false`.
The three policy files each follow the same shape: a `zaaktype_allowed` gate, then one rule per permission of
the form `some role in {…}; role.rol in user.rollen` (optionally combined with zaak/taak/document state such
as `zaak.open`). A companion capability, `application-role-permission-matrix`, already established the
project's convention that every role's grants must be explicit — no role may rely on also holding a "lower"
role — which this change follows for the new role too.

None of `ZaakData`, `TaakData`, or `DocumentData` currently carry any "is this zaakspecifiek geautoriseerd"
signal, so today OPA cannot distinguish a zaakspecifiek geautoriseerde zaak from any other zaak of the same
zaaktype: any behandelaar/raadpleger/coordinator with zaaktype access already has full access to it, its
taken, and its documenten.

## Goals / Non-Goals

**Goals:**
- Only users holding `zaakspecifiek_autorisatie_behandelaar` for the zaak's zaaktype can read or act on a
  zaakspecifiek geautoriseerde zaak, its taken, and its documenten, among the roles this change touches.
- Any application role of that same zaaktype, without also holding `zaakspecifiek_autorisatie_behandelaar`,
  is denied — the restriction is not specific to `behandelaar`, `raadpleger`, or `coordinator` individually,
  it applies uniformly to whichever of those roles a user happens to hold — including via direct URL /
  deep-link access to the zaak, a taak, or a document.
- `zaakspecifiek_autorisatie_behandelaar`'s own effective rights, once unlocked, equal `behandelaar`'s rights
  (which are a superset of `raadpleger`'s), granted explicitly per the no-hierarchy convention.
- A denied user sees the same generic "onvoldoende rechten" message ZAC already shows for any other policy
  denial — no new user-facing error path.

**Non-Goals:**
- `recordmanager` and `beheerder` access to a zaakspecifiek geautoriseerde zaak is explicitly out of scope for
  this change. Both roles already have unconditional access to every zaak/taak/document today; this change
  does not add, remove, or formally specify/test that access for the zaakspecifiek geautoriseerde case — it is
  left as-is, with the actual restriction/authorisation design for these two roles deferred to a follow-up
  story.
- Werklijsten and Solr-backed zoekresultaten (`ZaakZoekObject`/`TaakZoekObject`/`DocumentZoekObject` rechten
  lookups) are explicitly out of scope — the ticket defers this to follow-up PZ-11954. Those call sites keep
  `geautoriseerd` defaulting to "not geautoriseerd", i.e. unrestricted, for now.
- Being able to mark a zaak as zaakspecifiek geautoriseerd from within ZAC (currently only possible directly
  in Open Zaak) is a separate follow-up story, not part of this change.
- No changes to Keycloak/PABC configuration or to how `LoggedInUser.applicationRolesPerZaaktype` is populated
  — that plumbing already exists and already produces the role correctly.

## Decisions

### 1. Add one `geautoriseerd` boolean field per policy input, sourced from the existing zaakeigenschap check

Add `geautoriseerd: Boolean` to `ZaakData`, `TaakData`, and `DocumentData` (`nl.info.zac.policy.input`).
`PolicyService` populates it for the three call sites that back an actual single-resource read/mutation:
- `readZaakRechten(zaak, zaaktype, loggedInUser)` — looks up the zaak's own zaakeigenschappen.
- `readTaakRechten(taskInfo, zaaktypeOmschrijving)` — resolves the zaak UUID via
  `TaakVariabelenService.readZaakUUID(taskInfo)` (already used elsewhere for other purposes), then the same
  zaakeigenschap lookup.
- `readDocumentRechten(enkelvoudigInformatieobject, lock, zaak)` — uses the already-passed `zaak: Zaak?`
  parameter (`false`/absent when `zaak` is `null`, i.e. a document not linked to any zaak — unchanged from
  today).

The two search-object-based overloads (`readZaakRechtenForZaakZoekObject`, `readTaakRechten(taakZoekObject)`,
`readDocumentRechten(enkelvoudigInformatieobject: DocumentZoekObject)`) leave `geautoriseerd` at its default
`false`, consistent with the Non-Goals above.

**Alternative considered**: index "is zaakspecifiek geautoriseerd" into Solr and thread it through the
`*ZoekObject` classes now, so all six call sites are covered symmetrically. Rejected for this change because
it would pull werklijsten/zoekresultaten scope into this story, which the ticket explicitly defers to
PZ-11954; doing it now also means a Solr reindex, which the ticket does not ask for.

**Shared lookup helper**: extract the zaakeigenschap check duplicated between `RestZaakConverter` and
`PolicyService` into one shared function next to the existing `ZAAKEIGENSCHAP_NAAM_GEAUTORISEERD` /
`ZAAKEIGENSCHAP_WAARDE_GEAUTORISEERD` constants (e.g. a small function on `ZrcClientService` or a top-level
helper in the `nl.info.zac.policy` or `nl.info.zac.app.zaak` package), so the "what counts as zaakspecifiek
geautoriseerd" rule is defined exactly once.

### 2. Gate only the non-privileged rule bodies with one shared `zaakspecifiek_toegankelijk` rule; leave `recordmanager`/`beheerder` bodies untouched

In each of `zaak-rechten.rego`, `taak-rechten.rego`, `document-rechten.rego`, add:
```rego
default zaakspecifiek_toegankelijk := false
zaakspecifiek_toegankelijk if {
    not zaak.geautoriseerd   # or taak.geautoriseerd / document.geautoriseerd
}
zaakspecifiek_toegankelijk if {
    zaakspecifiekAutorisatieBehandelaar.rol in user.rollen
}
```
Per the scope decision above, this gate is added only to the rule body/bodies that grant `raadpleger`,
`behandelaar`, and/or `coordinator` a permission. The separate rule bodies that already grant
`recordmanager`/`beheerder` a permission unconditionally are left completely untouched — no gate, no new
role reference, no behaviour change.

Where a permission already has two separate `if` bodies today (e.g. `wijzigen`: one body for
`{behandelaar, coordinator}` + `zaak.open`, a second body for `{recordmanager, beheerder}` unconditionally),
only the first body is touched:
```rego
default wijzigen := false
wijzigen if {
    zaaktype_allowed
    zaak.open
    zaakspecifiek_toegankelijk
    some role in {behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
wijzigen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}
```
Where a permission today has a single body combining every role (e.g. `lezen`:
`{raadpleger, behandelaar, coordinator, recordmanager, beheerder}`), that body is split in two so the gate
can be added to the non-privileged half only:
```rego
default lezen := false
lezen if {
    zaaktype_allowed
    zaakspecifiek_toegankelijk
    some role in {raadpleger, behandelaar, coordinator, zaakspecifiekAutorisatieBehandelaar}
    role.rol in user.rollen
}
lezen if {
    zaaktype_allowed
    some role in {recordmanager, beheerder}
    role.rol in user.rollen
}
```
Rules whose only roles are `recordmanager`/`beheerder` (`heropenen`, `bekijken_zaakdata`, `brondatum_zetten`)
are not touched at all.

**Alternative considered**: gate every rule uniformly, including the `recordmanager`/`beheerder` bodies (with
those two roles added to the gate's allow-list). Rejected per explicit scope direction: this change is not to
specify or test `recordmanager`/`beheerder` behaviour for the zaakspecifiek geautoriseerde case at all — that
is a follow-up story's concern. Leaving their rule bodies untouched keeps this change's diff scoped exactly to
the `zaakspecifiek_autorisatie_behandelaar` role and avoids asserting anything (via new code or new tests)
about the other two roles' interaction with this feature.

**Alternative considered**: only add the gate to `lezen`/`behandelen` (the two rights explicitly called out
in the acceptance criteria). Rejected: the ticket is explicit that unauthorised employees may not see or
treat any right on such a zaak/taak/document ("mogen geen … kunnen zien of behandelen"), and leaving e.g.
`wijzigen` or `toevoegen_document` ungated would let a plain behandelaar mutate a zaak they cannot even read.
Gating every non-privileged rule body is also simpler to reason about and test than tracking which rules are
gated.

### 3. Grant `zaakspecifiek_autorisatie_behandelaar` behandelaar-equivalent rights explicitly

Wherever `behandelaar` appears in a role set in the three files, add `zaakspecifiekAutorisatieBehandelaar`
alongside it (import it from `rollen.rego`, where the constant already exists but is currently unused). This
directly implements the acceptance criterion that this role's rights equal behandelaar's (a superset of
raadpleger's), granted explicitly rather than by relying on the user also separately holding `behandelaar` —
consistent with the `application-role-permission-matrix` capability's existing no-hierarchy principle.

Combined with Decision 2: a plain `behandelaar` (without the new role) fails `zaakspecifiek_toegankelijk` for
a geautoriseerde zaak and is denied everywhere; a user who additionally holds
`zaakspecifiek_autorisatie_behandelaar` both passes the gate and is present in every role set `behandelaar`
is present in, so their effective rights on a geautoriseerde zaak equal a normal behandelaar's rights on a
non-geautoriseerde zaak — matching the acceptance criteria exactly, and with no different behaviour on
non-geautoriseerde zaken of that zaaktype (`zaakspecifiek_toegankelijk` is trivially true there).

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
- [`recordmanager`/`beheerder` access to a zaakspecifiek geautoriseerde zaak is left unspecified by this
  change] → explicitly accepted per scope direction; a follow-up story is expected to formally
  specify/restrict (or confirm) that access. Since this change does not touch their rule bodies at all, their
  current unconditional access is preserved by construction, not by an assertion this change makes.
- [Splitting combined-role rule bodies touches most rules in the three files] → larger diff than a minimal
  `lezen`/`behandelen`-only change, but the Rego and Kotlin unit tests added by this change (one per
  permission per file, mirroring the existing `application-role-permission-matrix` test style) catch any rule
  that was missed, mis-split, or mis-gated.

## Migration Plan

No data migration. Rollout is a normal backend deploy: the new Rego rules and `geautoriseerd` input field
ship together, are covered by `opa test` in CI, and take effect immediately since OPA policies are deployed
fresh on ZAC startup. No existing zaken need be touched — a zaak only becomes subject to the new restriction
if it already carries the `ZAAK_GEAUTORISEERD` zaakeigenschap, which today exists on exactly the one zaak
created for INFO testomgeving testing (per the PZ-11909 Jira ticket) and any Docker Compose test fixture zaak
set up the same way.

## Open Questions

None — the acceptance criteria and existing groundwork fully determine the behaviour; the werklijsten/
zoekresultaten boundary is explicitly deferred by the ticket itself, not an open decision for this change.
