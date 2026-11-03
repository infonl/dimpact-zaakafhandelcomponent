## ADDED Requirements

### Requirement: The inrichtingscheck rejects unsupported afleidingswijzen of the brondatum

The inrichtingscheck SHALL report a CMMN zaaktype as not valid when one of its resultaattypes has the
afleidingswijze `ander_datumkenmerk` or `zaakobject`, and SHALL list the omschrijving of each such resultaattype
in the error. A zaaktype that is not valid SHALL NOT be offered for zaak creation.

#### Scenario: Ander datumkenmerk is reported
- **GIVEN** a CMMN zaaktype with a resultaattype *Toegekend* with afleidingswijze `ander_datumkenmerk`
- **WHEN** a beheerder opens the inrichtingscheck
- **THEN** the zaaktype is shown as not valid, and its expanded row has an error naming *Toegekend* as a
  resultaattype with a brondatum afhandelwijze that ZAC does not support

#### Scenario: Zaakobject is reported
- **GIVEN** a CMMN zaaktype with a resultaattype with afleidingswijze `zaakobject`
- **WHEN** a beheerder opens the inrichtingscheck
- **THEN** the zaaktype is shown as not valid with the same error

#### Scenario: All unsupported resultaattypes are named
- **GIVEN** a CMMN zaaktype with two resultaattypes with an unsupported afleidingswijze
- **WHEN** a beheerder opens the inrichtingscheck
- **THEN** the error names both resultaattypes

#### Scenario: Supported afleidingswijzen give no error
- **GIVEN** a CMMN zaaktype whose resultaattypes only use other afleidingswijzen, such as `eigenschap`,
  `ingangsdatum_besluit`, `vervaldatum_besluit`, `hoofdzaak` or `afgehandeld`
- **WHEN** a beheerder opens the inrichtingscheck
- **THEN** no error about the brondatum afhandelwijze is shown

#### Scenario: No zaak can be created for the zaaktype
- **GIVEN** a CMMN zaaktype with a resultaattype with afleidingswijze `ander_datumkenmerk`
- **WHEN** a behandelaar lists the zaaktypes to start a zaak
- **THEN** the zaaktype is not offered
