## Purpose

Defines how ZAC starts, terminates, and cleans up the process of a zaak in the engine (CMMN or BPMN) that the
zaaktype configuration is bound to, so that the callers of these operations behave the same for both engines.

## ADDED Requirements

### Requirement: A zaaktype configuration is bound to at most one engine

The system SHALL bind each zaaktype configuration to at most one process engine, together with the definition
key in that engine: the case definition for CMMN, the process definition key for BPMN. Both REST resources
SHALL keep exposing the definition key in their existing fields.

#### Scenario: Existing configurations keep their engine
- **GIVEN** a CMMN configuration with case definition `C` and a BPMN configuration with process definition key
  `K`, both stored before this change
- **WHEN** the database migrations of this change have run
- **THEN** the first configuration is bound to CMMN with key `C`, and the second is bound to BPMN with key `K`

### Requirement: A zaak starts in the engine its zaaktype is bound to

When ZAC creates a zaak, through the REST API or from a productaanvraag, the system SHALL start the process of
that zaak in the engine that the zaaktype configuration is bound to, with the bound definition key. When the
zaaktype has no configuration, or its configuration has no process binding, the system SHALL refuse to create
the zaak.

#### Scenario: Zaak of a CMMN zaaktype starts a CMMN case
- **WHEN** a user creates a zaak of a zaaktype that is bound to CMMN with case definition `C`
- **THEN** a CMMN case of definition `C` is started for the zaak, and no BPMN process is started

#### Scenario: Zaak of a BPMN zaaktype starts a BPMN process
- **WHEN** a user creates a zaak of a zaaktype that is bound to BPMN with process definition key `K`
- **THEN** a BPMN process of definition `K` is started for the zaak, and no CMMN case is started

#### Scenario: Zaaktype without a configuration
- **WHEN** a user creates a zaak of a zaaktype that has no configuration
- **THEN** the request fails with the zaaktype-configuration-not-found error and no zaak is created

### Requirement: A zaak terminates in the engine its zaaktype is bound to

When a user terminates a zaak, the system SHALL terminate the process of that zaak in the engine that its
zaaktype configuration is bound to.

#### Scenario: BPMN zaak is terminated
- **WHEN** a user terminates a zaak of a zaaktype that is bound to BPMN
- **THEN** the BPMN process instance of that zaak no longer runs

### Requirement: Deleting a zaak cleans up its process in either engine

When ZAC receives a notification that a zaak was deleted, the system SHALL delete the CMMN case or the BPMN
process instance of that zaak, and the variables that ZAC holds for it. The cleanup SHALL succeed when the
zaak has no process in one or both engines.

#### Scenario: BPMN process is deleted with its zaak
- **GIVEN** a zaak with a running BPMN process instance
- **WHEN** ZAC receives the zaak-delete notification for that zaak
- **THEN** the BPMN process instance and its history no longer exist

#### Scenario: Zaak without a process
- **GIVEN** a zaak with neither a CMMN case nor a BPMN process instance
- **WHEN** ZAC receives the zaak-delete notification for that zaak
- **THEN** the notification is acknowledged without an error

### Requirement: Productaanvraag intake selects one configuration

When ZAC handles a productaanvraag, the system SHALL select the current configuration of the zaaktype whose
productaanvraagtype matches, whatever engine it is bound to, and SHALL create the zaak in that engine. When no
configuration matches, the system SHALL register the productaanvraag in the inbox. When more than one
configuration matches (data stored before the uniqueness check), the system SHALL use the most recently
created one and SHALL log a warning.

#### Scenario: Productaanvraag for a BPMN zaaktype
- **GIVEN** a BPMN zaaktype configuration with productaanvraagtype `P`
- **WHEN** ZAC receives a productaanvraag of type `P`
- **THEN** a zaak is created and a BPMN process is started for it

#### Scenario: No configuration for the productaanvraagtype
- **WHEN** ZAC receives a productaanvraag whose type matches no configuration
- **THEN** no zaak is created and the productaanvraag is registered in the inbox

### Requirement: BPMN confirmation email falls back to the configuration

When a BPMN process sends a confirmation email, the system SHALL use the template, the sender, and the
reply-to address that the process definition sets. For each of these values that the process definition does
not set, the system SHALL use the confirmation email parameters of the zaaktype configuration. When neither
source sets a template, or the configured confirmation email is disabled and the process definition sets no
template, the system SHALL send no email.

#### Scenario: Process definition sets the template
- **GIVEN** a BPMN process definition whose confirmation email task sets template `T1`, and a zaaktype
  configuration with confirmation template `T2`
- **WHEN** the task runs
- **THEN** the email uses template `T1`

#### Scenario: Process definition sets no template
- **GIVEN** a BPMN process definition whose confirmation email task sets no template, and a zaaktype
  configuration with an enabled confirmation email that uses template `T2`
- **WHEN** the task runs
- **THEN** the email uses template `T2` and the configured sender and reply-to address
