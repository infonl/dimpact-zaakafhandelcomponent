## ADDED Requirements

### Requirement: The configuration list is served in pages

`GET /zaakafhandelparameters` SHALL return one page of the items that the requirement "The configuration list
carries only what the list page shows" defines. It SHALL filter the items on the filters of the request, sort the
filtered items on the sort field and sort order of the request, and return the requested page of the requested
number of rows. It SHALL return the number of items that match the filters, and the zaaktypes and process
definitions of all items, each once, to filter on. Without a sort field, the items SHALL keep the order of the
catalogus. It SHALL require the beheren right. The Zaakafhandelparameters page SHALL show 25 items per page.

#### Scenario: A page of 25 items
- **GIVEN** 60 zaaktypes in the default catalogus
- **WHEN** a beheerder requests the third page of 25 rows
- **THEN** the last 10 items are returned, and the total is 60

#### Scenario: Filters apply to all items, not only to a page
- **GIVEN** zaaktypes that are and are not valid today
- **WHEN** a beheerder requests a page with the geldig filter on
- **THEN** only items of zaaktypes that are valid today are returned, and the total counts only those

#### Scenario: A date range includes both ends
- **GIVEN** a zaaktype with a begin geldigheid of 1 January 2025
- **WHEN** a beheerder filters on a begin geldigheid from 1 January 2025 to 31 December 2025
- **THEN** the item of that zaaktype is returned

#### Scenario: An item without the sort value sorts last when ascending
- **GIVEN** a zaaktype without a process definition
- **WHEN** a beheerder sorts on the process definition, ascending
- **THEN** its item is after the items with a process definition

#### Scenario: The filter options do not depend on the filters
- **GIVEN** a filter that matches no item
- **WHEN** a beheerder requests a page
- **THEN** no items are returned, and the zaaktypes and process definitions of all items are still offered

#### Scenario: Listing a page requires the beheren right
- **GIVEN** a user without the beheren right
- **WHEN** the user requests a page of the zaaktype configurations
- **THEN** the request is refused
