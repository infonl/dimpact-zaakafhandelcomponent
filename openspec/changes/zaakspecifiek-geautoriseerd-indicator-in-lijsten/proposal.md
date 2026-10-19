## Why

Only the zaakdetailpagina shows that a zaak is zaakspecifiek geautoriseerd, with a lock icon in front of the
zaaknummer. Everywhere else a zaaknummer appears (zoekresultaten, werklijsten, dashboard cards, the taak and document
detail pages, the persoon, bedrijf and BAG pages, and the zaak koppelen screen) an employee cannot see it.
[PZ-12078](https://dimpact.atlassian.net/browse/PZ-12078) asks for the same indication in all those places, so an
employee sees at a glance which zaken are zaakspecifiek geautoriseerd. The Figma design (section *Slotjes*) places
the lock directly in front of the zaaknummer.

## What Changes

- **One shared lock component** in the frontend. It takes a single input, the `isZaakspecifiekGeautoriseerd` flag,
  and renders the lock icon that the zaakdetailpagina shows today, with the same title text. The zaakdetailpagina
  uses the new component instead of its inline markup, without a visible change.
- **The lock appears directly in front of the zaaknummer** wherever a zaaknummer of a zaakspecifiek geautoriseerde
  zaak is listed or shown:
  - zoekresultaten for zaken, and the *hoort bij zaak* zaaknummer of taken and documenten;
  - the werklijsten: zaken werkvoorraad, mijn zaken, afgehandelde zaken, taken werkvoorraad and mijn taken;
  - the dashboard cards that list zaken or taken;
  - the zaak card on the taak detailpagina and on the document detailpagina;
  - the zaken tables on the persoon, bedrijf and BAG pages;
  - the result list of the zaak koppelen screen, and of the screen that links a document to a zaak;
  - on the zaakdetailpagina: the gerelateerde zaken tab and the zaak column of the documenten table;
  - the zaak column of the ontkoppelde documenten list.
  Where Figma has no design for a place, the lock follows the same pattern: directly in front of the zaaknummer.
- **The backend exposes the flag on every DTO behind these places** that does not have it yet:
  `RestTaakZoekObject`, `RestDocumentZoekObject`, `RestZaakKoppelenZoekObject`, `RestZaakOverzicht`,
  `RestSignaleringTaskSummary`, `RestZaakInformatieobject`, `RestGerelateerdeZaak`,
  `RestGekoppeldeZaakEnkelvoudigInformatieObject` and `RestDetachedDocument`. `RestZaak` and `RestZaakZoekObject` already have it.
  The field is additive; no existing field changes.

## Capabilities

### New Capabilities

- `zaakspecifiek-geautoriseerd-indicator-in-lijsten`: showing the lock icon in front of the zaaknummer of a
  zaakspecifiek geautoriseerde zaak in every list and detail card other than the zaak details card of the
  zaakdetailpagina, and the REST fields that carry the flag there.

### Modified Capabilities

None. The lock on the zaakdetailpagina (`zaakspecifiek-geautoriseerd-indicator`) keeps its behaviour; this change
only adds places. The search index already records the flag for zaken, taken and documenten
(`zaakspecifiek-geautoriseerde-zoekindex`).

## Impact

- **Backend**: the nine DTOs above and their converters (`RestTaakZoekObject`, `RestDocumentZoekObject`,
  `RestZaakKoppelenZoekObject` conversion functions, `RestZaakOverzichtConverter`, `SignaleringTaskConverters`,
  `EnkelvoudigInformatieObjectRestService`, `RestGerelateerdeZaakConverter`, `RestInformatieobjectConverter`, `RestDetachedDocumentConverter`), and a
  `SearchService` lookup of the flag in the index for the taken signaleringen and the ontkoppelde documenten.
  The generated OpenAPI spec and frontend types change additively.
- **Frontend**: a new shared lock component; templates of the zoek-object components, werklijsten, dashboard cards,
  `zaak-verkort`, `informatie-object-view`, `klant-zaken-tabel`, `bag-zaken-tabel`, `zaak-link`,
  `informatie-object-link`, `zaak-details-card`, `zaak-details-gerelateerde-zaken-tab`, `zaak-documenten` and
  `ontkoppelde-documenten-list`; the
  hand-written `TaakZoekObject` and `DocumentZoekObject` types.
- **No new i18n keys**: the component reuses `zaak.is-zaakspecifiek-geautoriseerd`.
- **Gebruikershandleiding**: explaining the lock in the manual follows in a separate pull request.
- **Related open PRs**: the PZ-12046 stack (#7412, #7413, #7414) and #7411 touch neighbouring code only; no
  functional overlap.
