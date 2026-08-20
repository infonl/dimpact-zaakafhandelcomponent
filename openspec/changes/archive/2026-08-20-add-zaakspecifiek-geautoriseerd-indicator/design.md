## Context

`ZaakRestService.readZaak` and `.readZaakById` both delegate to `RestZaakConverter.toRestZaak`,
which is the single place that assembles a `RestZaak` from a ZGW `Zaak`, its `ZaakType`, rights, and
the logged-in user. All other derived booleans on `RestZaak` (`isDeelzaak`, `isHeropend`,
`isOpgeschort`, etc.) are computed there, not in `ZaakRestService` itself. `ZrcClientService`
already exposes `listZaakeigenschappen(zaakUUID): List<ZaakEigenschap>`, where `ZaakEigenschap` has
`naam` and `waarde` string fields — this is the mechanism the wiki page describes for detecting
zaakspecifieke autorisatie ("ZAAK_GEAUTORISEERD" / "true").

See proposal.md - Why for the PZ-11909 background.

## Goals / Non-Goals

**Goals:**
- Determine `isZaakspecifiekGeautoriseerd` for a `RestZaak` from the zaak's zaakeigenschappen.
- Show the indicator as a lock icon on the zaakdetailpagina only.

**Non-Goals:**
- No authorization/access enforcement based on this field (follow-up story).
- No UI for setting/toggling `ZAAK_GEAUTORISEERD` from ZAC (follow-up story, per the wiki page).
- No changes to werklijsten, search results, or any page other than the zaakdetailpagina.
- No changes to the `zaaktype` eigenschap-specificatie check (whether the zaaktype even supports
  zaakspecifieke autorisatie) — this step only reads the zaak-level eigenschap.

## Decisions

- **Compute the field in `RestZaakConverter.toRestZaak`, not in `ZaakRestService`.** This mirrors
  every other derived `RestZaak` boolean and keeps `ZaakRestService` a thin REST layer; the ticket's
  phrase "extend the read functions" is satisfied because both `readZaak` and `readZaakById` return
  the new field automatically through the shared converter, with no separate logic to keep in sync.
- **Fetch zaakeigenschappen via the existing `zrcClientService.listZaakeigenschappen(zaak.uuid)`.**
  No new ZGW client call is needed. Alternative considered: extending `ZaakEigenschap`-specific
  caching — rejected as unnecessary for a single small lookup already used elsewhere in the
  converter's call graph (roles, besluiten, etc. are all fetched per-read the same way).
- **Match by exact `naam == "ZAAK_GEAUTORISEERD"` and exact `waarde == "true"`.** The wiki page
  states both the name and the value "must be exactly this text", so no case-insensitive or
  `toBoolean()`-style parsing is used — an unexpected value (e.g. `"True"`, `"1"`) is treated as not
  authorized, consistent with the source of truth being Open Zaak configuration that ZAC itself
  writes as `"true"`.
- **Lock icon placement**: add a `mat-icon` named `lock` immediately before `{{ zaak.identificatie
  }}` inside the existing `mat-card-title` in `zaak-view.component.html`, conditioned on
  `zaak.isZaakspecifiekGeautoriseerd`. This reuses the Material `lock` icon already used elsewhere in
  the app (e.g. for vergrendelde documenten) rather than introducing a new icon or component.

## Risks / Trade-offs

- [Extra ZGW API call per zaak read] → Already within the pattern of `RestZaakConverter`, which
  makes several such calls per read; no batching exists today for any of them, so this is consistent
  with current performance characteristics rather than a regression.
- [`naam`/`waarde` matching is exact-string and silently treats malformed data as "not authorized"]
  → Matches the wiki page's explicit "must be exactly this text" specification; safer default is to
  treat anything else as not authorized.

## Migration Plan

Additive change: new field on `RestZaak` (with generated TypeScript type update) and a new icon on
one page. No data migration, no endpoint or contract removal. Deployable and revertible
independently.
