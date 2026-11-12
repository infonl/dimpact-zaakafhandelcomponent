## 1. Frontend

- [ ] 1.1 `TakenService.updateTaakdata` navigates on success with the same function as `TakenService.complete`.
- [ ] 1.2 Specs: the navigation tests of `complete` run for `updateTaakdata` too.
- [ ] 1.3 Before leaving the taak, after saving or completing it, mark the werklijst queries stale: the Solr
      lists (takenwerkvoorraad, dashboard zoek-cards), the dashboard taken-signaleringen and the taken of the
      zaak ([PZ-13506](https://dimpact.atlassian.net/browse/PZ-13506)).
- [ ] 1.4 The shared navigation of saving and completing follows the page before the taak only when the user
      visited it; for the unvisited parent route of a taak opened directly or from search it goes to the
      dashboard. Specs for saving and completing with such a trail.

## 2. Docs

- [ ] 2.1 Gebruikershandleiding, *Inzien en behandelen van taken*: where ZAC goes after saving or completing a taak.

## 3. Backend

- [ ] 3.1 `TaskRestService.updateTaskData` reindexes the taak, with a unit test.
