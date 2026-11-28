## Context

This is the first flow that follows the accepted
[ADR 0008](../../../docs/solution-architecture/decisions/0008-update-the-ui-without-waiting-for-notifications-on-zac-initiated-changes.md):
ZAC updates the UI when the external system confirms a change it initiated, not when the notification arrives.

- `ZaakAssignAndReleaseRestService.assignFromList` / `releaseZakenFromList`
  (`src/main/kotlin/nl/info/zac/app/zaak/ZaakAssignAndReleaseRestService.kt:61, 144`) check
  `canZakenTakenVerdelen` and run `ZaakService.assignZaken` / `releaseZaken` in
  `dispatcher.launchAsLoggedInUser`.
- `assignZaken` (`ZaakService.kt:126`) and `releaseZaken` (`:277`) first read **all** zaken
  (`zaakUUIDs.map(zrcClientService::readZaak)`), then handle them one by one in `assignZaakFromBatch` (`:350`) and
  `releaseZaakFromBatch` (`:392`). A skipped zaak gets `ZAAK_ROLLEN.skipped(zaak)`. A successful zaak gets no
  event from ZAC. Any other exception ends the job, and the final `ZAKEN_VERDELEN` / `ZAKEN_VRIJGEVEN.updated`
  (`:156`, `:295`) is never sent (`ZaakServiceTest.kt:1081`).
- `assignZaak` (`:198`) already writes the rollen under a lock and indexes the zaak directly
  (`indexeerDirect(..., false)`), before the rol notification arrives.
- The werkvoorraad (`zaken-werkvoorraad.component.ts:333`) subscribes per zaak to `ZAAK_ROLLEN` (opcode `ANY`)
  and has no `finalSubscription`, so the final events above go unused. The verdelen dialog sends a fresh
  `crypto.randomUUID()` (`zaken-verdelen-dialog.component.ts:164`); the vrijgeven dialog sends none
  (`zaken-vrijgeven-dialog.component.ts:76`).
- `ZAAK_ROLLEN` comes from every `rol` notification (`ScreenEventType.kt:151`, `NotificationReceiver.kt:197`).
  Verdelen deletes the old behandelaar rol before it creates the new one (`ZaakService.kt:466-468`), so a
  notification can arrive between both.
- The taken werkvoorraad already works as wanted: a `TAAK` event per taak and a final event via
  `finalSubscription` (`taken-werkvoorraad.component.ts:303-347`).
- Websocket subscriptions match on the `resource` of the event only, not on its `detail`
  (`ScreenEventId.kt:11`, `SessionRegistry.kt:44`, `websocket.service.ts:227`).

## Goals / Non-Goals

**Goals**: the werkvoorraad progress and rows follow ZAC's own confirmation per zaak; a batch always ends; one
failing zaak does not stop the rest; fewer PABC and ZRC calls per batch.

**Non-goals**: see *Out of scope* in the proposal. The REST paths and payloads stay the same.

## Decisions

### 1. A per-zaak `ZAKEN_VERDELEN` / `ZAKEN_VRIJGEVEN` event with the zaak UUID as resource

ZAC sends `ZAKEN_VERDELEN.updated(zaak.uuid)` after `assignZaak` returns for a zaak, and
`ZAKEN_VERDELEN.skipped(zaak.uuid)` where it now sends `ZAAK_ROLLEN.skipped(zaak)` (likewise `ZAKEN_VRIJGEVEN`
for release). `updated(uuid: UUID)` exists already (`ScreenEventType.kt:227`); a `skipped(uuid: UUID)` factory
is added next to `skipped(eventResourceId: String)` (`:348`). The user-not-in-group path (`isUserInGroup`,
`:500`) sends `skipped(uuid)` for each UUID and no longer reads the zaken for that.

The final event keeps the `screenEventResourceId` as resource. Zaak UUIDs and the random resource ID cannot
collide.

`UPDATED` means Open Zaak confirmed the rol changes, as AC2 asks. `assignZaak` still indexes the zaak directly,
and the rol notification indexes it again, as today. `indexeerDirect` only logs an indexing error
(`ReindexSupportService.kt:468-481`); a batch-specific indexing path that fails the zaak instead is not part of
this change: a stale row needs both an indexing error and an unavailable Open Notificaties.

Why these types: rol notifications never produce them, so a notification can neither count twice nor count a
zaak too early, without the frontend having to filter. Why the zaak UUID as resource: the existing
`BatchProcessService` subscribes per ID on the resource, as the taken werkvoorraad does with `TAAK`.

Accepted: two coordinators who distribute the same zaak at the same moment both see each other's per-zaak
event for that zaak. The taken werkvoorraad has the same property.

- Rejected: ZAC sending `ZAAK_ROLLEN.updated` itself. Notifications produce the same event, so they still count
  and can count a zaak before its rollen are written.
- Rejected: resource = `screenEventResourceId`, detail = zaak UUID. Subscriptions ignore the detail, so this
  needs a new mode in `BatchProcessService`.
- Rejected: a synchronous REST call that returns the failed zaken. It loses the progress dialog and runs into
  HTTP timeouts of ingresses in front of ZAC on large selections.

### 2. Each zaak in its own try, the failed count in the final event

`assignZaken` / `releaseZaken` read and handle one zaak at a time. Every exception other than the known skip
exceptions is caught per zaak (`@Suppress("TooGenericExceptionCaught")`, as in `ScreenEventObserver.kt:37`),
logged at `SEVERE` with the zaak UUID only (server errors are `SEVERE` and carry no personal data,
`docs/development/logging.md`), and the loop goes on. The final event is sent once, in a `finally` block, so it
also goes out when something outside the loop fails. The group and user lookups that
`ZaakAssignAndReleaseRestService` now does before `assignZaken` (`ZaakAssignAndReleaseRestService.kt:67-79`)
move inside that `try`, so a failing lookup ends the batch the same way.

A failed zaak gets no per-zaak event. The failed count is the number of zaken of the request that got no
per-zaak event, so when the batch stops early (a failing lookup or group membership check), every zaak not yet
handled counts as failed. ZAC puts that number in the `detail` of the final event
(`updated(eventResourceId, detail)`, next to `ScreenEventType.kt:284`, through the existing private
`instance(opcode, type, id, detail)`). Subscriptions match on the resource only, so the `finalSubscription` still
matches; `websocket.service.ts:337` already reads a `detail` on the client.

Why the backend counts: screen events go out through separate `fireAsync` calls (`EventingService.kt:45`) on
their own threads, so the final event can overtake the last per-zaak events. A count worked out in the frontend
would then report a distributed zaak as failed.

- Rejected: a new `FAILED` opcode per zaak. The frontend would still miss failures whose event comes after the
  final event.

Known limit, as today: ZGW has no transactions. When Open Zaak fails after the old behandelaar rol is deleted,
the zaak can stay without behandelaar. It is reported as failed, so the employee can look at it.

### 3. Per-zaaktype rights in the batch

Both `assignZaken` and `releaseZaken` use this; for release only the zaakspecifiek part applies, because release
does no PABC check.

`assignZaken` keeps a map per batch from zaaktype UUID to the result of
`isAuthorisedForApplicationRoleAndZaaktype` (`ZaakService.kt:533`), so the PABC is called once per zaaktype.
Not an Infinispan cache: PABC changes must keep working at once outside a batch.

For the zaakspecifiek marking, the batch checks whether the zaaktype defines the `ZAAK_GEAUTORISEERD` eigenschap
via `ztcClientService.findEigenschap` (`ZtcClientService.kt:455`, cached per zaaktype in
`uriToEigenschappenCache`). If not, a zaak of that zaaktype cannot carry the eigenschap, and the batch passes
`isZaakspecifiekGeautoriseerd = false` to `readZaakToewijzing` (`ZaakspecifiekeAutorisatieService.kt:85`, which
already accepts it) through a new optional parameter on `assignZaak(zaak, zaakAssignment, reason)`. Otherwise
the behaviour is as today.

`assignZaak` also passes the marking it already holds (`zaakToewijzing.isZaakspecifiekGeautoriseerd`) to
`reindexZaakspecifiekeAutorisatieDependents` (`ZaakspecifiekeAutorisatieService.kt:370`), which now reads it
again. That saves a call for single-zaak assignment too, without changing its behaviour.

Indexing the zaak (`indexeerDirect` → `ZaakZoekObjectConverter.convert`, `ZaakZoekObjectConverter.kt:46-49`)
still reads the marking through `zaakAutorisatieGegevens`. That converter is shared by all indexing, so changing
it is not part of this story; the requirement is limited to the skip decision and the dependents reindex.

- Rejected: reading the marking from Solr. The index can lag (PZ-12678 names this risk).
- Rejected: `isZaakspecifiekAutoriseerbaar` as the check. It also needs the roltype, so a marked zaak whose
  zaaktype lacks that roltype would no longer be recognised and no longer skipped.

### 4. Frontend: `finalSubscription` and counting the missing events

`handleAssigmentOrReleaseWorkflow` creates one `screenEventResourceId` and passes it to both dialogs, as
`taken-werkvoorraad.component.ts:307` does. It subscribes:
- `progressSubscription`: opcode `ANY`, objectType `ZAKEN_VERDELEN` or `ZAKEN_VRIJGEVEN`, per zaak ID. On
  `UPDATED` it updates the row as today.
- `finalSubscription`: opcode `UPDATED`, the same objectType, the `screenEventResourceId`. ZAC always sends the
  final event as `UPDATED` with the failed count, also on the user-not-in-group path (count `0`, which today
  sends `ZAKEN_VERDELEN.skipped(id)` at `ZaakService.kt:140`), so the frontend never meets a missing detail.

`BatchProcessService` gets an optional `onNotification(event)` on `finalSubscription`, called with the final
event before `finally` (today the final listener calls only `finally`, `batch-process.service.ts:83-91`). The
taken werkvoorraad does not pass it and works as before. The werkvoorraad's callback reads the failed count from
the `detail` of the final event and, when it is above zero, shows a snackbar with the new text and the count.
Only that callback shows it: `BatchProcessService`
also calls `finally` on a timeout (`batch-process.service.ts:126-129`), and then only the timeout message
shows. `finally` stays as it is: the selection is cleared, the index committed and the list reloaded
with `dataSource.load()` without the 5 second delay: the Solr commit waits for the new searcher
(`ReindexSupportService.kt:89`), and ZAC has indexed every zaak before reporting it.

The verdelen path no longer marks zaken whose groep and behandelaar already match as done up front
(`zaken-werkvoorraad.component.ts:397-407`): ZAC now sends an `UPDATED` event for them. The frontend filters of
vrijgeven (zaken without behandelaar and zaakspecifiek geautoriseerde zaken are not sent) and the existing
"niet vrijgegeven omdat ... zaakspecifiek geautoriseerd" message stay.

New i18n keys (`nl.json`, `en.json`): `msg.zaken.verdelen.mislukt.enkelvoud|meervoud`,
`msg.zaken.vrijgeven.mislukt.enkelvoud|meervoud`.

### 5. Compatibility between the PRs

The backend PR keeps sending `ZAAK_ROLLEN.skipped` next to the new events, so `main` works with the old
frontend in between. The frontend PR switches to the new events and removes the `ZAAK_ROLLEN.skipped` sends.
Nothing else listens to `ZAAK_ROLLEN` `SKIPPED`: the zaakdetailpagina listens to `UPDATED` only
(`zaak-view.component.ts:311`).

No Flyway migration, Solr field, Flowable model, configuration or new ADR is needed: the change follows ADR 0008
and reuses the existing screen event and batch-progress patterns.

## Risks / Trade-offs

- A zaak that ZAC reports before its rol notification is processed: the row in the werkvoorraad already shows the
  new state, and the signalering for the new behandelaar comes a bit later. Acceptable; that is the point.
- If ZAC restarts during a batch, the final event never comes; the existing 5 minute timeout still ends the
  progress.

## PR split

1. `docs(openspec)`: this change.
2. `feat`: backend — per-zaak events, per-zaak error handling with the final event in `finally`, per-zaaktype
   rights, unit tests and itests. Keeps `ZAAK_ROLLEN.skipped`.
3. `feat(frontend)`: werkvoorraad and dialogs on the new events, the error message, no 5 second wait, specs;
   removes the `ZAAK_ROLLEN.skipped` sends from `ZaakService` and adapts the itests that listen to them.
4. `docs`: the gebruikershandleiding, and the archive of this change.
