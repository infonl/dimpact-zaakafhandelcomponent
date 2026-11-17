# zaaktype-process-binding Specification

## Purpose

Defines how ZAC starts, terminates, and cleans up the process of a zaak in the engine (CMMN or BPMN) that the
zaaktype configuration is bound to, so that the callers of these operations behave the same for both engines.

## Requirements

### Requirement: A zaaktype configuration is bound to at most one engine

The system SHALL bind each zaaktype configuration to at most one process engine, together with the definition
key in that engine: the case definition for CMMN, the process definition key for BPMN. A bound configuration SHALL
change its engine or its definition key only while no zaak of its zaaktype version exists; once a zaak of its
zaaktype version exists, it SHALL keep both. A BPMN-bound configuration SHALL have no CMMN extension, and a
CMMN-bound configuration SHALL hold only the plan item settings of its case definition. The REST resource SHALL
expose the binding as one `processDefinition` that names the engine and the definition key.

#### Scenario: Existing configurations keep their engine
- **GIVEN** a CMMN configuration with case definition `C` and a BPMN configuration with process definition key
  `K`, both stored before this change
- **WHEN** the database migrations of this change have run
- **THEN** the first configuration is bound to CMMN with key `C`, and the second is bound to BPMN with key `K`

#### Scenario: A configuration without zaken can change its definition
- **GIVEN** a configuration bound to an engine with definition key `K1`, and no zaak of its zaaktype version
- **WHEN** a beheerder saves it with definition key `K2` of the same engine
- **THEN** the configuration is bound to that engine with key `K2`, and the next zaak of the zaaktype version starts
  definition `K2`

#### Scenario: A configuration without zaken can change its engine
- **GIVEN** a configuration bound to one engine, and no zaak of its zaaktype version
- **WHEN** a beheerder saves it with a process definition of the other engine
- **THEN** the configuration is bound to the other engine with that definition key, and the next zaak of the zaaktype
  version starts that process definition

#### Scenario: A configuration with a zaak keeps its definition
- **GIVEN** a configuration bound to an engine with definition key `K1`, and a zaak of its zaaktype version
- **WHEN** a beheerder saves it with definition key `K2` of the same engine
- **THEN** the save is rejected with a validation error that says the zaaktype version already has zaken, and the
  configuration keeps key `K1`

#### Scenario: A configuration with a zaak keeps its engine
- **GIVEN** a configuration bound to one engine, and a zaak of its zaaktype version
- **WHEN** a beheerder saves it with a process definition of the other engine
- **THEN** the save is rejected with a validation error that says the zaaktype version already has zaken, and the
  configuration keeps its binding

#### Scenario: A new zaaktype version can get another process
- **GIVEN** a zaaktype version bound to an engine with key `K1` that has zaken, and a newly published version of that
  zaaktype, which takes over that binding
- **WHEN** a beheerder saves the configuration of the new version with another process definition, of either engine,
  before its first zaak exists
- **THEN** new zaken of the new version start the chosen process definition, and the zaken of the previous version
  keep the process definition they started with

#### Scenario: A BPMN binding has no CMMN extension
- **GIVEN** an unbound configuration, or a CMMN configuration without zaken, with a CMMN extension
- **WHEN** a beheerder binds it to BPMN
- **THEN** the CMMN extension and its plan item settings are removed

#### Scenario: A CMMN binding holds only the plan item settings of its case definition
- **GIVEN** a CMMN configuration with case definition `C1` and its plan item settings, and no zaak of its zaaktype
  version
- **WHEN** a beheerder saves it with case definition `C2` and the plan item settings of `C2`
- **THEN** the configuration holds the plan item settings of `C2` and none of `C1`

#### Scenario: A change of case definition clears the settings of the previous one
- **GIVEN** a CMMN configuration with case definition `C1` and its plan item settings, and no zaak of its zaaktype
  version
- **WHEN** a beheerder saves it with case definition `C2` and no plan item settings, or with the plan item settings
  of `C1`
- **THEN** the configuration holds no plan item settings of `C1`

#### Scenario: A CMMN binding has a setting for every user event listener
- **GIVEN** a BPMN configuration, and no zaak of its zaaktype version
- **WHEN** a beheerder binds it to a CMMN case definition with user event listeners, and the save sends no plan item
  settings
- **THEN** the configuration holds a setting without toelichting for each user event listener of that case
  definition, and the actions of a new zaak of the zaaktype version can be listed

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
productaanvraagtype matches, whatever engine it is bound to, and SHALL create the zaak in that engine. The
system SHALL ignore a matching configuration that is not bound to an engine, and SHALL log a warning for it.
When no bound configuration matches, the system SHALL register the productaanvraag in the inbox. When more
than one bound configuration matches (data stored before the uniqueness check), the system SHALL use the most
recently created one and SHALL log a warning.

#### Scenario: Productaanvraag for a BPMN zaaktype
- **GIVEN** a BPMN zaaktype configuration with productaanvraagtype `P`
- **WHEN** ZAC receives a productaanvraag of type `P`
- **THEN** a zaak is created and a BPMN process is started for it

#### Scenario: No configuration for the productaanvraagtype
- **WHEN** ZAC receives a productaanvraag whose type matches no configuration
- **THEN** no zaak is created and the productaanvraag is registered in the inbox

#### Scenario: Only an unbound configuration for the productaanvraagtype
- **GIVEN** a CMMN zaaktype configuration with productaanvraagtype `P` and no case definition
- **WHEN** ZAC receives a productaanvraag of type `P`
- **THEN** no zaak is created, the productaanvraag is registered in the inbox, and a warning is logged

### Requirement: The automatic confirmation of receipt is CMMN only

When ZAC creates a zaak from a productaanvraag, the system SHALL send the automatic confirmation of receipt only
for a zaak of a CMMN zaaktype, as the confirmation email parameters of its configuration set. For a zaak of a BPMN
zaaktype the system SHALL NOT send it; a BPMN process sends a confirmation email itself when it needs one, with
the template and sender that its process definition sets.

#### Scenario: Productaanvraag for a CMMN zaaktype with an enabled confirmation email
- **GIVEN** a CMMN zaaktype configuration with productaanvraagtype `P` and an enabled confirmation email
- **WHEN** ZAC receives a productaanvraag of type `P` whose initiator has an email address
- **THEN** ZAC sends the confirmation of receipt to that address

#### Scenario: Productaanvraag for a BPMN zaaktype
- **GIVEN** a BPMN zaaktype configuration with productaanvraagtype `P` and a process definition without a
  confirmation email task
- **WHEN** ZAC receives a productaanvraag of type `P` whose initiator has an email address
- **THEN** no confirmation email is sent
