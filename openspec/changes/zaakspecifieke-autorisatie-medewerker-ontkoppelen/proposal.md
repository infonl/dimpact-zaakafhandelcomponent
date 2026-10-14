## Why

[PZ-12023](https://dimpact.atlassian.net/browse/PZ-12023) lets an employee give a medewerker access to a
zaakspecifiek geautoriseerde zaak by hand. Previous zaak- and taakbehandelaars get the same access
automatically. There is no way to take that access away again, for example when the wrong medewerker was
added. [PZ-12046](https://dimpact.atlassian.net/browse/PZ-12046) adds it.

## What Changes

- **A "Medewerker ontkoppelen" action** per medewerker in the *Medewerkers* tab of a zaakspecifiek
  geautoriseerde zaak, only for an employee with `wijzigen` on it. It asks for a reden.
- **Only medewerkers with a *Zaakspecifiek geautoriseerde medewerker* rol can be ontkoppeld**: manually added
  medewerkers and previous zaak- and taakbehandelaars. The current zaakbehandelaar and the behandelaars of
  open taken of the zaak cannot, and nobody can ontkoppel themselves.
- **Ontkoppelen deletes the rol** from the zaak in Open Zaak, with the reden as audit toelichting. The
  medewerker loses access at once, no longer finds the zaak in werkvoorraden and zoekresultaten, and is no
  longer listed in the *Medewerkers* tab. The zaakhistorie shows the removal.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `zaakspecifieke-autorisatie-extra-medewerkers`: removing a medewerker's access again.

## Impact

- **Backend**: a new function in `ZaakspecifiekeAutorisatieService`, a `DELETE` endpoint in
  `ZaakspecifiekeAutorisatieRestService`, an `isOntkoppelbaar` field on `RestGeautoriseerdeMedewerker`,
  three error codes.
- **Frontend**: an action column in the *Medewerkers* tab, a reden dialog, i18n texts.
- **Docs**: gebruikershandleiding.
- **Solr, OPA, Open Zaak catalogus**: no change.
- **Out of scope**: lifting the zaakspecifieke autorisatie of the zaak itself
  ([PZ-12022](https://dimpact.atlassian.net/browse/PZ-12022)).
