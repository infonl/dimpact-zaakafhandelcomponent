## ADDED Requirements

### Requirement: One REST resource for the configuration of a zaaktype

The system SHALL expose the configuration of a zaaktype version for both engines through one REST resource,
`/zaakafhandelparameters`, keyed by the zaaktype UUID, with one payload for both engines. A read SHALL return
every setting at the top level, and the process definition that the configuration is bound to as
`processDefinition`, which names its engine, its key, its name, and its plan item definitions. The plan item
settings SHALL configure the plan items of that process definition: a process definition without plan items has
none. A configuration that is not bound to a process definition SHALL have no `processDefinition`, and a write
SHALL name one. Every operation of the resource SHALL require the beheren right. The resource
`/zaaktype-bpmn-configuration` SHALL NOT exist.

#### Scenario: BPMN configuration is read with its process definition
- **GIVEN** a configuration for zaaktype UUID `U` that is bound to BPMN with process definition key `K`
- **WHEN** a beheerder reads `GET /zaakafhandelparameters/U`
- **THEN** the response has a `processDefinition` with engine BPMN and key `K`, no human task or user event
  listener settings, and no intake or afronden mail option

#### Scenario: CMMN configuration is read with its process definition
- **GIVEN** a configuration for zaaktype UUID `U` that is bound to CMMN with case definition `C`
- **WHEN** a beheerder reads `GET /zaakafhandelparameters/U`
- **THEN** the response has a `processDefinition` with engine CMMN, key `C`, and the plan item definitions of
  `C`, and the settings of those plan items

#### Scenario: Unsaved configuration has no process definition
- **GIVEN** a zaaktype version `U` without a saved configuration
- **WHEN** a beheerder reads `GET /zaakafhandelparameters/U`
- **THEN** the response has no `processDefinition`

#### Scenario: Settings round-trip for both engines
- **GIVEN** a CMMN zaaktype and a BPMN zaaktype
- **WHEN** a beheerder stores the configuration of each through `PUT /zaakafhandelparameters/{zaaktypeUuid}` as it
  was read, and reads it back
- **THEN** both responses contain the stored values for the process definition, groep, default behandelaar,
  productaanvraagtype, deadline warning windows, SmartDocuments toggle, betrokkene koppelingen, BRP doelbindingen,
  niet-ontvankelijk resultaattype, zaakbeeindig parameters, plan item settings, confirmation email parameters,
  mailtemplate koppelingen, and zaakafzenders

#### Scenario: Write without a process definition is rejected
- **WHEN** a beheerder stores a configuration without a `processDefinition`
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

### Requirement: The process definitions of both engines are listed together

The system SHALL list the process definitions that a zaaktype can be bound to, of both engines, in one list
through `GET /zaakafhandelparameters/process-definitions`. Each entry SHALL name its engine, its key, its name,
and its plan item definitions. The list SHALL require the beheren right.

#### Scenario: Both engines are listed
- **GIVEN** a deployed CMMN case definition with human tasks and a deployed BPMN process definition
- **WHEN** a beheerder lists the process definitions
- **THEN** the list has the case definition with engine CMMN and its human tasks, and the BPMN process definition
  with engine BPMN and no plan item definitions

## MODIFIED Requirements

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

**Reason**: PZ-12754 replaces the two REST resources with one resource and one payload for both engines. See
"One REST resource for the configuration of a zaaktype".

**Migration**: Only this repository calls the configuration endpoints. The Angular admin service and the itest
client move from `/zaaktype-bpmn-configuration` to `/zaakafhandelparameters/{zaaktypeUuid}` in the same pull
request that removes the old resource. The clients of `GET /zaakafhandelparameters/case-definitions` and
`GET /zaakafhandelparameters/case-definitions/{key}` move to `GET /zaakafhandelparameters/process-definitions`.

### Requirement: A BPMN update preserves settings that its payload does not carry

**Reason**: The payload of a BPMN configuration now carries every setting, so no setting is left out of it. The
deadline warning windows are covered by "Settings round-trip for both engines". The admin screen sends the stored
values of the settings that a process definition has no use for, see "Saving stores the complete configuration"
in `zaaktype-configuration-admin-screen`.

**Migration**: None. A client that stores a BPMN configuration sends the confirmation email parameters it read.
