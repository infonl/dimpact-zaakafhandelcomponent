## Why

PZ-11909 introduces "zaakspecifieke autorisatie": a zaak can be marked as specifically authorized in
Open Zaak by giving it a `ZAAK_GEAUTORISEERD` zaakeigenschap with value `true` (see
[20260714 - zaakspecifieke autorisatie - voorstel voor oplossingsrichting #2](https://dimpact.atlassian.net/wiki/spaces/PZW/pages/1127252010)).
Before ZAC can enforce or display anything about this state, it first needs to know, per zaak,
whether that eigenschap is set. This change delivers that small, self-contained first step: expose
the fact as a new `RestZaak` field and surface it as a lock icon on the zaakdetailpagina, without yet
changing any authorization or access behavior.

## What Changes

- Add a new `isZaakspecifiekGeautoriseerd` boolean field to `RestZaak`.
- When converting a `Zaak` to a `RestZaak` (used by the `ZaakRestService` read endpoints
  `readZaak` and `readZaakById`), determine this field by reading the zaak's zaakeigenschappen from
  the ZGW ZRC API and checking whether one exists with `naam == "ZAAK_GEAUTORISEERD"` and
  `waarde == "true"`.
- On the zaakdetailpagina (`zaak-view` component) only, show a lock icon directly in front of the
  zaaknummer when `isZaakspecifiekGeautoriseerd` is `true`. When it is `false`, the row is unchanged
  from current behavior (no icon).
- No other pages, endpoints, or authorization behavior change in this step.

## Capabilities

### New Capabilities
- `zaakspecifiek-geautoriseerd-indicator`: exposes whether a zaak is zaakspecifiek geautoriseerd
  (based on its `ZAAK_GEAUTORISEERD` zaakeigenschap) on `RestZaak`, and shows a lock icon for it on
  the zaakdetailpagina.

### Modified Capabilities
(none)

## Impact

- Backend: `RestZaak` (`src/main/kotlin/nl/info/zac/app/zaak/model/RestZaak.kt`), `RestZaakConverter`
  (`src/main/kotlin/nl/info/zac/app/zaak/converter/RestZaakConverter.kt`), which is used by
  `ZaakRestService.readZaak` and `ZaakRestService.readZaakById`
  (`src/main/kotlin/nl/info/zac/app/zaak/ZaakRestService.kt`). Uses the existing
  `ZrcClientService.listZaakeigenschappen` ZGW ZRC API client call.
- Frontend: `zaak-view` component
  (`src/main/app/src/app/zaken/zaak-view/zaak-view.component.html`) and the generated
  `RestZaak` TypeScript type.
- No database, API contract removals, or authorization/policy changes.
