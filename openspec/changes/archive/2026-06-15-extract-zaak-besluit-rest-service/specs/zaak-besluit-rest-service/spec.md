## ADDED Requirements

### Requirement: List besluiten for zaak
The system SHALL expose `GET /zaken/besluit/zaakUuid/{zaakUuid}` on `ZaakBesluitRestService` returning all `RestDecision` objects linked to the zaak, enforcing `zaakRechten.lezen`.

#### Scenario: Authorized user retrieves besluiten
- **WHEN** a logged-in user with `lezen` permission calls `GET /zaken/besluit/zaakUuid/{zaakUuid}`
- **THEN** the system returns the list of `RestDecision` objects for that zaak

#### Scenario: Unauthorized user is rejected
- **WHEN** a logged-in user without `lezen` permission calls `GET /zaken/besluit/zaakUuid/{zaakUuid}`
- **THEN** the system throws a policy violation exception

### Requirement: Create besluit for zaak
The system SHALL expose `POST /zaken/besluit` on `ZaakBesluitRestService`, enforcing `zaakRechten.vastleggenBesluit` and that the zaaktype has besluittypen configured.

#### Scenario: Authorized user creates besluit
- **WHEN** a logged-in user with `vastleggenBesluit` permission submits valid `RestDecisionCreateData`
- **THEN** the system creates the besluit, sends a `ZAAK_BESLUITEN` screen event, and returns the created `RestDecision`

#### Scenario: Unauthorized user is rejected
- **WHEN** a logged-in user without `vastleggenBesluit` permission submits `RestDecisionCreateData`
- **THEN** the system throws a policy violation exception

### Requirement: Update besluit
The system SHALL expose `PUT /zaken/besluit` on `ZaakBesluitRestService`, enforcing `zaakRechten.vastleggenBesluit`.

#### Scenario: Authorized user updates besluit
- **WHEN** a logged-in user with `vastleggenBesluit` permission submits valid `RestDecisionChangeData`
- **THEN** the system updates the besluit, sends a `ZAAK_BESLUITEN` screen event, and returns the updated `RestDecision`

#### Scenario: Unauthorized user is rejected
- **WHEN** a logged-in user without `vastleggenBesluit` permission submits `RestDecisionChangeData`
- **THEN** the system throws a policy violation exception

### Requirement: Withdraw besluit
The system SHALL expose `PUT /zaken/besluit/intrekken` on `ZaakBesluitRestService`, enforcing that the zaak is open and `zaakRechten.behandelen`.

#### Scenario: Authorized user withdraws besluit from open zaak
- **WHEN** a logged-in user with `behandelen` permission submits valid `RestDecisionWithdrawalData` for an open zaak
- **THEN** the system withdraws the besluit, sends a `ZAAK_BESLUITEN` screen event, and returns the withdrawn `RestDecision`

#### Scenario: Unauthorized user is rejected
- **WHEN** a logged-in user without `behandelen` permission or the zaak is closed
- **THEN** the system throws a policy violation exception

### Requirement: List besluit history
The system SHALL expose `GET /zaken/besluit/{uuid}/historie` on `ZaakBesluitRestService` returning the audit trail for a besluit as `List<HistoryLine>`.

#### Scenario: User retrieves besluit history
- **WHEN** a user calls `GET /zaken/besluit/{uuid}/historie` with a valid besluit UUID
- **THEN** the system returns the converted audit trail history lines

### Requirement: List besluit types for zaaktype
The system SHALL expose `GET /zaken/besluittypes/{zaaktypeUUID}` on `ZaakBesluitRestService`, enforcing `werklijstRechten.zakenTaken`.

#### Scenario: Authorized user lists besluit types
- **WHEN** a logged-in user with `zakenTaken` permission requests besluit types for a zaaktype UUID
- **THEN** the system returns the currently-valid besluit types as `List<RestDecisionType>`

#### Scenario: Unauthorized user is rejected
- **WHEN** a logged-in user without `zakenTaken` permission requests besluit types
- **THEN** the system throws a policy violation exception
