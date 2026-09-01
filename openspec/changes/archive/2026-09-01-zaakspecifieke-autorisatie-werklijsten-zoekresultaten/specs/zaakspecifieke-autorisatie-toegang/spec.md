## REMOVED Requirements

### Requirement: Werklijsten and zoekresultaten are not restricted by this capability
**Reason**: Superseded by `zaakspecifiek-geautoriseerde-zoekindex`, which excludes zaakspecifiek
geautoriseerde zaken (and their taken/documenten) from werklijsten and zoekresultaten for users who lack
the `zaakspecifiek_geautoriseerd` role for the zaak's zaaktype, and by the new requirement below, which
makes rechten computed for a werklijst/zoekresultaat row consistent with the single-resource rechten this
capability already enforces.
**Migration**: No caller-visible migration: rechten for a `ZaakZoekObject`, `TaakZoekObject`, or
`DocumentZoekObject` are now computed the same way as for the equivalent single-resource lookup, and rows
the user isn't authorized for no longer appear at all (see `zaakspecifiek-geautoriseerde-zoekindex`).

## ADDED Requirements

### Requirement: Rechten computed for werklijst and zoekresultaat rows respect zaakspecifieke autorisatie

Rechten computed from a `ZaakZoekObject`, `TaakZoekObject`, or `DocumentZoekObject` SHALL reflect whether
the underlying zaak is zaakspecifiek geautoriseerd, applying the same flag+role rule already enforced for
the equivalent single-resource (`Zaak`/`TaskInfo`/`EnkelvoudigInformatieObject`) rechten in this capability.

#### Scenario: A zaakspecifiek geautoriseerde zaak's rechten in a werklijst match its detail-view rechten
- **WHEN** rechten are computed for a `ZaakZoekObject` representing a zaakspecifiek geautoriseerde zaak, for
  a user who holds a given combination of application roles (with or without `zaakspecifiek_geautoriseerd`)
  for that zaak's zaaktype
- **THEN** the computed rechten are identical to the rechten that would be computed for the same zaak's
  detail view for the same user

#### Scenario: A zaakspecifiek geautoriseerde taak's rechten in a werklijst match its detail-view rechten
- **WHEN** rechten are computed for a `TaakZoekObject` whose associated zaak is zaakspecifiek geautoriseerd,
  for a user who holds a given combination of application roles for that zaaktype
- **THEN** the computed rechten are identical to the rechten that would be computed for the same taak's
  detail view for the same user

#### Scenario: A zaakspecifiek geautoriseerde document's rechten in a zoekresultaat match its detail-view rechten
- **WHEN** rechten are computed for a `DocumentZoekObject` whose associated zaak is zaakspecifiek
  geautoriseerd, for a user who holds a given combination of application roles for that zaaktype
- **THEN** the computed rechten are identical to the rechten that would be computed for the same document's
  detail view for the same user

#### Scenario: A non-geautoriseerde zaak's werklijst rechten are unaffected
- **WHEN** rechten are computed for a `ZaakZoekObject`, `TaakZoekObject`, or `DocumentZoekObject` whose
  underlying zaak is not zaakspecifiek geautoriseerd
- **THEN** the computed rechten are unaffected by this requirement, identical to what they were before this
  capability existed
