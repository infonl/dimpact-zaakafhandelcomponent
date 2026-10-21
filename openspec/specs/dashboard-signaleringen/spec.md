# dashboard-signaleringen Specification

## Purpose

Ensures that the dashboard cards built from signaleringen and from deadline warnings only show zaken and taken
that are still assigned to the user and that the user may read, and that ZAC removes signaleringen that no
longer apply.

## Requirements

### Requirement: A deleted rol removes the zaak signaleringen of users who are no longer behandelaar

When ZAC receives a `zaken` notification that a rol of a zaak was deleted, it SHALL delete the user
signaleringen of type `ZAAK_OP_NAAM` and `ZAAK_DOCUMENT_TOEGEVOEGD` for that zaak whose user is not the
behandelaar of the zaak at the moment ZAC handles the notification. Signaleringen of the current behandelaar
SHALL be kept. The rule SHALL apply regardless of whether ZAC or another application deleted the rol.

#### Scenario: Reassigning a zaak removes the previous behandelaar's signaleringen
- **WHEN** a zaak with behandelaar A and signaleringen `ZAAK_OP_NAAM` and `ZAAK_DOCUMENT_TOEGEVOEGD` for A is
  assigned to behandelaar B, and ZAC receives the notification that the behandelaar rol of A was deleted
- **THEN** the signaleringen of A for that zaak are deleted and *Recent aan mij toegewezen zaken* and *Mijn
  zaken met recent toegevoegde documenten* of A no longer list the zaak

#### Scenario: The new behandelaar keeps their signalering
- **WHEN** ZAC handles the rol-deleted notification of a reassignment from A to B after it has stored the
  `ZAAK_OP_NAAM` signalering of B for the zaak
- **THEN** the signalering of B is kept

#### Scenario: Releasing a zaak removes the behandelaar's signaleringen
- **WHEN** behandelaar A is removed from a zaak without a new behandelaar, and ZAC receives the rol-deleted
  notification
- **THEN** all user signaleringen of type `ZAAK_OP_NAAM` and `ZAAK_DOCUMENT_TOEGEVOEGD` for that zaak are
  deleted

#### Scenario: Deleting another rol leaves the behandelaar's signaleringen alone
- **WHEN** a rol other than the behandelaar rol, such as an initiator or belanghebbende, is deleted from a zaak
  whose behandelaar A has a `ZAAK_OP_NAAM` signalering
- **THEN** the signalering of A is kept

#### Scenario: A rol-deleted notification for a zaak that no longer exists is ignored
- **WHEN** ZAC receives a rol-deleted notification for a zaak that can no longer be read from Open Zaak
- **THEN** ZAC deletes no signaleringen because of this notification, and handling of the other aspects of the
  notification continues

### Requirement: Assigning or releasing a taak removes the taak signaleringen of the previous assignee

When ZAC assigns a taak to a user or releases a taak, it SHALL delete the `TAAK_OP_NAAM` signaleringen for that
taak of every user other than the new assignee.

#### Scenario: Reassigning a taak removes the previous assignee's signalering
- **WHEN** a taak assigned to user A, with a `TAAK_OP_NAAM` signalering for A, is assigned to user B
- **THEN** the signalering of A is deleted and *Recent aan mij toegewezen taken* of A no longer lists the taak

#### Scenario: Releasing a taak removes the assignee's signalering
- **WHEN** a taak assigned to user A, with a `TAAK_OP_NAAM` signalering for A, is released
- **THEN** the signalering of A is deleted

### Requirement: The zaken signalering cards only list zaken the user is behandelaar of and may read

*Recent aan mij toegewezen zaken* (`ZAAK_OP_NAAM`) and *Mijn zaken met recent toegevoegde documenten*
(`ZAAK_DOCUMENT_TOEGEVOEGD`) SHALL only list a zaak when the requesting user is the current behandelaar of the
zaak and has read rights on it. ZAC SHALL delete every signalering it does not list for this reason from its
database and SHALL log a warning for each that names the signalering, its type, the zaak UUID and the reason.
The warning SHALL NOT contain the username or name of the user.

#### Scenario: A zaak assigned to another user is not listed and its signalering is deleted
- **WHEN** user A has a `ZAAK_OP_NAAM` signalering for a zaak whose behandelaar is now user B, because the
  rol-deleted notification never reached ZAC, and A opens the dashboard
- **THEN** the zaak is not listed, the signalering is deleted and a warning is logged

#### Scenario: A zaak the user can no longer read is not listed and its signalering is deleted
- **WHEN** user A is still the behandelaar of a zaak but has no read rights on it, and has a
  `ZAAK_DOCUMENT_TOEGEVOEGD` signalering for it
- **THEN** the zaak is not listed, the signalering is deleted and a warning is logged

#### Scenario: A zaak still assigned to and readable by the user is listed
- **WHEN** user A is the behandelaar of a zaak, has read rights on it and has a `ZAAK_OP_NAAM` signalering for
  it
- **THEN** the zaak is listed, the signalering is kept and no warning is logged

#### Scenario: The card reloads after stale signaleringen are removed
- **WHEN** a page of a zaken or taken signalering card is requested and signaleringen on that page are deleted
  as stale
- **THEN** the response leaves out the deleted signaleringen, and ZAC notifies the user's dashboard that their
  signaleringen changed, so the card is loaded again with the next valid signaleringen

#### Scenario: The total count leaves out deleted signaleringen
- **WHEN** a page of a zaken signalering card is requested and signaleringen on that page are deleted as stale
- **THEN** the total number of zaken returned with the page does not include the deleted signaleringen

### Requirement: The taken signalering card only lists taken assigned to the user that the user may read

*Recent aan mij toegewezen taken* (`TAAK_OP_NAAM`) SHALL only list a taak when the requesting user is the current
assignee of the taak and has read rights on it. ZAC SHALL delete every signalering it does not list for this
reason from its database and SHALL log a warning for each that names the signalering, its type, the taak ID and
the reason. The warning SHALL NOT contain the username or name of the user.

#### Scenario: A taak assigned to another user is not listed and its signalering is deleted
- **WHEN** user A has a `TAAK_OP_NAAM` signalering for a taak that is now assigned to user B or to no user,
  and A opens the dashboard
- **THEN** the taak is not listed, the signalering is deleted and a warning is logged

#### Scenario: A taak the user can no longer read is not listed and its signalering is deleted
- **WHEN** user A is the assignee of a taak but has no read rights on it, and has a `TAAK_OP_NAAM` signalering
  for it
- **THEN** the taak is not listed, the signalering is deleted and a warning is logged

#### Scenario: A taak still assigned to and readable by the user is listed
- **WHEN** user A is the assignee of a taak, has read rights on it and has a `TAAK_OP_NAAM` signalering for it
- **THEN** the taak is listed, the signalering is kept and no warning is logged

### Requirement: Mijn binnenkort verlopende zaken only lists zaken the user is behandelaar of and may read

*Mijn binnenkort verlopende zaken* SHALL only list an open zaak with an approaching deadline when the requesting
user is the current behandelaar of the zaak and has read rights on it. A zaak in which the user holds only
another medewerker rol, such as the *Zaakspecifiek geautoriseerde medewerker* rol, SHALL NOT be listed.

#### Scenario: A previous behandelaar who kept zaakspecifieke access does not see the zaak
- **WHEN** a zaakspecifiek geautoriseerde zaak with an approaching deadline was reassigned from user A to user B,
  so that A holds the *Zaakspecifiek geautoriseerde medewerker* rol, and A opens the dashboard
- **THEN** *Mijn binnenkort verlopende zaken* of A does not list the zaak, and that of B does

#### Scenario: A behandelaar without read rights does not see the zaak
- **WHEN** user A is the behandelaar of a zaak with an approaching deadline but has no read rights on it
- **THEN** *Mijn binnenkort verlopende zaken* of A does not list the zaak

#### Scenario: A behandelaar with read rights sees the zaak
- **WHEN** user A is the behandelaar of a zaak with an approaching deadline and has read rights on it
- **THEN** *Mijn binnenkort verlopende zaken* of A lists the zaak, exactly as before this change
