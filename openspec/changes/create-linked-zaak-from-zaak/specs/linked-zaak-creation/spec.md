## Purpose

Lets a zaakbehandelaar create a new zaak from an existing zaak, linked to it as gerelateerde zaak or as deelzaak, so
that the relation between the two zaken is recorded at the moment the new zaak is made.

## ADDED Requirements

### Requirement: The Zaak koppelen sidebar offers to create a new zaak

The *Zaak koppelen* sidebar on the zaak detail page SHALL show two tabs: *Bestaande zaak koppelen*, which holds the
existing search and link form, and *Nieuwe zaak aanmaken*. The *Nieuwe zaak aanmaken* tab SHALL be available only when
the current zaak is open, the employee has the `koppelen` right on the current zaak, and the employee may start a zaak
of at least one zaaktype.

#### Scenario: Employee opens Zaak koppelen on an open zaak

- **WHEN** an employee with the `koppelen` right and the right to start zaken opens *Zaak koppelen* in the action menu
  of an open zaak
- **THEN** the sidebar shows the tabs *Bestaande zaak koppelen* and *Nieuwe zaak aanmaken*
- **AND** *Bestaande zaak koppelen* is the selected tab

#### Scenario: Zaak is closed

- **WHEN** an employee opens *Zaak koppelen* on a closed zaak
- **THEN** the *Nieuwe zaak aanmaken* tab is not available

#### Scenario: Employee may not start any zaak

- **WHEN** an employee who may not start a zaak of any zaaktype opens *Zaak koppelen*
- **THEN** the *Nieuwe zaak aanmaken* tab is not available

### Requirement: The employee chooses the relation before the new zaak is made

In the *Nieuwe zaak aanmaken* tab the employee SHALL choose a *Soort koppeling* before the zaak form can be submitted.
The choices SHALL be *Gerelateerde zaak* and *Deelzaak*, where *Deelzaak* means that the new zaak becomes a deelzaak of
the current zaak. *Deelzaak* SHALL NOT be selectable when the current zaak is itself a deelzaak, and the tab SHALL
say why.

#### Scenario: Choose a gerelateerde zaak

- **WHEN** the employee selects *Gerelateerde zaak*
- **THEN** the tab says that the new zaak is linked to the current zaak as gerelateerde zaak
- **AND** the *Zaak aanmaken* form shows below the choice

#### Scenario: Current zaak is a deelzaak

- **WHEN** the employee opens the tab on a zaak that is a deelzaak of another zaak
- **THEN** *Deelzaak* cannot be selected
- **AND** the tab explains that a deelzaak cannot have deelzaken

### Requirement: The offered zaaktypen depend on the relation and on the employee's rights

The zaaktype field SHALL offer only zaaktypen for which the employee may start a zaak and which are ready for zaak
creation. For *Deelzaak* it SHALL further offer only the zaaktypen that the zaaktype of the current zaak lists as its
deelzaaktypen. For *Gerelateerde zaak* it SHALL offer every zaaktype from the first rule. Changing the relation SHALL
clear a selected zaaktype that is no longer offered.

#### Scenario: Deelzaak offers only deelzaaktypen

- **WHEN** the current zaaktype lists zaaktypen A and B as deelzaaktypen, the employee may start A and C, and the
  employee selects *Deelzaak*
- **THEN** the zaaktype field offers only A

#### Scenario: No deelzaaktype is available

- **WHEN** the employee selects *Deelzaak* and no deelzaaktype of the current zaaktype can be started by the employee
- **THEN** the zaaktype field offers no zaaktypen
- **AND** the tab says that no zaaktype is available for a deelzaak of this zaak

#### Scenario: Switching the relation clears an unavailable zaaktype

- **WHEN** the employee selected zaaktype C under *Gerelateerde zaak* and then selects *Deelzaak*, and C is not a
  deelzaaktype of the current zaaktype
- **THEN** the zaaktype field is empty

### Requirement: The initiator of the current zaak is copied to the new zaak

When the current zaak has an initiator, the *Nieuwe zaak aanmaken* form SHALL show that initiator, read-only, and the
new zaak SHALL get the same initiator. When the selected zaaktype does not allow an initiator of that kind, the new
zaak SHALL be created without an initiator and the form SHALL say so.

#### Scenario: Initiator is copied

- **WHEN** the current zaak has a person as initiator, the selected zaaktype allows persons as initiator, and the
  employee creates the new zaak
- **THEN** the new zaak has the same person as initiator

#### Scenario: Zaaktype does not allow the initiator

- **WHEN** the current zaak has a company as initiator and the selected zaaktype does not allow companies as initiator
- **THEN** the form says that the initiator is not copied for this zaaktype
- **AND** the new zaak is created without an initiator

### Requirement: Creating a zaak with a link checks the link before the zaak is created

The zaak creation endpoint SHALL accept an optional zaak to link to, with the relation type `GERELATEERD` or
`DEELZAAK`. Before it creates the new zaak, ZAC SHALL check that the employee may read and link the zaak to link to,
that this zaak is open, and, for `DEELZAAK`, that this zaak is not a deelzaak and that its zaaktype lists the zaaktype
of the new zaak as deelzaaktype. When a check fails, ZAC SHALL create no zaak and SHALL return an error. Any other
relation type SHALL be rejected. Without a zaak to link to, zaak creation SHALL work as before.

#### Scenario: Link to a zaak without the koppelen right

- **WHEN** an employee without the `koppelen` right on zaak X asks to create a new zaak linked to X
- **THEN** the request is refused as not authorised
- **AND** no zaak is created

#### Scenario: Deelzaak of a zaaktype that is not a deelzaaktype

- **WHEN** an employee asks to create a zaak of zaaktype C as deelzaak of zaak X, and the zaaktype of X does not list C
  as deelzaaktype
- **THEN** the request is refused with a validation error
- **AND** no zaak is created

#### Scenario: Unsupported relation type

- **WHEN** an employee asks to create a new zaak linked to zaak X with relation type `HOOFDZAAK`
- **THEN** the request is refused with a validation error
- **AND** no zaak is created

#### Scenario: Zaak creation without a link

- **WHEN** an employee creates a zaak without a zaak to link to
- **THEN** the zaak is created as before, without a hoofdzaak and without gerelateerde zaken

### Requirement: Both zaken show the relation

After a new zaak is created as *Gerelateerde zaak* of zaak X, the *Gerelateerde zaken* of the new zaak SHALL list X
and the *Gerelateerde zaken* of X SHALL list the new zaak, both with relation *Gerelateerde zaak*. After a new zaak is
created as *Deelzaak* of zaak X, the new zaak SHALL list X as hoofdzaak and X SHALL list the new zaak as deelzaak.

#### Scenario: Gerelateerde zaak is shown on both zaken

- **WHEN** an employee creates zaak Y from zaak X as *Gerelateerde zaak*
- **THEN** the zaak detail page of X lists Y under *Gerelateerde zaken* as gerelateerde zaak
- **AND** the zaak detail page of Y lists X under *Gerelateerde zaken* as gerelateerde zaak

#### Scenario: Deelzaak is shown on both zaken

- **WHEN** an employee creates zaak Y from zaak X as *Deelzaak*
- **THEN** the zaak detail page of X lists Y under *Gerelateerde zaken* as deelzaak
- **AND** the zaak detail page of Y lists X under *Gerelateerde zaken* as hoofdzaak

### Requirement: The history records the creation and the link

The history of the new zaak SHALL show its creation, including its hoofdzaak or its gerelateerde zaak.

#### Scenario: History after creating a gerelateerde zaak

- **WHEN** an employee creates zaak Y from zaak X as *Gerelateerde zaak*
- **THEN** the history of Y has a line for its creation that shows X as gerelateerde zaak

#### Scenario: History after creating a deelzaak

- **WHEN** an employee creates zaak Y from zaak X as *Deelzaak*
- **THEN** the history of Y has a line for its creation that shows X as hoofdzaak

### Requirement: After creation the employee stays on the current zaak

After the new zaak is created, the sidebar SHALL close, a message SHALL name the new zaak, and the current zaak SHALL
reload so that the new zaak shows in its *Gerelateerde zaken*, from where the employee can open it. *Annuleren* SHALL
close the sidebar without creating a zaak.

#### Scenario: Zaak created from the sidebar

- **WHEN** the employee submits the *Nieuwe zaak aanmaken* form with valid data
- **THEN** the sidebar closes
- **AND** a message names the identificatie of the new zaak
- **AND** the new zaak shows in the *Gerelateerde zaken* of the current zaak, with a link to open it

#### Scenario: Creation fails

- **WHEN** the server refuses the request
- **THEN** the sidebar stays open with the entered data
- **AND** the error is shown
