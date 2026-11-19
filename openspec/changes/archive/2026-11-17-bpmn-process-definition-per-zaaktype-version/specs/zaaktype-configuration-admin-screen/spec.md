## MODIFIED Requirements

### Requirement: The process definition is chosen from one list

The system SHALL let a beheerder choose the process definition of a zaaktype configuration from one list, which
SHALL offer the CMMN case definitions and the BPMN process definitions. For a bound configuration, the screen SHALL
show its process definition and SHALL let the beheerder choose another one, of either engine, only while the backend
allows it: while its zaaktype version has no zaken. When the screen does not let the beheerder choose another
process definition, it SHALL explain why. When it lets the beheerder choose another process definition for a bound
configuration, it SHALL say that the process definition locks once the first zaak of the zaaktype version exists.
The process model step SHALL name the zaaktype version it configures, so the beheerder can tell versions with the
same omschrijving apart.

#### Scenario: Unsaved configuration offers the process definitions of every engine
- **GIVEN** a zaaktype version without a saved configuration
- **WHEN** a beheerder opens its configuration
- **THEN** the process model step offers the CMMN case definitions and the BPMN process definitions in one list,
  and the screen shows the other steps once the beheerder has chosen one

#### Scenario: Configuration without zaken offers the process definitions of every engine
- **GIVEN** a zaaktype version without zaken, whose saved configuration is bound to a CMMN case definition or a BPMN
  process definition
- **WHEN** a beheerder opens the process model step
- **THEN** the step shows that process definition, offers the CMMN case definitions and the BPMN process definitions,
  and lets the beheerder choose and save another one

#### Scenario: Configuration with a zaak keeps its process definition
- **GIVEN** a zaaktype version with a zaak, whose saved configuration is bound to a CMMN case definition or a BPMN
  process definition
- **WHEN** a beheerder opens the process model step
- **THEN** the step shows that process definition, does not let the beheerder choose another one, and explains
  that the zaaktype version already has zaken and that a new version of the zaaktype is needed for another process
  definition

#### Scenario: A rejected change shows the lock
- **GIVEN** a beheerder who chose another process definition for a zaaktype version without zaken
- **AND** a zaak of that zaaktype version is created before the beheerder saves
- **WHEN** the beheerder saves the configuration
- **THEN** the screen shows that nothing was saved because the zaaktype version already has zaken, and the process
  model step shows the stored process definition, locked, with its explanation

#### Scenario: An unbound configuration has no hint
- **GIVEN** a configuration that is not bound
- **WHEN** a beheerder opens the process model step
- **THEN** the step shows no hint about changing or locking the process definition

#### Scenario: A bound configuration without zaken says when it locks
- **GIVEN** a zaaktype version without zaken, whose saved configuration is bound to a CMMN case definition or a BPMN
  process definition
- **WHEN** a beheerder opens the process model step
- **THEN** the step shows no explanation about a locked process definition, and says that the process definition
  can change until the first zaak of the zaaktype version is created

#### Scenario: The process model step names the zaaktype version
- **GIVEN** a zaaktype version, with or without an einde geldigheid
- **WHEN** a beheerder opens the process model step
- **THEN** the step shows the omschrijving of the zaaktype and, under it, the begin geldigheid of the zaaktype version,
  and its einde geldigheid when it has one

#### Scenario: The process definition follows the backend once the configuration is saved
- **GIVEN** a zaaktype version without a saved configuration, for which a beheerder chose a process definition
- **WHEN** the beheerder saves the configuration
- **THEN** the process model step lets the beheerder choose another process definition only when the saved
  configuration allows it

#### Scenario: Another process definition keeps the filled in settings
- **GIVEN** a zaaktype version without zaken, for which a beheerder chose a process definition and filled in the
  productaanvraagtype
- **WHEN** the beheerder goes back to the process model step and chooses another process definition, of either engine
- **THEN** the screen shows the steps of the new process definition, and the productaanvraagtype is kept

#### Scenario: Returning to the stored process definition keeps its settings
- **GIVEN** a saved configuration without zaken, bound to a CMMN case definition with changed task and action
  settings
- **WHEN** a beheerder chooses another process definition and then the stored case definition again
- **THEN** the screen shows the stored task and action settings of that case definition, not the defaults
