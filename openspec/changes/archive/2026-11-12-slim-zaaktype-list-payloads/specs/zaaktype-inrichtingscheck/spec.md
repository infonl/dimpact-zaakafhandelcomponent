## ADDED Requirements

### Requirement: The inrichtingscheck list carries only the zaaktype fields that the page shows

`GET /rest/health-check/zaaktypes` SHALL return, for each checked zaaktype, all inrichtingscheck results and only
the `uuid`, `omschrijving`, `doel` and `beginGeldigheid` of the zaaktype. The Inrichtingscheck page SHALL show,
filter, sort and expand the zaaktypes as before this change.

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

#### Scenario: The page shows the same rows
- **GIVEN** a beheerder who filters the Inrichtingscheck page on an omschrijving and sorts it on begin geldigheid
- **WHEN** the page shows the list
- **THEN** it shows the same rows, in the same order, with the same findings in each expanded row as before this
  change
