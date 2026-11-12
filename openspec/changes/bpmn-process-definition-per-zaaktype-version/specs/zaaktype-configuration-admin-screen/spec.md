## MODIFIED Requirements

### Requirement: The process definition is chosen from one list

The system SHALL let a beheerder choose the process definition of a zaaktype configuration from one list. For a
configuration that is not bound to a process definition, the list SHALL offer the process definitions of every
engine. For a bound configuration, the screen SHALL show its process definition, SHALL offer only the process
definitions of its engine, and SHALL let the beheerder choose another one only while the backend allows it: for a
BPMN configuration whose zaaktype version has no zaken. When the screen does not let the beheerder choose another
process definition, it SHALL explain why.

#### Scenario: Unsaved configuration offers the process definitions of every engine
- **GIVEN** a zaaktype version without a saved configuration
- **WHEN** a beheerder opens its configuration
- **THEN** the process model step offers the CMMN case definitions and the BPMN process definitions in one list,
  and the screen shows the other steps once the beheerder has chosen one

#### Scenario: BPMN configuration without zaken offers the other BPMN process definitions
- **GIVEN** a zaaktype version without zaken, whose saved configuration is bound to a BPMN process definition
- **WHEN** a beheerder opens the process model step
- **THEN** the step shows that process definition, offers only the BPMN process definitions, and lets the
  beheerder choose and save another one

#### Scenario: BPMN configuration with a zaak keeps its process definition
- **GIVEN** a zaaktype version with a zaak, whose saved configuration is bound to a BPMN process definition
- **WHEN** a beheerder opens the process model step
- **THEN** the step shows that process definition, does not let the beheerder choose another one, and explains
  that the zaaktype version already has zaken and that a new version of the zaaktype is needed for another process
  definition

#### Scenario: CMMN configuration keeps its process definition
- **GIVEN** a zaaktype whose saved configuration is bound to a CMMN case definition
- **WHEN** a beheerder opens the process model step
- **THEN** the step shows that case definition, does not let the beheerder choose another one, and explains that
  the process definition of a CMMN zaaktype cannot change

#### Scenario: A process definition that may change has no explanation
- **GIVEN** a configuration that is not bound, or a BPMN configuration whose zaaktype version has no zaken
- **WHEN** a beheerder opens the process model step
- **THEN** the step shows no explanation about a locked process definition

#### Scenario: The process definition follows the backend once the configuration is saved
- **GIVEN** a zaaktype version without a saved configuration, for which a beheerder chose a process definition
- **WHEN** the beheerder saves the configuration
- **THEN** the process model step lets the beheerder choose another process definition of the same engine only
  when the saved configuration allows it

#### Scenario: Another process definition keeps the filled in settings
- **GIVEN** a zaaktype version without a saved configuration, for which a beheerder chose a process definition
  and filled in the productaanvraagtype
- **WHEN** the beheerder goes back to the process model step and chooses another process definition
- **THEN** the screen shows the steps of the new process definition, and the productaanvraagtype is kept
