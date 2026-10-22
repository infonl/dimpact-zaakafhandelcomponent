## ADDED Requirements

### Requirement: The verdelen dialog requires a behandelaar for a zaakspecifiek geautoriseerde zaak

When the selection in the zaken werkvoorraad holds one or more zaakspecifiek geautoriseerde zaken, the verdelen
dialog SHALL mark the medewerker field as required, SHALL show a notice that a behandelaar is required, SHALL list
the selected zaken that are zaakspecifiek geautoriseerd, and SHALL keep *Verdelen* disabled until a medewerker is
chosen.

#### Scenario: Only a group is chosen
- **GIVEN** a selection with a zaakspecifiek geautoriseerde zaak and a zaak that is not
- **WHEN** the employee chooses only a group in the verdelen dialog
- **THEN** *Verdelen* stays disabled

#### Scenario: A group and a medewerker are chosen
- **GIVEN** a selection with a zaakspecifiek geautoriseerde zaak
- **WHEN** the employee chooses a group and a medewerker
- **THEN** *Verdelen* is enabled

#### Scenario: The dialog shows why
- **GIVEN** a selection with zaakspecifiek geautoriseerde zaak ZAAK-002 and ZAAK-001, which is not
- **WHEN** the verdelen dialog opens
- **THEN** the medewerker field is marked required with a notice that a behandelaar is required
- **AND** the dialog lists ZAAK-002 and not ZAAK-001

#### Scenario: A selection without zaakspecifiek geautoriseerde zaken
- **WHEN** the employee distributes a selection without zaakspecifiek geautoriseerde zaken
- **THEN** the medewerker stays optional and the dialog shows no notice and no list

### Requirement: The zaak edit form requires a behandelaar for a zaakspecifiek geautoriseerde zaak

While *Zaak zaakspecifiek autoriseren* is ticked in the zaakgegevens edit form, the form SHALL mark the
behandelaar field as required, SHALL show a notice that a zaakspecifiek geautoriseerde zaak always has a
behandelaar, and SHALL keep *Opslaan* disabled until a behandelaar is chosen.

#### Scenario: A marked zaak without behandelaar
- **GIVEN** the edit form of a zaakspecifiek geautoriseerde zaak
- **WHEN** the behandelaar field is empty
- **THEN** *Opslaan* is disabled and the field is marked required with the notice

#### Scenario: Ticking the checkbox
- **GIVEN** the edit form of a zaak that is not zaakspecifiek geautoriseerd
- **WHEN** the employee ticks *Zaak zaakspecifiek autoriseren* and leaves the behandelaar empty
- **THEN** *Opslaan* is disabled and the field is marked required

#### Scenario: A zaak that is not zaakspecifiek geautoriseerd
- **WHEN** the checkbox is not ticked
- **THEN** the behandelaar is optional and no notice is shown

### Requirement: A zaakspecifiek geautoriseerde zaak can be handed over to another behandelaar

The system SHALL let an employee assign a zaakspecifiek geautoriseerde zaak to another behandelaar, from the
zaakdetailpagina as well as from the zaken werkvoorraad. The previous behandelaar SHALL keep access to the zaak as a
*Zaakspecifiek geautoriseerde medewerker*. Removing the behandelaar without a new one stays a release, which is
refused.

#### Scenario: Handing over from the werkvoorraad
- **GIVEN** a zaakspecifiek geautoriseerde zaak with behandelaar Z
- **WHEN** an employee distributes it to a group and medewerker M
- **THEN** M is the behandelaar of the zaak
- **AND** Z keeps access to the zaak as a *Zaakspecifiek geautoriseerde medewerker*

## REMOVED Requirements

### Requirement: The behandelaar cannot be changed while a zaak is zaakspecifiek geautoriseerd
**Reason**: Since [PZ-10202](https://dimpact.atlassian.net/browse/PZ-10202) a zaakspecifiek geautoriseerde zaak can
be handed over and the previous behandelaar keeps access. This spec was not updated then; the requirement above
describes the current behaviour.
**Migration**: None. The backend already hands such a zaak over.

## MODIFIED Requirements

### Requirement: The werkvoorraad dialogs state that zaakspecifiek geautoriseerde zaken were left out

When a werkvoorraad vrijgeven action leaves one or more of the selected zaken unprocessed because they are
zaakspecifiek geautoriseerd, the system SHALL tell the employee how many zaken were left out and that this is the
reason. Verdelen never leaves such a zaak out, because the verdelen dialog requires a behandelaar for it. The
werkvoorraad lists open zaken only, so no other skip reason can occur there.

#### Scenario: Releasing a selection containing zaakspecifiek geautoriseerde zaken
- **WHEN** an employee releases a selection of zaken in which some are zaakspecifiek geautoriseerd
- **THEN** the employee is told how many zaken were left out because they are zaakspecifiek geautoriseerd,
  in a message that names that reason

#### Scenario: Distributing a selection containing zaakspecifiek geautoriseerde zaken
- **WHEN** an employee distributes a selection of zaken in which some are zaakspecifiek geautoriseerd, to a group
  and a medewerker
- **THEN** every zaak is distributed and no reason message is shown

#### Scenario: A selection in which every zaak can be processed
- **WHEN** an employee distributes or releases a selection of zaken in which none are zaakspecifiek
  geautoriseerd
- **THEN** no reason message is shown, and the feedback is exactly what it was before this capability
  existed
