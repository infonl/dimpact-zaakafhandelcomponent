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

The system SHALL let a beheerder choose the process definition of a zaaktype configuration from one list. For a
configuration that was never saved, the list SHALL offer the process definitions of every engine. For a saved
configuration, the list SHALL offer only the process definitions of the engine that the configuration is bound
to, because a configuration never changes engine.

#### Scenario: Unsaved configuration offers the process definitions of every engine
- **GIVEN** a zaaktype version without a saved configuration
- **WHEN** a beheerder opens its configuration
- **THEN** the process model step offers the CMMN case definitions and the BPMN process definitions in one list,
  and the screen shows the other steps once the beheerder has chosen one

#### Scenario: Saved configuration keeps its engine
- **GIVEN** a zaaktype whose saved configuration is bound to a BPMN process definition
- **WHEN** a beheerder opens the process model step
- **THEN** the step offers only BPMN process definitions, so the beheerder can choose another process definition
  but no case definition

#### Scenario: Another process definition keeps the filled in settings
- **GIVEN** a zaaktype version without a saved configuration, for which a beheerder chose a process definition
  and filled in the productaanvraagtype
- **WHEN** the beheerder goes back to the process model step and chooses another process definition
- **THEN** the screen shows the steps of the new process definition, and the productaanvraagtype is kept

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
