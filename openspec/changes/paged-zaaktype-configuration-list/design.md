## Context

`GET /rest/zaakafhandelparameters` returns a `RestZaaktypeConfigurationOverzicht` for every zaaktype of the default
catalogus (see the archived change `slim-zaaktype-list-payloads`). `ParametersComponent` puts the whole list in a
`MatTableDataSource`, filters it with `ClientMatcher`, sorts it with a `sortingDataAccessor`, and derives the
options of the zaaktype and procesmodel filters from all rows. It has no paginator.

## Goals / Non-Goals

**Goals:**
- The page shows 25 zaaktype configurations per page.
- Filters and sorting keep working over all configurations, not only over the rows of the current page.

**Non-Goals:**
- Reading fewer zaaktypes or configurations on the server. The ZTC client and the cache of zaaktype
  configurations already serve the list from memory (`ZaaktypeConfigurationService`, 2000 entries).
- A page size that the beheerder can change.
- Other improvements that PZ-12505 allows.

## Decisions

### D1. The server filters, sorts and pages

With paging in the browser, the full list would still travel and render. With paging on the server and filtering
in the browser, a filter would only see the current page. So the server does all three, in that order: filter,
sort, page. The total in the response is the number of rows that match the filters.

### D2. `GET /rest/zaakafhandelparameters` with query parameters

Reading a page changes nothing on the server, so it is a `GET`, which a browser or proxy can cache and a beheerder
could link to. `docs/development/paging.md` asks for a `PUT` with a body, as `/ontkoppeldedocumenten` and
`/inboxdocumenten` do; this list deviates from it after review feedback on #7973.

The query parameters (`RestZaaktypeConfigurationListParameters`, a `@BeanParam`) are `page` (default 0), `rows`
(default 25), `sortField` (`RestZaaktypeConfigurationSortField`), `sortOrder` (`SorteerRichting`, default `ASC`)
and one parameter per filter: `isValide`, `isNuGeldig`, `zaaktypeIdentificatie`, `processDefinitionKey`,
`beginGeldigheidVan`, `beginGeldigheidTot`, `eindeGeldigheidVan` and `eindeGeldigheidTot`. A filter that is not
given does not filter. The dates use the ISO date format (`2025-01-31`). A new `LocalDateParamConverter` reads them
and refuses a value with a time with HTTP 400, so a time in UTC is never read as another day.

The story is delivered as a stack of pull requests, backend first. One path has one `GET`, so the paged `GET`
replaces the `GET` of all configurations in the backend pull request. To keep the page working on that branch,
the backend pull request lets `ZaakafhandelParametersService` read one page with all configurations and hand its
rows to the page, which filters and sorts them in the browser as before. The frontend pull request replaces that
with real paging.

### D3. The filters and the sort order match the page

| Page filter or column | Query parameter | Rule |
|---|---|---|
| valide toggle | `isValide` | `isValide` of the row equals the value |
| geldig toggle | `isNuGeldig` | `zaaktype.isNuGeldig` equals the value |
| zaaktype select | `zaaktypeIdentificatie` | `zaaktype.identificatie` equals the value |
| procesmodel select | `processDefinitionKey` | `processDefinition.key` equals the value |
| begin / einde geldigheid | `beginGeldigheidVan`, `beginGeldigheidTot` / `eindeGeldigheidVan`, `eindeGeldigheidTot` | the date lies in the range, both ends included; a row without the date does not match |

The sort fields are `VALIDE`, `OMSCHRIJVING`, `PROCESS_DEFINITION` (by `naam`), `GELDIG`, `BEGIN_GELDIGHEID` and
`EINDE_GELDIGHEID`. A missing process definition or einde geldigheid sorts last when ascending and first when
descending, as `MatTableDataSource` did with the text `"undefined"`. The sort is stable, so rows with the same value
keep the order of the catalogus. Without a sort field, the list keeps the order of the catalogus.

### D4. The filter options come with every page

The page offers each zaaktype (by `identificatie`, with the `omschrijving` of its first version) and each process
definition (by `key`) of all configurations, ordered by name with a Dutch `Collator`, so that case and accents
do not change the order, as the `localeCompare` of the page did. The response carries them as `filterZaaktypes`
(`RestZaaktypeFilterOptie`: `identificatie`, `omschrijving`) and `filterProcessDefinitions`
(`RestProcessDefinitionOverzicht`), computed over all configurations before filtering, as the page did. This
follows `filterOntkoppeldDoor` of `RestDetachedDocumentResult`.

*Alternative:* a separate endpoint for the filter options. Rejected: a second request for data that the server has
in hand while it builds the page.

### D5. The filtering, sorting and paging are functions on the REST rows

`RestZaaktypeConfigurationOverzicht` already holds every value that the filters and the sort fields need, so the
functions work on the converted rows (`RestZaaktypeConfigurationOverzichtPage.kt`). They are pure, and have their
own unit test.

### D6. The page keeps its state for the session and shows when it reads a page

The page stores its filters, sort order and page in sessionStorage under `zaakafhandelParametersListParameters`, as
it already stored its filters, so a beheerder who returns from the edit page sees the same page. The key is new:
the old key `parameterFilters` holds a sort (`valide`, ascending) that the old page stored but never applied, and
the page would otherwise sort on it. A stored page that lies past the last page, because fewer configurations match
than when it was stored, moves to the last page with rows after the response arrives.

While the next page is read, the page keeps the rows of the current page (`keepPreviousData`), so the table does
not empty between pages. A loading bar shows above the table whenever the page reads while it shows rows
(`isFetching` and not `isPending`): for the next page, and for a cached page that is read again because it is stale
or a save invalidated it. The first read shows the message "Bezig met het ophalen van de gegevens" instead. With the filters on the server, the message
"Geen gegevens gevonden" also shows when no configuration matches the filters; before, a filter that matched
nothing left an empty table without a message.

Each page is a `GET` query of its own, keyed by its query parameters, and stays fresh for 5 minutes, as the list
did before. A save of a configuration invalidates every stored page through the key of the `GET` without query
parameters, which is the start of the key of every page.

## Risks / Trade-offs

- [The server still converts every configuration for each page.] → The conversion reads from the ZTC cache and the
  cache of zaaktype configurations; the measurement of `slim-zaaktype-list-payloads` gave 10 ms for 211 zaaktypes.
  The gain of this change is the smaller response and the smaller table.
- [String sorting is by code point, as in the browser, so a lowercase omschrijving sorts after uppercase ones.] →
  Unchanged behaviour; not in the scope of PZ-12505.
- [The requirement "The configuration REST contract stays unchanged" still names `GET /zaakafhandelparameters`.] →
  The active change `unify-zaaktype-configuration-frontend` removes that requirement.

## Migration Plan

No data migration. Frontend and backend are deployed together in one image. Rollback is a revert of the two pull
requests.
