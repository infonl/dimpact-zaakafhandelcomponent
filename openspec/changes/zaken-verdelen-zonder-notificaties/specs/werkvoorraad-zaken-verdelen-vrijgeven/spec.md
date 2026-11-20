## ADDED Requirements

### Requirement: Verdelen and vrijgeven run in the background

An employee with the werklijst right *zaken taken verdelen* SHALL be able to distribute a selection of zaken from
the zaken werkvoorraad to a groep and optionally a behandelaar (`PUT /rest/zaken/lijst/verdelen`), and to release
a selection from its behandelaar (`PUT /rest/zaken/lijst/vrijgeven`). ZAC SHALL answer at once and process the
zaken in the background, while the werkvoorraad shows a progress dialog.

#### Scenario: Distributing a selection of zaken
- **WHEN** an employee with the right *zaken taken verdelen* distributes two open zaken to a groep and a
  behandelaar
- **THEN** ZAC answers with HTTP 204 before the zaken are processed
- **AND** both zaken end up with that groep and behandelaar

#### Scenario: Releasing a selection of zaken
- **WHEN** an employee with the right *zaken taken verdelen* releases two zaken that have a behandelaar
- **THEN** both zaken lose their behandelaar and keep their groep

#### Scenario: An employee without the right
- **WHEN** an employee without the right *zaken taken verdelen* calls either endpoint
- **THEN** ZAC refuses the request and changes no zaak

### Requirement: ZAC skips zaken it may not or cannot change

During verdelen and vrijgeven ZAC SHALL skip a zaak without changing its rollen when it may not or cannot change
it, and SHALL process the rest of the selection as usual. A skipped zaak SHALL count as processed in the
progress, as it does today.

#### Scenario: A closed zaak in the selection
- **WHEN** an employee distributes a selection of one open and one closed zaak
- **THEN** the open zaak is distributed and the closed zaak keeps its rollen
- **AND** both count as processed in the progress

#### Scenario: A zaak with more than one groep or behandelaar rol
- **WHEN** a zaak in the selection has two groep rollen or two behandelaar rollen
- **THEN** ZAC skips that zaak and processes the others

#### Scenario: A zaakspecifiek geautoriseerde zaak that would lose its behandelaar
- **WHEN** an employee releases a zaakspecifiek geautoriseerde zaak, or distributes it to a groep without
  behandelaar
- **THEN** ZAC skips that zaak and it keeps its behandelaar

#### Scenario: A zaakspecifiek geautoriseerde zaak whose zaaktype cannot keep the previous behandelaar authorised
- **WHEN** an employee distributes a zaakspecifiek geautoriseerde zaak whose zaaktype has no *Zaakspecifiek
  geautoriseerde medewerker* roltype
- **THEN** ZAC skips that zaak

#### Scenario: A groep without the behandelaar rol for the zaaktype
- **WHEN** an employee distributes a zaak to a groep that has no *behandelaar* rol for its zaaktype in the PABC
- **THEN** ZAC skips that zaak

#### Scenario: A behandelaar outside the groep
- **WHEN** an employee distributes a selection to a groep and a behandelaar who is not a member of that groep
- **THEN** ZAC skips every zaak of the selection and the werkvoorraad finishes the progress

### Requirement: The werkvoorraad follows ZAC's own event per zaak

For every zaak in a verdelen or vrijgeven batch, ZAC SHALL send a `ZAKEN_VERDELEN` or `ZAKEN_VRIJGEVEN` screen
event with the zaak UUID as resource: `UPDATED` once Open Zaak has confirmed all rol changes of the zaak,
`SKIPPED` when ZAC skips it. The zaken werkvoorraad SHALL count a zaak as processed, and update
its row, on that event only.

#### Scenario: Distributing updates the werkvoorraad as soon as Open Zaak confirms
- **WHEN** an employee distributes a selection of zaken and Open Zaak confirms the rol changes of a zaak with a
  successful response
- **THEN** ZAC sends a `ZAKEN_VERDELEN` `UPDATED` event for that zaak
- **AND** the werkvoorraad counts the zaak as processed and shows its new groep and behandelaar, without waiting
  for the rol notification

#### Scenario: Releasing updates the werkvoorraad as soon as Open Zaak confirms
- **WHEN** an employee releases a selection of zaken and Open Zaak confirms the removal of the behandelaar rol of
  a zaak
- **THEN** ZAC sends a `ZAKEN_VRIJGEVEN` `UPDATED` event for that zaak
- **AND** the werkvoorraad counts the zaak as processed and shows it without behandelaar

#### Scenario: A zaak that already has the chosen groep and behandelaar
- **WHEN** an employee distributes a zaak that ZAC does not skip to the groep and behandelaar it already has
- **THEN** ZAC changes nothing in Open Zaak and sends a `ZAKEN_VERDELEN` `UPDATED` event for it

#### Scenario: A zaak that ZAC skips and that already has the chosen groep and behandelaar
- **WHEN** an employee distributes a closed zaak, or a selection whose behandelaar is not a member of the groep,
  and a zaak already has that groep and behandelaar
- **THEN** ZAC skips the zaak and sends a `ZAKEN_VERDELEN` `SKIPPED` event for it, as the skip checks come first

#### Scenario: A skipped zaak counts as before
- **WHEN** ZAC skips a zaak of the selection
- **THEN** ZAC sends a `ZAKEN_VERDELEN` or `ZAKEN_VRIJGEVEN` `SKIPPED` event for that zaak
- **AND** the werkvoorraad counts it as processed without changing its row

### Requirement: The batch ends on ZAC's final event

When ZAC has handled every zaak of a batch, it SHALL send one final `ZAKEN_VERDELEN` or `ZAKEN_VRIJGEVEN` event
with the `screenEventResourceId` of the request as resource and the number of failed zaken as detail. The zaken
werkvoorraad SHALL send a `screenEventResourceId` with both requests and SHALL end the progress, commit the search
index and reload the list without extra wait when the final event arrives.

#### Scenario: Open Notificaties is unavailable
- **WHEN** Open Notificaties is unavailable and an employee distributes or releases a selection of zaken
- **THEN** the werkvoorraad finishes the progress when ZAC's final event arrives
- **AND** shows no "Het proces duurde te lang en is afgebroken." message

#### Scenario: The final event arrives before the last per-zaak event
- **WHEN** the final event of a batch without failed zaken reaches the werkvoorraad before the per-zaak event of
  the last zaak
- **THEN** the werkvoorraad shows no error message
- **AND** the reloaded list shows the last zaak with its new groep and behandelaar

#### Scenario: The final event never arrives
- **WHEN** no event of the batch arrives for 5 minutes, for example because ZAC restarted
- **THEN** the werkvoorraad ends the progress with "Het proces duurde te lang en is afgebroken.", as today
- **AND** shows no message about zaken that could not be distributed or released

### Requirement: An error on one zaak does not stop the batch

When Open Zaak answers with an error for a zaak, or the zaak cannot be read, ZAC SHALL log the error with the
zaak UUID, send no per-zaak event for it and continue with the next zaak. The final event SHALL count every zaak
of the request that got no per-zaak event as failed. When the
final event reports one or more failed zaken, the zaken werkvoorraad SHALL show one general error message with
that number.

#### Scenario: Open Zaak fails for one zaak of the selection
- **WHEN** an employee distributes three zaken and Open Zaak answers with an error for the second zaak
- **THEN** ZAC still distributes the first and the third zaak and sends the final event with 1 failed zaak
- **AND** the second zaak does not count as distributed
- **AND** the werkvoorraad shows "Eén zaak kon niet verdeeld worden." after the progress ends

#### Scenario: The batch cannot start
- **WHEN** an employee distributes three zaken and ZAC cannot read the chosen groep or behandelaar, or cannot
  check whether the behandelaar is a member of the groep
- **THEN** ZAC changes no zaak and sends the final event with 3 failed zaken
- **AND** the werkvoorraad shows "3 zaken konden niet verdeeld worden." without waiting for the timeout

#### Scenario: Distributing with more failing zaken
- **WHEN** an employee distributes a selection and Open Zaak answers with an error for two of the zaken
- **THEN** the werkvoorraad shows "2 zaken konden niet verdeeld worden." after the progress ends

#### Scenario: Releasing with one failing zaak
- **WHEN** an employee releases a selection and Open Zaak answers with an error for one of the zaken
- **THEN** the werkvoorraad shows "Eén zaak kon niet vrijgegeven worden." after the progress ends

#### Scenario: Releasing with more failing zaken
- **WHEN** an employee releases a selection and Open Zaak answers with an error for two of the zaken
- **THEN** the werkvoorraad shows "2 zaken konden niet vrijgegeven worden." after the progress ends

#### Scenario: All zaken succeed or are skipped
- **WHEN** every zaak of the selection is distributed, released or skipped
- **THEN** the werkvoorraad shows no error message

### Requirement: Rol notifications do not move the werkvoorraad progress

ZAC SHALL process the `rol` notifications that follow a verdelen or vrijgeven as it does today, including the
`ZAAK_ROLLEN` screen event for other screens. The zaken werkvoorraad SHALL NOT count such an event in the progress
or update a row from it during verdelen or vrijgeven.

#### Scenario: The rol notification arrives after ZAC's event
- **WHEN** the rol notification of a distributed zaak arrives after ZAC sent its `ZAKEN_VERDELEN` `UPDATED` event
- **THEN** ZAC creates the signalering for the new behandelaar, removes those of former behandelaren and updates
  the search index as today
- **AND** the werkvoorraad progress does not count the zaak a second time

#### Scenario: A rol notification arrives while the zaak is still being changed
- **WHEN** a rol notification for a zaak arrives after its old behandelaar rol is removed but before its new one
  is created
- **THEN** the werkvoorraad does not count the zaak as processed until ZAC's own event for it arrives

### Requirement: Rights are determined once per zaaktype in a batch

Within one verdelen or vrijgeven batch, ZAC SHALL ask the PABC which groups have the *behandelaar* rol for a
zaaktype at most once per zaaktype, and SHALL NOT read the zaakeigenschappen of a zaak whose zaaktype defines no
*zaakspecifiek geautoriseerd* eigenschap. Which zaken are skipped SHALL not change.

#### Scenario: Many zaken of the same zaaktype
- **WHEN** an employee distributes 50 zaken of one zaaktype that defines no *zaakspecifiek geautoriseerd*
  eigenschap
- **THEN** ZAC asks the PABC once for that zaaktype and reads no zaakeigenschappen

#### Scenario: A zaaktype that can be zaakspecifiek geautoriseerd
- **WHEN** a selection holds zaken of a zaaktype that defines the *zaakspecifiek geautoriseerd* eigenschap
- **THEN** ZAC still reads the zaakeigenschappen of each of those zaken, so a marked zaak is skipped or handed
  over as before
