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

## MODIFIED Requirements

### Requirement: The configuration list carries only what the list page shows

`PUT /zaakafhandelparameters` SHALL return, in pages, one item for each zaaktype of the default catalogus that
matches the filters of the request, with only the fields that the Zaakafhandelparameters page shows, filters and
sorts on: the `uuid`, `identificatie`, `omschrijving`, `doel`, `isNuGeldig`, `beginGeldigheid` and
`eindeGeldigheid` of the zaaktype, the `key` and `naam` of the process definition that the configuration is bound
to, the `creatiedatum` of the configuration, and whether the configuration `isValide` for zaak creation. A zaaktype without a saved configuration SHALL have an
item without a process definition and without a creatiedatum, and SHALL NOT be valid. The list SHALL require the
beheren right. `GET /zaakafhandelparameters/{zaaktypeUuid}` SHALL keep returning the complete configuration.

#### Scenario: A list item has only the fields of the page
- **GIVEN** a zaaktype `U` with a saved CMMN configuration with groep, SmartDocuments and BRP doelbindingen
- **WHEN** a beheerder lists the zaaktype configurations
- **THEN** the item for `U` has the zaaktype fields, the key and naam of its process definition, its creatiedatum
  and `isValide`
- **AND** it has no other settings, such as the default groep, the SmartDocuments toggle, the betrokkene
  koppelingen or the BRP doelbindingen

#### Scenario: A zaaktype without a saved configuration is listed as not valid
- **GIVEN** a zaaktype `U` without a saved configuration
- **WHEN** a beheerder lists the zaaktype configurations
- **THEN** the item for `U` has no process definition and no creatiedatum, and is not valid

#### Scenario: The list and the health check agree on validity
- **GIVEN** a configuration for zaaktype `U` without a process binding
- **WHEN** a beheerder lists the zaaktype configurations
- **THEN** the item for `U` is not valid, as the health check reports for `U`

#### Scenario: Reading one configuration still returns everything
- **GIVEN** a zaaktype `U` with a saved configuration
- **WHEN** a beheerder reads `GET /zaakafhandelparameters/U`
- **THEN** the response contains the complete configuration, as before this change

#### Scenario: Listing requires the beheren right
- **GIVEN** a user without the beheren right
- **WHEN** the user lists the zaaktype configurations
- **THEN** the request is refused

#### Scenario: The page shows the same rows
- **GIVEN** a beheerder who filters the Zaakafhandelparameters page on zaaktype, procesmodel, valide, geldig and
  begin and einde geldigheid
- **WHEN** the page shows the list
- **THEN** its pages together show the same rows, in the same order, as before this change, and each row links to
  the edit page of its zaaktype
