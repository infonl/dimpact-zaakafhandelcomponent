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
