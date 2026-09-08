## Why

[PZ-10198](https://dimpact.atlassian.net/browse/PZ-10198) made a zaaktype markable as *zaakspecifiek
autoriseerbaar*, [PZ-11909](https://dimpact.atlassian.net/browse/PZ-11909) made the
`zaakspecifiek_geautoriseerd` application role gate single-zaak/taak/document access, and
[PZ-11954](https://dimpact.atlassian.net/browse/PZ-11954) extended that gate to werklijsten and
zoekresultaten. What none of them delivered is the ability to *activate* zaakspecifieke autorisatie on an
individual zaak from ZAC: today the only way is to add the `ZAAK_GEAUTORISEERD` zaakeigenschap by hand,
directly in Open Zaak. That is exactly what should never happen, because the business rules around this
flag (a zaak must have a behandelaar; only certain employees may set it) live in ZAC and are invisible to
the zaakregister.

[PZ-10200](https://dimpact.atlassian.net/browse/PZ-10200) closes that gap. It also introduces the first
*identity-based* exception to the access rule: from the moment a zaak is zaakspecifiek geautoriseerd, its
current behandelaar keeps access to it even without holding the `zaakspecifiek_geautoriseerd` role — which
in turn is why the zaak may no longer be released or reassigned while it is flagged.

## What Changes

- **Activation from the zaakgegevens edit form.** `PATCH /rest/zaken/zaak/{uuid}` accepts a new
  `isZaakspecifiekGeautoriseerd` field. When it flips from `false` to `true`, ZAC creates the
  `ZAAK_GEAUTORISEERD` zaakeigenschap (value `true`) on the zaak in Open Zaak, resolving the eigenschap URL
  from the zaaktype's own `ZAAK_GEAUTORISEERD` eigenschap.
- **Four preconditions, all enforced in the ZAC backend**, each with its own `ErrorCode` — the backend
  returns the code string only, and the frontend translates it via `nl.json`/`en.json`, as ZAC already does
  for every other error:
  1. the zaaktype must define the `ZAAK_GEAUTORISEERD` eigenschap (i.e. be zaakspecifiek autoriseerbaar);
  2. the zaak must have a behandelaar in the resulting state of the request;
  3. the acting employee must be the zaak's current behandelaar, or hold `zaakspecifiek_geautoriseerd` for
     the zaaktype;
  4. **BREAKING for the request contract**: sending `isZaakspecifiekGeautoriseerd = false` for a zaak that
     is already flagged is rejected. Deactivation is out of scope and arrives with
     [PZ-12022](https://dimpact.atlassian.net/browse/PZ-12022).
- **The current behandelaar keeps access without the flag.** `zaak_allowed`, `taak_allowed` and
  `document_allowed` in the OPA policies gain a second exception, driven by a new policy input carrying
  whether the logged-in user is the zaak's behandelaar. This is the first non-role-based term in these
  guards.
- **Worklists and search follow that exception.** The zaak's behandelaar is denormalized onto
  `ZaakZoekObject`, `TaakZoekObject` and `DocumentZoekObject` via a new Solr schema version, and
  `SearchService`'s zaakspecifiek-geautoriseerd filter query is widened so a flagged zaak, its taken and its
  documenten stay visible to their own behandelaar. Without this, an employee would lose their own zaak from
  "Mijn zaken" the moment they flagged it. As with `SolrSchemaV8`, the schema version deliberately triggers
  **no automated reindex**; the reindex is run manually per environment.
- **A zaak can no longer be released or reassigned while it is flagged.** Every entry point that touches the
  behandelaar rol rejects the attempt: `updateZaak` (the edit form carries `groep`/`behandelaar`),
  `assignZaak`, `assignZaakToLoggedInUser`, `assignZaakToLoggedInUserFromList`, and the two asynchronous
  batch operations `assignFromList` and `releaseZakenFromList`. Both restrictions are evaluated per request
  against the zaak's current marking, so lifting the marking in
  [PZ-12022](https://dimpact.atlassian.net/browse/PZ-12022) makes the zaak releasable and reassignable again;
  reassignment while flagged arrives with [PZ-10202](https://dimpact.atlassian.net/browse/PZ-10202).
- **The werkvoorraad dialogs say why a zaak was left out.** Distributing or releasing a selection that
  contains flagged zaken reports them separately from zaken skipped because they are already afgehandeld,
  rather than as one combined message. Today neither reason surfaces at all — nothing in the frontend
  inspects the `SKIPPED` websocket opcode — so this adds reason-aware feedback where there was none.
- **Recordmanagers and beheerders get no hard-coded exemption.** Per
  [PZ-10203](https://dimpact.atlassian.net/browse/PZ-10203)'s own conclusion, their access to flagged zaken
  is arranged by granting them `zaakspecifiek_geautoriseerd` in PABC, exactly like any other employee. This
  change updates the Docker Compose and PABC seed data accordingly rather than adding a policy branch.
- **No new visual indicator.** [PZ-10201](https://dimpact.atlassian.net/browse/PZ-10201)'s lock icon next to
  the zaaknummer already shipped with `2026-08-20-add-zaakspecifiek-geautoriseerd-indicator`; PZ-10200's
  corresponding acceptance criterion is met by existing behaviour.

## Capabilities

### New Capabilities
- `zaakspecifieke-autorisatie-activatie`: activating zaakspecifieke autorisatie on an individual zaak from
  the zaakgegevens edit form — the preconditions, the irreversibility within this change, and the
  restrictions that activation places on releasing and reassigning the zaak.

### Modified Capabilities
- `zaakspecifieke-autorisatie-toegang`: the access guard is no longer purely role-based. The current
  behandelaar of a zaakspecifiek geautoriseerde zaak retains their application role's rights on that zaak,
  and on its taken and documenten, without holding `zaakspecifiek_geautoriseerd`.
- `zaakspecifiek-geautoriseerde-zoekindex`: werklijst and zoekresultaat queries no longer exclude a flagged
  zaak (or its taken and documenten) from its own behandelaar, and the search index therefore records each
  row's zaak-behandelaar.

## Impact

- `src/main/kotlin/nl/info/zac/app/zaak/ZaakRestService.kt` — `updateZaak` gains the activation branch and
  its precondition checks.
- `src/main/kotlin/nl/info/zac/app/zaak/model/{RestZaakCreateData,RestZaak}.kt` — the new request field.
- `src/main/kotlin/nl/info/zac/app/zaak/exception/` + `src/main/kotlin/nl/info/zac/exception/ErrorCode.kt`
  + `src/main/app/src/assets/i18n/{nl,en}.json` — new error codes and messages.
- `src/main/kotlin/nl/info/zac/app/zaak/ZaakAssignAndReleaseRestService.kt` and
  `src/main/kotlin/nl/info/zac/zaak/ZaakService.kt` — assignment and release restrictions, including the
  asynchronous batch paths.
- `src/main/resources/policies/{zaak,taak,document}-rechten.rego` and
  `src/main/kotlin/nl/info/zac/policy/input/{ZaakData,TaakData,DocumentData}.kt` +
  `src/main/kotlin/nl/info/zac/policy/PolicyService.kt` — the behandelaar exception.
- `src/main/kotlin/nl/info/zac/search/model/zoekobject/*.kt`,
  `src/main/kotlin/nl/info/zac/search/converter/*.kt`, `src/main/kotlin/nl/info/zac/solr/schema/` (new
  `SolrSchemaV9`), `src/main/kotlin/nl/info/zac/search/SearchService.kt` — the zaak-behandelaar field and
  the widened filter query.
- `src/main/app/src/app/zaken/zaak-details-wijzigen/` — the activation control and the Figma-designed
  confirmation and refusal dialogs; `src/main/app/src/app/zaken/zaken-vrijgeven-dialog/` and
  `zaken-verdelen-dialog/` for the werkvoorraad refusal.
- `scripts/docker-compose/imports/` (PABC mapping, Keycloak realm, Open Zaak seed SQL) — recordmanager and
  beheerder mappings for `zaakspecifiek_geautoriseerd`.
- `docs/solution-architecture/accessControlPolicies.md` — documents the behandelaar exception alongside the
  existing flag description.
- `src/itest/kotlin/nl/info/zac/itest/` — integration tests, extending the existing
  `*ZaakspecifiekAutorisatie*Test.kt` family.
