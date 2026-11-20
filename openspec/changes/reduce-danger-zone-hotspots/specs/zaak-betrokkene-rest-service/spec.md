## ADDED Requirements

### Requirement: The betrokkene endpoints are served by ZaakBetrokkeneRestService
`ZaakBetrokkeneRestService` SHALL serve these endpoints at the `@Path("zaken")` root, with the same method names,
signatures and annotations they had in `ZaakRestService`, and `ZaakRestService` SHALL no longer declare them:
`POST /zaken/betrokkene` (`addBetrokkene`), `DELETE /zaken/betrokkene/{uuid}` (`deleteBetrokkene`),
`DELETE /zaken/{uuid}/initiator` (`deleteInitiator`), `GET /zaken/zaak/{uuid}/betrokkene` (`listBetrokkenenVoorZaak`)
and `PATCH /zaken/initiator` (`updateInitiator`).

#### Scenario: A betrokkene is added through the same URI
- **WHEN** a user with the right to add a betrokkene of that identification type posts a betrokkene to
  `POST /zaken/betrokkene`
- **THEN** the rol is created in Open Zaak with the same explanation as before, and the response is the zaak

#### Scenario: A betrokkene is deleted through the same URI
- **WHEN** a user with the right to delete betrokkenen deletes a betrokkene that is neither a behandelaar nor a
  zaakspecifiek geautoriseerde medewerker, with a reden
- **THEN** the rol is deleted in Open Zaak with that reden, and the response is the zaak

#### Scenario: A behandelaar cannot be deleted as a betrokkene
- **WHEN** a user deletes the behandelaar rol of a zaak through `DELETE /zaken/betrokkene/{uuid}`
- **THEN** the request is refused as before and the rol stays

#### Scenario: The initiator is deleted through the same URI
- **WHEN** a user with the right to delete the initiator deletes it with a reden
- **THEN** the initiator rol is deleted in Open Zaak with that reden, and the response is the zaak

#### Scenario: The betrokkenen are listed through the same URI
- **WHEN** a user who may read the zaak lists its betrokkenen
- **THEN** the response lists the same betrokkenen in the same form as before

#### Scenario: The initiator is replaced through the same URI
- **WHEN** a user with the right to add an initiator of that identification type replaces the initiator with an
  explanation
- **THEN** the old initiator rol is deleted, the new one is created, and the response is the zaak

### Requirement: The initiator policy check exists once
The check whether a user may add an initiator of an identification type SHALL be implemented in one place and used
both when a zaak is created with an initiator and when the initiator is replaced: `canToevoegenInitiatorPersoon` for
BSN, `canToevoegenBetrokkeneBedrijf` for VN and RSIN.

#### Scenario: Creating a zaak with an initiator checks the same right
- **WHEN** a user without `canToevoegenInitiatorPersoon` creates a zaak with a BSN initiator
- **THEN** the request is refused by a policy exception, as before

#### Scenario: Replacing the initiator checks the same right
- **WHEN** a user without `canToevoegenBetrokkeneBedrijf` replaces the initiator with a vestiging
- **THEN** the request is refused by a policy exception, as before

### Requirement: The domain does not read constants from the REST layer
Code outside `nl.info.zac.app` SHALL NOT import from `ZaakRestService` or `ZaakBetrokkeneRestService`. The vestiging
identification delimiter and the name of the Aanvullende informatie task SHALL be declared in the domain layer, with
their current values.

#### Scenario: The domain imports no REST constants
- **WHEN** the sources under `src/main/kotlin/nl/info/zac` outside `nl.info.zac.app` are searched for imports of
  `nl.info.zac.app.zaak.ZaakRestService` or `nl.info.zac.app.zaak.ZaakBetrokkeneRestService`
- **THEN** there are none

#### Scenario: A vestiging identification keeps its format
- **WHEN** a vestiging is stored as initiator and read back
- **THEN** its identification is the kvkNummer and the vestigingsnummer joined by `|`, as before
