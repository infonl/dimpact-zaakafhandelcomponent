## ADDED Requirements

### Requirement: Each inrichtingscheck on a page carries only the zaaktype fields that the page shows

`GET /rest/health-check/zaaktypes` SHALL return, for each checked zaaktype on the requested page, all inrichtingscheck
results and only the `uuid`, `omschrijving`, `doel` and `beginGeldigheid` of the zaaktype.

#### Scenario: A check item has only the zaaktype fields of the page
- **GIVEN** a valid CMMN zaaktype *Melding openbare ruimte*
- **WHEN** a beheerder lists the inrichtingschecks
- **THEN** the zaaktype of its item has its `uuid`, `omschrijving`, `doel` and `beginGeldigheid`
- **AND** it has no other zaaktype fields, such as the identificatie, the versiedatum or the
  vertrouwelijkheidaanduiding

#### Scenario: A check item keeps all check results
- **GIVEN** a CMMN zaaktype without a resultaattype
- **WHEN** a beheerder lists the inrichtingschecks
- **THEN** its item reports that the zaaktype is not valid and that no resultaattype is present, as before this
  change

### Requirement: The inrichtingscheck list is read in pages

`GET /rest/health-check/zaaktypes` SHALL accept the query parameters `page` (zero-based, default 0) and `rows`
(default 25, from 1 to 100), and SHALL return `totaal`, the number of zaaktypes that match the filters, and
`resultaten`, the inrichtingschecks of the requested page only. Only a user with the right to beheren SHALL read it.

#### Scenario: The first page holds 25 zaaktypes
- **GIVEN** 60 checked zaaktypes in the default catalogue
- **WHEN** a beheerder requests the list without query parameters
- **THEN** the response has `totaal` 60 and 25 items in `resultaten`

#### Scenario: The last page holds the rest
- **GIVEN** 60 checked zaaktypes in the default catalogue
- **WHEN** a beheerder requests `page` 2 with `rows` 25
- **THEN** the response has `totaal` 60 and 10 items in `resultaten`

#### Scenario: A page after the last page is empty
- **GIVEN** 60 checked zaaktypes in the default catalogue
- **WHEN** a beheerder requests `page` 5 with `rows` 25
- **THEN** the response has `totaal` 60 and no items in `resultaten`

#### Scenario: A page size above the maximum is refused
- **WHEN** a beheerder requests the list with `rows` 101
- **THEN** the response is `400 Bad Request`

#### Scenario: A negative page is refused
- **WHEN** a beheerder requests the list with `page` -1
- **THEN** the response is `400 Bad Request`

#### Scenario: A user without the right to beheren is refused
- **GIVEN** a logged-in behandelaar without the right to beheren
- **WHEN** the behandelaar requests the list
- **THEN** the response is `403 Forbidden`

### Requirement: The inrichtingscheck list is filtered before it is paged

`GET /rest/health-check/zaaktypes` SHALL apply the query parameters `omschrijving` and `status` to all checked
zaaktypes before it takes the page, and SHALL count `totaal` after them. `omschrijving` SHALL keep the zaaktypes whose
omschrijving contains the value, ignoring case and surrounding white space. `status` SHALL be `AANDACHT_NODIG` (not
valid, or with a warning) or `IN_ORDE` (valid, without a warning); without `status` all checked zaaktypes are kept.

#### Scenario: Filter on part of the omschrijving
- **GIVEN** the checked zaaktypes *Melding openbare ruimte*, *Melding evenement* and *Aanvraag vergunning*
- **WHEN** a beheerder requests the list with `omschrijving` `MELDING `
- **THEN** `resultaten` holds *Melding evenement* and *Melding openbare ruimte*, and `totaal` is 2

#### Scenario: Filter on the zaaktypes that need attention
- **GIVEN** a valid zaaktype without warnings, a valid zaaktype with a warning and a zaaktype that is not valid
- **WHEN** a beheerder requests the list with `status` `AANDACHT_NODIG`
- **THEN** `resultaten` holds the zaaktype with the warning and the zaaktype that is not valid, and `totaal` is 2

#### Scenario: Filter on the zaaktypes that are in order
- **GIVEN** a valid zaaktype without warnings, a valid zaaktype with a warning and a zaaktype that is not valid
- **WHEN** a beheerder requests the list with `status` `IN_ORDE`
- **THEN** `resultaten` holds only the valid zaaktype without warnings, and `totaal` is 1

#### Scenario: An unknown status is refused
- **WHEN** a beheerder requests the list with `status` `ONBEKEND`
- **THEN** the response is `400 Bad Request`

### Requirement: The inrichtingscheck list is sorted before it is paged

`GET /rest/health-check/zaaktypes` SHALL sort all matching zaaktypes before it takes the page, on the query
parameter `sortField` (`OMSCHRIJVING`, `DOEL`, `BEGIN_GELDIGHEID` or `VALIDE`, default `OMSCHRIJVING`) in the
direction `sortOrder` (`ASC` or `DESC`, default `ASC`).

#### Scenario: The sort applies to all pages
- **GIVEN** 30 checked zaaktypes
- **WHEN** a beheerder requests `page` 0 and `page` 1 with `rows` 25, `sortField` `BEGIN_GELDIGHEID` and
  `sortOrder` `DESC`
- **THEN** the 30 zaaktypes over both pages are in descending order of begin geldigheid

#### Scenario: The default order is by omschrijving
- **WHEN** a beheerder requests the list without `sortField` and `sortOrder`
- **THEN** the zaaktypes are in ascending order of omschrijving

#### Scenario: An unknown sort field is refused
- **WHEN** a beheerder requests the list with `sortField` `IDENTIFICATIE`
- **THEN** the response is `400 Bad Request`

### Requirement: The Inrichtingscheck page shows the list in pages of 25

The Inrichtingscheck page SHALL show at most 25 zaaktypes at a time with a paginator that shows the total number of
matching zaaktypes, and SHALL read each page from the backend. The page SHALL send the filter text as `omschrijving`,
the toggle as `status` and the column sort as `sortField` and `sortOrder`, and SHALL go back to the first page when
one of them changes. The table and its filter row SHALL stay on screen while a page loads.

#### Scenario: The page opens on the first page of zaaktypes that need attention
- **GIVEN** 40 checked zaaktypes, of which 30 need attention
- **WHEN** a beheerder opens the Inrichtingscheck page
- **THEN** the table shows 25 zaaktypes that need attention, sorted on omschrijving, and the paginator shows 30
  zaaktypes in total

#### Scenario: The beheerder goes to the next page
- **GIVEN** the Inrichtingscheck page shows the first page of 30 zaaktypes
- **WHEN** the beheerder goes to the next page
- **THEN** the table shows the other 5 zaaktypes

#### Scenario: The beheerder switches the toggle to all zaaktypes
- **GIVEN** 40 checked zaaktypes, of which 30 need attention
- **AND** the Inrichtingscheck page shows the second page of the zaaktypes that need attention
- **WHEN** the beheerder switches the toggle to all zaaktypes
- **THEN** the table shows the first page of all checked zaaktypes, and the paginator shows 40 zaaktypes in total

#### Scenario: The beheerder switches the toggle to the zaaktypes that are in order
- **GIVEN** 40 checked zaaktypes, of which 30 need attention
- **WHEN** the beheerder switches the toggle to the zaaktypes that are in order
- **THEN** the table shows only the 10 zaaktypes that are in order, and the paginator shows 10 zaaktypes in total

#### Scenario: Typing a filter starts on the first page
- **GIVEN** the Inrichtingscheck page shows the second page
- **WHEN** the beheerder types *melding* in the filter
- **THEN** the table shows the first page of the zaaktypes whose omschrijving contains *melding*
- **AND** the filter field keeps its text and focus while the page loads

#### Scenario: A zaaktype that needs attention still expands
- **GIVEN** a zaaktype without a resultaattype on the current page
- **WHEN** the beheerder clicks its row
- **THEN** the row expands and shows the finding that no resultaattype is present

#### Scenario: No zaaktype matches
- **WHEN** the beheerder types a filter that no omschrijving contains
- **THEN** the page shows that no data was found, and the filter row stays on screen

### Requirement: The reference-table check is read as a resource

`GET /rest/health-check/referentietabellen` SHALL return an object with the field
`isCommunicatiekanaalEformulierAanwezig`, which is true when the system reference table *Communicatiekanaal* holds
the value *E-formulier*. Only a user with the right to beheren SHALL read it. The Inrichtingscheck page SHALL show
its result as before this change.

#### Scenario: The E-formulier communicatiekanaal is present
- **GIVEN** the default reference tables that ZAC provisions on startup
- **WHEN** a beheerder requests `GET /rest/health-check/referentietabellen`
- **THEN** the response is `200 OK` with `isCommunicatiekanaalEformulierAanwezig` true

#### Scenario: The E-formulier communicatiekanaal is missing
- **GIVEN** a Communicatiekanaal reference table without the value *E-formulier*
- **WHEN** a beheerder opens the Inrichtingscheck page
- **THEN** the page shows that the communicatiekanaal E-formulier is missing, as before this change

## REMOVED Requirements

### Requirement: The inrichtingscheck list carries only the zaaktype fields that the page shows
**Reason**: Its scenario "The page shows the same rows" no longer holds: the page now shows one page of 25 zaaktypes,
and its filter matches the omschrijving only.
**Migration**: The field rule moves unchanged to the requirement "Each inrichtingscheck on a page carries only the
zaaktype fields that the page shows"; filtering, sorting and paging are in the new requirements of this change.
