## Purpose

Lets ZAC and its users know, per zaak, whether that zaak has been marked as zaakspecifiek
geautoriseerd in Open Zaak, as a foundation for the zaakspecifieke autorisatie feature described in
PZ-11909.

## ADDED Requirements

### Requirement: RestZaak exposes whether a zaak is zaakspecifiek geautoriseerd
Every `RestZaak` returned by the system SHALL include a boolean `isZaakspecifiekGeautoriseerd` field
that is `true` when the underlying zaak in Open Zaak has a zaakeigenschap with name
`ZAAK_GEAUTORISEERD` and value `true`, and `false` otherwise.

#### Scenario: Zaak has the ZAAK_GEAUTORISEERD zaakeigenschap set to true
- **WHEN** a zaak is read whose zaakeigenschappen include one with name `ZAAK_GEAUTORISEERD` and
  value `true`
- **THEN** the returned `RestZaak.isZaakspecifiekGeautoriseerd` is `true`

#### Scenario: Zaak does not have the ZAAK_GEAUTORISEERD zaakeigenschap
- **WHEN** a zaak is read whose zaakeigenschappen do not include one with name
  `ZAAK_GEAUTORISEERD`
- **THEN** the returned `RestZaak.isZaakspecifiekGeautoriseerd` is `false`

#### Scenario: Zaak has the ZAAK_GEAUTORISEERD zaakeigenschap set to a value other than true
- **WHEN** a zaak is read whose zaakeigenschappen include one with name `ZAAK_GEAUTORISEERD` and a
  value other than `true` (for example `false` or an empty string)
- **THEN** the returned `RestZaak.isZaakspecifiekGeautoriseerd` is `false`

### Requirement: Zaakdetailpagina shows a lock icon for a zaakspecifiek geautoriseerde zaak
The zaakdetailpagina SHALL show a lock icon directly in front of the zaaknummer, in the same row,
when the displayed zaak's `isZaakspecifiekGeautoriseerd` is `true`. No other page SHALL be changed
by this requirement, and when `isZaakspecifiekGeautoriseerd` is `false` the zaaknummer row SHALL be
displayed exactly as before this capability existed, with no lock icon.

#### Scenario: Viewing a zaakspecifiek geautoriseerde zaak
- **WHEN** a user opens the zaakdetailpagina for a zaak whose `isZaakspecifiekGeautoriseerd` is
  `true`
- **THEN** a lock icon is shown directly in front of the zaaknummer in that row

#### Scenario: Viewing a zaak that is not zaakspecifiek geautoriseerd
- **WHEN** a user opens the zaakdetailpagina for a zaak whose `isZaakspecifiekGeautoriseerd` is
  `false`
- **THEN** no lock icon is shown next to the zaaknummer, unchanged from current behavior
