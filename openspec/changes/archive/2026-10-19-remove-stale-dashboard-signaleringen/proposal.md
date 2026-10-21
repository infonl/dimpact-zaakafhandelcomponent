## Why

Dashboard cards can show zaken and taken that are no longer assigned to the user, and that the user may no
longer be allowed to read ([PZ-12857](https://dimpact.atlassian.net/browse/PZ-12857)).

For the cards that list signaleringen, a reassignment deletes the behandelaar rol of
the previous behandelaar in Open Zaak, or changes the assignee of a Flowable task, but the `signalering`
rows of the previous behandelaar or assignee stay in the ZAC database. They are only removed when that user
opens the zaak or taak, when the zaak is destroyed, or when the admin cleanup endpoint removes signaleringen
older than `SIGNALERINGEN_DELETE_OLDER_THAN_DAYS`.

*Mijn binnenkort verlopende zaken* lists the open zaken in which the user has any medewerker rol, so it also
lists zaken where the user only holds the *Zaakspecifiek geautoriseerde medewerker* rol, for example as the
previous behandelaar. It lists zaken the user cannot read as well, with their details left empty.

## What Changes

- **A ROL DESTROY notification cleans up zaak signaleringen.** When ZAC receives a `zaken` notification for
  a deleted rol, it deletes the `ZAAK_OP_NAAM` and `ZAAK_DOCUMENT_TOEGEVOEGD` signaleringen for that zaak
  of every user who is not the current behandelaar of the zaak. This covers reassignments by ZAC and by
  other applications that change the rollen in Open Zaak.
- **Reassigning or releasing a taak cleans up taak signaleringen.** Flowable tasks send no notifications,
  so ZAC deletes the `TAAK_OP_NAAM` signaleringen of every user other than the new assignee when it assigns
  or releases a taak.
- **The dashboard lists check every signalering before showing it.** The zaken cards *Recent aan mij
  toegewezen zaken* and *Mijn zaken met recent toegevoegde documenten*, and the taken card *Recent aan mij
  toegewezen taken*, only show a signalering when the user is still the behandelaar of the zaak (or the
  assignee of the taak) and has read rights on it. Any other signalering is deleted from the ZAC database
  and logged as a warning, because it means a notification was missed or a cleanup failed.
- **Mijn binnenkort verlopende zaken only lists zaken the user is behandelaar of and may read.** Zaken
  where the user holds another medewerker rol, or has no read rights, are left out. Nothing is stored for
  this card, so nothing is deleted or logged.
- **The zaken card count matches the listed zaken.** The count is read after the stale signaleringen of the
  page have been deleted. The deletion notifies the dashboard, which loads the card again.

## Capabilities

### New Capabilities

- `dashboard-signaleringen`: which zaken and taken the dashboard cards built from signaleringen and from
  deadline warnings show to a user, and when ZAC removes signaleringen that no longer apply.

### Modified Capabilities

None.

## Impact

- **Backend**: `NotificationReceiver` (ROL DESTROY handling), `SignaleringService` (cleanup and the checks
  in the zaken and taken listings), `TaskService` (cleanup on assign and release), `SignaleringRestService`
  (count after listing), `ZaakRestService.listZaakWarnings` (behandelaar and read-rights filter).
- **No database migration.** Signaleringen that are stale today are removed by the dashboard check the first
  time their user opens the dashboard.
- **Extra ZRC calls**: one `listRollen` call per ROL DESTROY notification, and one per listed zaak
  signalering. The taken card evaluates the taak rechten for each listed taak.
- **Zaakspecifieke autorisatie**: no special handling. The read-rights checks use the existing zaak and
  taak rechten, which already include zaakspecifieke autorisatie, and a previous behandelaar who keeps
  access through the *Zaakspecifiek geautoriseerde medewerker* rol is not the behandelaar, so their entries
  disappear from the cards.
- **Not changed**: *Aan mij toegewezen zaken/taken*. They query the search index for the current behandelaar
  or assignee, which ZAC updates on every assignment and on ROL notifications, and the search already leaves
  out zaken and taken the user cannot read (`zoekresultaten-leesrecht`).
- **Not changed**: signalering e-mails. ZAC sends them when the signalering is created, to the user who is
  then the behandelaar or assignee, so a stale signalering never causes an e-mail.
