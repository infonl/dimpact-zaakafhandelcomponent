## ADDED Requirements

### Requirement: The configuration list carries only what the list page shows

`GET /zaakafhandelparameters` SHALL return one item for each zaaktype of the default catalogus, with only the
fields that the Zaakafhandelparameters page shows, filters and sorts on: the `uuid`, `identificatie`,
`omschrijving`, `doel`, `isNuGeldig`, `beginGeldigheid` and `eindeGeldigheid` of the zaaktype, the `key` and
`naam` of the process definition that the configuration is bound to, the `creatiedatum` of the configuration, and
whether the configuration `isValide` for zaak creation. A zaaktype without a saved configuration SHALL have an
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
- **THEN** it shows the same rows, in the same order, as before this change, and each row links to the edit page of
  its zaaktype
