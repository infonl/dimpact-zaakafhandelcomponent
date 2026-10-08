## Why

A zaakspecifiek geautoriseerde zaak can be reached by employees holding `zaakspecifiek_geautoriseerd` for the
zaaktype, by its zaakbehandelaar, and by medewerkers holding the *Zaakspecifiek geautoriseerde medewerker*
rol: previous zaakbehandelaars ([PZ-10202](https://dimpact.atlassian.net/browse/PZ-10202)) and current and
previous taakbehandelaars ([PZ-12506](https://dimpact.atlassian.net/browse/PZ-12506)).

There is no way to give anyone else access. [PZ-12023](https://dimpact.atlassian.net/browse/PZ-12023) lets
an employee with edit rights on such a zaak add extra medewerkers by hand.

## What Changes

- **A "Medewerker toevoegen" koppeling** in the zaak side menu, only on a zaakspecifiek geautoriseerde zaak
  and only for an employee with `wijzigen` on it. It opens a side panel where the employee first picks a
  groep with the `behandelaar` application role for the zaaktype, then a medewerker of that groep.
- **The medewerker list leaves out who already has access**: the zaakbehandelaar, holders of the
  *Zaakspecifiek geautoriseerde medewerker* rol, and members of a groep with `zaakspecifiek_geautoriseerd`
  for the zaaktype. It is sorted by name, without paging or search.
- **Adding stores the existing rol.** ZAC adds a *Zaakspecifiek geautoriseerde medewerker* rol to the zaak in
  Open Zaak, identical to the one previous zaak- and taakbehandelaars get. The added medewerker can use their
  own application role on the zaak, can add others in turn, and finds the zaak in werkvoorraden and
  zoekresultaten.
- **The zaakhistorie shows the addition**; the betrokkenen tab does not show the medewerker.

## Capabilities

### New Capabilities

- `zaakspecifieke-autorisatie-extra-medewerkers`: manually adding a medewerker to a zaakspecifiek
  geautoriseerde zaak.

### Modified Capabilities

None. The rol already feeds the OPA policies (`zaakspecifieke-autorisatie-toegang`) and the search index
(`zaakspecifiek-geautoriseerde-zoekindex`).

## Impact

- **Backend**: new functions in `ZaakspecifiekeAutorisatieService`, a new `ZaakspecifiekeAutorisatieRestService`
  with two endpoints, three error codes.
- **Frontend**: a new side menu item and side panel in the zaak view, i18n texts.
- **Solr, OPA, Open Zaak catalogus**: no change.
- **Out of scope**:
  - the list of medewerkers with access on the zaakdetailpagina
    ([PZ-12079](https://dimpact.atlassian.net/browse/PZ-12079));
  - removing a medewerker's access ([PZ-12046](https://dimpact.atlassian.net/browse/PZ-12046)).
