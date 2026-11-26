## ADDED Requirements

### Requirement: ZAC signals a handled zaaktype notification to the open list page

After ZAC has handled a `create` or `update` notification about a zaaktype, it SHALL send the screen event
`ZAAKTYPE_CONFIGURATIES` with opcode `UPDATED` and id `zaaktype-configuraties`. ZAC SHALL send it only after the
configuration change of that notification is committed. ZAC SHALL NOT send it for a `destroy` notification about a
zaaktype, nor when handling the notification fails.

#### Scenario: A published zaaktype version is signalled after its configuration is stored
- **GIVEN** a zaaktype with a configuration in ZAC
- **WHEN** ZAC handles the `update` notification about a new, published version of that zaaktype
- **THEN** ZAC creates the configuration of the new version
- **AND** ZAC sends the `ZAAKTYPE_CONFIGURATIES` screen event after that configuration is committed

#### Scenario: A deleted zaaktype version is not signalled
- **WHEN** ZAC handles a `destroy` notification about a zaaktype
- **THEN** ZAC does not send the `ZAAKTYPE_CONFIGURATIES` screen event

#### Scenario: A failed notification is not signalled
- **GIVEN** an `update` notification about a zaaktype that ZAC cannot read from Open Zaak
- **WHEN** ZAC handles the notification
- **THEN** ZAC does not send the `ZAAKTYPE_CONFIGURATIES` screen event

## MODIFIED Requirements

### Requirement: The configuration REST contract stays unchanged

The system SHALL keep the paths, HTTP verbs, path keys, request payloads, and response payloads of
`/zaakafhandelparameters` and `/zaaktype-bpmn-configuration` exactly as they were before this change, except the
response of the list `GET /zaakafhandelparameters`, which the requirement "The configuration list carries only what
the list page shows" defines. The generated OpenAPI specification SHALL NOT change, except that `defaultGroepId` of
`RestZaaktypeConfiguration` is required and must not be blank, except the list item type of
`GET /zaakafhandelparameters`, and except the value `ZAAKTYPE_CONFIGURATIES` of `ScreenEventType`.

#### Scenario: OpenAPI specification is unchanged apart from the required default groep
- **WHEN** the OpenAPI specification is generated from the changed backend
- **THEN** it is identical to the specification that was generated before the change, except that
  `RestZaaktypeConfiguration` lists `defaultGroepId` as required, that `GET /zaakafhandelparameters` returns
  a list of `RestZaaktypeConfigurationOverzicht`, and that `ScreenEventType` has the value `ZAAKTYPE_CONFIGURATIES`

#### Scenario: Shared fields round-trip through both resources
- **GIVEN** a BPMN zaaktype configuration that was stored through `POST /zaaktype-bpmn-configuration`
- **WHEN** a client reads it through `GET /zaakafhandelparameters/{zaaktypeUuid}` and through
  `GET /zaaktype-bpmn-configuration/{processDefinitionKey}`
- **THEN** both responses contain the same values for groep, default behandelaar, productaanvraagtype,
  SmartDocuments toggle, betrokkene koppelingen, BRP doelbindingen, niet-ontvankelijk resultaattype, and
  zaakbeeindig parameters

### Requirement: The list page shows the configurations as they are now

The Zaakafhandelparameters page SHALL read its list each time a beheerder opens the page. While the page is open,
it SHALL read its list again when it receives the `ZAAKTYPE_CONFIGURATIES` screen event, so that it shows a zaaktype
version for which ZAC created a configuration in the meantime. The page SHALL NOT read its list again because its
browser tab or window gets the focus. While the list is read again, the page SHALL keep showing the rows it read
before.

#### Scenario: A new zaaktype version shows when the beheerder opens the list again
- **GIVEN** a beheerder who opened the Zaakafhandelparameters page
- **AND** a new version of a zaaktype was published in Open Zaak afterwards, and ZAC created its configuration
- **WHEN** the beheerder opens the page again
- **THEN** the page shows the new version with the procesmodel it took over, and its row links to the edit page of
  the new version

#### Scenario: A new zaaktype version shows while the page is open
- **GIVEN** a beheerder with the Zaakafhandelparameters page open
- **WHEN** a new version of a zaaktype is published in Open Zaak, and ZAC creates its configuration and sends the
  `ZAAKTYPE_CONFIGURATIES` screen event
- **THEN** the page shows the new version with the procesmodel it took over, without a reload

#### Scenario: The page does not read its list again on focus
- **GIVEN** a beheerder with the Zaakafhandelparameters page open
- **WHEN** the beheerder comes back to its browser tab or window
- **THEN** the page does not read its list again
