## Why

[PZ-12506](https://dimpact.atlassian.net/browse/PZ-12506): the Inrichtingscheck page loads the checks of all
zaaktypes of the catalogue at once and renders them all in one table. With hundreds of zaaktypes the page is slow to
show, and every filter or sort waits for the whole list. The beheerder wants the list in pages of 25 zaaktypes, a
filter on (part of) the name of the zaaktype, and the simple performance improvements done as well.

## What Changes

- **The inrichtingscheck list is paged on the server**: `GET /rest/health-check/zaaktypes` takes the page, the page
  size (default 25), a filter on (part of) the omschrijving or the doel of the zaaktype (`zoekterm`), a filter on the state of the check and
  a sort field and direction as query parameters, and returns one page plus the total number of matching zaaktypes.
  **BREAKING** for the REST contract: the response is a page object `{ totaal, resultaten }` instead of an array.
- **The reference-table check becomes a resource**: `GET /rest/health-check/bestaat-communicatiekanaal-eformulier`,
  which returns a bare `true` or `false`, is replaced by `GET /rest/health-check/referentietabellen`, which returns
  an object with the field `isCommunicatiekanaalEformulierAanwezig`. **BREAKING** for the REST contract. No
  acceptance criterion asks for it and it does not make the page faster; it is a deliberate clean-up of the API of
  this page while the change touches it, and the frontend is its only client.
- **The backend checks each zaaktype once from the list it already has**: it no longer reads every zaaktype again
  from the catalogue before it checks it.
- **The Inrichtingscheck page** shows a paginator with pages of 25 zaaktypes, sends the filter, the toggle and the
  sort to the backend, and keeps the table and its filter row visible while a new page loads. The component is
  rebuilt on Angular signals: no `MatTableDataSource`, no `effect` that copies query data into the table and no
  `ngOnInit`.
- **A new ADR** records that a paged list is read with `GET` and query parameters instead of `PUT` with a body, and
  `docs/development/paging.md` describes both forms.
- **Measurements**: the response time and size of the list are measured with a k6 script before and after the
  change; the numbers go into the pull request and into this change. The script is not checked in.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `zaaktype-inrichtingscheck`: the list of inrichtingschecks is paged, filtered and sorted on the server, and the
  reference-table check is read as a resource.

## Impact

- **Backend**: `HealthCheckRestService` (query parameters, page response, new `referentietabellen` resource),
  `HealthCheckService` (check a `ZaakType` instead of a URL), new REST model classes, unit tests and the
  integration test `HealthCheckRestServiceTest`.
- **Zaak creation**: `CmmnProcessBinding.isZaaktypeReady` also calls `HealthCheckService.controleerZaaktype`, and
  through `ZaaktypeRestService` it decides for which zaaktypes a behandelaar can start a zaak. It now reads the
  zaaktype and its configuration itself and passes them on (D3); the zaaktypes for zaak creation stay the same.
- **Frontend**: `HealthCheckService`, `InrichtingscheckComponent` and its template and spec, the generated
  OpenAPI types.
- **Docs**: a new ADR in `docs/solution-architecture/decisions`, `docs/development/paging.md`, and the
  Inrichtingscheck section of the handleiding.
- **Other clients**: none known. The test-data scripts in `scripts/test-data/` do not call these endpoints.
- **Database, Solr, OPA, Open Zaak**: no change.
