# zaak-creation-zaaktype-choice Specification

## Purpose

Defines which zaaktypes a user can choose from when creating a zaak, what the list of those zaaktypes carries, and
how the Zaak aanmaken form reads and applies the settings of the zaaktype that the user chose.

## Requirements

### Requirement: The zaaktype list for zaak creation carries only what the choice needs

`GET /rest/zaaktypen?startbaar=true` SHALL return, for each zaaktype, only its `uuid` and its `omschrijving`.
It SHALL offer the same zaaktypes as before: the zaaktypes of the default catalogus that are not a concept, that are
valid today, whose configuration is valid for zaak creation, and for which the user may start a zaak.

#### Scenario: A list item has only the UUID and the omschrijving
- **GIVEN** a zaaktype *Melding openbare ruimte* that the user may start and whose configuration is valid
- **WHEN** the user lists the zaaktypes for zaak creation
- **THEN** the item for *Melding openbare ruimte* has its `uuid` and its `omschrijving`
- **AND** it has no other fields, such as the vertrouwelijkheidaanduiding, the zaaktype relaties or the zaaktype
  configuration

#### Scenario: A zaaktype the user may not start is not listed
- **GIVEN** a zaaktype with a valid configuration for which the user may not start a zaak
- **WHEN** the user lists the zaaktypes for zaak creation
- **THEN** the zaaktype is not in the list

#### Scenario: A zaaktype without a valid configuration is not listed
- **GIVEN** a zaaktype that the user may start, whose configuration has no groep
- **WHEN** the user lists the zaaktypes for zaak creation
- **THEN** the zaaktype is not in the list

### Requirement: The zaaktype collection lists the default catalogus

`GET /rest/zaaktypen` without the `startbaar` filter SHALL return every zaaktype of the default catalogus, each with
only its `uuid` and its `omschrijving`, in the order of the ZTC. With `startbaar=true` it SHALL return only the
zaaktypes that are offered for zaak creation. With `startbaar=false` it SHALL return the zaaktypes of the default
catalogus that are not offered for zaak creation.

#### Scenario: Without a filter every zaaktype of the catalogus is listed
- **GIVEN** a default catalogus with a zaaktype A that the user may start, a concept zaaktype B and a zaaktype C for
  which the user may not start a zaak
- **WHEN** the user reads `GET /rest/zaaktypen`
- **THEN** the list has A, B and C, each with only its `uuid` and its `omschrijving`

#### Scenario: startbaar=false lists the zaaktypes that are not offered
- **GIVEN** the same default catalogus
- **WHEN** the user reads `GET /rest/zaaktypen?startbaar=false`
- **THEN** the list has B and C, and not A

### Requirement: The settings of the chosen zaaktype are read separately

`GET /rest/zaaktypen/{zaaktypeUuid}` SHALL return the settings of one zaaktype that the Zaak
aanmaken form applies: its `uuid`, its `omschrijving`, its `vertrouwelijkheidaanduiding`, the `defaultGroepId` and
the `defaultBehandelaarId` of its configuration, and whether its configuration allows a KVK and a BRP initiator. It
SHALL answer only for a zaaktype that `GET /rest/zaaktypen?startbaar=true` offers to the user. It SHALL refuse the request
with HTTP 403 when the user may not start a zaak of the zaaktype, and SHALL answer HTTP 404 when the zaaktype does
not exist or is not offered for zaak creation for another reason.

#### Scenario: The settings of an offered zaaktype are returned
- **GIVEN** a zaaktype `U` that the user may start, with vertrouwelijkheidaanduiding *openbaar*, default groep
  `G`, default behandelaar `B`, the KVK koppeling switched on and the BRP koppeling switched off
- **WHEN** the user reads `GET /rest/zaaktypen/U`
- **THEN** the response has `uuid` `U`, its omschrijving, vertrouwelijkheidaanduiding *openbaar*, `defaultGroepId`
  `G`, `defaultBehandelaarId` `B`, the KVK koppeling switched on and the BRP koppeling switched off

#### Scenario: A zaaktype the user may not start is refused
- **GIVEN** a zaaktype `U` for which the user may not start a zaak
- **WHEN** the user reads `GET /rest/zaaktypen/U`
- **THEN** the response is HTTP 403 and has no settings

#### Scenario: A zaaktype that is not offered is not found
- **GIVEN** a zaaktype `U` that the user may start, but whose configuration is not valid for zaak creation
- **WHEN** the user reads `GET /rest/zaaktypen/U`
- **THEN** the response is HTTP 404

### Requirement: The Zaak aanmaken form applies the settings of the chosen zaaktype after the choice

The Zaak aanmaken form SHALL fill its zaaktype field from the list for zaak creation, and SHALL read the settings of
a zaaktype when the user chooses it. When the settings arrive, the form SHALL apply them as it did before this
change: it SHALL set the vertrouwelijkheidaanduiding, select the default groep when that groep is offered, select
the default behandelaar when that user is in the groep, and allow an initiator only when the KVK or the BRP
koppeling of the zaaktype is switched on. The form SHALL NOT be submitted before the settings of the chosen
zaaktype are applied. When the zaak is created, the form SHALL send only the UUID of the chosen zaaktype.

#### Scenario: Choosing a zaaktype applies its settings
- **GIVEN** a zaaktype with vertrouwelijkheidaanduiding *openbaar*, default groep *Afdeling A* and default
  behandelaar *Jan*, a member of *Afdeling A*
- **WHEN** the user chooses that zaaktype in the Zaak aanmaken form
- **THEN** the form shows vertrouwelijkheidaanduiding *openbaar*, groep *Afdeling A* and behandelaar *Jan*

#### Scenario: An initiator is allowed only when a koppeling is switched on
- **GIVEN** a zaaktype with the KVK and the BRP koppeling switched off
- **WHEN** the user chooses that zaaktype after adding an initiator under another zaaktype
- **THEN** the initiator is removed and the user cannot add an initiator

#### Scenario: Changing the zaaktype applies the settings of the new choice
- **GIVEN** the user chose zaaktype A with default groep *Afdeling A*
- **WHEN** the user chooses zaaktype B with default groep *Afdeling B*
- **THEN** the form shows the settings of zaaktype B, including groep *Afdeling B*

#### Scenario: The form waits for the settings before it can be submitted
- **GIVEN** the user chose a zaaktype and its settings have not arrived yet
- **WHEN** the user tries to submit the form
- **THEN** the form cannot be submitted

#### Scenario: The created zaak gets the chosen zaaktype
- **WHEN** the user creates a zaak with zaaktype `U`
- **THEN** the request to create the zaak names zaaktype `U` by its UUID, and the zaak is created with zaaktype `U`

#### Scenario: A gekoppelde zaak still offers only the allowed zaaktypes
- **GIVEN** a zaak whose zaaktype lists zaaktypen A and B as deelzaaktypen, and a user who may start A and C
- **WHEN** the user creates a deelzaak from the Zaak koppelen sidebar
- **THEN** the zaaktype field offers only A, and choosing A applies the settings of A
- **AND** a copied initiator is kept only when the koppeling of A for that kind of initiator is switched on
