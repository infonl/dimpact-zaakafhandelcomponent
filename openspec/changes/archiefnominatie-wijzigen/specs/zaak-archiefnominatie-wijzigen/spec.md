## ADDED Requirements

### Requirement: Only a recordmanager or beheerder may change the archief of an afgehandelde zaak

The zaak rechten SHALL include `wijzigen_archief`, granted to an employee with the `recordmanager` or `beheerder`
application role on an afgehandelde zaak of a zaaktype they are authorised for and that is accessible to them.

#### Scenario: A recordmanager on an afgehandelde zaak
- **WHEN** the rechten of an employee with the `recordmanager` role on an afgehandelde zaak are read
- **THEN** `wijzigen_archief` is granted

#### Scenario: An open zaak has no archief to change
- **WHEN** the rechten of an employee with the `beheerder` role on an open zaak are read
- **THEN** `wijzigen_archief` is not granted

### Requirement: The archiefnominatie of an afgehandelde zaak can be changed with a reden

The archiefnominatie update operation SHALL accept the new archiefnominatie, `BLIJVEND_BEWAREN` or `VERNIETIGEN`,
and a reden from an employee with `wijzigen_archief`. It SHALL patch only the archiefnominatie of the zaak in Open
Zaak, with the reden as audit toelichting, and return the updated zaak with its rechten.

#### Scenario: Changing Vernietigen to Blijvend bewaren
- **GIVEN** an afgehandelde zaak with archiefnominatie `VERNIETIGEN`
- **AND** an employee with the `recordmanager` role
- **WHEN** the employee changes the archiefnominatie to `BLIJVEND_BEWAREN` with reden "Proceseigenaar wil bewaren"
- **THEN** the zaak in Open Zaak has archiefnominatie `BLIJVEND_BEWAREN`
- **AND** the returned zaak reports archiefnominatie `BLIJVEND_BEWAREN`
- **AND** the zaakhistorie shows the change with the reden

#### Scenario: A beheerder changes the archiefnominatie
- **WHEN** an employee with the `beheerder` role changes the archiefnominatie of an afgehandelde zaak
- **THEN** the archiefnominatie is changed

#### Scenario: The archiefactiedatum is left to Open Zaak
- **WHEN** the archiefnominatie of an afgehandelde zaak is changed
- **THEN** ZAC sends no archiefactiedatum to Open Zaak

#### Scenario: An open zaak
- **WHEN** an employee requests to change the archiefnominatie of a zaak that is not afgehandeld
- **THEN** the request is refused with the generic insufficient-rights response

#### Scenario: An employee without the right
- **WHEN** an employee with only the `behandelaar` or `coordinator` role requests to change the archiefnominatie
  of an afgehandelde zaak
- **THEN** the request is refused with the generic insufficient-rights response and the zaak is not changed

#### Scenario: A reden is mandatory
- **WHEN** the request has an empty reden
- **THEN** the request is refused as invalid and the zaak is not changed
