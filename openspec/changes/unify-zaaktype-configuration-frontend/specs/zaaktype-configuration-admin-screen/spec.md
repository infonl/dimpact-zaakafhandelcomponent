## Purpose

Defines the admin screen in which a beheerder views and edits the configuration of a zaaktype version, so that
CMMN and BPMN zaaktypen are configured in one screen that shows each setting that applies to the engine of the
zaaktype.

## ADDED Requirements

### Requirement: One admin screen configures both engines

The system SHALL show the configuration of a zaaktype in one admin screen, whatever engine the zaaktype is
bound to. The screen SHALL show these steps for both engines: the process model, the general settings, the mail
settings, the zaak beëindiging settings, the koppelingen, and the SmartDocuments settings. The screen SHALL show
the humantask step and the user event listener step only for a CMMN zaaktype.

#### Scenario: CMMN zaaktype shows every step
- **GIVEN** a zaaktype whose configuration is bound to CMMN
- **WHEN** a beheerder opens its configuration
- **THEN** the screen shows the process model, general, humantask, user event listener, mail, zaak
  beëindiging, koppelingen, and SmartDocuments steps

#### Scenario: BPMN zaaktype hides the CMMN plan item steps
- **GIVEN** a zaaktype whose configuration is bound to BPMN
- **WHEN** a beheerder opens its configuration
- **THEN** the screen shows the process model, general, mail, zaak beëindiging, koppelingen, and SmartDocuments
  steps, and does not show the humantask and user event listener steps

#### Scenario: Shared steps behave the same for both engines
- **GIVEN** a CMMN zaaktype and a BPMN zaaktype with the same zaakbeeindig parameters and the same BRP
  doelbindingen
- **WHEN** a beheerder opens the zaak beëindiging step and the koppelingen step of each
- **THEN** both screens show the same fields with the same values, and every field has an accessible name

### Requirement: The engine is chosen once

The system SHALL let a beheerder choose the engine and its definition only for a zaaktype configuration that was
never saved. For a saved configuration the screen SHALL show the engine without the option to change it. The
beheerder SHALL still be able to choose another definition of the same engine.

#### Scenario: Unsaved configuration offers the engine choice
- **GIVEN** a zaaktype version without a saved configuration
- **WHEN** a beheerder opens its configuration
- **THEN** the process model step lets the beheerder choose CMMN or BPMN, and then a case definition or a process
  definition

#### Scenario: Saved configuration keeps its engine
- **GIVEN** a zaaktype whose saved configuration is bound to BPMN
- **WHEN** a beheerder opens its configuration
- **THEN** the process model step shows BPMN, offers no choice of engine, and lets the beheerder choose another
  process definition

### Requirement: A BPMN zaaktype has deadline warning windows

The system SHALL let a beheerder set the einddatum-gepland warning window and the uiterlijke-einddatum-afdoening
warning window for a BPMN zaaktype, in the same way as for a CMMN zaaktype.

#### Scenario: Beheerder sets a warning window for a BPMN zaaktype
- **GIVEN** a zaaktype whose configuration is bound to BPMN
- **WHEN** a beheerder sets the uiterlijke-einddatum-afdoening warning window to 2 days and saves
- **THEN** the stored configuration has a warning window of 2 days, and the screen shows 2 days when the
  beheerder opens the configuration again

### Requirement: The mail step shows the settings that apply to the engine

The system SHALL let a beheerder set the mailtemplate koppelingen and the zaakafzenders for both engines. The
system SHALL show the intake and afronden mail options and the confirmation email settings only for a CMMN
zaaktype. For a BPMN zaaktype, the mail step SHALL show a collapsed confirmation email section without fields,
with the text that the BPMN process sends the confirmation of receipt.

#### Scenario: BPMN zaaktype gets mailtemplate koppelingen and zaakafzenders
- **GIVEN** a zaaktype whose configuration is bound to BPMN
- **WHEN** a beheerder adds a mailtemplate koppeling and a zaakafzender in the mail step and saves
- **THEN** the stored configuration has that koppeling and that zaakafzender

#### Scenario: BPMN zaaktype explains the confirmation email
- **GIVEN** a zaaktype whose configuration is bound to BPMN
- **WHEN** a beheerder opens the mail step
- **THEN** the step shows no intake or afronden mail options and no confirmation email fields, and shows a
  collapsed section with the text that the BPMN process sends the confirmation of receipt

#### Scenario: CMMN zaaktype keeps its full mail step
- **GIVEN** a zaaktype whose configuration is bound to CMMN
- **WHEN** a beheerder opens the mail step
- **THEN** the step shows the intake and afronden mail options, the confirmation email settings, the
  mailtemplate koppelingen, and the zaakafzenders

### Requirement: Saving stores the complete configuration

When a beheerder saves the configuration, the system SHALL store every setting of every step in one request,
including the steps that the beheerder did not open. The screen SHALL show the stored values after the save.

#### Scenario: Unopened step keeps its stored values
- **GIVEN** a zaaktype configuration with SmartDocuments template mappings and BRP doelbindingen
- **WHEN** a beheerder changes only the default groep in the general step and saves
- **THEN** the stored configuration has the new default groep and still has its SmartDocuments template mappings
  and BRP doelbindingen
