## Context

See proposal.md for the problem. Relevant current state:

- `ZAAK_OP_NAAM` signaleringen are created from a ROL CREATE notification (`SignaleringEventUtil.getEvents`);
  ROL DESTROY notifications produce no events. `ZAAK_DOCUMENT_TOEGEVOEGD` signaleringen go to the behandelaar
  at the moment the document is added.
- `ZaakService.changeBehandelaar` deletes the behandelaar rollen and creates a new one. `TaskService.assignTask`
  and `releaseTask` change the Flowable assignee and fire a `TAAK_OP_NAAM` event for the new assignee only.
- Signaleringen are only deleted when their user opens the zaak or taak (`deleteSignaleringenForZaak`,
  `TaskRestService.deleteSignaleringen`), when the zaak is destroyed (`NotificationReceiver`), or by the admin
  cleanup endpoint.
- `SignaleringService.listZakenSignaleringenPage` pages in the database when sorting on `SIGNALERING_TIJDSTIP`,
  and otherwise loads all signaleringen of the type and sorts in memory. Each listed zaak is read from the ZRC
  and converted with `RestZaakOverzichtConverter.convertForDisplay`, which already evaluates the zaak rechten.
  `SignaleringRestService.listZakenSignaleringen` counts before it lists.
- `SignaleringZoekParameters` and `SignaleringEventUtil` are Java. Changing them would require a separate
  Java-to-Kotlin pull request first.
- `ZaakRestService.listZaakWarnings` (*Mijn binnenkort verlopende zaken*) lists the zaken from the ZRC filtered
  on `rol__betrokkeneIdentificatie__medewerker__identificatie`, which matches any medewerker rol. Within ZAC
  those are the behandelaar rol and the *Zaakspecifiek geautoriseerde medewerker* rol. The converter evaluates
  the zaak rechten and only empties the fields when `canLezen` is false.
- A ROL DESTROY notification has the deleted rol as resource and the zaak as main resource. The rol itself can
  no longer be read, so ZAC cannot tell from the notification whose rol was deleted or which roltype it had.

## Goals / Non-Goals

**Goals:**
- Clean up at the moment the assignment changes, so the safety net in the listings rarely triggers.
- Make the listings correct even when notifications never arrive.
- No changes to Java classes, the REST API contract or the database schema.

**Non-Goals:**
- Rights checks before sending signalering e-mails. ZAC sends the e-mail when it creates the signalering, to
  the user who is then the behandelaar or assignee, and policy rules can only be evaluated for the logged-in
  user, not for an arbitrary target user.
- *Aan mij toegewezen zaken/taken*. They query the search index, which ZAC updates on assignment and on ROL
  notifications, and the search already applies the read rights.
- Group signaleringen (`ZAAK_OP_NAAM` with a group target). They are not shown on any dashboard.
- The `informatieobjecten` signalering listing. No signalering type has `DOCUMENT` as subject; the documents
  card uses `ZAAK_DOCUMENT_TOEGEVOEGD`, which has the zaak as subject.
- Signaleringen whose zaak or taak no longer exists. The listings fail on them as they do today.
- `ZAAK_VERLOPEND`/`TAAK_VERLOPEN` and their `signalering_verzonden` records.

## Decisions

### Reconcile against the current behandelaar on ROL DESTROY

On a `zaken` ROL DESTROY notification, `NotificationReceiver.handleSignaleringen` reads the zaak (main
resource), looks up its current behandelaar medewerker with `ZgwApiService.findBehandelaarMedewerkerRoleForZaak`
and deletes the user `ZAAK_OP_NAAM` and `ZAAK_DOCUMENT_TOEGEVOEGD` signaleringen for the zaak whose target is
not that behandelaar.

- *Why not delete the signaleringen of the user whose rol was deleted?* The deleted rol cannot be read, so the
  user is unknown.
- *Why not clean up in `ZaakService.changeBehandelaar`?* It would miss rol changes made by other applications.
- Comparing with the current behandelaar makes the handling idempotent and safe for any order of the DESTROY
  and CREATE notifications of a reassignment. If DESTROY is handled first, the new behandelaar has no
  signalering yet; if it is handled later, the new behandelaar is current and is kept. Deleting a non-behandelaar
  rol leaves the current behandelaar's signaleringen alone.
- When the zaak cannot be read because it was destroyed, the notification is skipped at `FINE`. The ZAAK DESTROY
  notification already deletes all its signaleringen.
- Each deletion is logged at `INFO`. It is the expected path.

### Clean up taken in `TaskService`

Flowable task changes produce no notifications. `TaskService.assignTask` (user assignment) and `releaseTask`
delete the `TAAK_OP_NAAM` signaleringen for the taak of every user other than the new assignee (all of them on
release). Without this, every task reassignment would end up as a warning in the listing check.
The cleanup runs under the zaak lock that assigning already takes, and releasing takes that lock too, so a
concurrent assignment of the same taak cannot delete the signalering of the newer assignee.

### One deletion helper in Kotlin, no change to `SignaleringZoekParameters`

`SignaleringService` gets a function that lists the user signaleringen of the given types for a subject and
deletes those whose target differs from the user to keep (`null` deletes all). It filters on target type and
target in Kotlin, because `SignaleringZoekParameters` has no "target is not" criterion and is Java. The
existing bulk delete by id and the screen events of `deleteSignaleringen` are reused by extracting them into a
private function that takes the list of signaleringen.

### Validate each listed signalering, delete what fails, warn

- **Zaken:** for each signalering, read the zaak, its behandelaar medewerker rol and its zaak rechten for the
  logged-in user. A signalering is valid when the behandelaar is the logged-in user and `canLezen` is true. The
  rechten are evaluated once per zaak: `RestZaakOverzichtConverter.convertForDisplayIfReadable` returns `null`
  when the user cannot read the zaak.
- **Taken:** for each signalering, read the taak and check `assignee == loggedInUser.id` and
  `policyService.readTaakRechten(task).canLezen`.
- Listing and deleting are separate. `SignaleringService.listCheckedZakenSignaleringenPage` and
  `listCheckedTakenSignaleringen` only read and check, and return the valid and the invalid signaleringen.
  `SignaleringRestService` then calls `SignaleringService.deleteInvalidSignaleringen`, the only part that runs
  in a transaction, so the ZRC and OPA calls of the check stay outside it.
- Invalid signaleringen are deleted with the bulk delete by id and logged at `WARNING` with the signalering id,
  type, subject and reason (`not behandelaar`/`not assignee` or `no read rights`). The username is left out
  because it is staff personal data (`docs/development/logging.md`); the signalering id identifies the row.
- Deletion only follows a positive determination. A failing ZRC, Flowable or OPA call propagates as today and
  deletes nothing.
- *Why delete when only the read rights are missing?* Keeping such rows would leave them in the count forever. A signalering is a short-lived notice, so losing it when rights
  come back is acceptable.
- *Why not only hide?* Hidden rows stay in the count and are checked again on every dashboard load.

### Filter Mijn binnenkort verlopende zaken after the ZRC query

`listZaakWarnings` keeps a zaak only when its behandelaar medewerker rol belongs to the logged-in user and the
zaak rechten give `canLezen`, using the rechten that `convertToRestZaakOverzicht` already evaluates (or evaluating
them once before conversion). No warning is logged: the card stores nothing, so a filtered zaak is not an
anomaly.

*Alternative:* add a `rol__omschrijvingGeneriek=behandelaar` filter to the ZRC query. Whether the ZRC applies
both rol filters to the same rol is not guaranteed, and the read-rights check is still needed afterwards.

### Zaakspecifieke autorisatie needs no special handling

The read-rights checks reuse `readZaakRechten` and `readTaakRechten`, which already take the zaakspecifieke
autorisatie data into account. A previous behandelaar or taakbehandelaar who keeps access through the
*Zaakspecifiek geautoriseerde medewerker* rol fails the behandelaar or assignee check, so their entries
disappear. Granting or removing that rol also produces ROL notifications; the cleanup compares with the current
behandelaar, so these leave the behandelaar's signaleringen alone.

### Let the card reload instead of refilling the page, and count afterwards

The page is read and validated once. On the in-memory sort path all signaleringen are validated before sorting
and slicing. Deleting signaleringen already sends a `SIGNALERINGEN` updated screen event for each target and
type, and the dashboard cards fetch their page again on that event. The second request finds the stale rows
gone and returns a full page, so the server does not refill the page itself.

*Alternative:* repeat the page query after deletions until a query deletes nothing. It returns a full page in
one response, at the cost of a loop and extra queries that the screen event makes unnecessary.

`SignaleringRestService.listZakenSignaleringen` keeps its `page > maxPages` check on the count before listing,
and returns a count made after listing, so the total leaves out the rows just deleted. Stale rows on later
pages are still counted until their page is requested or a notification removes them.

*Alternative:* validate all signaleringen of the type on every request. That makes every dashboard load cost one
ZRC round trip per signalering, which the database paging path exists to avoid.

## Risks / Trade-offs

- [Burst of warnings after deployment for rows that are stale today] → One-off; each row is deleted the first
  time its user opens the dashboard. Mention it in the release notes.
- [More ZRC calls: one `listRollen` per ROL DESTROY and per listed zaak signalering, rechten per listed taak] →
  The ZRC fan-out of the listing is already parallel with bounded parallelism; the ROL DESTROY handling runs once
  per notification.
- [A previous behandelaar of a zaakspecifiek geautoriseerde zaak keeps access but loses the signalering] →
  Intended: the card lists zaken assigned to the user, not zaken the user can open.
- [Two dashboard requests delete the same row at the same time] → The bulk delete by id already tolerates rows
  deleted by a concurrent request.
- [A `ZAAK_OP_NAAM` event for the previous behandelaar is still being handled when its rol is deleted] → The
  observer reads the rol, which no longer exists, so no signalering is stored. If it is stored anyway, the
  listing check removes it.
- [The count of later pages can still include stale rows] → Accepted; it corrects itself when those pages are
  listed.
- [The first response after a cleanup can contain fewer zaken than requested] → The card loads again on the
  screen event a moment later and then shows a full page.

## Migration Plan

No data migration. Deploying removes stale signaleringen gradually through the listing check. Rollback is a
plain redeploy of the previous version; deleted signaleringen are not restored, which matches what the admin
cleanup already does.
