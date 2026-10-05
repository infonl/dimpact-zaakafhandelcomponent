## Purpose

Defines how ZAC stores, validates, versions, and applies the configuration of a zaaktype version
(the zaakafhandelparameters), so that CMMN and BPMN zaaktypen behave the same wherever the setting does not
describe the process engine itself.

## ADDED Requirements

### Requirement: The configuration REST contract stays unchanged

The system SHALL keep the paths, HTTP verbs, path keys, request payloads, and response payloads of
`/zaakafhandelparameters` and `/zaaktype-bpmn-configuration` exactly as they were before this change. The
generated OpenAPI specification SHALL NOT change.

#### Scenario: OpenAPI specification is unchanged
- **WHEN** the OpenAPI specification is generated from the changed backend
- **THEN** it is identical to the specification that was generated before the change

#### Scenario: Shared fields round-trip through both resources
- **GIVEN** a BPMN zaaktype configuration that was stored through `POST /zaaktype-bpmn-configuration`
- **WHEN** a client reads it through `GET /zaakafhandelparameters/{zaaktypeUuid}` and through
  `GET /zaaktype-bpmn-configuration/{processDefinitionKey}`
- **THEN** both responses contain the same values for groep, default behandelaar, productaanvraagtype,
  SmartDocuments toggle, betrokkene koppelingen, BRP doelbindingen, niet-ontvankelijk resultaattype, and
  zaakbeeindig parameters

### Requirement: A BPMN update preserves settings that its payload does not carry

When the system stores a BPMN zaaktype configuration through `POST /zaaktype-bpmn-configuration`, the system
SHALL keep the stored values of every engine-agnostic setting that the BPMN payload does not contain. These
are the deadline warning windows, the confirmation email parameters, the zaakafzenders, and the mailtemplate
koppelingen.

#### Scenario: Deadline warning windows survive a BPMN update
- **GIVEN** a stored BPMN zaaktype configuration with an einddatum-gepland warning window of 3 days
- **WHEN** a beheerder changes its groep through `POST /zaaktype-bpmn-configuration`
- **THEN** the stored configuration has the new groep and still has a warning window of 3 days

### Requirement: Both engines are validated the same way

The system SHALL apply the same validation rules to the engine-agnostic settings of a CMMN configuration and
of a BPMN configuration. It SHALL reject an invalid configuration with a validation error and store nothing.
An update for a zaaktype UUID that already has a configuration SHALL update that configuration and SHALL NOT
create a second one.

#### Scenario: BPMN configuration without groep is rejected
- **WHEN** a beheerder stores a BPMN zaaktype configuration without a groep
- **THEN** the request fails with a validation error and no configuration is stored

#### Scenario: Update with an unknown id does not create a duplicate
- **GIVEN** a stored configuration for zaaktype UUID `U`
- **WHEN** a beheerder stores a configuration for `U` with an id that does not exist
- **THEN** the existing configuration for `U` is updated and `U` still has exactly one configuration

### Requirement: A productaanvraagtype belongs to at most one zaaktype

The system SHALL reject storing a configuration whose productaanvraagtype is already used by the current
configuration of a zaaktype with a different omschrijving, whatever engine either configuration uses.
Versions of the same zaaktype, which share the omschrijving, SHALL be allowed to share a
productaanvraagtype.

#### Scenario: Productaanvraagtype in use by a CMMN zaaktype blocks a BPMN zaaktype
- **GIVEN** a CMMN zaaktype "Melding" whose current configuration uses productaanvraagtype `P`
- **WHEN** a beheerder stores a BPMN configuration for zaaktype "Vergunning" with productaanvraagtype `P`
- **THEN** the request fails with error code `ERROR_CODE_PRODUCTAANVRAAGTYPE_ALREADY_IN_USE`

#### Scenario: New version of a BPMN zaaktype keeps its productaanvraagtype
- **GIVEN** a BPMN zaaktype "Vergunning" whose current configuration uses productaanvraagtype `P`
- **WHEN** a beheerder stores the configuration of a new version of "Vergunning" with productaanvraagtype `P`
- **THEN** the configuration is stored

### Requirement: A new zaaktype version inherits the configuration of its predecessor

When ZAC receives a notification that a zaaktype version was published, and that version has no
configuration yet, the system SHALL create its configuration from the most recently created configuration of a
zaaktype with the same omschrijving. It SHALL copy every engine-agnostic setting and the process binding, for
CMMN and BPMN alike. For a CMMN zaaktype it SHALL also copy the CMMN extension. When no configuration with
that omschrijving exists, the system SHALL create nothing.

#### Scenario: Engine-agnostic settings are copied for both engines
- **GIVEN** a CMMN or a BPMN configuration for version 1 of zaaktype "Melding", with groep, default
  behandelaar, productaanvraagtype, SmartDocuments toggle and template mappings, betrokkene koppelingen, BRP
  doelbindingen, deadline warning windows, confirmation email parameters, zaakafzenders, and mailtemplate
  koppelingen
- **WHEN** ZAC receives the notification for version 2 of "Melding"
- **THEN** the configuration of version 2 has the same values for all of these settings and is bound to the
  same engine and definition key

#### Scenario: Zaaktype without a previous configuration
- **WHEN** ZAC receives the notification for a zaaktype whose omschrijving has no configuration
- **THEN** no configuration is created and the notification is acknowledged

#### Scenario: Notification for an older version does not overwrite a newer one
- **GIVEN** configurations for version 1 and version 2 of "Melding"
- **WHEN** ZAC receives a second notification for version 1
- **THEN** the configuration of version 2 is unchanged

### Requirement: Resultaattype references follow the omschrijving

The system SHALL identify the resultaattype of the niet-ontvankelijk setting and of every zaakbeeindig
parameter by its omschrijving within the zaaktype version that the configuration belongs to. When a new
version inherits a configuration, a reference whose omschrijving does not exist in the new version SHALL be
dropped. The REST resources SHALL keep exposing and accepting resultaattype UUIDs.

#### Scenario: Zaakbeeindig parameter is remapped to the new version
- **GIVEN** version 1 of "Melding" with a zaakbeeindig parameter that points at resultaattype "Toegekend"
- **WHEN** version 2 inherits the configuration and has its own resultaattype "Toegekend" with a new UUID
- **THEN** the configuration of version 2 returns the UUID of the "Toegekend" resultaattype of version 2

#### Scenario: Unknown omschrijving is dropped
- **GIVEN** version 1 of "Melding" with a zaakbeeindig parameter that points at resultaattype "Ingetrokken"
- **WHEN** version 2 inherits the configuration and has no resultaattype "Ingetrokken"
- **THEN** the configuration of version 2 has no zaakbeeindig parameter for that reden

#### Scenario: Existing rows are backfilled
- **GIVEN** a configuration stored before this change, which holds only resultaattype UUIDs
- **WHEN** the backfill runs while ZTC can still resolve those UUIDs
- **THEN** each reference also holds the omschrijving of its resultaattype

### Requirement: Engine-agnostic settings apply to BPMN zaken

The system SHALL apply these settings to a zaak of a BPMN zaaktype in the same way as to a zaak of a CMMN
zaaktype: the BRP doelbindingen, the deadline warning windows, and the zaakafzenders.

#### Scenario: BRP query of a BPMN zaak uses its doelbinding
- **GIVEN** a BPMN zaaktype configuration with BRP zoek-doelbinding `D`
- **WHEN** a user searches BRP personen in the context of a zaak of that zaaktype
- **THEN** the BRP request uses doelbinding `D` and not the default from the environment

#### Scenario: BPMN zaak gets a deadline warning
- **GIVEN** a BPMN zaaktype configuration with a uiterlijke-einddatum-afdoening warning window of 2 days
- **WHEN** a zaak of that zaaktype is 1 day from its uiterlijke einddatum afdoening
- **THEN** the zaak is listed by the zaak warnings endpoint and its due-date signalering is sent

#### Scenario: BPMN zaak lists the configured afzenders
- **GIVEN** a BPMN zaaktype configuration with a zaakafzender
- **WHEN** a user lists the afzenders for a zaak of that zaaktype
- **THEN** the configured zaakafzender is in the list

### Requirement: One answer for the configuration of a zaaktype

The system SHALL use the same configuration of a zaaktype UUID to decide whether the zaaktype can be used to
create a zaak, to report the zaaktype in the health check, to render the zaaktype, and to create the zaak. A
configuration SHALL be valid for zaak creation when it has a groep and a process binding with a definition
key. A CMMN configuration SHALL also have a niet-ontvankelijk resultaattype.

#### Scenario: Configuration without a process binding is not offered
- **GIVEN** a configuration for zaaktype UUID `U` without a process binding
- **WHEN** a user lists the zaaktypen for zaak creation
- **THEN** `U` is not in the list, and the health check reports `U` as not valid
