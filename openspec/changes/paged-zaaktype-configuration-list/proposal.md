## Why

The Zaakafhandelparameters page loads the configurations of all zaaktypes of the default catalogus in one
response, and filters and sorts them in the browser. With many zaaktypes, the response is large and the table
renders all rows at once. PZ-12505 asks for the list in pages of 25 zaaktypes, so that the page is available
sooner.

## What Changes

- `GET /rest/zaakafhandelparameters` returns one page of the zaaktype configurations of the default catalogus. Its
  query parameters carry the page, the number of rows, the sort field and sort order, and the filters of the page:
  valide, geldig, zaaktype, process definition, and the ranges of begin and einde geldigheid. The
  response carries the rows of the page, the number of rows that match the filters, and the zaaktypes and process
  definitions of all configurations, for the filter lists of the page.
- The server filters and sorts the list, as the page did in the browser.
- The Zaakafhandelparameters page shows 25 rows per page, with a paginator, and requests a page from the server
  when the beheerder changes a filter, the sort order or the page.
- **BREAKING (internal API)** `GET /rest/zaakafhandelparameters` returns that page (`resultaten`, `totaal` and the
  filter options) instead of a list of all configurations. Only this repository calls it.

## Capabilities

### Modified Capabilities
- `zaaktype-configuration`: the configuration list is served in pages, filtered and sorted by the server.

## Impact

- Backend: `ZaaktypeConfigurationRestService`, new REST models for the list parameters, the sort field, the page
  result and the zaaktype filter option, the functions that filter, sort and page the list, and a `ParamConverter`
  for `LocalDate` query parameters.
- Generated OpenAPI spec and the generated frontend types.
- Frontend: `ParametersComponent`, `ZaakafhandelParametersListParameters`, `ZaakafhandelParametersService` and
  their specs.
- Integration tests: `ZaaktypeConfigurationRestServicePagingTest` and `ZaaktypeConfigurationRestServiceTest`.
- The test-data scripts in `scripts/test-data/` do not call the list endpoint and do not change.
