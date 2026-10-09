# zaakspecifieke-autorisatie-toegangsbepaling Specification

## Purpose

How ZAC works out the zaakspecifieke autorisatie data of a zaak: whether the zaak is zaakspecifiek
geautoriseerd and which medewerkers are individually authorised for it. One derivation and one check serve the
rechten and the search index, callers reuse data they already read in the same request, and data is read again
after a request changes the marking or the rollen of a zaak.

## Requirements

### Requirement: Rechten and responses are unchanged by the refactoring

For every zaak, taak and document, the rechten ZAC computes SHALL be identical to the rechten it computed before
this change. The same SHALL hold for every REST response and every search index entry. The input that ZAC sends to
the OPA policies SHALL contain the same values, including `zaakspecifiekGeautoriseerd` and
`loggedInUserIsGeautoriseerdeMedewerker`.

#### Scenario: A geautoriseerde medewerker of a zsa zaak
- **GIVEN** a zaakspecifiek geautoriseerde zaak with a *Zaakspecifiek geautoriseerde medewerker* rol for medewerker A
- **WHEN** rechten are computed for A on the zaak, on one of its taken and on one of its documenten
- **THEN** the OPA input marks A as a geautoriseerde medewerker in all three, exactly as before

#### Scenario: A medewerker without individual access to a zsa zaak
- **GIVEN** a zaakspecifiek geautoriseerde zaak on which medewerker B holds no *Behandelaar* or *Zaakspecifiek
  geautoriseerde medewerker* rol
- **WHEN** rechten are computed for B on the zaak
- **THEN** the OPA input does not mark B as a geautoriseerde medewerker

#### Scenario: A zaak that is not zsa
- **WHEN** rechten are computed on a zaak that is not zaakspecifiek geautoriseerd
- **THEN** the OPA input marks the zaak as not zaakspecifiek geautoriseerd
- **AND** the rollen of the zaak are not read for the access check

### Requirement: One derivation of the zsa access data of a zaak

ZAC SHALL work out in one function whether a zaak is zaakspecifiek geautoriseerd and which medewerkers are
individually authorised for it. The rechten of zaken, taken and documenten SHALL use that function, and so SHALL the
search index entries of zaken, taken and documenten. Whether a medewerker is one of the geautoriseerde medewerkers
SHALL be decided by one check, which the rechten computed from a zoekobject also use.

#### Scenario: The search index and the rechten agree
- **GIVEN** a zaakspecifiek geautoriseerde zaak
- **WHEN** the zaak is indexed, and rechten are computed for it
- **THEN** both use the same set of geautoriseerde medewerkers

### Requirement: Callers reuse zsa access data they already read

When a request already read the zsa access data of a zaak, ZAC SHALL reuse that data for the same zaak in the same
request instead of asking Open Zaak again. This SHALL hold at least for:
- opening a zaak, by UUID or by identificatie: the rechten and the zsa indicator of the response;
- listing the taken of a zaak;
- listing the documenten of a zaak.

The geautoriseerde medewerkers SHALL only be read when the zaak is zaakspecifiek geautoriseerd and somebody needs
them.

#### Scenario: Opening a zaak
- **WHEN** a medewerker opens a zaak
- **THEN** ZAC asks Open Zaak only once whether the zaak is zaakspecifiek geautoriseerd

#### Scenario: Listing the taken of a zaak
- **GIVEN** a zaak with several taken
- **WHEN** a medewerker lists the taken of the zaak
- **THEN** ZAC asks Open Zaak only once whether the zaak is zaakspecifiek geautoriseerd, whatever the number of taken
- **AND** it does not read the zaak again for every taak

#### Scenario: Listing the documenten of a zaak
- **GIVEN** a zaak with several documenten
- **WHEN** a medewerker lists the documenten of the zaak
- **THEN** ZAC asks Open Zaak only once whether the zaak is zaakspecifiek geautoriseerd, whatever the number of
  documenten

### Requirement: After a write the zsa access data is read again

When a request changes the zsa marking or the rollen of a zaak, ZAC SHALL build the response from zsa access data
read after that change. It SHALL NOT reuse data read before the change.

#### Scenario: Marking a zaak in the same request that returns it
- **GIVEN** a zaak that is not zaakspecifiek geautoriseerd
- **WHEN** a medewerker marks it as zaakspecifiek geautoriseerd through `PATCH /rest/zaken/zaak/{uuid}`
- **THEN** the returned zaak shows `isZaakspecifiekGeautoriseerd` as `true`, exactly as before
