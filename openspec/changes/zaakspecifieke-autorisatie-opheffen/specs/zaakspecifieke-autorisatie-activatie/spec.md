## REMOVED Requirements

### Requirement: Zaakspecifieke autorisatie cannot be lifted
**Reason**: [PZ-12022](https://dimpact.atlassian.net/browse/PZ-12022) lets an employee lift the marking again.
**Migration**: A client that sends `isZaakspecifiekGeautoriseerd: false` for a marked zaak now lifts the marking
instead of getting `msg.error.zaakspecifieke.autorisatie.cannot.be.lifted`. Send no value to leave the marking
unchanged.

## ADDED Requirements

### Requirement: An employee with edit rights can lift the zaakspecifieke autorisatie of a zaak

The zaak update operation SHALL accept a request to mark a zaakspecifiek geautoriseerde zaak as no longer
zaakspecifiek geautoriseerd from every employee with `wijzigen` on the zaak. ZAC SHALL then, before the request
returns, delete every *Zaakspecifiek geautoriseerde medewerker* rol of the zaak and then its
`ZAAK_GEAUTORISEERD` zaakeigenschap in Open Zaak, each with the reden of the update as audit toelichting, and
the updated zaak returned by the operation SHALL report `isZaakspecifiekGeautoriseerd` as `false`.

#### Scenario: Lifting the marking
- **GIVEN** a zaakspecifiek geautoriseerde zaak with behandelaar Z and *Zaakspecifiek geautoriseerde
  medewerkers* A and P
- **AND** an employee with `wijzigen` on the zaak
- **WHEN** the employee updates the zaakgegevens with `isZaakspecifiekGeautoriseerd: false` and reden
  "Niet meer vertrouwelijk"
- **THEN** the zaak has no `ZAAK_GEAUTORISEERD` zaakeigenschap and no *Zaakspecifiek geautoriseerde medewerker*
  rol
- **AND** Z is still its behandelaar
- **AND** the zaak returned by the operation reports `isZaakspecifiekGeautoriseerd` as `false`

#### Scenario: A zaakspecifiek geautoriseerde medewerker lifts the marking
- **GIVEN** medewerker A, who has access to the zaakspecifiek geautoriseerde zaak through a *Zaakspecifiek
  geautoriseerde medewerker* rol and has `wijzigen` on it
- **WHEN** A lifts the marking
- **THEN** the marking is lifted

#### Scenario: Lifting without edit rights
- **WHEN** an employee without `wijzigen` on the zaak requests that the marking be lifted
- **THEN** the request is refused with the generic insufficient-rights response and the zaak stays marked

#### Scenario: Unmarking a zaak that is not marked changes nothing
- **WHEN** an employee updates a zaak that is not zaakspecifiek geautoriseerd with
  `isZaakspecifiekGeautoriseerd: false`
- **THEN** the update succeeds and no rol or zaakeigenschap of the zaak is deleted

### Requirement: A lifted zaak behaves as a normal zaak again

Once its marking is lifted, the zaak SHALL no longer be zaakspecifiek geautoriseerd: every employee authorised
for its zaaktype SHALL get the rechten of their application roles on it, it SHALL be releasable and reassignable,
and the zaak, its taken and its documenten SHALL be found in the werkvoorraden and zoekresultaten by every
employee authorised for the zaaktype, without a manual reindex. The lock icon and the *Medewerkers* tab SHALL no
longer be shown.

#### Scenario: Another behandelaar of the zaaktype finds and opens the zaak
- **GIVEN** medewerker B, with the `behandelaar` application role for the zaaktype but without access to the
  zaakspecifiek geautoriseerde zaak
- **WHEN** the marking is lifted and B searches for the zaak and opens it
- **THEN** B finds the zaak and gets the rechten of a behandelaar on it

#### Scenario: Releasing after lifting the marking
- **WHEN** an employee lifts the marking and then releases the zaak
- **THEN** the zaak is released and has no behandelaar

#### Scenario: Handing over in the same update that lifts the marking
- **GIVEN** a zaakspecifiek geautoriseerde zaak with behandelaar Z
- **WHEN** an employee lifts the marking and hands the zaak to behandelaar N in the same update
- **THEN** N is the behandelaar and Z has no *Zaakspecifiek geautoriseerde medewerker* rol

#### Scenario: A medewerker is added while the marking is lifted
- **WHEN** an employee adds a medewerker to the zaak, or a taak of the zaak is assigned, while another employee
  lifts the marking
- **THEN** once both are done, the zaak has no *Zaakspecifiek geautoriseerde medewerker* rol, on a single ZAC
  instance

### Requirement: Marking a lifted zaak again starts without extra medewerkers

When a zaak whose marking was lifted is marked as zaakspecifiek geautoriseerd again, only the medewerkers that
marking grants access to SHALL have individual access; the *Zaakspecifiek geautoriseerde medewerkers* of the
earlier marking SHALL NOT get it back.

#### Scenario: Marking again
- **GIVEN** a zaak whose marking was lifted while medewerker A was a *Zaakspecifiek geautoriseerde medewerker*
- **WHEN** the behandelaar marks the zaak again
- **THEN** A has no *Zaakspecifiek geautoriseerde medewerker* rol on the zaak

### Requirement: The lifting shows in the zaakhistorie

The zaakhistorie SHALL show every deleted *Zaakspecifiek geautoriseerde medewerker* rol as ontkoppeld, with the
medewerker's name and the reden of the update.

#### Scenario: Viewing the zaakhistorie after lifting
- **GIVEN** a zaak whose marking was lifted with reden "Niet meer vertrouwelijk" while medewerker A was a
  *Zaakspecifiek geautoriseerde medewerker*
- **WHEN** an employee opens the zaakhistorie
- **THEN** it shows an ontkoppeld *Zaakspecifiek geautoriseerde medewerker* line with A's name and the reden

### Requirement: The edit form asks for confirmation before lifting

The zaakgegevens edit form SHALL let an employee with `wijzigen` untick *Zaak zaakspecifiek autoriseren* on a
zaakspecifiek geautoriseerde zaak. On saving with the box unticked, it SHALL open a dialog *Zaakspecifieke
autorisatie opheffen* that explains the zaak becomes accessible to every employee with access to the zaaktype,
without a reden field, with the buttons *Opheffen* and *Sluiten*. Only *Opheffen* SHALL save the update.

#### Scenario: Confirming the lifting
- **GIVEN** the edit form of a zaakspecifiek geautoriseerde zaak
- **WHEN** the employee unticks *Zaak zaakspecifiek autoriseren*, enters a reden, saves and chooses *Opheffen*
- **THEN** the update is sent with `isZaakspecifiekGeautoriseerd: false`

#### Scenario: Closing the dialog
- **WHEN** the employee chooses *Sluiten* in the dialog
- **THEN** nothing is saved and the edit form stays open

#### Scenario: Saving other changes on a marked zaak
- **WHEN** the employee saves the edit form of a zaakspecifiek geautoriseerde zaak with the box still ticked
- **THEN** no dialog opens and the marking is unchanged
