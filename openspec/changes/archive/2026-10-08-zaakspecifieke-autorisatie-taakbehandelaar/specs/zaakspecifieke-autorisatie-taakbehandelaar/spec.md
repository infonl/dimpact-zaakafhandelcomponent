## ADDED Requirements

### Requirement: A taakbehandelaar gets the Zaakspecifiek geautoriseerde medewerker rol on assignment

When a taak of a zaakspecifiek geautoriseerde zaak gets a medewerker as taakbehandelaar, ZAC SHALL add a
*Zaakspecifiek geautoriseerde medewerker* rol for that medewerker to the zaak in Open Zaak. This SHALL apply
to CMMN human tasks and BPMN user tasks alike. It SHALL hold for every way a medewerker assigns a taak:
- starting a human task plan item with a medewerker;
- `PATCH /rest/taken/toekennen`;
- `PATCH /rest/taken/toekennen/mij`;
- `PATCH /rest/taken/lijst/toekennen/mij`;
- `PUT /rest/taken/lijst/verdelen`.

The implicit assignment to the logged-in user in `PATCH /rest/taken/complete` SHALL NOT add a rol. Completing
requires the `wijzigen` right on the taak, and on a zaakspecifiek geautoriseerde zaak that right already
requires access to the zaak. The completer therefore already has access, and keeps it.

For an assignment through a REST endpoint, the rol SHALL exist before the request returns. The rol SHALL be
identical to the one a previous zaakbehandelaar receives on handover: same roltype, same `roltoelichting`. The
audit toelichting that ZAC sends to Open Zaak when it creates the rol, and that shows in the zaakhistorie,
SHALL be the fixed text "Zaakspecifiek geautoriseerde medewerker van de zaak". A
taakbehandelaar's rol SHALL NOT be distinguishable from a previous zaakbehandelaar's or a manually added
medewerker's.

#### Scenario: A behandelaar without the flag is selected when starting a taak
- **GIVEN** a zaakspecifiek geautoriseerde zaak
- **AND** medewerker A, who holds the `behandelaar` application role for the zaaktype but not
  `zaakspecifiek_geautoriseerd`, and who is not the zaakbehandelaar
- **WHEN** an employee starts a human task plan item on that zaak with a groep and A selected
- **THEN** the zaak has a *Zaakspecifiek geautoriseerde medewerker* rol for A
- **AND** A can read and treat the zaak, the new taak and the documenten of the zaak

#### Scenario: Completing a taak that is assigned to someone else adds no rol
- **GIVEN** a taak of a zaakspecifiek geautoriseerde zaak
- **WHEN** a medewerker who already has access to the zaak completes it while it is not assigned to them
- **THEN** no *Zaakspecifiek geautoriseerde medewerker* rol is added for that medewerker
- **AND** the taak is completed, also when the zaaktype lacks the roltype

#### Scenario: Reassigning a taak
- **GIVEN** a taak of a zaakspecifiek geautoriseerde zaak with taakbehandelaar A
- **AND** A and B both hold the `behandelaar` application role for the zaaktype but not
  `zaakspecifiek_geautoriseerd`, and neither is the zaakbehandelaar
- **WHEN** an employee assigns the taak to B
- **THEN** the zaak has a *Zaakspecifiek geautoriseerde medewerker* rol for A and one for B
- **AND** both A and B can read and treat the zaak

#### Scenario: A medewerker assigns a taak to themselves
- **WHEN** medewerker B uses "toekennen aan mij" on a taak of a zaakspecifiek geautoriseerde zaak, from the
  taakdetailpagina or from the takenwerkvoorraad
- **THEN** the zaak has a *Zaakspecifiek geautoriseerde medewerker* rol for B

#### Scenario: Verdelen a selection of taken from the takenwerkvoorraad
- **WHEN** an employee distributes a selection of taken of zaakspecifiek geautoriseerde zaken to medewerker B
- **THEN** each of those zaken has a *Zaakspecifiek geautoriseerde medewerker* rol for B

#### Scenario: Reassigning a BPMN user task
- **GIVEN** a BPMN user task of a zaakspecifiek geautoriseerde zaak
- **WHEN** an employee assigns it to medewerker B through the same REST endpoints as a CMMN taak
- **THEN** the zaak has a *Zaakspecifiek geautoriseerde medewerker* rol for B

#### Scenario: Starting a taak with only a groep adds no rol
- **WHEN** an employee starts a human task plan item on a zaakspecifiek geautoriseerde zaak with a groep but
  without a medewerker
- **THEN** no *Zaakspecifiek geautoriseerde medewerker* rol is added to the zaak

#### Scenario: Assigning a taak of a zaak that is not zaakspecifiek geautoriseerd adds no rol
- **WHEN** a taak of a zaak that is not zaakspecifiek geautoriseerd is assigned to a medewerker
- **THEN** no *Zaakspecifiek geautoriseerde medewerker* rol is added to the zaak

### Requirement: Marking a zaak grants the rol to the taakbehandelaars of its open taken

When a zaak is marked as zaakspecifiek geautoriseerd, ZAC SHALL add a *Zaakspecifiek geautoriseerde medewerker*
rol for every medewerker who is the taakbehandelaar of an open taak of that zaak. This applies to CMMN and
BPMN taken alike. The rules of the next requirement still apply: no duplicate rollen, and none for the
zaakbehandelaar. The rollen SHALL exist before the marking request returns.

#### Scenario: Marking a zaak with an assigned open taak
- **GIVEN** a zaak that is not zaakspecifiek geautoriseerd, with an open taak assigned to medewerker A who
  is not the zaakbehandelaar
- **WHEN** the zaak is marked as zaakspecifiek geautoriseerd
- **THEN** the zaak has a *Zaakspecifiek geautoriseerde medewerker* rol for A
- **AND** A can still open the taak and the zaak, and finds them in the werkvoorraden

#### Scenario: Marking a zaak whose open taken have no taakbehandelaar
- **WHEN** a zaak whose open taken are assigned to a groep only is marked as zaakspecifiek geautoriseerd
- **THEN** no *Zaakspecifiek geautoriseerde medewerker* rol is added because of those taken

### Requirement: A taakbehandelaar keeps access after leaving the taak

ZAC SHALL NOT remove a *Zaakspecifiek geautoriseerde medewerker* rol when a taakbehandelaar is replaced,
released, or completes the taak. The former taakbehandelaar SHALL keep access to the zaak, its taken and its
documenten.

#### Scenario: Releasing a taak
- **GIVEN** a taak of a zaakspecifiek geautoriseerde zaak with taakbehandelaar A
- **WHEN** an employee releases the taak, from the taakdetailpagina, via `PUT /rest/taken/lijst/vrijgeven`,
  or by verdelen to a groep without a medewerker
- **THEN** the zaak still has the *Zaakspecifiek geautoriseerde medewerker* rol for A
- **AND** A can still read and treat the zaak

#### Scenario: The taakbehandelaar completes their taak
- **GIVEN** a taak of a zaakspecifiek geautoriseerde zaak with taakbehandelaar A
- **WHEN** A completes the taak
- **THEN** the zaak still has the *Zaakspecifiek geautoriseerde medewerker* rol for A
- **AND** A can still read the zaak and its completed taak

#### Scenario: A taak goes from A to B and back to A
- **WHEN** a taak of a zaakspecifiek geautoriseerde zaak goes from A to B and back to A
- **THEN** the zaak has exactly one *Zaakspecifiek geautoriseerde medewerker* rol for A and exactly one for B

### Requirement: A medewerker is not stored twice as Zaakspecifiek geautoriseerde medewerker

ZAC SHALL NOT add a *Zaakspecifiek geautoriseerde medewerker* rol for a medewerker who already holds one on
the zaak, wherever that rol came from. ZAC SHALL NOT add one for the zaak's current zaakbehandelaar, whose
*Behandelaar* rol already gives access. This is the same rule the zaak handover flow applies.

#### Scenario: Assigning a taak to a medewerker who already holds the rol
- **GIVEN** a medewerker who already holds a *Zaakspecifiek geautoriseerde medewerker* rol on the zaak, as a
  previous zaakbehandelaar, from an earlier taak, or added manually
- **WHEN** a taak of that zaak is assigned to them
- **THEN** the zaak still has exactly one *Zaakspecifiek geautoriseerde medewerker* rol for them

#### Scenario: The zaakbehandelaar starts a taak with themselves
- **WHEN** the zaakbehandelaar Z of a zaakspecifiek geautoriseerde zaak starts a taak with Z as taakbehandelaar
- **THEN** no *Zaakspecifiek geautoriseerde medewerker* rol is added for Z
- **AND** Z keeps access through the *Behandelaar* rol

### Requirement: Access through a taak gives the medewerker's own application role rights, which are edit rights

The *Zaakspecifiek geautoriseerde medewerker* rol SHALL unlock the rights of the application role the
medewerker holds for the zaaktype, and nothing more. A taakbehandelaar always holds the `behandelaar`
application role, so this access SHALL include reading and editing the zaak, treating its taken, and
managing its documenten. The access SHALL only apply to the zaak the rol belongs to.

#### Scenario: The taakbehandelaar can edit the zaak
- **GIVEN** a medewerker with the `behandelaar` application role but not `zaakspecifiek_geautoriseerd`, who
  was assigned a taak of a zaakspecifiek geautoriseerde zaak
- **WHEN** rechten are computed for that medewerker on the zaak
- **THEN** `lezen`, `wijzigen` and `behandelen` are all `true`

#### Scenario: The access is limited to the zaak of the taak
- **GIVEN** a medewerker who was assigned a taak of zaakspecifiek geautoriseerde zaak X
- **WHEN** that medewerker requests a different zaakspecifiek geautoriseerde zaak Y of the same zaaktype,
  where they are not otherwise authorised
- **THEN** access to Y is refused with the generic insufficient-rights message

### Requirement: Taakbehandelaars are not shown as betrokkenen

Neither a current nor a previous taakbehandelaar of a taak of a zaakspecifiek geautoriseerde zaak SHALL
appear in the betrokkenen tab of the zaakdetailpagina (`GET /rest/zaken/zaak/{uuid}/betrokkene`).

#### Scenario: Viewing the betrokkenen after a taak was reassigned
- **GIVEN** a taak of a zaakspecifiek geautoriseerde zaak that was reassigned from A to B
- **WHEN** an employee opens the betrokkenen tab of the zaak
- **THEN** neither A nor B is listed as a betrokkene

### Requirement: The taakhistorie shows that a Zaakspecifiek geautoriseerde medewerker was added

The taakhistorie on the taakdetailpagina already shows a line when the taakbehandelaar changes. Whenever ZAC
adds a *Zaakspecifiek geautoriseerde medewerker* rol because of a taak, the taakhistorie of that taak SHALL
show an extra line:
- **gegeven**: *Zaakspecifiek geautoriseerde medewerker*;
- **oude waarde**: empty;
- **nieuwe waarde**: the full name of the medewerker;
- **toelichting**: the fixed, untranslated text "Zaakspecifiek geautoriseerde medewerker van de zaak".

The line SHALL only appear when ZAC actually added a rol. The gegeven label SHALL be the omschrijving of the
roltype, exactly as the zaakhistorie shows it for the same rol. Neither the label nor the toelichting is
translated.

#### Scenario: Viewing the taakhistorie after an assignment
- **GIVEN** a taak of zaakspecifiek geautoriseerde zaak ZAAK-2026-0000000001 that was assigned to B
- **WHEN** an employee opens the taakhistorie of the taak
- **THEN** a line with gegeven *behandelaar* shows B as the new value
- **AND** a line with gegeven *Zaakspecifiek geautoriseerde medewerker* shows B as the new value
- **AND** that line's toelichting is "Zaakspecifiek geautoriseerde medewerker van de zaak"

#### Scenario: No line when the medewerker already had the rol
- **WHEN** a taak of a zaakspecifiek geautoriseerde zaak is assigned to a medewerker who already holds the
  rol, or who is the zaakbehandelaar
- **THEN** the taakhistorie shows no *Zaakspecifiek geautoriseerde medewerker* line

### Requirement: Werkvoorraden and zoekresultaten show the zaak and taak to its taakbehandelaars

The zakenwerkvoorraad, the takenwerkvoorraad and the zoekresultaten SHALL show a zaakspecifiek geautoriseerde
zaak, its taken and its documenten to every medewerker who holds the *Zaakspecifiek geautoriseerde
medewerker* rol through a taak. They SHALL do so without a manual reindex.

#### Scenario: The new taakbehandelaar finds the taak and its zaak
- **GIVEN** a taak of a zaakspecifiek geautoriseerde zaak that was just assigned to B, who does not hold
  `zaakspecifiek_geautoriseerd`
- **WHEN** B opens the takenwerkvoorraad or the zakenwerkvoorraad, or searches for the zaak
- **THEN** B finds the taak in the takenwerkvoorraad
- **AND** B finds the zaak in the zakenwerkvoorraad and in the zoekresultaten

#### Scenario: The previous taakbehandelaar still finds the zaak
- **GIVEN** a taak of a zaakspecifiek geautoriseerde zaak that was reassigned from A to B
- **WHEN** A searches for the zaak
- **THEN** A finds the zaak in the zoekresultaten

### Requirement: A taak is not assigned when the zaaktype cannot record the access

When a taak of a zaakspecifiek geautoriseerde zaak would be assigned to a medewerker through a REST
endpoint, but the zaaktype does not define the *Zaakspecifiek geautoriseerde medewerker* roltype, the
assignment SHALL be refused and nothing of the taak SHALL change.
- A single request SHALL fail with the existing
  `ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ROLTYPE_NOT_FOUND` error code.
- Bulk verdelen SHALL skip such a taak, send a `SKIPPED` screen event for it, and continue with the rest of
  the selection. When the verdelen is finished, the takenwerkvoorraad SHALL show a message with the number of
  taken that were not distributed, in the same way as the zakenwerkvoorraad shows skipped zaken.
- Starting a human task plan item with a medewerker SHALL be refused before the taak is created.

#### Scenario: Assigning a single taak on a zaaktype without the roltype
- **GIVEN** a zaakspecifiek geautoriseerde zaak whose zaaktype lacks the *Zaakspecifiek geautoriseerde
  medewerker* roltype
- **WHEN** an employee assigns one of its taken to a medewerker
- **THEN** the request fails with `ERROR_CODE_ZAAKSPECIFIEK_GEAUTORISEERDE_MEDEWERKER_ROLTYPE_NOT_FOUND`
- **AND** the taak keeps its previous groep and taakbehandelaar

#### Scenario: Verdelen a selection that contains such a taak
- **WHEN** an employee distributes a selection of taken to a medewerker, and one taak belongs to a
  zaakspecifiek geautoriseerde zaak whose zaaktype lacks the roltype
- **THEN** that taak is skipped and left unchanged
- **AND** every other taak in the selection is assigned
- **AND** the employee sees a message that one taak was not distributed
