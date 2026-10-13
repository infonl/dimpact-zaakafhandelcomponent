## ADDED Requirements

### Requirement: An employee with edit rights can add a medewerker to a zaakspecifiek geautoriseerde zaak

An employee with the `wijzigen` right on a zaakspecifiek geautoriseerde zaak SHALL be able to add a
medewerker through `POST /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers`, by choosing a
groep and a medewerker of that groep. ZAC SHALL then add a *Zaakspecifiek geautoriseerde medewerker* rol for
that medewerker to the zaak in Open Zaak, before the request returns. The rol SHALL be identical to the one a
previous zaak- or taakbehandelaar receives. The audit toelichting SHALL be
"Zaakspecifiek geautoriseerd medewerker van zaak {zaaknummer}".

#### Scenario: Adding a medewerker
- **GIVEN** a zaakspecifiek geautoriseerde zaak and an employee with `wijzigen` on it
- **AND** medewerker A in groep G, which holds the `behandelaar` application role for the zaaktype
- **WHEN** the employee adds A from G
- **THEN** the zaak has a *Zaakspecifiek geautoriseerde medewerker* rol for A

#### Scenario: Adding to a zaak that is not zaakspecifiek geautoriseerd
- **WHEN** an employee adds a medewerker to a zaak that is not zaakspecifiek geautoriseerd
- **THEN** the request is refused and no rol is added

#### Scenario: Adding without edit rights
- **WHEN** an employee without `wijzigen` on the zaak adds a medewerker
- **THEN** the request is refused with the generic insufficient-rights response

#### Scenario: Adding from a groep without the behandelaar role
- **WHEN** an employee adds a medewerker from a groep that does not hold the `behandelaar` application role for
  the zaaktype, or a medewerker who is not in the chosen groep
- **THEN** the request is refused and no rol is added

#### Scenario: Adding when the zaaktype lacks the roltype
- **WHEN** an employee adds a medewerker to a zaak whose zaaktype lacks the *Zaakspecifiek geautoriseerde
  medewerker* roltype
- **THEN** the request fails with `ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ROLTYPE_NOT_FOUND`

### Requirement: Only medewerkers without access can be chosen

`GET /rest/zaken/zaak/{uuid}/zaakspecifiek-geautoriseerde-medewerkers/kandidaten?groepId={groepId}` SHALL
return the medewerkers of a groep with the `behandelaar` application role for the zaaktype, sorted by name,
without paging. It SHALL leave out every medewerker who already has access to the zaak:
- the zaakbehandelaar;
- every holder of a *Zaakspecifiek geautoriseerde medewerker* rol on the zaak;
- every member of a groep with the `zaakspecifiek_geautoriseerd` application role for the zaaktype.

Adding such a medewerker anyway SHALL be refused with
`ERROR_CODE_MEDEWERKER_ALREADY_ZAAKSPECIFIEK_GEAUTORISEERD`.

#### Scenario: The candidates leave out the zaakbehandelaar and existing rol holders
- **GIVEN** a groep with medewerkers Z, A and B, where Z is the zaakbehandelaar and A holds the rol
- **WHEN** an employee requests the candidates of that groep
- **THEN** only B is returned

#### Scenario: The candidates leave out medewerkers with access through IAM
- **GIVEN** medewerker C, who is in a groep with `zaakspecifiek_geautoriseerd` for the zaaktype
- **WHEN** an employee requests the candidates of a behandelaar groep that also contains C
- **THEN** C is not returned

#### Scenario: Adding a medewerker who already has access
- **WHEN** an employee adds a medewerker who is the zaakbehandelaar or already holds the rol
- **THEN** the request fails with `ERROR_CODE_MEDEWERKER_ALREADY_ZAAKSPECIFIEK_GEAUTORISEERD`
- **AND** no new *Zaakspecifiek geautoriseerde medewerker* rol is added for them

### Requirement: An added medewerker can use their own application role on the zaak

An added medewerker SHALL get the rights of the application role they hold for the zaaktype on the zaak, its
taken and its documenten. When those rights include `wijzigen`, as the `behandelaar` role does on an open zaak,
they SHALL be able to add further medewerkers. They SHALL find the zaak, its taken and
its documenten in the werkvoorraden and zoekresultaten without a manual reindex.

#### Scenario: The added medewerker edits the zaak and adds another medewerker
- **GIVEN** medewerker A, with `behandelaar` but not `zaakspecifiek_geautoriseerd`, who was added to an open zaak
- **WHEN** rechten are computed for A on the zaak
- **THEN** `lezen` and `wijzigen` are `true`
- **AND** A can add medewerker B to the zaak

#### Scenario: The added medewerker finds the zaak
- **WHEN** medewerker A was just added to a zaakspecifiek geautoriseerde zaak
- **THEN** A finds the zaak in the zakenwerkvoorraad and in the zoekresultaten

### Requirement: The addition shows in the zaakhistorie but not as betrokkene

The zaakhistorie SHALL show the added rol with the label *Zaakspecifiek geautoriseerde medewerker* and the
medewerker's name. The betrokkenen tab SHALL NOT list the added medewerker.

#### Scenario: Viewing the zaak after adding a medewerker
- **GIVEN** medewerker A was added to a zaakspecifiek geautoriseerde zaak
- **WHEN** an employee opens the zaakhistorie and the betrokkenen tab
- **THEN** the zaakhistorie shows a *Zaakspecifiek geautoriseerde medewerker* line with A's name
- **AND** the betrokkenen tab does not list A

### Requirement: The zaak view offers "Medewerker toevoegen" only on a zaakspecifiek geautoriseerde zaak

The zaak side menu SHALL show a *Medewerker toevoegen* item under *Koppelingen* only when the zaak is
zaakspecifiek geautoriseerd and the employee has `wijzigen` on it. It SHALL open a side panel with a groep
select, listing the `behandelaar` groepen for the zaaktype, and a medewerker select, listing the candidates of
the chosen groep. After a successful addition the panel SHALL close and show a confirmation.

#### Scenario: The menu item on a zaak that is not zaakspecifiek geautoriseerd
- **WHEN** an employee opens a zaak that is not zaakspecifiek geautoriseerd
- **THEN** the side menu has no *Medewerker toevoegen* item

#### Scenario: Choosing a medewerker
- **GIVEN** the *Medewerker toevoegen* panel on a zaakspecifiek geautoriseerde zaak
- **WHEN** the employee picks a groep
- **THEN** the medewerker select lists the candidates of that groep
- **AND** submitting adds the chosen medewerker and closes the panel
