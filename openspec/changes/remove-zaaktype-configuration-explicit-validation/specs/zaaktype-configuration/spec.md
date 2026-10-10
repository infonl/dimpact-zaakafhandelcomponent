## MODIFIED Requirements

### Requirement: The configuration REST contract stays unchanged

The system SHALL keep the paths, HTTP verbs, path keys, request payloads, and response payloads of
`/zaakafhandelparameters` and `/zaaktype-bpmn-configuration` exactly as they were before this change. The
generated OpenAPI specification SHALL NOT change, except that `defaultGroepId` of `RestZaaktypeConfiguration` is
required and must not be blank.

#### Scenario: OpenAPI specification is unchanged apart from the required default groep
- **WHEN** the OpenAPI specification is generated from the changed backend
- **THEN** it is identical to the specification that was generated before the change, except that
  `RestZaaktypeConfiguration` lists `defaultGroepId` as required

#### Scenario: Shared fields round-trip through both resources
- **GIVEN** a BPMN zaaktype configuration that was stored through `POST /zaaktype-bpmn-configuration`
- **WHEN** a client reads it through `GET /zaakafhandelparameters/{zaaktypeUuid}` and through
  `GET /zaaktype-bpmn-configuration/{processDefinitionKey}`
- **THEN** both responses contain the same values for groep, default behandelaar, productaanvraagtype,
  SmartDocuments toggle, betrokkene koppelingen, BRP doelbindingen, niet-ontvankelijk resultaattype, and
  zaakbeeindig parameters

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
- **THEN** the request fails with a validation error on the default groep and the stored configuration keeps its
  default groep

#### Scenario: CMMN configuration with a zaakafzender without e-mail address is rejected
- **GIVEN** a CMMN zaaktype with a stored configuration
- **WHEN** a beheerder stores a configuration for it with a zaakafzender whose e-mail address is blank
- **THEN** the request fails with a validation error on the zaakafzender e-mail address and the stored
  configuration is unchanged

#### Scenario: Update with an unknown id does not create a duplicate
- **GIVEN** a stored configuration for zaaktype UUID `U`
- **WHEN** a beheerder stores a configuration for `U` with an id that does not exist
- **THEN** the existing configuration for `U` is updated and `U` still has exactly one configuration
