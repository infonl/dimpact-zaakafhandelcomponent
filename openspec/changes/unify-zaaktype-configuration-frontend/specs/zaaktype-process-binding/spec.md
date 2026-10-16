## MODIFIED Requirements

### Requirement: A zaaktype configuration is bound to at most one engine

The system SHALL bind each zaaktype configuration to at most one process engine, together with the definition
key in that engine: the case definition for CMMN, the process definition key for BPMN. Once bound, a
configuration SHALL keep its engine, and a BPMN-bound configuration SHALL have no CMMN extension. The REST
resource SHALL expose the binding as one `processDefinition` that names the engine and the definition key.

#### Scenario: Existing configurations keep their engine
- **GIVEN** a CMMN configuration with case definition `C` and a BPMN configuration with process definition key
  `K`, both stored before this change
- **WHEN** the database migrations of this change have run
- **THEN** the first configuration is bound to CMMN with key `C`, and the second is bound to BPMN with key `K`

#### Scenario: A bound configuration never changes engine
- **GIVEN** a configuration bound to one engine
- **WHEN** a beheerder saves it with a process definition of the other engine
- **THEN** the save is rejected with a validation error, and the configuration keeps its binding

#### Scenario: A bound configuration can change its definition
- **GIVEN** a configuration bound to BPMN with process definition key `K1`
- **WHEN** a beheerder saves it with process definition key `K2`
- **THEN** the configuration is bound to BPMN with key `K2`

#### Scenario: A BPMN binding has no CMMN extension
- **GIVEN** an unbound configuration with a CMMN extension
- **WHEN** a beheerder binds it to BPMN
- **THEN** the CMMN extension and its plan item settings are removed
