## Context

The zaakdetailpagina shows a lock icon in front of the zaaknummer when `RestZaak.isZaakspecifiekGeautoriseerd` is
`true` (`zaak-details-card.component.html`, styled by `.zaakspecifiek-geautoriseerd-icon` in
`zaak-details-card.component.less`). No other place shows it.

Which DTO feeds each place, and whether it carries the flag today:

| Place | DTO | Flag today |
|---|---|---|
| Zoekresultaten: zaak; zaken werkvoorraad, mijn zaken, afgehandelde zaken; zaak zoekopdracht card; persoon/bedrijf/BAG zaken tables | `RestZaakZoekObject` | yes |
| Zoekresultaten: taak; taken werkvoorraad, mijn taken; taak zoekopdracht card | `RestTaakZoekObject` | no (index has it) |
| Zoekresultaten: document | `RestDocumentZoekObject` | no (index has it) |
| Zaak koppelen; document koppelen aan zaak | `RestZaakKoppelenZoekObject` (built from `RestZaakZoekObject`) | no |
| Dashboard: zaken op mijn naam, zaken met recent toegevoegde documenten (signaleringen), binnenkort verlopende zaken | `RestZaakOverzicht` (`convertForDisplay` resp. `convert`) | no |
| Dashboard: taken op mijn naam (signaleringen) | `RestSignaleringTaskSummary` (from Flowable task variables) | no |
| Taak detailpagina, zaak card (`zaak-verkort`) | `RestZaak` | yes |
| Document detailpagina, zaak cards | `RestZaakInformatieobject` | no |
| Zaakdetailpagina, gerelateerde zaken tab | `RestGerelateerdeZaak` | no |
| Zaakdetailpagina, zaak column of the documenten table | `RestGekoppeldeZaakEnkelvoudigInformatieObject` | no |
| Ontkoppelde documenten, zaak column (the zaak the document was unlinked from) | `RestDetachedDocument` (zaaknummer stored as text in the ZAC database) | no |

The Solr documents `ZaakZoekObject`, `TaakZoekObject` and `DocumentZoekObject` already store `isZaakspecifiekGeautoriseerd`
(capability `zaakspecifiek-geautoriseerde-zoekindex`). `PolicyService.readZaakRechten` already reads a
`ZaakAutorisatieGegevens` (which holds `isZaakspecifiekGeautoriseerd`) when the caller does not pass one in.

## Goals / Non-Goals

**Goals:**
- One shared lock component, used in every place this change lists, and on the zaakdetailpagina.
- Every DTO behind those places carries `isZaakspecifiekGeautoriseerd`.
- No extra Open Zaak call where the value is already at hand.

**Non-Goals:**
- A page title with the zaaknummer on the zaakdetailpagina (the Figma frame shows one; ZAC does not have it).
- A new or different tooltip or screen-reader text; the existing title text is reused as is.
- The header of the procesverloop side panel on the zaakdetailpagina: it belongs to the zaak that is already open,
  whose zaak details card shows the lock.
- A zaaknummer inside running text, such as the explanation "Koppel zaak 'X' aan een andere zaak"; the Figma design
  shows no lock there.
- The gebruikershandleiding; that follows in a separate pull request.
- Showing the lock depending on the user's rights on the zaak; a separate story will cover that. Until then the lock
  shows whenever the zaak is zaakspecifiek geautoriseerd.
- Places outside the UI that show a zaaknummer, such as the CSV export, e-mails and generated documents.
- Changing the lock on the zaakdetailpagina or the existing `zaakspecifiek-geautoriseerd-indicator` spec.

## Decisions

### 1. Shared component with a single boolean input
A standalone component `ZaakspecifiekGeautoriseerdIconComponent` (selector `zac-zaakspecifiek-geautoriseerd-icon`)
in `src/main/app/src/app/shared/zaakspecifiek-geautoriseerd-icon/`, with one required signal input
`isZaakspecifiekGeautoriseerd: boolean | undefined`. The generated frontend types make every backend boolean optional
(also `RestZaak.isZaakspecifiekGeautoriseerd`), so the input accepts `undefined`, which shows no lock, like `false`. Its template is the current markup of the zaakdetailpagina, with the input
in place of `zaak().isZaakspecifiekGeautoriseerd`:

```html
@if (isZaakspecifiekGeautoriseerd()) {
  <mat-icon
    title="{{ 'zaak.is-zaakspecifiek-geautoriseerd' | translate }}"
    class="zaakspecifiek-geautoriseerd-icon"
    >lock</mat-icon
  >
}
```

The `.zaakspecifiek-geautoriseerd-icon` style moves from `zaak-details-card.component.less` to the component.
`zaak-details-card` uses the component, so there is one lock in the code base.

*Alternative*: input the whole zaak object. Rejected: the places use five different object types, and a boolean
keeps the component independent of all of them.

### 2. Placement in front of the zaaknummer
- **Table cells** (werklijsten, dashboard cards, persoon/bedrijf/BAG tables, koppelen results): the component goes
  inside the `<td>`, directly before the zaaknummer interpolation.
- **Search results with `zac-static-text`** (taak and document results, *hoort bij zaak*): the component is
  projected into the existing `position="content-before"` slot of `zac-static-text`, so `zac-static-text` itself
  does not change.
- **Zaak search result**: the zaaknummer is the link text in `zoek-object-link`; the component goes in front of it
  for zaak results only.
- **Card titles** (`zaak-verkort` on the taak detailpagina, the zaak cards on the document detailpagina): the
  component goes inside `mat-card-title`, directly before the zaaknummer, as on the zaakdetailpagina.

### 3. Taak and document zoek objects take the flag from the index
`toRestTaakZoekObject` and `toRestDocumentZoekObject` copy `isZaakspecifiekGeautoriseerd` from the Solr object. No
call to Open Zaak. The hand-written frontend types `TaakZoekObject` and `DocumentZoekObject` get the field.

### 4. `RestZaakKoppelenZoekObject` copies it from the zaak zoek object
`RestZaakKoppelenZoekObject` is built in two places, and both copy the flag: `toRestZaakKoppelenZoekObject` maps from
`RestZaakZoekObject` (document koppelen), and `ZaakKoppelenRestService` maps from the indexed `ZaakZoekObject`
(zaak koppelen).

### 5. Converters that already read the zaakrechten read the autorisatie gegevens once
`RestZaakOverzichtConverter.convert` and `convertForDisplay`,
`EnkelvoudigInformatieObjectRestService.toRestZaakInformatieobject` and `RestGerelateerdeZaakConverter.convert`
read the `ZaakAutorisatieGegevens` through
`ZaakspecifiekeAutorisatieService.readZaakAutorisatieGegevens`, pass it to `PolicyService.readZaakRechten` (which
otherwise reads it itself) and take `isZaakspecifiekGeautoriseerd` from it. That avoids a second
`listZaakeigenschappen` call per zaak.

For the documenten table, `EnkelvoudigInformatieObjectRestService` lists the documents per zaak (the zaak itself and
its gerelateerde zaken). It reads the `ZaakAutorisatieGegevens` once per gekoppelde zaak and passes its flag to
`RestInformatieobjectConverter`, which sets it on each `RestGekoppeldeZaakEnkelvoudigInformatieObject` of that zaak.

The field is set regardless of `canLezen`, like `identificatie`: it marks the zaaknummer that is already shown.
*Alternative*: hide it without `canLezen`. Rejected: the werklijsten and dashboard only list zaken the user may
read, and a lock that comes and goes per right would be inconsistent with the zoek objects, which always carry it.

### 6. Lists without the flag at hand look it up in the search index, in one query
Two lists do not have the zaak at hand: the *taken op mijn naam* dashboard card (`RestSignaleringTaskSummary`, built
from Flowable task variables) and the ontkoppelde documenten list (`RestDetachedDocument`, whose zaaknummer is plain
text in the ZAC database). Both read the flag from the Solr index with one query per request:

- A new `SearchService` function returns, out of a given set of values of one index field and one object type, the
  values whose object is zaakspecifiek geautoriseerd (`zaakspecifiek_geautoriseerd:true`).
- `SignaleringService.listTakenSignaleringenPage` passes the task IDs of the listed taken (type `TAAK`, field `id`)
  and gives each `toRestSignaleringTaakSummary` its flag.
- `DetachedDocumentRestService` passes the zaaknummers of the page (type `ZAAK`, field `zaak_identificatie`) and
  gives `RestDetachedDocumentConverter` the flag per row. A zaak that no longer exists is not in the index and gets
  `false`.

The lookup returns only the flag, never content, so it does not apply the user-specific filters of `search` (the
allowed zaaktypen and the zaakspecifieke-autorisatie filter). Applying them would make the flag `false` for exactly
the zaken the user cannot open, which would hide the lock where it is most informative.

*Alternatives*:
- One `ZrcClientService.isZaakspecifiekGeautoriseerd` call per distinct zaak. Rejected: the taken card loads all
  taken at once without paging, so the number of sequential Open Zaak calls grows with the user's taken; the
  ontkoppelde documenten would also need a lookup of the zaak by zaaknummer first, so two calls per row.
- Storing the flag as a Flowable task variable or in the `detached_document` table. Rejected: every marking of a zaak
  would have to update it, in yet another place.

## Risks / Trade-offs

- [The index lookup of decision 6 bypasses the user-specific search filters] → It returns only which of the given
  values are zaakspecifiek geautoriseerd, never other data, and only for task IDs or zaaknummers the list already
  shows.
- [Werklijsten and zoekresultaten show the flag from the index, which can lag behind Open Zaak] → Same freshness as
  the inclusion rules that already use this field; activating zaakspecifieke autorisatie reindexes the zaak, its
  taken and documenten.
- [Many templates change, some of them also touched by the open breadcrumbs PR #7411 (in `.ts` files only)] → The
  changes here are mostly in `.html`; at most an `imports` array conflict to resolve when merging.
