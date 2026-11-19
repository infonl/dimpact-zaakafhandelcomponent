## Purpose

Defines the admin screen in which a beheerder views and edits the configuration of a zaaktype version, so that
CMMN and BPMN zaaktypen are configured in one screen that shows each setting that the process definition of the
zaaktype has use for.

## ADDED Requirements

### Requirement: One admin screen configures both engines

The system SHALL show the configuration of a zaaktype in one admin screen, whatever engine the zaaktype is
bound to. The screen SHALL show these steps for both engines: the process model, the general settings, the mail
settings, the zaak beëindiging settings, the koppelingen, and the SmartDocuments settings. The screen SHALL show
the humantask step only for a process definition that has human tasks, and the user event listener step only
for a process definition that has user event listeners.

#### Scenario: CMMN zaaktype shows every step
- **GIVEN** a zaaktype whose configuration is bound to a case definition with human tasks and user event
  listeners
- **WHEN** a beheerder opens its configuration
- **THEN** the screen shows the process model, general, humantask, user event listener, mail, zaak
  beëindiging, koppelingen, and SmartDocuments steps

#### Scenario: BPMN zaaktype hides the plan item steps
- **GIVEN** a zaaktype whose configuration is bound to a BPMN process definition, which has no plan items
- **WHEN** a beheerder opens its configuration
- **THEN** the screen shows the process model, general, mail, zaak beëindiging, koppelingen, and SmartDocuments
  steps, and does not show the humantask and user event listener steps

#### Scenario: Shared steps behave the same for both engines
- **GIVEN** a CMMN zaaktype and a BPMN zaaktype with the same zaakbeeindig parameters and the same BRP
  doelbindingen
- **WHEN** a beheerder opens the zaak beëindiging step and the koppelingen step of each
- **THEN** both screens show the same fields with the same values, and every field has an accessible name

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

### Requirement: A BPMN zaaktype has deadline warning windows

The system SHALL let a beheerder set the einddatum-gepland warning window and the uiterlijke-einddatum-afdoening
warning window for a BPMN zaaktype, in the same way as for a CMMN zaaktype.

#### Scenario: Beheerder sets a warning window for a BPMN zaaktype
- **GIVEN** a zaaktype whose configuration is bound to BPMN
- **WHEN** a beheerder sets the uiterlijke-einddatum-afdoening warning window to 2 days and saves
- **THEN** the stored configuration has a warning window of 2 days, and the screen shows 2 days when the
  beheerder opens the configuration again

### Requirement: The mail step shows the settings that the process definition has use for

The system SHALL let a beheerder set the mailtemplate koppelingen and the zaakafzenders for every process
definition. The system SHALL show the intake mail option only for a process definition with the intake user
event listener (`INTAKE_AFRONDEN`), and the afronden mail option only for one with the afhandelen user event
listener (`ZAAK_AFHANDELEN`). The system SHALL show the confirmation email settings only for a CMMN zaaktype,
because ZAC sends the confirmation of receipt only for a CMMN zaak. For a BPMN zaaktype, the mail step SHALL show
a collapsed confirmation email section without fields, with the text that the process sends the confirmation of
receipt itself.

#### Scenario: BPMN zaaktype gets mailtemplate koppelingen and zaakafzenders
- **GIVEN** a zaaktype whose configuration is bound to BPMN
- **WHEN** a beheerder adds a mailtemplate koppeling and a zaakafzender in the mail step and saves
- **THEN** the stored configuration has that koppeling and that zaakafzender

#### Scenario: BPMN zaaktype explains the confirmation email
- **GIVEN** a zaaktype whose configuration is bound to BPMN
- **WHEN** a beheerder opens the mail step
- **THEN** the step shows no intake or afronden mail options and no confirmation email fields, and shows a
  collapsed section with the text that the process sends the confirmation of receipt itself

#### Scenario: CMMN zaaktype keeps its full mail step
- **GIVEN** a zaaktype whose configuration is bound to a case definition with the intake and the afhandelen user
  event listeners
- **WHEN** a beheerder opens the mail step
- **THEN** the step shows the intake and afronden mail options, the confirmation email settings, the
  mailtemplate koppelingen, and the zaakafzenders

#### Scenario: The mail option follows the user event listeners of the case definition
- **GIVEN** a zaaktype whose configuration is bound to a case definition with the afhandelen user event listener
  and without the intake user event listener
- **WHEN** a beheerder opens the mail step
- **THEN** the step shows the afronden mail option and not the intake mail option

### Requirement: Saving stores the complete configuration

When a beheerder saves the configuration, the system SHALL store every setting of every step in one request,
including the steps that the beheerder did not open. The screen SHALL show the stored values after the save.

#### Scenario: Settings that the process definition has no use for keep their stored values
- **GIVEN** a BPMN zaaktype configuration with stored confirmation email parameters
- **WHEN** a beheerder changes only the default groep in the general step and saves
- **THEN** the stored configuration has the new default groep and still has the same confirmation email parameters

#### Scenario: Unopened step keeps its stored values
- **GIVEN** a zaaktype configuration with SmartDocuments template mappings and BRP doelbindingen
- **WHEN** a beheerder changes only the default groep in the general step and saves
- **THEN** the stored configuration has the new default groep and still has its SmartDocuments template mappings
  and BRP doelbindingen
