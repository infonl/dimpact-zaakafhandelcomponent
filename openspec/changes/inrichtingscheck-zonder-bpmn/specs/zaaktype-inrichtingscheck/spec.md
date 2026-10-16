## Purpose

Lets a beheerder see which zaaktypes are not set up correctly in Open Zaak and ZAC, without false errors for
zaaktypes the checks do not apply to.

## ADDED Requirements

### Requirement: The inrichtingscheck skips BPMN zaaktypes

The inrichtingscheck SHALL NOT check or show a zaaktype whose zaakafhandelparameters choose BPMN. It SHALL
check and show every other zaaktype that is not a concept and is currently valid, whether its
zaakafhandelparameters choose CMMN or it has no zaakafhandelparameters.

#### Scenario: A BPMN zaaktype is not shown
- **GIVEN** a valid zaaktype with BPMN in its zaakafhandelparameters, which lacks the statustype *Intake*
- **WHEN** a beheerder opens the inrichtingscheck
- **THEN** the zaaktype is not in the list and no error is reported for it

#### Scenario: A BPMN zaaktype is not checked
- **GIVEN** a valid zaaktype with BPMN in its zaakafhandelparameters
- **WHEN** the inrichtingscheck runs
- **THEN** ZAC reads none of its statustypen, resultaattypen, roltypen, informatieobjecttypen or besluittypen
  from Open Zaak

#### Scenario: A CMMN zaaktype is still checked
- **GIVEN** a valid zaaktype with CMMN in its zaakafhandelparameters, which lacks the statustype *Intake*
- **WHEN** a beheerder opens the inrichtingscheck
- **THEN** the zaaktype is in the list with the error for the missing statustype

#### Scenario: A zaaktype without zaakafhandelparameters is still checked
- **GIVEN** a valid zaaktype without zaakafhandelparameters
- **WHEN** a beheerder opens the inrichtingscheck
- **THEN** the zaaktype is in the list and reported as not valid

#### Scenario: Concept and expired zaaktypes stay hidden
- **WHEN** a beheerder opens the inrichtingscheck
- **THEN** a zaaktype that is a concept or not currently valid is not in the list

### Requirement: The inrichtingscheck offers no manual synchronisation

The inrichtingscheck SHALL NOT offer an action to synchronise the zaaktypecatalogus or to clear the cached
catalogue data, and SHALL NOT show when that was last done. The health-check REST operations SHALL NOT offer
either. The admin operation that clears all caches of ZAC stays available as a manual fallback.

#### Scenario: Opening the inrichtingscheck
- **WHEN** a beheerder opens the inrichtingscheck
- **THEN** there is no *Zaaktype catalogus synchronisatie* card and no synchronise button

#### Scenario: Calling the removed operations
- **WHEN** a client calls `DELETE /rest/health-check/ztc-cache` or `GET /rest/health-check/ztc-cache`
- **THEN** ZAC answers that the resource does not exist
