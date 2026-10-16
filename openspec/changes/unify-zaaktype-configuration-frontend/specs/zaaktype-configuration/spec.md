## ADDED Requirements

### Requirement: One REST resource for the configuration of a zaaktype

The system SHALL expose the configuration of a zaaktype version for both engines through one REST resource,
`/zaakafhandelparameters`, keyed by the zaaktype UUID. A read SHALL return the engine-agnostic settings, the
engine that the configuration is bound to as `processEngine`, and only the block of that engine:

- the `cmmn` block holds the case definition, the humantask parameters, the user event listener parameters, the
  intake and afronden mail options, and the automatic confirmation email.
- the `bpmn` block holds the process definition key.

A configuration that is not bound to an engine SHALL have no `processEngine` and no engine block. A write SHALL
carry the block of the engine it binds to and no other block. Every operation of the resource SHALL require the
beheren right. The resource `/zaaktype-bpmn-configuration` SHALL NOT exist.

#### Scenario: BPMN configuration is read with its engine block
- **GIVEN** a configuration for zaaktype UUID `U` that is bound to BPMN with process definition key `K`
- **WHEN** a beheerder reads `GET /zaakafhandelparameters/U`
- **THEN** the response has `processEngine` BPMN, a `bpmn` block with process definition key `K`, and no `cmmn`
  block

#### Scenario: CMMN configuration is read with its engine block
- **GIVEN** a configuration for zaaktype UUID `U` that is bound to CMMN with case definition `C`
- **WHEN** a beheerder reads `GET /zaakafhandelparameters/U`
- **THEN** the response has `processEngine` CMMN, a `cmmn` block with case definition `C`, and no `bpmn` block

#### Scenario: Unsaved configuration has no engine
- **GIVEN** a zaaktype version `U` without a saved configuration
- **WHEN** a beheerder reads `GET /zaakafhandelparameters/U`
- **THEN** the response has no `processEngine`, no `cmmn` block, and no `bpmn` block

#### Scenario: Settings round-trip for both engines
- **GIVEN** a CMMN zaaktype and a BPMN zaaktype
- **WHEN** a beheerder stores the same engine-agnostic settings for each through
  `PUT /zaakafhandelparameters/{zaaktypeUuid}` and reads them back
- **THEN** both responses contain the stored values for groep, default behandelaar, productaanvraagtype,
  deadline warning windows, SmartDocuments toggle, betrokkene koppelingen, BRP doelbindingen, niet-ontvankelijk
  resultaattype, zaakbeeindig parameters, mailtemplate koppelingen, and zaakafzenders

#### Scenario: Write with the block of the other engine is rejected
- **WHEN** a beheerder stores a configuration with `processEngine` BPMN and a `cmmn` block
- **THEN** the request fails with a validation error and nothing is stored

#### Scenario: Path key and payload disagree
- **WHEN** a beheerder stores `PUT /zaakafhandelparameters/U` with a payload for another zaaktype UUID
- **THEN** the request fails with a validation error and nothing is stored

#### Scenario: Reading requires the beheren right
- **GIVEN** a user who has the right to start a zaak but not the beheren right
- **WHEN** the user reads `GET /zaakafhandelparameters/U`
- **THEN** the request is refused

#### Scenario: The BPMN resource is gone
- **WHEN** a client calls `GET /zaaktype-bpmn-configuration`
- **THEN** the response is 404 Not Found

## MODIFIED Requirements

### Requirement: A BPMN update preserves settings that its payload does not carry

When the system stores a BPMN zaaktype configuration through `PUT /zaakafhandelparameters/{zaaktypeUuid}`, the
system SHALL keep the stored confirmation email parameters, because the payload of a BPMN configuration has no
`cmmn` block that carries them. Every other engine-agnostic setting SHALL be stored as the payload sets it.

#### Scenario: Confirmation email parameters survive a BPMN update
- **GIVEN** a stored BPMN zaaktype configuration with confirmation email parameters, inherited from a
  predecessor version
- **WHEN** a beheerder changes its groep through `PUT /zaakafhandelparameters/{zaaktypeUuid}`
- **THEN** the stored configuration has the new groep and still has the same confirmation email parameters

#### Scenario: Deadline warning windows survive a BPMN update
- **GIVEN** a stored BPMN zaaktype configuration with an einddatum-gepland warning window of 3 days
- **WHEN** a beheerder reads it, changes only its groep, and stores it through
  `PUT /zaakafhandelparameters/{zaaktypeUuid}`
- **THEN** the stored configuration has the new groep and still has a warning window of 3 days

#### Scenario: Deadline warning windows of a BPMN update are stored
- **GIVEN** a stored BPMN zaaktype configuration with an einddatum-gepland warning window of 3 days
- **WHEN** a beheerder stores it with an einddatum-gepland warning window of 5 days
- **THEN** the stored configuration has a warning window of 5 days

### Requirement: Engine-agnostic settings apply to BPMN zaken

The system SHALL apply these settings to a zaak of a BPMN zaaktype in the same way as to a zaak of a CMMN
zaaktype: the BRP doelbindingen, the deadline warning windows, the mailtemplate koppelingen, and the
zaakafzenders.

#### Scenario: BRP query of a BPMN zaak uses its doelbinding
- **GIVEN** a BPMN zaaktype configuration with BRP zoek-doelbinding `D`
- **WHEN** a user searches BRP personen in the context of a zaak of that zaaktype
- **THEN** the BRP request uses doelbinding `D` and not the default from the environment

#### Scenario: BPMN zaak gets a deadline warning
- **GIVEN** a BPMN zaaktype configuration with a uiterlijke-einddatum-afdoening warning window of 2 days
- **WHEN** a zaak of that zaaktype is 1 day from its uiterlijke einddatum afdoening
- **THEN** the zaak is listed by the zaak warnings endpoint and its due-date signalering is sent

#### Scenario: BPMN zaak uses its mailtemplate koppeling
- **GIVEN** a BPMN zaaktype configuration that links mail `ZAAK_ALGEMEEN` to mail template `T`
- **WHEN** a user opens the mail dialog for a zaak of that zaaktype
- **THEN** the dialog proposes mail template `T` and not the default mail template

#### Scenario: BPMN zaak offers its zaakafzenders
- **GIVEN** a BPMN zaaktype configuration with a default zaakafzender `A`
- **WHEN** a user lists the afzenders for a zaak of that zaaktype
- **THEN** the list contains `A`, marked as the default

## REMOVED Requirements

### Requirement: The configuration REST contract stays unchanged

**Reason**: PZ-12754 replaces the two REST resources with one resource that has engine blocks. See
"One REST resource for the configuration of a zaaktype".

**Migration**: Only this repository calls the configuration endpoints. The Angular admin service and the itest
client move from `/zaaktype-bpmn-configuration` to `/zaakafhandelparameters/{zaaktypeUuid}` in the same pull
request that removes the old resource.
