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

### D2. `PUT /rest/zaakafhandelparameters` with a body, next to the `GET`

`docs/development/paging.md` asks for a `PUT` with a body that extends `RestPageParameters`, and the other paged
lists (`/ontkoppeldedocumenten`, `/inboxdocumenten`) follow it. The body `RestZaaktypeConfigurationListParameters`
has `page`, `rows`, `sortField` (`RestZaaktypeConfigurationSortField`), `sortOrder` (`SorteerRichting`) and one
nullable field per filter: `isValide`, `isNuGeldig`, `zaaktypeIdentificatie`, `processDefinitionKey`,
`beginGeldigheid` and `eindeGeldigheid` (`RestDatumRange`). A `null` filter does not filter.

The story is delivered as a stack of two pull requests, backend first. The backend pull request adds the `PUT`
and keeps the `GET`, so the frontend on that branch still compiles against the generated types. The frontend pull
request moves the page to the `PUT` and removes the `GET`.

### D3. The filters and the sort order match the page

| Page filter or column | Request field | Rule |
|---|---|---|
| valide toggle | `isValide` | `isValide` of the row equals the value |
| geldig toggle | `isNuGeldig` | `zaaktype.isNuGeldig` equals the value |
| zaaktype select | `zaaktypeIdentificatie` | `zaaktype.identificatie` equals the value |
| procesmodel select | `processDefinitionKey` | `processDefinition.key` equals the value |
| begin / einde geldigheid | `beginGeldigheid` / `eindeGeldigheid` | the date lies in the range, both ends included; a row without the date does not match |

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
