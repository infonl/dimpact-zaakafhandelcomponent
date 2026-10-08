## MODIFIED Requirements

### Requirement: Both engines are validated the same way

The system SHALL apply the same validation rules to the engine-agnostic settings of a CMMN configuration and
of a BPMN configuration. It SHALL reject an invalid configuration with a validation error and store nothing.
An update for a zaaktype UUID that already has a configuration SHALL update that configuration and SHALL NOT
create a second one.

A configuration that the REST resource accepts but that breaks a constraint of the stored configuration or of any
of its parts SHALL be rejected, and the system SHALL store nothing of it. This applies to every part of the
configuration, whichever part breaks the constraint.

#### Scenario: BPMN configuration without groep is rejected
- **WHEN** a beheerder stores a BPMN zaaktype configuration without a groep
- **THEN** the request fails with a validation error and no configuration is stored

#### Scenario: CMMN configuration without groep is rejected
- **GIVEN** a CMMN zaaktype with a stored configuration
- **WHEN** a beheerder stores a configuration for it without a default groep
- **THEN** the request fails and the stored configuration keeps its default groep

#### Scenario: CMMN configuration with a zaakafzender without e-mail address is rejected
- **GIVEN** a CMMN zaaktype with a stored configuration
- **WHEN** a beheerder stores a configuration for it with a zaakafzender whose e-mail address is blank
- **THEN** the request fails with a validation error and the stored configuration is unchanged

#### Scenario: Update with an unknown id does not create a duplicate
- **GIVEN** a stored configuration for zaaktype UUID `U`
- **WHEN** a beheerder stores a configuration for `U` with an id that does not exist
- **THEN** the existing configuration for `U` is updated and `U` still has exactly one configuration
