## MODIFIED Requirements

### Requirement: A zaaktype configuration is bound to at most one engine

The system SHALL bind each zaaktype configuration to at most one process engine, together with the definition
key in that engine: the case definition for CMMN, the process definition key for BPMN. Once bound, a
configuration SHALL keep its engine, and a BPMN-bound configuration SHALL have no CMMN extension. A BPMN-bound
configuration SHALL change its definition key only while no zaak of its zaaktype version exists; a CMMN-bound
configuration SHALL keep its definition key. The REST resource SHALL expose the binding as one
`processDefinition` that names the engine and the definition key.

#### Scenario: Existing configurations keep their engine
- **GIVEN** a CMMN configuration with case definition `C` and a BPMN configuration with process definition key
  `K`, both stored before this change
- **WHEN** the database migrations of this change have run
- **THEN** the first configuration is bound to CMMN with key `C`, and the second is bound to BPMN with key `K`

#### Scenario: A bound configuration never changes engine
- **GIVEN** a configuration bound to one engine
- **WHEN** a beheerder saves it with a process definition of the other engine
- **THEN** the save is rejected with a validation error that says the engine cannot change, and the configuration
  keeps its binding

#### Scenario: A BPMN configuration without zaken can change its definition
- **GIVEN** a configuration bound to BPMN with process definition key `K1`, and no zaak of its zaaktype version
- **WHEN** a beheerder saves it with process definition key `K2`
- **THEN** the configuration is bound to BPMN with key `K2`, and the next zaak of the zaaktype version starts
  process definition `K2`

#### Scenario: A BPMN configuration with a zaak keeps its definition
- **GIVEN** a configuration bound to BPMN with process definition key `K1`, and a zaak of its zaaktype version
- **WHEN** a beheerder saves it with process definition key `K2`
- **THEN** the save is rejected with a validation error that says the zaaktype version already has zaken, and the
  configuration keeps key `K1`

#### Scenario: A CMMN configuration keeps its definition
- **GIVEN** a configuration bound to CMMN with case definition `C1`
- **WHEN** a beheerder saves it with case definition `C2`
- **THEN** the save is rejected with a validation error that says a CMMN case definition cannot change, and the
  configuration keeps case definition `C1`

#### Scenario: A new zaaktype version can get another BPMN process
- **GIVEN** a zaaktype version bound to BPMN with key `K1` that has zaken, and a newly published version of that
  zaaktype, which takes over key `K1`
- **WHEN** a beheerder saves the configuration of the new version with process definition key `K2` before its first
  zaak exists
- **THEN** new zaken of the new version start process definition `K2`, and the zaken of the previous version keep
  the process definition they started with

#### Scenario: A BPMN binding has no CMMN extension
- **GIVEN** an unbound configuration with a CMMN extension
- **WHEN** a beheerder binds it to BPMN
- **THEN** the CMMN extension and its plan item settings are removed
