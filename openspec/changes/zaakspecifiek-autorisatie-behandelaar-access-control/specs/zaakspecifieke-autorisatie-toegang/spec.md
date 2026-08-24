## ADDED Requirements

### Requirement: Only zaakspecifiek_autorisatie_behandelaar unlocks raadpleger/behandelaar/coordinator access to a zaakspecifiek geautoriseerde zaak

The `zaak-rechten` OPA policy SHALL deny every `raadpleger`, `behandelaar`, and `coordinator`-granted
permission on a zaak whose `geautoriseerd` input is `true` to a user who does not also hold, for that zaak's
zaaktype, the `zaakspecifiek_autorisatie_behandelaar` application role. `recordmanager` and `beheerder`
access to such a zaak is out of scope for this requirement (and this capability): their existing rule bodies
are unaffected by this change, and their authorisation for the zaakspecifiek geautoriseerde case is left to a
follow-up story.

#### Scenario: A plain behandelaar cannot read a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `behandelaar` application role for a zaaktype requests the `lezen`
  permission on a zaak of that zaaktype whose `geautoriseerd` input is `true`
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A coordinator cannot change a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `coordinator` application role for a zaaktype requests the `wijzigen`
  permission on an open zaak of that zaaktype whose `geautoriseerd` input is `true`
- **THEN** the `zaak-rechten` policy's `wijzigen` permission SHALL evaluate to `false`

#### Scenario: A raadpleger cannot read a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `raadpleger` application role for a zaaktype requests the `lezen`
  permission on a zaak of that zaaktype whose `geautoriseerd` input is `true`
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A zaak that is not zaakspecifiek geautoriseerd is unaffected
- **WHEN** any user requests any permission on a zaak whose `geautoriseerd` input is `false`
- **THEN** that permission SHALL evaluate exactly as it did before this capability existed, unaffected by the
  user's `zaakspecifiek_autorisatie_behandelaar` role membership

### Requirement: zaakspecifiek_autorisatie_behandelaar has explicit behandelaar-equivalent rights

The `zaak-rechten` OPA policy SHALL grant the `zaakspecifiek_autorisatie_behandelaar` application role the
same explicit permissions as the `behandelaar` application role, for both zaakspecifiek geautoriseerde and
non-zaakspecifiek-geautoriseerde zaken of an allowed zaaktype, without relying on the user also separately
holding the `behandelaar` or `raadpleger` application role.

#### Scenario: The role alone grants read access to a zaakspecifiek geautoriseerde zaak
- **WHEN** a user holds only the `zaakspecifiek_autorisatie_behandelaar` application role (and none of
  `raadpleger`, `behandelaar`, `coordinator`, `recordmanager`, `beheerder`) for a zaaktype
- **THEN** the `zaak-rechten` policy's `lezen` permission SHALL evaluate to `true` for a zaak of that
  zaaktype whose `geautoriseerd` input is `true`

#### Scenario: The role alone grants behandelaar-equivalent mutation rights
- **WHEN** a user holds only the `zaakspecifiek_autorisatie_behandelaar` application role for a zaaktype
- **THEN** the `zaak-rechten` policy's `wijzigen`, `toekennen`, `behandelen`, and `afbreken` permissions
  SHALL evaluate to `true` for an open zaak of that zaaktype whose `geautoriseerd` input is `true`, identical
  to what a user holding only the `behandelaar` application role would get for a non-geautoriseerde zaak

#### Scenario: The role grants no rights beyond behandelaar's
- **WHEN** a user holds only the `zaakspecifiek_autorisatie_behandelaar` application role for a zaaktype
- **THEN** the `zaak-rechten` policy's `heropenen`, `bekijken_zaakdata`, and `brondatum_zetten` permissions
  (which `behandelaar` also does not have) SHALL evaluate to `false`

### Requirement: Access restriction extends to taken and documenten of a zaakspecifiek geautoriseerde zaak

The `taak-rechten` and `document-rechten` OPA policies SHALL apply the same `raadpleger`/`behandelaar`/
`coordinator` access restriction and the same explicit `zaakspecifiek_autorisatie_behandelaar` grants as the
`zaak-rechten` policy, based on whether the taak's or document's associated zaak is zaakspecifiek
geautoriseerd. As with `zaak-rechten`, `recordmanager`/`beheerder` access to such a taak or document is out
of scope for this requirement.

#### Scenario: A plain behandelaar cannot read a taak of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `behandelaar` application role for a zaaktype requests the `lezen`
  permission on a taak whose associated zaak is of that zaaktype and whose `geautoriseerd` input is `true`
- **THEN** the `taak-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A zaakspecifiek_autorisatie_behandelaar can read and treat a taak of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds the `zaakspecifiek_autorisatie_behandelaar` application role for a zaaktype
  requests the `lezen` or `wijzigen` permission on a taak whose associated zaak is of that zaaktype and whose
  `geautoriseerd` input is `true`
- **THEN** both permissions SHALL evaluate to `true`

#### Scenario: A plain behandelaar cannot read a document of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds only the `behandelaar` application role for a zaaktype requests the `lezen`
  permission on a document linked to a zaak of that zaaktype whose `geautoriseerd` input is `true`
- **THEN** the `document-rechten` policy's `lezen` permission SHALL evaluate to `false`

#### Scenario: A zaakspecifiek_autorisatie_behandelaar can read and manage a document of a zaakspecifiek geautoriseerde zaak
- **WHEN** a user who holds the `zaakspecifiek_autorisatie_behandelaar` application role for a zaaktype
  requests the `lezen` or `downloaden` permission on a document linked to a zaak of that zaaktype whose
  `geautoriseerd` input is `true`
- **THEN** both permissions SHALL evaluate to `true`

#### Scenario: A document not linked to any zaak is unaffected
- **WHEN** any user requests any permission on a document that is not linked to any zaak
- **THEN** that permission SHALL evaluate exactly as it did before this capability existed, since such a
  document's `geautoriseerd` input is `false`

### Requirement: Direct access to a zaakspecifiek geautoriseerde zaak an employee may not see fails with the generic insufficient-rights message

When a `raadpleger`, `behandelaar`, or `coordinator` without the `zaakspecifiek_autorisatie_behandelaar`
application role requests a zaak, taak, or document that is denied per the requirements above, the system
SHALL respond with the same generic insufficient-rights error the system already returns for any other
policy denial, and SHALL NOT reveal that the underlying reason is that the resource is zaakspecifiek
geautoriseerd.

#### Scenario: Direct URL access to a zaakspecifiek geautoriseerde zaak by an unauthorised employee
- **WHEN** an employee who holds only `raadpleger`, `behandelaar`, and/or `coordinator` for the zaaktype (and
  not `zaakspecifiek_autorisatie_behandelaar`) requests a zaakspecifiek geautoriseerde zaak directly (for
  example via a bookmarked URL)
- **THEN** the request is rejected with the same HTTP 403 / generic "insufficient rights" response the system
  already returns for any other policy denial, with no indication that the zaak is zaakspecifiek
  geautoriseerd

### Requirement: Werklijsten and zoekresultaten are not restricted by this capability

Rechten computed from a `ZaakZoekObject`, `TaakZoekObject`, or `DocumentZoekObject` (the Solr-backed search
index representations used for werklijsten and zoekresultaten) SHALL NOT be restricted based on whether the
underlying zaak is zaakspecifiek geautoriseerd.

#### Scenario: A zaakspecifiek geautoriseerde zaak's rechten in a werklijst are computed as before
- **WHEN** rechten are computed for a `ZaakZoekObject`, `TaakZoekObject`, or `DocumentZoekObject` representing
  a zaakspecifiek geautoriseerde zaak (or its taak/document)
- **THEN** the computed rechten are identical to what they would be if the underlying zaak were not
  zaakspecifiek geautoriseerd
