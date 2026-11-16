## MODIFIED Requirements

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

#### Scenario: A bound configuration never changes engine
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
